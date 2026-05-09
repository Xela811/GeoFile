package com.geofile.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_file_hash")
public class FileHash {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 物理文件唯一标识(SHA-256值) */
    private String fileHash;

    /** 前端快速校验值(MD5值) */
    private String md5;

    private Long fileSize;

    private String storagePath;

    /** 存储类型: local, oss, minio */
    private String storageType;

    /** 逻辑引用计数 */
    private Integer referenceCount;

    /** 物理文件状态: 1-可用, 0-上传中/已损坏 */
    private Integer status;

    private String mimeType;

    private String extension;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    private String sampleHash;
}
