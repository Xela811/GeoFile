package com.geofile.dto;

import lombok.Data;

@Data
public class SecUploadDTO {
    private String hash;        // 前端算的 SHA-256
    private String fileName;    // 文件名
    private Double lat;         // 经度
    private Double lng;         // 纬度
    private Integer radius;      // 半径
    private Integer maxDownloads; // 最大下载次数
    private Integer validMinutes; // 有效时长
    private Boolean needCode;     // 是否私有
    // 关键：前端传入的当前批次共用 Token
    private String uploadToken;
}
