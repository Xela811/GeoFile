package com.geofile.task;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.geofile.entity.File;
import com.geofile.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class FileLifecycleTask {
    @Autowired
    private FileService fileService;

    /**
     * 每分钟扫描一次过期文件
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0/15 * * * ?")
    public void handleFileExpiration() {
        // 逻辑 A：处理时间过期 (1 -> 2)
        fileService.update(new LambdaUpdateWrapper<File>()
                .set(File::getStatus, 2)
                .eq(File::getStatus, 1)
                .lt(File::getExpireTime, new Date()));

        // 逻辑 B：处理残影到期 (3 -> 4)
        // 凡是状态为 3 的，说明它们已经至少经历了一次下载满额，
        // 并在当前 15 分钟周期内停留过。现在统一将其设为 4 (不再可见)
        fileService.update(new LambdaUpdateWrapper<File>()
                .set(File::getStatus, 4)
                .eq(File::getStatus, 3));

        log.info("定时任务：已清理过期文件，并下架了上一周期的满额残影文件");
    }
}
