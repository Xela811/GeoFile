package com.geofile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.geofile.dto.SecUploadDTO;
import com.geofile.entity.*;
import com.geofile.mapper.FileMapper;
import com.geofile.service.*;
import com.geofile.util.*;
import com.geofile.exception.DownloadException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.geofile.util.IpUtils.getClientIp;

/**
 * 文件上传服务实现
 */
@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private FileLocationService fileLocationService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileValidator fileValidator;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private IpUtils ipUtils;

    @Autowired
    private FileBatchService fileBatchService;

    @Autowired
    private FileHashService fileHashService;

    @Value("${file.upload.path:/home/xela/Projects/GeoFile/uploads}")
    private String uploadPath;

    @Autowired
    private DownloadLimitService downloadLimitService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public FileVO uploadFile(MultipartFile file, Integer maxDownloads, Integer validMinutes, Boolean needCode) {
        // 调用带位置参数的版本，位置为空
        return uploadFile(file, null, null, null, maxDownloads, validMinutes, needCode, null, null,null);
    }

    /**
     * 上传文件并记录地理位置
     *
     * @param file 文件
     * @param lat 纬度
     * @param lng 经度
     * @param radius 搜索半径（米）
     * @param maxDownloads 最大下载次数
     * @param validMinutes 有效时长（分钟）
     * @return 文件信息
     */
    @Transactional
    @Override
    public FileVO uploadFile(MultipartFile file, Double lat, Double lng, Integer radius,
                             Integer maxDownloads, Integer validMinutes, Boolean needCode, String providedToken, String sampleHash, String fullHash) {
        try {
            // 1. 验证文件
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            long size = file.getSize();

            if (!fileValidator.validate(originalFilename, contentType, size)) {
                throw new IllegalArgumentException("文件验证失败: " + originalFilename);
            }

            // 计算 MD5 和 SHA-256 (使用你编写的 HashUtils)
            //String md5;
            String sha256;
            // --- 核心改动：如果有前端传来的 Hash，直接使用 ---
            if (fullHash != null && !fullHash.isEmpty()) {
                sha256 = fullHash;
                // 如果前端只传了 SHA256 没传 MD5，后端可以只算一个 MD5，
                // 或者干脆让前端把 MD5 也传过来。这里假设只传了 SHA256：
                //md5 = HashUtils.getMd5(file.getInputStream());
                log.info("使用前端提供的全量 Hash: {}", sha256);
            } else {
                // 前端没传（兜底逻辑），后端自己全量算
                log.warn("前端未提供 Hash，后端执行全量计算: {}", originalFilename);
                sha256 = HashUtils.getSha256(file.getInputStream());
                //md5 = HashUtils.getMd5(file.getInputStream());
            }

            // 3. 物理查重与存盘逻辑
            // A. 第一步：直接尝试原子增加引用计数（这不仅是增加计数，也是在尝试“激活”可能存在的 status=0 的记录）
            boolean isExisting = fileHashService.incrementReference(sha256);

            String finalRelativePath;
            if (isExisting) {
                // --- 情况 1：数据库中已存在该哈希记录（无论原状态是 0 还是 1） ---
                // 此时 incrementReference 已经完成了 count+1, status=1, update_time=now 的操作
                // 我们只需要把 path 查出来，供后续 file 业务表使用
                FileHash existingRecord = fileHashService.findByHash(sha256);
                if (existingRecord == null) {
                    // 防御性编程：如果 increment 成功但查不到（极罕见），说明可能刚被物理删除了
                    throw new RuntimeException("文件指纹激活冲突，请重试");
                }
                finalRelativePath = existingRecord.getStoragePath();
                log.info("触发秒传逻辑并激活指纹记录: hash={}", sha256);
            } else {
                // --- 情况 2：数据库中彻底没有该哈希记录 ---
                String extension = getFileExtension(originalFilename);
                finalRelativePath = generateComplexPath(sha256, extension);
                String fullPath = java.nio.file.Paths.get(uploadPath, finalRelativePath).toString();

                // 执行物理写盘
                //fileStorageService.saveFileCustomPath(fileBytes, fullPath);
                try (InputStream saveStream = file.getInputStream()) {
                    fileStorageService.saveFileCustomPath(saveStream, fullPath);
                }

                // 封装新对象执行 INSERT
                FileHash newHash = new FileHash();
                newHash.setFileHash(sha256);
                //newHash.setMd5(md5);
                newHash.setSampleHash(sampleHash);
                newHash.setFileSize(size);
                newHash.setStoragePath(finalRelativePath);
                newHash.setStorageType("LOCAL");
                newHash.setReferenceCount(1);
                newHash.setStatus(1);
                newHash.setMimeType(contentType);
                newHash.setExtension(extension);
                newHash.setCreatedTime(LocalDateTime.now());

                try {
                    fileHashService.save(newHash);
                    log.info("新物理文件存盘完成: hash={}", sha256);
                } catch (org.springframework.dao.DuplicateKeyException e) {
                    // 极致并发兜底：如果两个用户同时上传新文件，此处的 INSERT 可能会由于唯一索引报错
                    // 此时我们直接转为执行一次更新即可
                    fileHashService.incrementReference(sha256);
                    FileHash conflictRecord = fileHashService.findByHash(sha256);
                    finalRelativePath = conflictRecord.getStoragePath();
                    log.info("并发上传冲突，已自动切换为秒传逻辑: hash={}", sha256);
                }
            }

            // 2. 保存文件
            //String filePath = fileStorageService.saveFile(file);

// 3. 一键调用业务落库方法
            return this.executeBusinessRecordSave(originalFilename, sha256, file.getSize(), finalRelativePath,
                    lat, lng, radius, maxDownloads, validMinutes,
                    needCode, providedToken);
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<FileVO> uploadFiles(List<MultipartFile> files) {
        return files.stream()
                .filter(file -> !file.isEmpty())
                .map(file -> this.uploadFile(file, null, null, null, 1, 30, true, null, null,null))
                .collect(Collectors.toList());
    }

    /**
     * 辅助方法：生成哈希分级路径
     */
    private String generateComplexPath(String sha256, String extension) {
        String dir1 = sha256.substring(0, 2);
        String dir2 = sha256.substring(2, 4);
        String fileName = sha256 + (extension != null && !extension.isEmpty() ? "." + extension : "");
        return String.join("/", dir1, dir2, fileName);
    }

    /**
     * 提取出的公用方法：仅负责业务表的落库（t_file 和 t_download_limit）
     */
    private FileVO executeBusinessRecordSave(String originalFilename, String sha256, long size, String finalRelativePath,
                                             Double lat, Double lng, Integer radius,
                                             Integer maxDownloads, Integer validMinutes,
                                             Boolean needCode, String providedToken) {

        // 3. 计算过期时间（默认30分钟，如果设置了有效时长则使用设置值）
        Date expireTime;
        if (validMinutes != null && validMinutes > 0) {
            expireTime = Date.from(LocalDateTime.now().plusMinutes(validMinutes).atZone(ZoneId.systemDefault()).toInstant());
            log.info("使用自定义有效时长: {} 分钟", validMinutes);
        } else {
            expireTime = Date.from(LocalDateTime.now().plusMinutes(30).atZone(ZoneId.systemDefault()).toInstant());
            log.info("使用默认有效时长: 30 分钟");
        }

        // 4. 生成文件信息
        File fileEntity = new File();
        fileEntity.setFileName(originalFilename);
        fileEntity.setFileType(getFileExtension(originalFilename));
        fileEntity.setFileSize(size);
        fileEntity.setFileHash(sha256);
        fileEntity.setFilePath(finalRelativePath);
        fileEntity.setOriginalName(originalFilename);
        fileEntity.setStorageType("LOCAL");
        fileEntity.setUploadTime(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        fileEntity.setStatus(1);
        fileEntity.setDownloadCount(0);
        fileEntity.setExpireTime(expireTime);
        // 根据前端传来的 needCode 设置数据库字段
        fileEntity.setIsPrivate(Boolean.TRUE.equals(needCode) ? 1 : 0);

        // 生成上传令牌（用于免登录身份验证）
        //String uploadToken = generateUploadToken();
        String uploadToken = (providedToken != null) ? providedToken : generateUploadToken();
        fileEntity.setUploadToken(uploadToken);

        // 生成下载令牌（用于下载验证）
        String downloadToken = generateDownloadToken(fileEntity.getId());
        fileEntity.setDownloadToken(downloadToken);

        fileService.save(fileEntity);

        // 5. 处理下载限制
        if (maxDownloads != null && maxDownloads > 0) {
            // 创建下载限制记录
            DownloadLimit downloadLimit = new DownloadLimit();
            downloadLimit.setFileId(fileEntity.getId());
            downloadLimit.setMaxDownloads(maxDownloads);
            downloadLimit.setValidMinutes(validMinutes != null ? validMinutes : 30); // 默认30分钟

            downloadLimitService.save(downloadLimit);

            // 设置关联ID
            fileEntity.setDownloadLimitId(downloadLimit.getId());
            log.info("已创建下载限制: 文件ID={}, 最大下载次数={}, 有效时长={}分钟",
                    fileEntity.getId(), maxDownloads, downloadLimit.getValidMinutes());
        } else {
            log.info("文件上传成功（不限制下载次数）: {}", originalFilename);
        }

        // 6. 设置地理位置信息
        if (lat != null && lng != null) {
            fileEntity.setLocationLat(lat);
            fileEntity.setLocationLng(lng);
            fileEntity.setLocationRadius(radius != null ? radius : 1000);
            log.info("文件上传并记录位置: {}, lat={}, lng={}, radius={}", originalFilename, lat, lng, radius);
            // ======= 新增 RedisGEO 写入逻辑 =======
            // ======= 核心优化：只有公开文件才入库 RedisGEO =======
            if (Boolean.FALSE.equals(needCode)) { // needCode 为 false 代表公开
                try {
                    String geoKey = "file:locations:public"; // 明确标识为公开
                    redisTemplate.opsForGeo().add(geoKey, new Point(lng, lat), uploadToken);
                    log.info("公开文件位置已同步至 Redis: token={}", uploadToken);
                } catch (Exception e) {
                    log.error("RedisGEO 写入失败", e);
                }
            } else {
                log.info("私有文件，跳过 RedisGEO 记录，仅保存数据库坐标");
            }
        } else {
            log.info("文件上传（无位置信息）: {}", originalFilename);
        }

        // 7. 保存到数据库
        fileService.updateById(fileEntity);


        // 8. 统一使用验证码服务生成并保存
        String downloadCode = null;

        if (Boolean.TRUE.equals(needCode) && providedToken == null) {
            // 调用验证码服务生成
            downloadCode = verificationCodeService.generateDownloadCode();

            // 确定过期时间（与文件过期时间一致，单位：分钟）
            long expireMinutes = (validMinutes != null && validMinutes > 0) ? validMinutes : 30;

            // 将验证码与上传令牌(uploadToken)绑定并存入 Redis
            String redisKey = "file:download:" + uploadToken;
            redisUtil.set(redisKey, downloadCode, expireMinutes, TimeUnit.MINUTES);
            redisUtil.set("code:to:token:" + downloadCode, uploadToken, expireMinutes, TimeUnit.MINUTES);

            log.info("已为私有文件生成下载验证码: fileId={}, code={}", fileEntity.getId(), downloadCode);
        } else {
            log.info("公开文件上传，跳过验证码生成步骤: fileId={}", fileEntity.getId());
        }


        // 9. 返回文件信息（包含验证码）
        FileVO result = convertToFileVO(fileEntity);

        // 如果生成了验证码，则设置到返回对象中；否则 result.downloadCode 默认为 null
        if (downloadCode != null) {
            result.setDownloadCode(downloadCode);
        }

        //result.setDownloadCode(downloadCode);
        return result;
    }

    /**
     * 批量上传文件并记录位置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<FileVO> uploadFilesWithLocation(List<MultipartFile> files, Double lat, Double lng, Integer radius, Integer maxDownloads, Integer validMinutes, Boolean needCode, String providedToken, List<String> sampleHashes, List<String> fullHashes, HttpServletRequest request) {
        List<FileVO> results = new ArrayList<>();

        // 1. 为这一批次生成一个共用的 uploadToken (用于后续上传者管理)
        //如果 providedToken 不为空，说明前面的秒传已经生成了 Token，直接沿用
        //String batchUploadToken = generateUploadToken();
        String batchUploadToken = (providedToken != null && !providedToken.isEmpty())
                ? providedToken
                : generateUploadToken();

        // 2. 【核心修改】为这一批次生成一个共用的取件码 (如果需要的话)
        //String downloadCode = null;
        // 如果是沿用 Token，尝试从数据库或 Redis 获取已有的提取码
        // 检查是否是已有批次
        FileBatch existingBatch = fileBatchService.getOne(new LambdaQueryWrapper<FileBatch>()
                .eq(FileBatch::getBatchToken, batchUploadToken));

        String downloadCode = null;
        if (existingBatch != null) {
            downloadCode = existingBatch.getExtractCode();
        } else if (Boolean.TRUE.equals(needCode)) {
            downloadCode = verificationCodeService.generateDownloadCode();
        }

        long totalSize = 0;
        int fileCount = files.size();

        for (int i = 0;i < files.size(); i++) {
            MultipartFile file = files.get(i);
            totalSize += file.getSize(); // 累计总大小

            // 获取当前文件对应的全量 Hash
            String currentFullHash = (fullHashes != null && fullHashes.size() > i)
                    ? fullHashes.get(i)
                    : null;

            // 核心修改：根据索引 i 获取对应的采样哈希
            String currentSampleHash = (sampleHashes != null && sampleHashes.size() > i)
                    ? sampleHashes.get(i)
                    : null;

            // 3. 修改保存逻辑：传入 sharedCode 而不是在 save 内部生成
            // 注意：你需要重构你底层的保存方法，使其接受外部传入的 code
            FileVO fileVO = this.uploadFile(file, lat, lng, radius,
                    maxDownloads, validMinutes,
                    needCode,batchUploadToken,currentSampleHash,currentFullHash);

            // 手动给返回对象塞入共用的取件码，让前端能显示
            if (downloadCode != null) {
                fileVO.setDownloadCode(downloadCode);
            }
            results.add(fileVO);
        }

        // --- 新增：持久化批次信息 ---
        // 3. 【核心修正】持久化或更新批次信息
        if (existingBatch != null) {
            // 如果批次已存在（秒传时创建的），则累加数据
            fileBatchService.update(new LambdaUpdateWrapper<FileBatch>()
                    .eq(FileBatch::getBatchToken, batchUploadToken)
                    .setSql("file_count = file_count + " + files.size())
                    .setSql("total_size = total_size + " + totalSize));
        } else {
            FileBatch batch = new FileBatch();
            batch.setBatchToken(batchUploadToken);
            batch.setExtractCode(downloadCode);
            batch.setClientIp(getClientIp(request)); // 使用工具类获取真实 IP
            batch.setFileCount(fileCount);
            batch.setTotalSize(totalSize);
            batch.setIsPrivate(Boolean.TRUE.equals(needCode) ? 1 : 0);

            int minutes = (validMinutes != null && validMinutes > 0) ? validMinutes : 30;
            batch.setExpireTime(LocalDateTime.now().plusMinutes(minutes));
            batch.setCreatedTime(LocalDateTime.now());

            fileBatchService.save(batch); // 执行落库
            // -------------------------
        }

        // 4. 将取件码与这批文件的关系存入 Redis (如果你之前的逻辑是存 Redis)
        // 建议存：code -> batchUploadToken，这样通过一个码就能找到一组文件
        if (Boolean.TRUE.equals(needCode) && downloadCode != null) {

            long expireMinutes = (validMinutes != null && validMinutes > 0) ? validMinutes : 30;
            // 绑定：取件码 -> 批量Token
            String redisKey = "file:download:" + batchUploadToken;
            redisUtil.set(redisKey, downloadCode, expireMinutes, TimeUnit.MINUTES);
            redisUtil.set("code:to:token:" + downloadCode, batchUploadToken, expireMinutes, TimeUnit.MINUTES);

            log.info("批量上传：已将取件码 {} 绑定至批次 Token {}", downloadCode, batchUploadToken);
        }

        return results;
    }

    @Override
    public File downloadFile(Long fileId, String downloadToken, Double lat, Double lng) {
        try {
            // 1. 查询文件
            File file = fileService.getById(fileId);
            if (file == null) {
                log.warn("文件不存在: fileId={}", fileId);
                throw new DownloadException("文件不存在或已被物理删除");
            }

            // 2. 验证下载令牌
            if (file.getDownloadToken() == null || !file.getDownloadToken().equals(downloadToken)) {
                log.warn("下载令牌验证失败: fileId={}, token={}", fileId, downloadToken);
                throw new DownloadException("无效的下载令牌");
            }

            // ================= 【新增逻辑】直链地理围栏强校验 =================
            // 如果文件本身有限制（比如 location_lat 不为 null），说明需要进行围栏校验
            if (file.getIsPrivate() != null && file.getIsPrivate() == 0 && file.getLocationLat() != null && file.getLocationLng() != null) {
                // 如果前端没有传来经纬度，直接拒绝
                if (lat == null || lng == null) {
                    log.warn("直链下载拦截：该文件开启了地理限制，但未提供位置参数。fileId={}", fileId);
                    throw new DownloadException("此链接受地理范围保护，请允许浏览器获取定位后下载");
                }

                // 使用哈弗辛公式计算用户当前位置与文件分享位置的距离
                double meters = calculateDistanceInMeters(lat, lng, file.getLocationLat(), file.getLocationLng());
                log.info("直链下载校验 -> 用户位置({}, {}), 文件限制圈中心({}, {}), 计算距离: {} 米",
                        lat, lng, file.getLocationLat(), file.getLocationLng(), meters);

                // 校验是否超出 1000 米限制（也可以动态读取数据库的 file.getLocationRadius()）
                double maxRadius = file.getLocationRadius() != null ? file.getLocationRadius() : 1000.0;
                if (meters > maxRadius) {
                    log.warn("直链下载拦截：用户距离 {} 米，超出 {} 米限制！", meters, maxRadius);
                    throw new DownloadException("超出分享范围，您无法下载此文件");
                }
                log.info("直链下载校验通过：距离在范围内。");
            }
            // =============================================================

            // 【新增逻辑】校验物理文件状态 (兼容 SHA256)
            // 确保物理表中的记录未被逻辑删除（status=1表示正常）
            if (file.getFileHash() != null) {
                FileHash hashRecord = fileHashService.findByHash(file.getFileHash());
                if (hashRecord == null || hashRecord.getStatus() == 0) {
                    log.error("文件指纹校验失败: fileId={}, hash={}", fileId, file.getFileHash());
                    throw new DownloadException("物理资源已失效");
                }
            }

            // 3. 检查文件状态
            // 如果 status 已经是 2(过期) 或 3(耗尽)，直接拦截
            if (file.getStatus() != null) {
                if (file.getStatus() == 0 || file.getDeleted() == 1) throw new DownloadException("文件已被分享者删除");
                if (file.getStatus() == 2 || file.getExpireTime() != null && file.getExpireTime().before(new Date())) throw new DownloadException("该文件的分享链接已过期");
                if (file.getStatus() == 3) throw new DownloadException("文件下载次数已达上限");
            }

            // 4. 获取下载限制配置
            DownloadLimit downloadLimit = downloadLimitService.getOne(
                    new LambdaQueryWrapper<DownloadLimit>().eq(DownloadLimit::getFileId, fileId)
            );

            // 5. 核心逻辑：执行下载计数
            int currentCount = (file.getDownloadCount() == null) ? 0 : file.getDownloadCount();
            int maxDownloads = (downloadLimit != null) ? downloadLimit.getMaxDownloads() : 0;

            // 如果当前状态已经是 3，说明在残影期，拦截下载
            if (file.getStatus() != null && file.getStatus() == 3) {
                throw new DownloadException("该文件下载次数已达上限");
            }

            // 二次检查（防御式编程）：防止并发情况下漏掉的状态
            if (maxDownloads > 0 && currentCount >= maxDownloads) {
                throw new DownloadException("该文件的下载次数已达上限");
            }

            // 正常计数更新
            int nextCount = currentCount + 1;
            file.setDownloadCount(nextCount);

            // 如果这一次刚好下满
            if (maxDownloads > 0 && nextCount >= maxDownloads) {
                // 关键点：设置为 3，表示“已满额但仍需展示”
                file.setStatus(3);
                log.info("文件 {} 已达到最大下载次数", fileId);
            }

            // 6. 保存到数据库
            fileService.updateById(file);


            log.info("文件下载成功: fileId={}, fileName={}, 下载次数={}", fileId, file.getFileName(), file.getDownloadCount());

            return file;

        } catch (DownloadException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件下载系统内部故障: fileId={}", fileId, e);
            throw new RuntimeException("服务器处理下载请求失败: " + e.getMessage());
        }
    }

    private double calculateDistanceInMeters(double lat1, double lng1, double lat2, double lng2) {
        double EARTH_RADIUS = 6371000;
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double a = radLat1 - radLat2;
        double b = Math.toRadians(lng1) - Math.toRadians(lng2);
        double s = 2 * Math.asin(Math.sqrt(
                Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)
        ));
        return s * EARTH_RADIUS;
    }

    @Override
    public String generateDownloadToken(Long fileId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        return token;
    }

    @Override
    public List<FileVO> verifyAndGetFiles(String code) {
        // 1. 通过 code 反查 uploadToken (这一步没问题)
        String uploadToken = (String) redisUtil.get("code:to:token:" + code);
        // 2. 如果 Redis 没了，尝试从数据库回源
        if (uploadToken == null) {
            FileBatch batch = fileBatchService.getOne(new LambdaQueryWrapper<FileBatch>()
                    .eq(FileBatch::getExtractCode, code));

            // 状态 A：数据库也搜不到 -> 提取码不存在
            if (batch == null) {
                throw new IllegalArgumentException("该提取码不存在，请检查输入是否正确");
            }

            // 状态 B：数据库搜到了，但时间已过 -> 提取码已过期
            if (batch.getExpireTime().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("该提取码已于 " + batch.getExpireTime() + " 过期失效");
            }

            // 状态 C：合法且未过期，补回 Redis 缓存
            uploadToken = batch.getBatchToken();
            long seconds = Duration.between(LocalDateTime.now(), batch.getExpireTime()).getSeconds();
            if (seconds > 0) {
                redisUtil.set("code:to:token:" + code, uploadToken, seconds, TimeUnit.SECONDS);
                redisUtil.set("file:download:" + uploadToken, code, seconds, TimeUnit.SECONDS);
            }
            log.info("提取码 {} 回源成功", code);
        }

        // 2. 根据 uploadToken 查数据库所有【正常状态】的文件
        // 增加 .in(File::getStatus, 1, 3) 确保达到上限的文件（残影）也能被上传者看到或根据业务需求过滤
        List<File> fileEntities = fileService.list(new LambdaQueryWrapper<File>()
                .eq(File::getUploadToken, uploadToken)
                .in(File::getStatus, Arrays.asList(1, 3))); // 仅查找未删除/未彻底过期的

        if (fileEntities == null || fileEntities.isEmpty()) {
            throw new IllegalArgumentException("该提取码内的文件已被全部删除");
        }

        // 3. 批量转化为 VO 返回
        return fileEntities.stream()
                .map(this::convertToFileVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileVO> getFilesByUploadToken(String uploadToken) {
        // 1. 直接查询数据库中该 Token 下 status 为 1 或 3 的文件
        List<File> fileEntities = fileService.list(new LambdaQueryWrapper<File>()
                .eq(File::getUploadToken, uploadToken)
                .in(File::getStatus, Arrays.asList(1, 3))
                .eq(File::getDeleted, 0));

        // 2. 如果文件全部被删除了，也要返回空列表，让前端知道该批次已失效
        if (fileEntities == null || fileEntities.isEmpty()) {
            return null;
        }

        // 3. 转化为 VO
        return fileEntities.stream()
                .map(this::convertToFileVO) // 复用你现有的转化方法
                .collect(Collectors.toList());
    }

    /**
     * 生成上传令牌
     */
    private String generateUploadToken() {
        return UUID.randomUUID().toString().replace("-", "") +
               Long.toHexString(System.currentTimeMillis());
    }

    @Transactional
    @Override
    public FileVO secUpload(SecUploadDTO dto, HttpServletRequest request) {
        System.out.println("前端传来的Hash: " + dto.getHash());
        // 1. 物理查重
        boolean isExisting = fileHashService.incrementReference(dto.getHash());
        if (!isExisting) {
            return null; // 没这文件，无法秒传
        }
        FileHash hashRecord = fileHashService.findByHash(dto.getHash());

        // 2. 准备批次 Token
        String batchUploadToken = (dto.getUploadToken() != null && !dto.getUploadToken().isEmpty())
                ? dto.getUploadToken()
                : generateUploadToken();

        // ======= 新增逻辑：处理秒传场景下的私有取件码 =======
        String downloadCode = null;
        if (Boolean.TRUE.equals(dto.getNeedCode())) {
            // 先查数据库里这个批次是否已经有码了
            FileBatch existingBatch = fileBatchService.getOne(new LambdaQueryWrapper<FileBatch>()
                    .eq(FileBatch::getBatchToken, batchUploadToken));

            if (existingBatch != null && existingBatch.getExtractCode() != null) {
                // 情况 A：批次已存在且已有码，直接沿用
                downloadCode = existingBatch.getExtractCode();
            } else {
                // 情况 B：该批次第一个进来的文件，需要生成新码并绑定 Redis
                downloadCode = verificationCodeService.generateDownloadCode();

                long expireMinutes = (dto.getValidMinutes() != null && dto.getValidMinutes() > 0)
                        ? dto.getValidMinutes() : 30;

                // 执行 Redis 绑定 (核心缺失逻辑补偿)
                redisUtil.set("file:download:" + batchUploadToken, downloadCode, expireMinutes, TimeUnit.MINUTES);
                redisUtil.set("code:to:token:" + downloadCode, batchUploadToken, expireMinutes, TimeUnit.MINUTES);

                log.info("秒传批次首个文件：生成取件码 {} 并绑定至 Token {}", downloadCode, batchUploadToken);
            }
        }
        // =================================================

        // 3. 调用业务落库 (File, Limit, Redis)
        FileVO vo = this.executeBusinessRecordSave(dto.getFileName(), dto.getHash(),
                hashRecord.getFileSize(), hashRecord.getStoragePath(),
                dto.getLat(), dto.getLng(), dto.getRadius(),
                dto.getMaxDownloads(), dto.getValidMinutes(),
                dto.getNeedCode(), batchUploadToken);

        // 4. 批次表关联 (FileBatch)
        // 注意：如果 Token 是新生成的，需要新建 Batch 记录；
        // 如果 Token 是传进来的，说明 uploadFilesWithLocation 后面会统一处理 Batch，这里甚至可以跳过。
        // 为了保险，我们通过检查数据库是否存在该 Batch 来决定是否保存
//        boolean batchExists = fileBatchService.exists(new LambdaQueryWrapper<FileBatch>()
//                .eq(FileBatch::getBatchToken, batchUploadToken));
// 尝试直接更新（累加）已存在的批次
        boolean updated = fileBatchService.update(new LambdaUpdateWrapper<FileBatch>()
                .eq(FileBatch::getBatchToken, batchUploadToken)
                .setSql("file_count = file_count + 1")
                .setSql("total_size = total_size + " + hashRecord.getFileSize()));
// 如果更新失败，说明批次还没创建，则执行创建
        if (!updated) {
            try {
                FileBatch batch = new FileBatch();
                batch.setBatchToken(batchUploadToken);
                batch.setExtractCode(downloadCode); // 只有第一个请求生成的 Code 会存入表
                batch.setClientIp(getClientIp(request));
                batch.setFileCount(1);
                batch.setTotalSize(hashRecord.getFileSize());
                batch.setIsPrivate(Boolean.TRUE.equals(dto.getNeedCode()) ? 1 : 0);
                batch.setExpireTime(LocalDateTime.now().plusMinutes(dto.getValidMinutes() > 0 ? dto.getValidMinutes() : 30));
                batch.setCreatedTime(LocalDateTime.now());
                fileBatchService.save(batch);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 如果并发创建冲突，说明刚才那一瞬间别人创建好了，再执行一次更新即可
                fileBatchService.update(new LambdaUpdateWrapper<FileBatch>()
                        .eq(FileBatch::getBatchToken, batchUploadToken)
                        .setSql("file_count = file_count + 1")
                        .setSql("total_size = total_size + " + hashRecord.getFileSize()));
            }
        }



        // 【同步 Code】确保返回给前端的 Code 是批次表中最终那个，避免前端弹窗不一致
        //FileBatch finalBatch = fileBatchService.getOne(new LambdaQueryWrapper<FileBatch>().eq(FileBatch::getBatchToken, batchUploadToken));
        vo.setDownloadCode(downloadCode);
        return vo;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 转换为FileVO
     */
    @Override
    public FileVO convertToFileVO(File file) {
        FileVO vo = new FileVO();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        vo.setId(file.getId());
        vo.setFileName(file.getFileName());
        vo.setFileType(file.getFileType());
        vo.setFileSize(file.getFileSize());
        vo.setFilePath(file.getFilePath());
        vo.setOriginalName(file.getOriginalName());
        vo.setStorageType(file.getStorageType());
//        vo.setUploadTime(file.getUploadTime().toString());
//        vo.setExpireTime(file.getExpireTime() != null ? file.getExpireTime().toString() : "");
        vo.setUploadTime(file.getUploadTime() != null ? sdf.format(file.getUploadTime()) : null);
        vo.setExpireTime(file.getExpireTime() != null ? sdf.format(file.getExpireTime()) : null);
//        vo.setDownloadCount(file.getDownloadCount());
        // 确保下载次数被赋值（如果 BeanUtils 没拷过去的话）
        vo.setDownloadCount(file.getDownloadCount() != null ? file.getDownloadCount() : 0);
        vo.setStatus(file.getStatus());
        vo.setStatusText(file.getStatus() == 1 ? "正常" : "已删除");
        vo.setUploadToken(file.getUploadToken());
        vo.setDownloadToken(file.getDownloadToken());
        vo.setIsPrivate(file.getIsPrivate());
        if (file.getLocationLat() != null) {
            vo.setLocationLat(file.getLocationLat());
        }
        if (file.getLocationLng() != null) {
            vo.setLocationLng(file.getLocationLng());
        }

        // 直接根据 fileId 去查询关联的下载限制表
        DownloadLimit downloadLimit = downloadLimitService.getOne(
                new LambdaQueryWrapper<DownloadLimit>().eq(DownloadLimit::getFileId, file.getId())
        );

        if (downloadLimit != null) {
            // 将数据库中的最大下载次数设置到 VO 中，供前端显示
            vo.setMaxDownloads(downloadLimit.getMaxDownloads());
            // 如果需要显示剩余时间，也可以在这里计算并设置有效时长
        } else {
            vo.setMaxDownloads(0); // 明确表示不限制
        }

        return vo;
    }
}
