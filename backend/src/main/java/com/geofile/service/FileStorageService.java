package com.geofile.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件存储服务
 * 负责文件的本地存储和删除
 */
public interface FileStorageService {

    String saveFileCustomPath(InputStream is, String relativePath) throws IOException;

    /**
     * 保存文件
     * @param file 上传的文件
     * @return 文件存储路径
     * @throws IOException 文件操作异常
     */
    String saveFile(MultipartFile file) throws IOException;

    /**
     * 保存文件到指定路径
     * @param file 上传的文件
     * @param targetPath 目标路径
     * @return 文件存储路径
     * @throws IOException 文件操作异常
     */
    String saveFile(MultipartFile file, String targetPath) throws IOException;

    /**
     * 删除文件
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    boolean deleteFile(String filePath);

    /**
     * 获取文件信息
     * @param filePath 文件路径
     * @return 文件大小（字节）
     * @throws IOException 文件不存在
     */
    long getFileSize(String filePath) throws IOException;

    /**
     * 检查文件是否存在
     * @param filePath 文件路径
     * @return 是否存在
     */
    boolean exists(String filePath);
}
