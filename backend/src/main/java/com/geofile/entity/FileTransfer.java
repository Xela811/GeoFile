package com.geofile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 文件传输记录表
 * @TableName t_file_transfer
 */
@TableName(value ="t_file_transfer")
@Data
public class FileTransfer {
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
     * 传输验证码
     */
    @TableField(value = "transfer_code")
    private String transferCode;

    /**
     * 下载IP
     */
    @TableField(value = "ip_address")
    private String ipAddress;

    /**
     * 用户代理
     */
    @TableField(value = "user_agent")
    private String userAgent;

    /**
     * 下载时间
     */
    @TableField(value = "download_time")
    private Date downloadTime;

    /**
     * 下载持续时间(秒)
     */
    @TableField(value = "download_duration")
    private Integer downloadDuration;

    /**
     * 是否完成下载: 0-未完成 1-完成
     */
    @TableField(value = "is_completed")
    private Integer isCompleted;
}