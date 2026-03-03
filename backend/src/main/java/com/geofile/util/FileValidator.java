package com.geofile.util;

import org.springframework.stereotype.Component;

/**
 * 文件验证工具类
 */
@Component
public class FileValidator {

    // 允许的文件类型
    private static final String[] ALLOWED_TYPES = {
        "jpg", "jpeg", "png", "gif", "bmp", "webp",  // 图片
        "pdf",                                      // PDF
        "doc", "docx",                              // Word
        "xls", "xlsx",                              // Excel
        "ppt", "pptx",                              // PowerPoint
        "txt",                                      // 文本
        "zip", "rar", "7z",                          // 压缩包
        "mp4", "avi", "mov", "mkv",                 // 视频
        "mp3", "wav", "flac",                        // 音频
        "json", "xml", "csv"                        // 数据文件
    };

    // 最大文件大小（100MB）
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

    /**
     * 验证文件类型
     * @param originalFilename 原始文件名
     * @param contentType 内容类型
     * @return 是否允许上传
     */
    public boolean isValidFileType(String originalFilename, String contentType) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return false;
        }

        String extension = getFileExtension(originalFilename).toLowerCase();

        // 检查扩展名是否在允许列表中
        for (String allowedType : ALLOWED_TYPES) {
            if (allowedType.equals(extension)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 验证文件大小
     * @param size 文件大小（字节）
     * @return 是否允许上传
     */
    public boolean isValidFileSize(long size) {
        return size > 0 && size <= MAX_FILE_SIZE;
    }

    /**
     * 验证文件（类型和大小）
     * @param originalFilename 原始文件名
     * @param contentType 内容类型
     * @param size 文件大小（字节）
     * @return 是否验证通过
     */
    public boolean validate(String originalFilename, String contentType, long size) {
        return isValidFileType(originalFilename, contentType) && isValidFileSize(size);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 获取文件大小限制
     */
    public long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }

    /**
     * 获取允许的文件类型列表
     */
    public String[] getAllowedTypes() {
        return ALLOWED_TYPES;
    }
}
