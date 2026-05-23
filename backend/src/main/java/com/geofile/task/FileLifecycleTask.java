package com.geofile.task;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.geofile.entity.File;
import com.geofile.entity.FileHash;
import com.geofile.service.FileHashService;
import com.geofile.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

@Component
@Slf4j
public class FileLifecycleTask {
    @Autowired
    private FileService fileService;

    @Autowired
    private FileHashService fileHashService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${file.upload.path}")
    private String uploadPath;

    private static final long MAX_STORAGE_BYTES = 20L * 1024 * 1024 * 1024;

    /**
     * 每分钟扫描一次过期文件
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0/15 * * * ?")
    public void handleFileExpiration() {

        try {
            checkAndEvictFilesByCapacity();
        } catch (Exception e) {
            log.error("执行存储容量控制淘汰失败", e);
        }

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

        // 用于记录这一批任务中涉及到的所有 Token，稍后统一检查
        Set<String> tokensToCheck = new HashSet<>();

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

                // 收集 Token
                if (f.getUploadToken() != null) {
                    tokensToCheck.add(f.getUploadToken());
                }
            } catch (Exception e) {
                log.error("处理文件生命周期异常: fileId={}", f.getId(), e);
            }
        }

        // ======= 新增：清理 RedisGEO 逻辑 =======
        for (String token : tokensToCheck) {
            // 检查这个 Token 下是否还有存活的文件 (status 1 或 3)
            long aliveCount = fileService.count(new LambdaQueryWrapper<File>()
                    .eq(File::getUploadToken, token)
                    .in(File::getStatus, Arrays.asList(1, 3))
                    .eq(File::getDeleted, 0));

            if (aliveCount == 0) {
                // 说明该批次文件已全部阵亡，从 Redis 中移除地理索引
                redisTemplate.opsForZSet().remove("file:locations:public", token);
                log.info("Token {} 对应的所有文件已失效，已清理 RedisGEO 索引", token);
            }
        }
        log.info("定时任务完成：处理了 {} 个过期/满额文件", expiringFiles.size());
    }

    private void checkAndEvictFilesByCapacity() {
        Path folder = Paths.get(uploadPath);
        if (!Files.exists(folder)) return;

        long currentSizeOfFiles = 0;
        try (Stream<Path> walk = Files.walk(folder)) {
            currentSizeOfFiles = walk.filter(Files::isRegularFile)
                    .mapToLong(p -> p.toFile().length())
                    .sum();
        } catch (IOException e) {
            log.error("计算上传目录大小失败", e);
            return;
        }

        log.info("当前存储占用: {} MB / 阈值: {} MB", currentSizeOfFiles / 1024 / 1024, MAX_STORAGE_BYTES / 1024 / 1024);

        if (currentSizeOfFiles <= MAX_STORAGE_BYTES) {
            return;
        }

        long bytesToFree = currentSizeOfFiles - MAX_STORAGE_BYTES;
        log.warn("存储空间超限！需要释放至少 {} MB 空间", bytesToFree / 1024 / 1024);

        // ==========================================
        //  阶段一：优先物理清理【引用计数为0】的闲置文件（废物利用）
        // ==========================================
        List<FileHash> garbageList = fileHashService.list(new LambdaQueryWrapper<FileHash>()
                .eq(FileHash::getReferenceCount, 0)
                .eq(FileHash::getStatus, 0)
                .orderByAsc(FileHash::getUpdatedTime)); // 从最老被删的记录开始清

        for (FileHash hashRecord : garbageList) {
            if (bytesToFree <= 0) break; // 空间够了，提早退出

            try {
                Path path = Paths.get(uploadPath, hashRecord.getStoragePath());
                long fileSize = Files.exists(path) ? Files.size(path) : 0;

                boolean deleted = Files.deleteIfExists(path);
                if (deleted || fileSize == 0) {
                    fileHashService.removeById(hashRecord.getId());
                    bytesToFree -= fileSize; // 扣减还需要释放的空间
                    log.info("容量限制-[优先强清无引用垃圾]: 物理路径={}, 释放空间={} MB", path, fileSize / 1024 / 1024);
                }
            } catch (Exception e) {
                log.error("容量限制-强清无引用垃圾失败: hash={}", hashRecord.getFileHash(), e);
            }
        }

        // 如果清理完无引用的死文件后，空间已经腾出来了，直接皆大欢喜，打道回府！
        if (bytesToFree <= 0) {
            log.info("通过物理清理闲置垃圾文件，成功将空间拉回安全线以内。");
            return;
        }

        // ==========================================
        //  阶段二：迫不得已，淘汰【正常在线】的最老文件（兜底防线）
        // ==========================================
        log.warn("清理完闲置垃圾后空间仍不足，开始强制下线正常文件，仍需释放: {} MB", bytesToFree / 1024 / 1024);

        List<File> activeFiles = fileService.list(new LambdaQueryWrapper<File>()
                .eq(File::getStatus, 1)
                .eq(File::getDeleted, 0)
                .orderByAsc(File::getCreatedTime)
        );

        Set<String> tokensToCheck = new HashSet<>();

        for (File f : activeFiles) {
            if (bytesToFree <= 0) break;

            try {
                if (f.getFileHash() != null) {
                    fileHashService.decrementReference(f.getFileHash());
                }

                f.setStatus(2);
                f.setDeleted(1);
                fileService.updateById(f);

                if (f.getUploadToken() != null) {
                    tokensToCheck.add(f.getUploadToken());
                }

                if (f.getFileSize() != null) {
                    bytesToFree -= f.getFileSize();
                }
                log.info("容量限制-[被迫牺牲在线文件]: id={}, fileName={}, size={} MB", f.getId(), f.getFileName(), f.getFileSize() / 1024 / 1024);

            } catch (Exception e) {
                log.error("容量淘汰正常文件失败: fileId={}", f.getId(), e);
            }
        }

        // 同步清理 GEO 索引
        for (String token : tokensToCheck) {
            long aliveCount = fileService.count(new LambdaQueryWrapper<File>()
                    .eq(File::getUploadToken, token)
                    .in(File::getStatus, Arrays.asList(1, 3))
                    .eq(File::getDeleted, 0));
            if (aliveCount == 0) {
                redisTemplate.opsForZSet().remove("file:locations:public", token);
            }
        }
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
