package com.geofile.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件上传批次增强索引表
 * 用于记录免登录上传的批次信息、IP及Redis回源保障
 */
@Data
@TableName("t_file_batch")
public class FileBatch implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 对应 t_file 中的 upload_token
     */
    private String batchToken;

    /**
     * 取件码(如果有)
     */
    private String extractCode;

    /**
     * 上传者真实IP
     */
    private String clientIp;

    /**
     * 批次内文件总数
     */
    private Integer fileCount;

    /**
     * 批次总大小(字节)
     */
    private Long totalSize;

    /**
     * 是否私有：1-私有，0-公开
     */
    private Integer isPrivate;

    /**
     * 失效时间(过期后取件码失效)
     */
    private LocalDateTime expireTime;

    /**
     * 创建时间（上传时间）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
