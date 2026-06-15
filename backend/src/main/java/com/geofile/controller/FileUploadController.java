package com.geofile.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.geofile.common.Result;
import com.geofile.dto.QuickCheckDTO;
import com.geofile.dto.SecUploadDTO;
import com.geofile.entity.ArchiveNode;
import com.geofile.entity.FileHash;
import com.geofile.entity.FileVO;
import com.geofile.exception.DownloadException;
import com.geofile.service.FileHashService;
import com.geofile.service.FileLogService;
import com.geofile.service.FileService;
import com.geofile.service.FileUploadService;
import com.geofile.util.IpUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件上传Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@CrossOrigin
@Tag(name = "文件上传", description = "文件上传相关接口")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileHashService fileHashService;

    @Autowired
    private FileLogService fileLogService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${file.upload.path}")
    private String uploadPath;

    // 用于存放临时分片的临时根目录
    @Value("${file.upload.path}/tmp/geofile")
    private String uploadMergePath;

    @PostMapping("/upload/chunk")
    @Operation(summary = "接收分片")
    public Result<String> uploadChunk(
            @RequestParam("chunk") MultipartFile chunk,
            @RequestParam("identifier") String identifier, // 文件的 SHA-256 作为唯一标识
            @RequestParam("chunkNumber") Integer chunkNumber) {
        try {
            // 创建临时文件夹存放该文件的分片：/tmp/geofile/chunks/fileSha256/
            java.io.File chunkDir = new java.io.File(uploadMergePath + "/chunks/" + identifier);
            if (!chunkDir.exists()) chunkDir.mkdirs();

            // 将分片保存为独立文件：如 0, 1, 2...
            java.io.File chunkFile = new java.io.File(chunkDir, String.valueOf(chunkNumber));
            //chunk.transferTo(chunkFile);
            try (InputStream is = chunk.getInputStream();
                 BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(chunkFile))) {
                byte[] buffer = new byte[64 * 1024]; // 64KB 高速读写缓冲区
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
                bos.flush();
            }
            return Result.success("分片 " + chunkNumber + " 上传成功");
        } catch (Exception e) {
            return Result.error("分片上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload/merge")
    @Operation(summary = "合并分片并触发原有业务落库")
    public Result<List<FileVO>> mergeChunks(
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        String identifier = (String) params.get("identifier");
        String sampleHash = (String) params.get("sampleHash");
        String fileName = (String) params.get("fileName");
        Double lat = params.get("lat") != null ? Double.valueOf(params.get("lat").toString()) : null;
        Double lng = params.get("lng") != null ? Double.valueOf(params.get("lng").toString()) : null;
        Integer maxDownloads = (Integer) params.get("maxDownloads");
        Integer validMinutes = (Integer) params.get("validMinutes");
        Boolean needCode = (Boolean) params.get("needCode");
        String providedToken = (String) params.get("providedToken");

        try {
            java.io.File chunkDir = new java.io.File(uploadMergePath + "/chunks/" + identifier);
            java.io.File[] chunks = chunkDir.listFiles();
            if (chunks == null || chunks.length == 0) return Result.error("找不到分片资源");

            // 严格按照分片顺序（0, 1, 2...）排序
            Arrays.sort(chunks, Comparator.comparingInt(f -> Integer.parseInt(f.getName())));

            // 创建最终合体文件
            //java.io.File mergedFile = new java.io.File(uploadMergePath + "/chunks/" + fileName);

            String extension = getFileExtension(fileName);
            // 调用你 uploadFile 里面的真实路径生成算法（生成分级混淆目录，如 /202603/xx/sha256.mp4）
            String finalRelativePath = generateComplexPath(identifier, extension);
            java.nio.file.Path finalAbsolutePath = java.nio.file.Paths.get(uploadPath, finalRelativePath);

            // 确保最终业务目录存在
            java.nio.file.Files.createDirectories(finalAbsolutePath.getParent());
            java.io.File mergedFile = finalAbsolutePath.toFile();

            try (FileOutputStream fos = new FileOutputStream(mergedFile);
                 FileChannel destChannel = fos.getChannel()) {
                for (java.io.File chunk : chunks) {
                    try (FileInputStream fis = new FileInputStream(chunk);
                         FileChannel srcChannel = fis.getChannel()) {
                        srcChannel.transferTo(0, srcChannel.size(), destChannel);
                    }
                    chunk.delete(); // 合并完随手物理删除分片，省空间
                }
            }
            chunkDir.delete(); // 删掉临时文件夹

            /*// 把合并出来的标准 File 转成 Spring 的 MultipartFile
            FileInputStream fileInputStream = new FileInputStream(mergedFile);
            MultipartFile mockMultipartFile = new org.springframework.mock.web.MockMultipartFile(
                    "files", fileName, java.nio.file.Files.probeContentType(mergedFile.toPath()), fileInputStream
            );

            // 注入 Service 原方法！
            List<FileVO> results = fileUploadService.uploadFilesWithLocation(
                    List.of(mockMultipartFile), lat, lng, 1000, maxDownloads, validMinutes, needCode,
                    providedToken, List.of(sampleHash != null ? sampleHash : ""), List.of(identifier), request
            );

            mergedFile.delete(); // 业务落库后，删除这个合并的临时文件（因为Service内部已经复制进分级目录了）
            return Result.success(results);*/
            long realFileSize = mergedFile.length();
            log.info("分片磁盘合并成功，文件真实大小: {} 字节", realFileSize);
            // 因为物理文件已经在最终路径躺好了
            // 伪造一个大小为 0 的 Mock 只是为了迎合原 Service 的参数要求，欺骗它走完落库校验即可
            MultipartFile mockMultipartFile = new org.springframework.mock.web.MockMultipartFile(
                    "files", fileName, java.nio.file.Files.probeContentType(finalAbsolutePath), new byte[]{0}
            ){
                @Override
                public long getSize() {
                    return realFileSize; // 当底层 fileValidator.validate 过来查岗时，交出真实大小
                }
            };

            // 注入 Service 之前，因为文件已经落盘成功，要防止底层 uploadFile 重复写盘
            //  uploadFile 内部执行了 fileStorageService.saveFileCustomPath(saveStream, fullPath);
            // 如果传一个 byte[0] 的流进去，它会写一个空文件覆盖我们刚合并好的大文件
            // boolean isExisting = fileHashService.incrementReference(sha256);
            // 我们可以直接在调用 Service 之前，抢先把数据库记录生成出来（status=1, count=1），这样进入 service 就会直接命中“秒传分支”，绝不会执行写盘覆盖，并且完美执行全部业务落库！

            boolean isAlreadyInDb = fileHashService.incrementReference(identifier);
            if (!isAlreadyInDb) {
                FileHash newHash = new FileHash();
                newHash.setFileHash(identifier);
                newHash.setSampleHash(sampleHash);
                newHash.setFileSize(realFileSize); // 必须记录真实大小
                newHash.setStoragePath(finalRelativePath);
                newHash.setStorageType("LOCAL");
                newHash.setReferenceCount(1);
                newHash.setStatus(1);
                newHash.setMimeType(java.nio.file.Files.probeContentType(finalAbsolutePath));
                newHash.setExtension(extension);
                newHash.setCreatedTime(LocalDateTime.now());
                fileHashService.save(newHash);
            }

            // 完美调用原 Service，无感闭环所有 RedisGEO 写入、提取码生成和持久化逻辑
            List<FileVO> results = fileUploadService.uploadFilesWithLocation(
                    List.of(mockMultipartFile), lat, lng, 1000, maxDownloads, validMinutes, needCode,
                    providedToken, List.of(sampleHash != null ? sampleHash : ""), List.of(identifier), request
            );

            return Result.success(results);
        } catch (Exception e) {
            log.error("分片合并并调用业务落库失败", e);
            return Result.error("合并失败: " + e.getMessage());
        }
    }
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    private String generateComplexPath(String sha256, String extension) {
        String dir1 = sha256.substring(0, 2);
        String dir2 = sha256.substring(2, 4);
        String fileName = sha256 + (extension != null && !extension.isEmpty() ? "." + extension : "");
        return String.join("/", dir1, dir2, fileName);
    }
    /**
     * 上传单个文件
     */
    @PostMapping("/upload")
    @Operation(summary = "上传单个文件", description = "支持大文件上传，包含进度显示")
    public Result<FileVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "0") Integer maxDownloads,
            @RequestParam(required = false, defaultValue = "0") Integer validMinutes,
            @RequestParam(required = false, defaultValue = "true") Boolean needCode) {
        try {
            log.info("文件上传开始: {}, 下载限制: {}次, 有效时长: {}分钟",
                    file.getOriginalFilename(), maxDownloads, validMinutes);

            FileVO result = fileUploadService.uploadFile(file, maxDownloads, validMinutes, needCode);

            return Result.success(result);

        } catch (Exception e) {
            log.error("文件上传失败: {}", file.getOriginalFilename(), e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传单个文件并记录地理位置
     *
     * @param file 文件
     * @param lat 纬度
     * @param lng 经度
     * @param radius 搜索半径（米）
     * @param maxDownloads 最大下载次数
     * @param validMinutes 有效时长（分钟）
     * @return 文件信息
     */
    @PostMapping("/upload-with-location")
    @Operation(summary = "上传单个文件并记录位置", description = "上传文件时同时记录地理位置信息")
    public Result<FileVO> uploadFileWithLocation(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false, defaultValue = "0") Integer maxDownloads,
            @RequestParam(required = false, defaultValue = "0") Integer validMinutes,
            @RequestParam(required = false, defaultValue = "true") Boolean needCode
    ) {
        try {
            log.info("文件上传并记录位置: {}, lat={}, lng={}, radius={}, 下载限制: {}次, 有效时长: {}分钟",
                    file.getOriginalFilename(), lat, lng, radius, maxDownloads, validMinutes);

            FileVO result = fileUploadService.uploadFile(file, lat, lng, radius, maxDownloads, validMinutes, needCode, null, null,null);

            return Result.success(result);

        } catch (Exception e) {
            log.error("文件上传失败: {}", file.getOriginalFilename(), e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传多个文件
     */
    @PostMapping("/upload/batch")
    @Operation(summary = "批量上传文件", description = "一次上传多个文件")
    public Result<List<FileVO>> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        try {
            log.info("批量文件上传开始: {} 个文件", files.length);

            List<FileVO> results = fileUploadService.uploadFiles(List.of(files));

            return Result.success(results);

        } catch (Exception e) {
            log.error("批量文件上传失败", e);
            return Result.error("批量文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传文件并记录位置
     *
     * @param files 文件列表
     * @param lat 纬度
     * @param lng 经度
     * @param radius 搜索半径（米）
     * @return 文件信息列表
     */
    @PostMapping("/upload/batch-with-location")
    @Operation(summary = "批量上传文件并记录位置", description = "批量上传文件时同时记录地理位置信息")
    public Result<List<FileVO>> uploadFilesWithLocation(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false, defaultValue = "1") Integer maxDownloads,
            @RequestParam(required = false, defaultValue = "0") Integer validMinutes,
            @RequestParam(required = false, defaultValue = "true") Boolean needCode,
            @RequestParam(required = false) String providedToken,
            @RequestParam(required = false) List<String> sampleHashes,
            @RequestParam(required = false) List<String> fullHashes,
            HttpServletRequest request) {
        String clientIp = IpUtils.getClientIp(request);
        try {
            log.info("批量文件上传并记录位置: {} 个文件, lat={}, lng={}, radius={}, 下载限制: {}次, 有效时长: {}分钟",
                    files.length, lat, lng, radius, maxDownloads, validMinutes);

            List<FileVO> results = fileUploadService.uploadFilesWithLocation(List.of(files), lat, lng, radius, maxDownloads, validMinutes, needCode, providedToken, sampleHashes, fullHashes, request);
            if (!CollectionUtils.isEmpty(results)) {
                for (FileVO vo : results) {
                    fileLogService.recordLog(vo.getId(), "BATCH_UPLOAD_WITH_LOCATION", 1, "批量带位置上传成功", lat, lng, clientIp);
                }
            }
            return Result.success(results);

        } catch (Exception e) {
            log.error("批量文件上传失败", e);
            fileLogService.recordLog(null, "BATCH_UPLOAD_WITH_LOCATION", 0, "批量位置上传失败: " + e.getMessage(), lat, lng, clientIp);
            return Result.error("批量文件上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/sec-upload")
    @Operation(summary = "文件秒传", description = "根据指纹校验实现秒传，支持加入现有批次")
    public Result<FileVO> secUpload(@RequestBody SecUploadDTO dto, HttpServletRequest request) {
        String clientIp = IpUtils.getClientIp(request);

        try {
            log.info("接收到秒传请求: fileName={}, hash={}", dto.getFileName(), dto.getHash());
            FileVO result = fileUploadService.secUpload(dto, request);

            if (result != null) {
                fileLogService.recordLog(result.getId(), "SEC_UPLOAD", 1, "秒传命中成功", dto.getLat(), dto.getLng(), clientIp);
                return Result.success(result);
            } else {
                // 404 状态码告诉前端：指纹不存在，请走普通上传逻辑
                return Result.error(404, "未命中秒传，请执行完整上传");
            }
        } catch (Exception e) {
            fileLogService.recordLog(null, "SEC_UPLOAD", 0, e.getMessage(), dto.getLat(), dto.getLng(), clientIp);
            log.error("秒传处理失败", e);
            return Result.error("秒传失败: " + e.getMessage());
        }
    }

    @PostMapping("/quick-check")
    public Result<Map<String, Object>> quickCheck(@RequestBody QuickCheckDTO dto) {
        // 核心逻辑：查询是否存在大小和采样哈希同时匹配的记录
        boolean exists = fileHashService.count(new LambdaQueryWrapper<FileHash>()
                .eq(FileHash::getFileSize, dto.getSize())
                .eq(FileHash::getSampleHash, dto.getSampleHash())
                .last("LIMIT 1") // 只要找到一个就立刻停止，提升性能
        ) > 0;

        Map<String, Object> response = new HashMap<>();
        response.put("canPotentiallySecUpload", exists);

        return Result.success(response);
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{fileId}")
    @Operation(summary = "下载文件", description = "通过下载令牌验证后下载文件")
    public ResponseEntity<?> downloadFile(
            @PathVariable Long fileId,
            @RequestParam String token,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            HttpServletRequest request,
            HttpServletResponse response) {
        String clientIp = IpUtils.getClientIp(request);

        try {
            log.info("请求下载文件: fileId={}, token={}, lat={}, lng={}", fileId, token, lat, lng);

            // 允许前端跨域提取 Content-Disposition 响应头获取真实文件名
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            boolean shouldCount = (rangeHeader == null || rangeHeader.startsWith("bytes=0-"));

            // 1. 验证并获取文件
            com.geofile.entity.File file = fileUploadService.downloadFile(fileId, token, lat, lng, shouldCount);

            // 2. 构建文件路径
            java.nio.file.Path fullPath = java.nio.file.Paths.get(uploadPath)
                    .resolve(file.getFilePath())
                    .normalize();

            if (!java.nio.file.Files.exists(fullPath)) {
                log.error("物理文件丢失，数据库记录路径: {}, 拼接后的绝对路径: {}", file.getFilePath(), fullPath);
                fileLogService.recordLog(fileId, "DOWNLOAD", 0, "物理文件在服务器上丢失", lat, lng, clientIp);
                return ResponseEntity.notFound().build();
            }

            // 3. 读取文件内容
            FileSystemResource resource = new FileSystemResource(fullPath.toFile());

            // 4. 设置响应头
            String encodedFileName = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
//            // 根据文件后缀动态探测 Content-Type
//            String contentType = java.nio.file.Files.probeContentType(fullPath);
//            headers.setContentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM);
//            //headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//            headers.setContentLength(file.getFileSize());
//            headers.setContentDispositionFormData("attachment", encodedFileName);

            // ====================  核心修改：多端兼容强制下载流 ====================
            // 彻底废弃 probeContentType。只要调用下载接口，无论什么文件格式，一律强行返回二进制流！
            // 这将直接切断 Safari、夸克等浏览器试图在网页内预览、渲染图片的念头，逼迫其直接拉起系统下载管理器
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(file.getFileSize());

            // 采用最严苛的标准 Header 组装，双引号锁死文件名，兼容国内各路魔改套壳浏览器
            //headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");

            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=utf-8''" + encodedFileName);
            // 严防死守：增加禁用浏览器缓存和代理中间件缓存的响应头，防止夸克等浏览器在后台偷偷进行二次预加载请求
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");
            // =========================================================================

            // 增加 SHA-256 响应头，方便前端校验文件完整性
            if (file.getFileHash() != null) {
                headers.add("X-File-Hash-Sha256", file.getFileHash());
            }

            log.info("文件下载成功: {}, 大小: {} bytes", file.getFileName(), file.getFileSize());

            fileLogService.recordLog(fileId, "DOWNLOAD", 1, null, lat, lng, clientIp);

            // 5. 返回文件内容
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (DownloadException e) {
            log.warn("拦截下载请求: {}", e.getMessage());

            fileLogService.recordLog(fileId, "DOWNLOAD", 0, e.getMessage(), lat, lng, clientIp);

            //  判断是否是前端 fetch 发起的异步请求
            String requestedWith = request.getHeader("X-Requested-With");
            if ("XMLHttpRequest".equals(requestedWith)) {
                // 如果是前端中转页调用的，体面地返回 400 状态码和标准错误 JSON
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Result.error(e.getMessage()));
            }

            return redirectToErrorPage(e.getMessage());
        } catch (Exception e) {
            log.error("下载接口崩溃", e);

            fileLogService.recordLog(fileId, "DOWNLOAD", 0, "系统故障: " + e.getMessage(), lat, lng, clientIp);

            String requestedWith = request.getHeader("X-Requested-With");
            if ("XMLHttpRequest".equals(requestedWith)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Result.error("服务器繁忙，请稍后再试"));
            }

            return redirectToErrorPage("服务器繁忙，请稍后再试");
        }
    }

    @GetMapping("/detail/{fileId}")
    @Operation(summary = "获取文件最新详情")
    public Result<FileVO> getFileDetail(
            @PathVariable Long fileId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {

        log.info("============== 🔍 详情接口地理校验断点开始 ==============");
        log.info("1. 前端传来的请求参数 -> fileId: {}, lat: {}, lng: {}", fileId, lat, lng);

        com.geofile.entity.File file = fileService.getById(fileId);
        if (file == null) {
            log.warn("拦截：数据库未找到对应文件记录");
            return Result.error("文件不存在");
        }

        FileVO fileVO = fileUploadService.convertToFileVO(file);

        // 开始空间校验
        if (lat != null && lng != null && file.getDownloadToken() != null && file.getIsPrivate() == 0) {
            try {
                String GEO_KEY = "file:locations:public";
                String memberKey = file.getUploadToken();

                log.info("2. 准备去 Redis 查询 -> Key: '{}', Member(uploadToken): '{}'", GEO_KEY, memberKey);

                // 检查这个 member 到底在不在 Redis 里
                java.util.List<org.springframework.data.geo.Point> posList =
                        redisTemplate.opsForGeo().position(GEO_KEY, memberKey);

                if (posList == null || posList.isEmpty() || posList.get(0) == null) {
                    log.error("🚨 警告：Redis 的 GeoKey '{}' 中【根本没有】这个成员 '{}'！", GEO_KEY, memberKey);
                    fileVO.setDistanceExceeded(true); // 查不到强制判定为超距
                } else {
                    org.springframework.data.geo.Point redisPoint = posList.get(0);
                    log.info("3. Redis 内部存储的真实坐标 -> lng: {}, lat: {}", redisPoint.getX(), redisPoint.getY());

                    // =================== 🛠️ 修复核心：改用内存计算，避开 Redis 报错坑 ===================
                    // 算出两点之间的物理距离（单位：米）
                    double meters = calculateDistanceInMeters(lat, lng, redisPoint.getY(), redisPoint.getX());
                    log.info("4. 🚀 内存基于 GPS 算出来的物理距离: {} 米", meters);

                    if (meters > 1000.0) {
                        log.warn("❌ 判定结果：距离为 {} 米，已超出 1000 米限制！成功标记为 true！", meters);
                        fileVO.setDistanceExceeded(true); // 标记超距
                    } else {
                        log.info("✅ 判定结果：距离在 1km 以内，准许下载。");
                    }
                    // ==============================================================================
                }
            } catch (Exception e) {
                log.error("🚨 空间计算爆发异常", e);
                fileVO.setDistanceExceeded(true); // 发生异常时稳妥起见，设为超距拦截
            }
        } else {
            log.info("跳过校验：参数不全（lat/lng 为 null 或免校验）");
        }

        log.info("============== 🔍 详情接口地理校验断点结束 ==============");
        return Result.success(fileVO);
    }

    /**
     * 使用半正矢公式(Haversine)计算两组经纬度之间的地面物理距离(米)
     */
    private double calculateDistanceInMeters(double lat1, double lng1, double lat2, double lng2) {
        double EARTH_RADIUS = 6371000; // 地球半径，单位：米
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double a = radLat1 - radLat2;
        double b = Math.toRadians(lng1) - Math.toRadians(lng2);

        double s = 2 * Math.asin(Math.sqrt(
                Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)
        ));
        return s * EARTH_RADIUS;
    }

    private ResponseEntity<?> redirectToErrorPage(String message) {
        try {
            // 对中文消息进行编码，防止 URL 乱码
            String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
            String targetUrl = "/error?msg=" + encodedMsg;

            log.info("重定向至错误页: {}", targetUrl);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(targetUrl))
                    .build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 生成下载令牌（用于前端获取下载链接）
     */
    @GetMapping("/generate-download-token/{fileId}")
    @Operation(summary = "生成下载令牌", description = "为文件生成下载令牌")
    public Result<String> generateDownloadToken(@PathVariable Long fileId) {
        try {
            log.info("生成下载令牌: fileId={}", fileId);

            String token = fileUploadService.generateDownloadToken(fileId);

            return Result.success(token);

        } catch (Exception e) {
            log.error("生成下载令牌失败: fileId={}", fileId, e);
            return Result.error("生成下载令牌失败: " + e.getMessage());
        }
    }
    /**
     * 通过取件码提取文件列表（支持批量）
     */
    @GetMapping("/extract/{code}")
    public Result<List<FileVO>> extractFilesByCode(@PathVariable String code) {
        if (code == null || code.trim().isEmpty()) {
            return Result.error("请输入取件码");
        }

        try {
            // 修改为返回 List
            List<FileVO> fileVOs = fileUploadService.verifyAndGetFiles(code);
            return Result.success(fileVOs);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 通过上传令牌获取文件列表（用于公开批次管理刷新/对账）
     */
    @GetMapping("/list-by-token")
    @Operation(summary = "通过Token获取文件列表", description = "用于前端公开批次管理的自动刷新和状态校验")
    public Result<List<FileVO>> getFilesByToken(@RequestParam String uploadToken) {
        if (uploadToken == null || uploadToken.trim().isEmpty()) {
            return Result.error("uploadToken不能为空");
        }

        try {
            // 调用 Service 层新方法
            List<FileVO> fileVOs = fileUploadService.getFilesByUploadToken(uploadToken);

            // 修改点：如果 list 为空，说明 Token 无效，显式告知前端
            if (fileVOs == null || fileVOs.isEmpty()) {
                return Result.error("分享链接已失效或不包含有效文件");
            }

            return Result.success(fileVOs);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/reconcile")
    public Result<List<Long>> reconcileFiles(@RequestBody List<Long> fileIds) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return Result.success(new ArrayList<>());
        }

        // 只查询数据库中 status 为 1 (正常) 或 3 (满额残影)
        // 且未被逻辑删除的文件 ID
        List<com.geofile.entity.File> validFiles = fileService.list(new LambdaQueryWrapper<com.geofile.entity.File>()
                .select(com.geofile.entity.File::getId)
                .in(com.geofile.entity.File::getId, fileIds)
                .in(com.geofile.entity.File::getStatus, 1, 3)
                .eq(com.geofile.entity.File::getDeleted, 0));

        List<Long> validIds = validFiles.stream()
                .map(com.geofile.entity.File::getId)
                .collect(Collectors.toList());

        return Result.success(validIds);
    }

    private static final Set<String> TEXT_EXTENSIONS = Stream.of(
            "txt", "md", "log", "csv", "json", "xml", "yaml", "yml",
            "ini", "env", "ts", "js", "vue", "html", "htm", "css",
            "scss", "less", "java", "py", "go", "rs", "c", "cpp",
            "h", "sql", "properties", "conf"
    ).collect(Collectors.toSet());

    @GetMapping("/preview/{fileId}")
    public ResponseEntity<Resource> previewFile(@PathVariable Long fileId, @RequestParam String token, @RequestHeader(value = "Range", required = false) String rangeHeader,HttpServletRequest request) {
        String clientIp = IpUtils.getClientIp(request);
        try {
            // 只有首次完整请求才计数，Range续传请求跳过计数
            boolean shouldCount = (rangeHeader == null || rangeHeader.startsWith("bytes=0-"));

            // 1. 业务逻辑校验（包含计数+1）
            com.geofile.entity.File fileEntity = fileService.processAccess(fileId, token, shouldCount);

            java.nio.file.Path fullPath = java.nio.file.Paths.get(uploadPath, fileEntity.getFilePath()).normalize();
            java.io.File diskFile = fullPath.toFile();
            if (!diskFile.exists()) {
                fileLogService.recordLog(fileId, "PREVIEW", 0, "物理文件丢失无法预览", null, null, clientIp);
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(diskFile.toURI());

            String originalFileName = fileEntity.getFileName();
            String extension = "";

            // 提取后缀（不含点）
            int lastDotIndex = originalFileName.lastIndexOf(".");
            if (lastDotIndex > 0) {
                extension = originalFileName.substring(lastDotIndex + 1).toLowerCase();
            }

            // 核心 MIME 识别逻辑
            //String contentType = Files.probeContentType(diskFile.toPath());
            String contentType = java.nio.file.Files.probeContentType(fullPath);

            // 满足你的要求：针对列表中的所有文本格式进行兜底
            if (TEXT_EXTENSIONS.contains(extension)) {
                // 关键安全点：无论 Files.probe 结果如何，强制设为 text/plain 避免 XSS 攻击
                // 并通过 charset=utf-8 解决预览乱码问题
                contentType = "text/plain; charset=utf-8";
            } else if ("pdf".equals(extension)) {
                contentType = "application/pdf";
            } else if (contentType == null) {
                contentType = "application/octet-stream";
            }

            fileLogService.recordLog(fileId, "PREVIEW", 1, null, null, null, clientIp);

            // 3. 构建响应头
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    // 关键优化：inline 模式
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" +
                            URLEncoder.encode(originalFileName, "UTF-8") + "\"")
                    // 关键修复：移除或配置 X-Frame-Options，允许 iframe 嵌入
                    // 注意：如果项目中开启了 Spring Security，还需要在配置类中设置 .frameOptions().disable()
                    .header("X-Frame-Options", "ALLOWALL")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
            // 原来直接 return ResponseEntity.ok()... 改为用 ResourceRegion 支持Range
            /*HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.parseMediaType(contentType));
            responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" +
                    URLEncoder.encode(originalFileName, "UTF-8") + "\"");
            responseHeaders.add("X-Frame-Options", "ALLOWALL");
            responseHeaders.add(HttpHeaders.ACCEPT_RANGES, "bytes");

// 处理Range请求
            if (rangeHeader != null) {
                long fileLength = diskFile.length();
                // 解析Range头 bytes=start-end
                String rangeValue = rangeHeader.substring(6); // 去掉"bytes="
                String[] parts = rangeValue.split("-");
                long start = Long.parseLong(parts[0]);
                long end = parts.length > 1 && !parts[1].isEmpty()
                        ? Long.parseLong(parts[1])
                        : fileLength - 1;

                long contentLength = end - start + 1;
                responseHeaders.add(HttpHeaders.CONTENT_RANGE,
                        "bytes " + start + "-" + end + "/" + fileLength);
                responseHeaders.setContentLength(contentLength);

                ResourceRegion region = new ResourceRegion(resource, start, contentLength);
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .headers(responseHeaders)
                        .body(region); // 注意类型，需要单独处理
            }

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(resource);*/

        } catch (IllegalArgumentException e) {
            fileLogService.recordLog(fileId, "PREVIEW", 0, "鉴权拒绝: " + e.getMessage(), null, null, clientIp);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("预览失败", e);
            fileLogService.recordLog(fileId, "PREVIEW", 0, "内部错误: " + e.getMessage(), null, null, clientIp);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/archive/list/{fileId}")
    public ResponseEntity<List<ArchiveNode>> listArchiveContent(@PathVariable Long fileId, @RequestParam String token) {
        try {
            // 1. 查询并校验
            com.geofile.entity.File file = fileService.getById(fileId);
            if (file == null) throw new IllegalArgumentException("文件不存在");

            // 2. 验证下载令牌
            if (file.getDownloadToken() == null || !file.getDownloadToken().equals(token)) {
                throw new IllegalArgumentException("无效的下载令牌");
            }

            // 3. 检查文件状态 (status=2过期, status=3耗尽)
            if (file.getDeleted() == 1) throw new IllegalArgumentException("文件已被删除");
            if (file.getStatus() != null) {
                if (file.getStatus() == 2 || (file.getExpireTime() != null && file.getExpireTime().before(new Date()))) {
                    throw new IllegalArgumentException("文件已过期");
                }
                if (file.getStatus() == 3) throw new IllegalArgumentException("文件下载次数已达上限");
            }


            java.nio.file.Path fullPath = java.nio.file.Paths.get(uploadPath, file.getFilePath()).normalize();
            java.io.File diskFile = fullPath.toFile();
            if (!diskFile.exists()) {
                return ResponseEntity.notFound().build();
            }
            // 4. 初始化虚拟根节点
            ArchiveNode root = new ArchiveNode("root", true, 0);

            try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(diskFile)) {
                java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    java.util.zip.ZipEntry entry = entries.nextElement();
                    // 5. 将扁平路径插入树中
                    insertToTree(root, entry);
                }
            }
            // 返回根节点的子集（即压缩包第一层）
            return ResponseEntity.ok(root.getChildren());
        } catch (Exception e) {
            log.error("解析压缩包失败", e);
            return ResponseEntity.status(500).build();
        }
    }

    private void insertToTree(ArchiveNode root, java.util.zip.ZipEntry entry) {
        String[] parts = entry.getName().split("/");
        ArchiveNode current = root;

        for (int i = 0; i < parts.length; i++) {
            String partName = parts[i];
            if (partName.isEmpty()) continue;

            boolean isLastPart = (i == parts.length - 1);
            // 判断当前 part 是不是目录：
            // 如果不是最后一个 part，那它一定是目录；如果是最后一个 part，看 ZipEntry 标记
            boolean isDir = !isLastPart || entry.isDirectory();

            // 在当前节点的子节点中查找
            ArchiveNode next = null;
            for (ArchiveNode child : current.getChildren()) {
                if (child.getName().equals(partName)) {
                    next = child;
                    break;
                }
            }

            // 如果不存在则创建
            if (next == null) {
                next = new ArchiveNode(partName, isDir, isLastPart ? entry.getSize() : 0);
                current.getChildren().add(next);
            }
            current = next;
        }
    }
}
