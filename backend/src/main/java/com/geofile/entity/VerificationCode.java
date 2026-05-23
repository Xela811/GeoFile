package com.geofile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 验证码表
 * @TableName t_verification_code
 */
@TableName(value ="t_verification_code")
@Data
public class VerificationCode {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 验证码
     */
    @TableField(value = "code")
    private String code;

    /**
     * 验证码类型: LOGIN/DOWNLOAD/UPLOAD
     */
    @TableField(value = "type")
    private String type;

    /**
     * 过期时间
     */
    @TableField(value = "expire_time")
    private Date expireTime;

    /**
     * 最大尝试次数
     */
    @TableField(value = "max_attempts")
    private Integer maxAttempts;

    /**
     * 当前尝试次数
     */
    @TableField(value = "attempt_count")
    private Integer attemptCount;

    /**
     * 是否已使用
     */
    @TableField(value = "is_used")
    private Integer isUsed;

    /**
     * 
     */
    @TableField(value = "created_time")
    private Date createdTime;
}