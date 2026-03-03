package com.geofile.service.impl;

import com.geofile.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件存储服务实现（本地存储）
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    @Value("${file.upload.path:/home/xela/Projects/GeoFile/uploads}")
    private String uploadPath;

    @Override
    public String saveFile(MultipartFile file) throws IOException {
        return saveFile(file, "");
    }

    @Override
    public String saveFile(MultipartFile file, String targetPath) throws IOException {
        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IOException("文件名不能为空");
        }

        // 生成新的文件名
        String extension = getFileExtension(originalFilename);
        String newFileName = generateFileName(extension);

        // 构建存储路径
        Path storagePath = buildStoragePath(targetPath, newFileName);

        // 确保目录存在
        Files.createDirectories(storagePath.getParent());

        // 保存文件
        Files.copy(file.getInputStream(), storagePath, StandardCopyOption.REPLACE_EXISTING);

        logger.info("文件保存成功: {} -> {}", originalFilename, storagePath);

        // 返回相对路径
        return storagePath.toString();
    }

    @Override
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("文件删除成功: {}", filePath);
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error("文件删除失败: {}", filePath, e);
            return false;
        }
    }

    @Override
    public long getFileSize(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.size(path);
    }

    @Override
    public boolean exists(String filePath) {
        return Files.exists(Paths.get(filePath));
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
     * 生成文件名
     */
    private String generateFileName(String extension) {
        // 生成UUID作为文件名
        String uuid = UUID.randomUUID().toString().replace("-", "");
        // 添加时间戳
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        // 组合文件名
        return timestamp + "_" + uuid.substring(0, 8) + "." + extension;
    }

    /**
     * 构建存储路径
     */
    private Path buildStoragePath(String targetPath, String fileName) {
        Path basePath = Paths.get(uploadPath, targetPath);
        return basePath.resolve(fileName);
    }
}
