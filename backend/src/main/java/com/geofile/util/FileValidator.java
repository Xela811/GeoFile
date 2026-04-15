package com.geofile.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 文件验证工具类
 */
@Component
public class FileValidator {

    // 允许的文件类型
    private static final String[] ALLOWED_TYPES = {
        "jpg", "jpeg", "png", "gif", "bmp", "webp","heic","m4a",  // 图片
        "pdf",                                      // PDF
        "doc", "docx",                              // Word
        "xls", "xlsx",                              // Excel
        "ppt", "pptx",                              // PowerPoint
        "txt",                                      // 文本
        "zip", "rar", "7z",                          // 压缩包
        "mp4", "avi", "mov", "mkv",                 // 视频
        "mp3", "wav", "flac",                        // 音频
        "json", "xml", "csv",                        // 数据文件
        "exe", "msi", "apk", "dmg",                  //安装包
        "md",
        "java", "py", "vue", "js", "ts", "html", "css", "sql", "sh"
    };

    // 1. 严格黑名单：禁止任何可能在服务器执行的脚本后缀
    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
            "jsp", "jspx", "php", "php5", "asp", "aspx", "sh", "py", "pl", "rb", "cgi", "bat"
    );

    // 2. 敏感 ContentType 关键字：防止 XSS 攻击或 MIME 嗅探攻击
    private static final List<String> SENSITIVE_CONTENT_KEYWORDS = List.of(
            "html", "javascript", "script"
    );

    // 最大文件大小（1GB）
    private static final long MAX_FILE_SIZE = 1024 * 1024 * 1024;

    /**
     * 验证文件类型
     * @param originalFilename 原始文件名
     * @param contentType 内容类型
     * @return 是否允许上传
     */
    public boolean isValidFileType(String originalFilename,String contentType) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return false;
        }

        // --- A. 后缀名清洗与黑名单检查 ---
        String extension = "";
        String trimmedName = originalFilename.trim();
        int lastDotIndex = trimmedName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < trimmedName.length() - 1) {
            // 处理末尾的点和空格，防止 file.php. 绕过
            extension = trimmedName.substring(lastDotIndex + 1).toLowerCase().replaceAll("\\.$", "");
        }

        if (FORBIDDEN_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("出于安全考虑，禁止上传脚本文件：" + extension);
        }

        // --- B. ContentType 敏感关键字检查 ---
        if (contentType != null) {
            String lowerType = contentType.toLowerCase();
            boolean hasSensitiveKey = SENSITIVE_CONTENT_KEYWORDS.stream()
                    .anyMatch(lowerType::contains);

            if (hasSensitiveKey) {
                // 如果后缀不是 html 但声明是 html，存在 XSS 风险
                if (!"html".equals(extension) && !"htm".equals(extension)) {
                    throw new IllegalArgumentException("文件内容类型声明异常，禁止上传");
                }
            }
        }

        // --- C. 放行 ---
        // 只要不是黑名单里的脚本，Matlab (mat)、Origin (opju)、PS (psd) 等都会返回 true
        return true;
    }
//    public boolean isValidFileType(String originalFilename, String contentType) {
//        if (originalFilename == null || originalFilename.isEmpty()) {
//            return false;
//        }
//
//        String extension = getFileExtension(originalFilename).toLowerCase();
//
//        // 检查扩展名是否在允许列表中
////        for (String allowedType : ALLOWED_TYPES) {
////            if (allowedType.equals(extension)) {
////                return true;
////            }
////        }
//        if (FORBIDDEN_TYPES.contains(extension)) {
//            throw new IllegalArgumentException("出于安全考虑，禁止上传脚本文件");
//        }
//
//        return true;
//    }

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
