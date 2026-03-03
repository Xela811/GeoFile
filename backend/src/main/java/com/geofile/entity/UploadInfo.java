package com.geofile.entity;

import lombok.Data;

/**
 * 分片上传初始化信息
 */
@Data
public class UploadInfo {
    /**
     * 文件唯一标识
     */
    private String uploadId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private long fileSize;

    /**
     * 分片大小
     */
    private int chunkSize;

    /**
     * 总分片数
     */
    private int totalChunks;

    /**
     * 已上传分片数
     */
    private int uploadedChunks;

    /**
     * 是否已完成
     */
    private boolean completed;
}
