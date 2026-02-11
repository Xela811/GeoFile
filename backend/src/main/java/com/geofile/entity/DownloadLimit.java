package com.geofile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 下载限制配置表
 * @TableName t_download_limit
 */
@TableName(value ="t_download_limit")
@Data
public class DownloadLimit {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文件ID
     */
    @TableField(value = "file_id")
    private Long fileId;

    /**
     * 最大下载次数
     */
    @TableField(value = "max_downloads")
    private Integer maxDownloads;

    /**
     * 有效时长(小时)
     */
    @TableField(value = "valid_hours")
    private Integer validHours;
}