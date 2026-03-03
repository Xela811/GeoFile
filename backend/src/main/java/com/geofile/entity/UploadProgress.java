package com.geofile.entity;

import lombok.Data;

/**
 * 上传进度
 */
@Data
public class UploadProgress {
    /**
     * 上传ID
     */
    private String uploadId;

    /**
     * 当前分片索引
     */
    private int currentChunk;

    /**
     * 总分片数
     */
    private int totalChunks;

    /**
     * 上传进度百分比（0-100）
     */
    private int progress;

    /**
     * 上传状态
     */
    private String status; // 'uploading', 'completed', 'error'

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private long fileSize;

    /**
     * 已上传字节数
     */
    private long uploadedBytes;
}
