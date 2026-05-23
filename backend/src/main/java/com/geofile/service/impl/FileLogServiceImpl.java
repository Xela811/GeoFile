package com.geofile.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.geofile.entity.FileLog;
import com.geofile.mapper.FileLogMapper;
import com.geofile.service.FileLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FileLogServiceImpl extends ServiceImpl<FileLogMapper,FileLog> implements FileLogService {
    @Autowired
    private FileLogMapper fileLogMapper;

    /**
     *  使用 @Async 开启异步线程写入日志，绝不拖慢用户的主文件下载/上传响应速度！
     * （记得在你的 Spring Boot 启动类上加上 @EnableAsync 标签）
     */
    @Override
    public void recordLog(Long fileId, String actionType, Integer status, String errorMsg,
                          Double lat, Double lng, String ipAddress) {
        try {


            FileLog fileLog = FileLog.builder()
                    .fileId(fileId)
                    .actionType(actionType)
                    .status(status)
                    .errorMsg(errorMsg)
                    .ipAddress(ipAddress != null ? ipAddress : "UNKNOWN")
                    .lat(lat)
                    .lng(lng)
                    .build();

            fileLogMapper.insert(fileLog);
        } catch (Exception e) {
            log.error("异步落库日志异常: ", e);
        }
    }
}
