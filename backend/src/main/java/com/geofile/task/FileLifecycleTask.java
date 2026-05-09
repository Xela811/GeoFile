package com.geofile.task;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.geofile.entity.File;
import com.geofile.entity.FileHash;
import com.geofile.service.FileHashService;
import com.geofile.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class FileLifecycleTask {
    @Autowired
    private FileService fileService;

    @Autowired
    private FileHashService fileHashService;

    @Value("${file.upload.path}")
    private String uploadPath;

    /**
     * 每分钟扫描一次过期文件
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0/15 * * * ?")
    public void handleFileExpiration() {
//        // 逻辑 A：处理时间过期 (1 -> 2)
//        fileService.update(new LambdaUpdateWrapper<File>()
//                .set(File::getStatus, 2)
//                .eq(File::getStatus, 1)
//                .lt(File::getExpireTime, new Date()));
//
//        // 逻辑 B：处理残影到期 (3 -> 4)
//        // 凡是状态为 3 的，说明它们已经至少经历了一次下载满额，
//        // 并在当前 15 分钟周期内停留过。现在统一将其设为 4 (不再可见)
//        fileService.update(new LambdaUpdateWrapper<File>()
//                .set(File::getStatus, 4)
//                .eq(File::getStatus, 3));
//
//        log.info("定时任务：已清理过期文件，并下架了上一周期的满额残影文件");
        Date now = new Date();

        // 1. 找出所有：即将过期的(status=1且时间到) 或 即将从残影下架的(status=3) 文件
        // 逻辑 A：处理时间过期 (1 -> 2)
        // 逻辑 B：处理残影到期 (3 -> 4)
        // 凡是状态为 3 的，说明它们已经至少经历了一次下载满额，
        // 并在当前 15 分钟周期内停留过。现在统一将其设为 4 (不再可见)
        // 这些文件在本次扫描后都将变为不可用，需要对其哈希引用减1

        List<File> expiringFiles = fileService.list(new LambdaQueryWrapper<File>()
                .eq(File::getDeleted, 0) // 只处理还没标记删除的
                .and(w -> w.and(sub -> sub.eq(File::getStatus, 1).lt(File::getExpireTime, now))
                        .or(sub -> sub.eq(File::getStatus, 3)))
        );

        if (expiringFiles.isEmpty()) return;

        for (File f : expiringFiles) {
            try {
                // 2. 核心逻辑：执行引用计数减1
                if (f.getFileHash() != null) {
                    fileHashService.decrementReference(f.getFileHash());
                }

                // 3. 更新业务表状态
                if (f.getStatus() == 1) {
                    f.setStatus(2); // 设为过期
                } else if (f.getStatus() == 3) {
                    f.setStatus(4); // 设为下架
                }
                f.setDeleted(1); // 建议过期也标记为逻辑删除，统一查询口径
                fileService.updateById(f);
            } catch (Exception e) {
                log.error("处理文件生命周期异常: fileId={}", f.getId(), e);
            }
        }

        log.info("定时任务完成：处理了 {} 个过期/满额文件", expiringFiles.size());
    }

    /**
     * 物理清理任务：每天凌晨 3 点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void physicalFileCleanup() {

        // 计算 T+1 的时间阈值：当前时间减去 24 小时
        // 只有更新时间早于这个阈值的记录，才会被真正删除
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);

        // 1. 查询所有引用计数为 0 且状态为待清理的文件记录
        List<FileHash> pendingDelete = fileHashService.list(new LambdaQueryWrapper<FileHash>()
                .eq(FileHash::getReferenceCount, 0)
                .eq(FileHash::getStatus, 0)
                .le(FileHash::getUpdatedTime, threshold));

        if (pendingDelete.isEmpty()) {
            return;
        }

        log.info("开始物理清理 T+1 任务，待处理文件数: {}", pendingDelete.size());

        for (FileHash hashRecord : pendingDelete) {
            try {
                // 2. 构建物理路径
                java.nio.file.Path path = java.nio.file.Paths.get(uploadPath, hashRecord.getStoragePath());

                // 3. 执行物理删除
                boolean deleted = java.nio.file.Files.deleteIfExists(path);

                if (deleted) {
                    // 4. 物理删除成功后，再删除数据库记录
                    fileHashService.removeById(hashRecord.getId());
                    log.info("物理文件已清理: {}", path);
                } else {
                    // 如果文件本身就不存在了，也清理数据库记录，保持一致性
                    fileHashService.removeById(hashRecord.getId());
                    log.warn("物理文件不存在，已清理数据库残余记录: {}", hashRecord.getFileHash());
                }
            } catch (Exception e) {
                log.error("清理物理文件失败: hash={}", hashRecord.getFileHash(), e);
            }
        }
    }
}
