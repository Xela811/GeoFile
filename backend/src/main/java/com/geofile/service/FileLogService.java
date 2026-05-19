package com.geofile.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.geofile.entity.FileLog;
import jakarta.servlet.http.HttpServletRequest;

public interface FileLogService extends IService<FileLog> {
    void recordLog(Long fileId, String actionType, Integer status, String errorMsg,
                          Double lat, Double lng, String ipAddress);
}
