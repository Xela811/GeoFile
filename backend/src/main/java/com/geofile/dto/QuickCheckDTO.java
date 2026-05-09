package com.geofile.dto;

import lombok.Data;

@Data
public class QuickCheckDTO {
    private Long size;           // 文件总大小
    private String sampleHash;   // 采样哈希
}
