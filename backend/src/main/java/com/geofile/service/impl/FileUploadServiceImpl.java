package com.geofile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.geofile.entity.*;
import com.geofile.service.*;
import com.geofile.util.FileValidator;
import com.geofile.util.JwtUtil;
import com.geofile.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Value("${file.upload.path:/home/xela/Projects/GeoFile/uploads}")
    private String uploadPath;

    // 存储分片上传信息（实际项目中应该存入数据库或Redis）
    private final Map<String, ChunkUploadInfo> chunkUploadMap = new ConcurrentHashMap<>();

    private static final String CHUNK_PREFIX = "chunk:";
    private static final int CHUNK_SIZE = 2 * 1024 * 1024; // 2MB

    @Autowired
    private DownloadLimitService downloadLimitService;



    @Override
    @Transactional
    public FileVO uploadFile(MultipartFile file, Integer maxDownloads, Integer validMinutes) {
        // 调用带位置参数的版本，位置为空
        return uploadFile(file, null, null, null, maxDownloads, validMinutes);
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
                             Integer maxDownloads, Integer validMinutes) {
        try {
            // 1. 验证文件
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            long size = file.getSize();

            if (!fileValidator.validate(originalFilename, contentType, size)) {
                throw new IllegalArgumentException("文件验证失败: " + originalFilename);
            }

            // 2. 保存文件
            String filePath = fileStorageService.saveFile(file);

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
            fileEntity.setFilePath(filePath);
            fileEntity.setOriginalName(originalFilename);
            fileEntity.setStorageType("LOCAL");
            fileEntity.setUploadTime(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
            fileEntity.setStatus(1);
            fileEntity.setDownloadCount(0);
            fileEntity.setExpireTime(expireTime);

            // 生成上传令牌（用于免登录身份验证）
            String uploadToken = generateUploadToken();
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
            } else {
                log.info("文件上传（无位置信息）: {}", originalFilename);
            }

            // 7. 保存到数据库
            fileService.updateById(fileEntity);

            log.info("文件上传成功: {}", originalFilename);

            // 8. 返回文件信息
            return convertToFileVO(fileEntity);

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
                .map(file -> this.uploadFile(file, null, null, null, 1, 30))
                .collect(Collectors.toList());
    }

    /**
     * 批量上传文件并记录位置
     */
    @Override
    public List<FileVO> uploadFilesWithLocation(List<MultipartFile> files, Double lat, Double lng, Integer radius, Integer maxDownloads, Integer validMinutes) {
        return files.stream()
                .filter(file -> !file.isEmpty())
                .map(file -> uploadFile(file, lat, lng, radius, maxDownloads, validMinutes))
                .collect(Collectors.toList());
    }

    @Override
    public UploadInfo initChunkUpload(String fileName, long fileSize, int chunkSize, int chunkIndex) {
        // 计算总分片数
        int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);

        // 生成上传ID
        String uploadId = UUID.randomUUID().toString();

        // 创建上传信息
        ChunkUploadInfo chunkInfo = new ChunkUploadInfo();
        chunkInfo.setUploadId(uploadId);
        chunkInfo.setFileName(fileName);
        chunkInfo.setFileSize(fileSize);
        chunkInfo.setChunkSize(chunkSize);
        chunkInfo.setTotalChunks(totalChunks);
        chunkInfo.setUploadedChunks(chunkIndex);
        chunkInfo.setCompleted(false);
        chunkInfo.setFileHash(generateFileHash()); // 简化版，实际应该计算文件的MD5

        // 存储到Map
        chunkUploadMap.put(uploadId, chunkInfo);

        // 存储到Redis
        String redisKey = CHUNK_PREFIX + uploadId;
        redisUtil.set(redisKey, chunkInfo, 30 * 60,TimeUnit.SECONDS); // 30分钟过期

        log.info("分片上传初始化成功: {}, 总分片数: {}", uploadId, totalChunks);

        // 返回上传信息
        UploadInfo info = new UploadInfo();
        info.setUploadId(uploadId);
        info.setFileName(fileName);
        info.setFileSize(fileSize);
        info.setChunkSize(chunkSize);
        info.setTotalChunks(totalChunks);
        info.setUploadedChunks(chunkIndex);
        info.setCompleted(false);

        return info;
    }

    @Override
    public UploadProgress uploadChunk(MultipartFile chunk, String fileName, int chunkIndex, int totalChunks, String fileHash) {
        // 获取上传信息
        ChunkUploadInfo chunkInfo = getChunkUploadInfo(fileName, chunkIndex);

        if (chunkInfo == null) {
            throw new IllegalArgumentException("上传信息不存在");
        }

        // 保存分片
        saveChunk(chunk, chunkInfo);

        // 更新已上传分片数
        chunkInfo.setUploadedChunks(chunkIndex + 1);

        // 更新上传进度
        int progress = (int) ((chunkInfo.getUploadedChunks() * 100.0) / chunkInfo.getTotalChunks());

        // 检查是否全部上传完成
        if (chunkInfo.getUploadedChunks() >= chunkInfo.getTotalChunks()) {
            chunkInfo.setCompleted(true);
        }

        // 更新Redis
        String redisKey = CHUNK_PREFIX + chunkInfo.getUploadId();
        redisUtil.set(redisKey, chunkInfo, 30 * 60, TimeUnit.SECONDS);

        log.info("分片上传成功: {}, 进度: {}%", chunkInfo.getUploadId(), progress);

        // 返回上传进度
        UploadProgress progressVO = new UploadProgress();
        progressVO.setUploadId(chunkInfo.getUploadId());
        progressVO.setCurrentChunk(chunkInfo.getUploadedChunks());
        progressVO.setTotalChunks(chunkInfo.getTotalChunks());
        progressVO.setProgress(progress);
        progressVO.setStatus(chunkInfo.isCompleted() ? "completed" : "uploading");
        progressVO.setFileName(chunkInfo.getFileName());
        progressVO.setFileSize(chunkInfo.getFileSize());
        progressVO.setUploadedBytes((chunkInfo.getUploadedChunks() * chunkInfo.getChunkSize()));

        return progressVO;
    }

    @Override
    public FileVO mergeChunks(String fileName, int totalChunks, String fileHash) {
        try {
            // 获取上传信息
            ChunkUploadInfo chunkInfo = getChunkUploadInfo(fileName, -1);

            if (chunkInfo == null || !chunkInfo.isCompleted()) {
                throw new IllegalArgumentException("分片上传未完成");
            }

            // 合并分片
            FileVO mergedFile = mergeChunksToFile(chunkInfo, fileHash);

            // 清理临时分片
            cleanupChunks(chunkInfo);

            log.info("分片合并成功: {}", fileName);

            return mergedFile;

        } catch (IOException e) {
            log.error("分片合并失败", e);
            throw new RuntimeException("分片合并失败: " + e.getMessage());
        }
    }

    /**
     * 保存分片
     */
    private void saveChunk(MultipartFile chunk, ChunkUploadInfo chunkInfo) {
        try {
            // 生成分片文件名
            String chunkFileName = chunkInfo.getUploadId() + "_chunk_" + chunkInfo.getUploadedChunks();

            // 保存分片
            Path chunkPath = Paths.get(uploadPath, "temp", chunkFileName);
            Files.createDirectories(chunkPath.getParent());
            chunk.transferTo(chunkPath);

            log.debug("分片保存成功: {}", chunkFileName);

        } catch (IOException e) {
            log.error("分片保存失败", e);
            throw new RuntimeException("分片保存失败: " + e.getMessage());
        }
    }

    /**
     * 获取分片上传信息
     */
    private ChunkUploadInfo getChunkUploadInfo(String fileName, int chunkIndex) {
        // 先从Map中查找
        for (ChunkUploadInfo info : chunkUploadMap.values()) {
            if (info.getFileName().equals(fileName)) {
                return info;
            }
        }

        // 再从Redis中查找
        for (ChunkUploadInfo info : chunkUploadMap.values()) {
            String redisKey = CHUNK_PREFIX + info.getUploadId();
            ChunkUploadInfo storedInfo = (ChunkUploadInfo) redisUtil.get(redisKey);
            if (storedInfo != null && storedInfo.getFileName().equals(fileName)) {
                chunkUploadMap.put(info.getUploadId(), storedInfo);
                return storedInfo;
            }
        }

        return null;
    }

    /**
     * 合并分片
     */
    private FileVO mergeChunksToFile(ChunkUploadInfo chunkInfo, String fileHash) throws IOException {
        // 生成最终文件名
        String extension = getFileExtension(chunkInfo.getFileName());
        String finalFileName = chunkInfo.getFileName() + "." + extension;

        // 创建目标路径
        Path targetPath = Paths.get(uploadPath, "temp", finalFileName);

        // 检查是否已存在同名文件（秒传逻辑）
        if (fileHash != null && checkFileExists(fileHash)) {
            log.info("文件已存在，跳过上传: {}", fileHash);
            return getFileByHash(fileHash);
        }

        // 合并分片
        Path tempDir = Paths.get(uploadPath, "temp", chunkInfo.getUploadId());
        List<Path> chunkFiles = new ArrayList<>();

        for (int i = 0; i < chunkInfo.getTotalChunks(); i++) {
            Path chunkFile = tempDir.resolve(chunkInfo.getUploadId() + "_chunk_" + i);
            if (Files.exists(chunkFile)) {
                chunkFiles.add(chunkFile);
            }
        }

        // 合并所有分片
        try (java.io.FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
            for (Path chunkFile : chunkFiles) {
                Files.copy(chunkFile, fos);
            }
        }

        // 生成文件信息
        File fileEntity = new File();
        fileEntity.setFileName(finalFileName);
        fileEntity.setFileType(extension);
        fileEntity.setFileSize(Files.size(targetPath));
        fileEntity.setFilePath(targetPath.toString());
        fileEntity.setOriginalName(chunkInfo.getFileName());
        fileEntity.setStorageType("LOCAL");
        fileEntity.setUploadTime(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        fileEntity.setStatus(1);
        fileEntity.setDownloadCount(0);
        fileEntity.setExpireTime(Date.from(LocalDateTime.now().plusMinutes(30).atZone(ZoneId.systemDefault()).toInstant())); // 默认30分钟

        // 生成上传令牌（用于免登录身份验证）
        String uploadToken = generateUploadToken();
        fileEntity.setUploadToken(uploadToken);

        // 生成下载令牌（用于下载验证）
        String downloadToken = generateDownloadToken(fileEntity.getId());
        fileEntity.setDownloadToken(downloadToken);

        // 处理下载限制（默认1次，30分钟）
        DownloadLimit downloadLimit = new DownloadLimit();
        downloadLimit.setFileId(fileEntity.getId());
        downloadLimit.setMaxDownloads(1); // 默认1次
        downloadLimit.setValidMinutes(30); // 默认30分钟
        downloadLimitService.save(downloadLimit);

        fileEntity.setDownloadLimitId(downloadLimit.getId());

        // 保存到数据库
        fileService.save(fileEntity);

        return convertToFileVO(fileEntity);
    }

    /**
     * 生成文件哈希（简化版，实际应该计算MD5）
     */
    private String generateFileHash() {
        return UUID.randomUUID().toString();
    }

    @Override
    public File downloadFile(Long fileId, String downloadToken) {
        try {
            // 1. 查询文件
            File file = fileService.getById(fileId);
            if (file == null) {
                log.warn("文件不存在: fileId={}", fileId);
                throw new IllegalArgumentException("文件不存在");
            }

            // 2. 验证下载令牌
            if (file.getDownloadToken() == null || !file.getDownloadToken().equals(downloadToken)) {
                log.warn("下载令牌验证失败: fileId={}, token={}", fileId, downloadToken);
                throw new IllegalArgumentException("无效的下载令牌");
            }

            // 3. 检查文件状态
            if (file.getStatus() != null && file.getStatus() == 0 && file.getDeleted() == 1) {
                throw new IllegalArgumentException("文件已被删除");
            }

            // 4. 检查是否过期
            if (file.getExpireTime() != null && file.getExpireTime().before(new Date())) {
                throw new IllegalArgumentException("文件已过期");
            }

            // 5. 检查下载次数限制
//            if (file.getDownloadLimitId() != null) {
//                DownloadLimit downloadLimit = downloadLimitService.getById(file.getDownloadLimitId());
//                if (downloadLimit != null) {
//                    Integer maxDownloads = downloadLimit.getMaxDownloads();
//                    if (maxDownloads != null && maxDownloads > 0) {
//                        // 获取当前下载次数
//                        Integer currentCount = file.getDownloadCount();
//                        if (currentCount == null) {
//                            currentCount = 0;
//                        }
//
//                        // 检查是否超过限制
//                        if (currentCount >= maxDownloads) {
//                            log.warn("下载次数超限: fileId={}, 当前次数={}, 最大次数={}", fileId, currentCount, maxDownloads);
//                            throw new IllegalArgumentException("下载次数已达上限，无法继续下载");
//                        }
//
//                        log.info("下载次数检查通过: fileId={}, 当前次数={}, 最大次数={}", fileId, currentCount, maxDownloads);
//                    }
//                }
//            }
            DownloadLimit downloadLimit = downloadLimitService.getOne(
                    new LambdaQueryWrapper<DownloadLimit>().eq(DownloadLimit::getFileId, fileId)
            );

            if (downloadLimit != null) {
                Integer maxDownloads = downloadLimit.getMaxDownloads();
                if (maxDownloads != null && maxDownloads > 0) {
                    // 获取当前下载次数 (如果为 null 则视为 0)
                    int currentCount = (file.getDownloadCount() == null) ? 0 : file.getDownloadCount();

                    // 检查是否超过限制
                    if (currentCount >= maxDownloads) {
                        log.warn("下载次数超限: fileId={}, 当前次数={}, 最大次数={}", fileId, currentCount, maxDownloads);
                        throw new IllegalArgumentException("该文件下载次数已达上限 (" + maxDownloads + "次)");
                    }
                    log.info("下载次数检查通过: fileId={}, 当前次数={}, 最大次数={}", fileId, currentCount, maxDownloads);
                }
            }

            // 6. 更新下载次数
            if (file.getDownloadCount() == null) {
                file.setDownloadCount(0);
            }
            file.setDownloadCount(file.getDownloadCount() + 1);
            fileService.updateById(file);

            log.info("文件下载成功: fileId={}, fileName={}, 下载次数={}", fileId, file.getFileName(), file.getDownloadCount());

            return file;

        } catch (IllegalArgumentException e) {
            log.error("文件下载失败: fileId={}, error={}", fileId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("文件下载失败: fileId={}", fileId, e);
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public String generateDownloadToken(Long fileId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        return token;
    }

    /**
     * 生成上传令牌
     */
    private String generateUploadToken() {
        return UUID.randomUUID().toString().replace("-", "") +
               Long.toHexString(System.currentTimeMillis());
    }

    /**
     * 检查文件是否存在
     */
    private boolean checkFileExists(String fileHash) {
        // TODO: 实现文件哈希查询逻辑
        return false;
    }

    /**
     * 根据哈希获取文件
     */
    private FileVO getFileByHash(String fileHash) {
        // TODO: 实现文件查询逻辑
        return null;
    }

    /**
     * 清理分片文件
     */
    private void cleanupChunks(ChunkUploadInfo chunkInfo) {
        try {
            Path tempDir = Paths.get(uploadPath, "temp", chunkInfo.getUploadId());
            Files.deleteIfExists(tempDir);
            log.debug("分片文件清理成功: {}", tempDir);
        } catch (IOException e) {
            log.error("分片文件清理失败", e);
        }
    }

    /**
     * 根据地理位置搜索附近文件
     */
    public List<FileVO> searchNearbyFiles(Double lat, Double lng, Integer radius, Long excludeFileId) {
        try {
            // 由于FileLocationService需要注入FileMapper，这里直接调用方法
            // 实际项目中应该通过Spring注入
            return fileLocationService.searchNearbyFiles(lat, lng, radius, excludeFileId,
                    1, 100, "upload_time", "DESC", null, null);
        } catch (Exception e) {
            log.error("搜索附近文件失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 更新文件地理位置信息
     */
    public void updateFileLocation(Long fileId, Double lat, Double lng, Integer radius) {
        try {
            fileLocationService.updateFileLocation(fileId, lat, lng, radius);
        } catch (Exception e) {
            log.error("更新文件地理位置失败", e);
        }
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
    private FileVO convertToFileVO(File file) {
        FileVO vo = new FileVO();
        vo.setId(file.getId());
        vo.setFileName(file.getFileName());
        vo.setFileType(file.getFileType());
        vo.setFileSize(file.getFileSize());
        vo.setFilePath(file.getFilePath());
        vo.setOriginalName(file.getOriginalName());
        vo.setStorageType(file.getStorageType());
        vo.setUploadTime(file.getUploadTime().toString());
        vo.setExpireTime(file.getExpireTime() != null ? file.getExpireTime().toString() : "");
        vo.setDownloadCount(file.getDownloadCount());
        vo.setStatus(file.getStatus());
        vo.setStatusText(file.getStatus() == 1 ? "正常" : "已删除");
        vo.setUploadToken(file.getUploadToken());
        vo.setDownloadToken(file.getDownloadToken());

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

    /**
     * 分片上传信息内部类
     */
    private static class ChunkUploadInfo {
        private String uploadId;
        private String fileName;
        private long fileSize;
        private int chunkSize;
        private int totalChunks;
        private int uploadedChunks;
        private boolean completed;
        private String fileHash;

        // getters and setters
        public String getUploadId() { return uploadId; }
        public void setUploadId(String uploadId) { this.uploadId = uploadId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public int getChunkSize() { return chunkSize; }
        public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
        public int getTotalChunks() { return totalChunks; }
        public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
        public int getUploadedChunks() { return uploadedChunks; }
        public void setUploadedChunks(int uploadedChunks) { this.uploadedChunks = uploadedChunks; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public String getFileHash() { return fileHash; }
        public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    }
}
