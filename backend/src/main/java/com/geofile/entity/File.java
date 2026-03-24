package com.geofile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 文件信息表
 * @TableName t_file
 */
@TableName(value ="t_file")
@Data
public class File {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文件名
     */
    @TableField(value = "file_name")
    private String fileName;

    /**
     * 文件类型
     */
    @TableField(value = "file_type")
    private String fileType;

    /**
     * 文件大小(字节)
     */
    @TableField(value = "file_size")
    private Long fileSize;

    /**
     * 文件存储路径
     */
    @TableField(value = "file_path")
    private String filePath;

    /**
     * 原始文件名
     */
    @TableField(value = "original_name")
    private String originalName;

    /**
     * 存储类型: LOCAL/OSS
     */
    @TableField(value = "storage_type")
    private String storageType;

    /**
     * 上传时间
     */
    @TableField(value = "upload_time")
    private Date uploadTime;

    /**
     * 有效截止时间
     */
    @TableField(value = "expire_time")
    private Date expireTime;

    /**
     * 下载次数
     */
    @TableField(value = "download_count")
    private Integer downloadCount;

    /**
     * 状态 0-删除 1-正常
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 地理位置纬度
     */
    @TableField(value = "location_lat")
    private Double locationLat;

    /**
     * 地理位置经度
     */
    @TableField(value = "location_lng")
    private Double locationLng;

    /**
     * 地理位置半径(米)
     */
    @TableField(value = "location_radius")
    private Integer locationRadius;

    /**
     * 区域ID
     */
    @TableField(value = "region_id")
    private String regionId;

    /**
     * 创建人
     */
    @TableField(value = "created_by")
    private Long createdBy;

    /**
     * 
     */
    @TableField(value = "created_time")
    private Date createdTime;

    /**
     * 更新人
     */
    @TableField(value = "updated_by")
    private Long updatedBy;

    /**
     * 
     */
    @TableField(value = "updated_time")
    private Date updatedTime;

    /**
     * 逻辑删除
     */
    @TableField(value = "deleted")
    private Integer deleted;

    /**
     * 上传令牌（用于免登录验证上传者身份）
     */
    @TableField(value = "upload_token")
    private String uploadToken;
}