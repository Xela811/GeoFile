package com.geofile.service.impl;

import com.geofile.entity.File;
import com.geofile.entity.FileVO;
import com.geofile.entity.UploadInfo;
import com.geofile.entity.UploadProgress;
import com.geofile.service.FileService;
import com.geofile.service.FileStorageService;
import com.geofile.service.FileUploadService;
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
import java.time.format.DateTimeFormatter;
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

    @Override
    @Transactional
    public FileVO uploadFile(MultipartFile file) {
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

            // 3. 生成文件信息
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
            fileEntity.setExpireTime(Date.from(LocalDateTime.now().plusDays(30).atZone(ZoneId.systemDefault()).toInstant())); // 默认30天有效期

            // 4. 保存到数据库
            fileService.save(fileEntity);

            log.info("文件上传成功: {}", originalFilename);

            // 5. 返回文件信息
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
                .map(this::uploadFile)
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
        fileEntity.setExpireTime(Date.from(LocalDateTime.now().plusDays(30).atZone(ZoneId.systemDefault()).toInstant()));

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
        vo.setExpireTime(file.getExpireTime().toString());
        vo.setDownloadCount(file.getDownloadCount());
        vo.setStatus(file.getStatus());
        vo.setStatusText(file.getStatus() == 1 ? "正常" : "已删除");
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
