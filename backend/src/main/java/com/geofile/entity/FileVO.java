package com.geofile.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 文件信息VO
 */
@Data
public class FileVO {
    /**
     * 文件ID
     */
    private Long id;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 存储类型
     */
    private String storageType;

    /**
     * 上传时间
     */
    private String uploadTime;

    /**
     * 有效截止时间
     */
    private String expireTime;

    /**
     * 下载次数
     */
    private Integer downloadCount;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusText;

    /**
     * 地理位置
     */
    private String location;

    /**
     * 地理位置（纬度）
     */
    private Double locationLat;

    /**
     * 地理位置（经度）
     */
    private Double locationLng;

    /**
     * 距离（米）
     */
    private Double distance;

    /**
     * 上传令牌（用于验证上传者身份）
     */
    private String uploadToken;

    /**
     * 下载令牌
     */
    private String downloadToken;

    /**
     * 下载次数上限（0表示不限制）
     */
    private Integer maxDownloads;

    /**
     * 下载验证码（5位字母数字，用于通过验证码下载）
     */
    private String downloadCode;

    /**
     * 是否私有（用于限制文件可见范围）
     */
    @TableField(value = "is_private")
    private Integer isPrivate;
}
