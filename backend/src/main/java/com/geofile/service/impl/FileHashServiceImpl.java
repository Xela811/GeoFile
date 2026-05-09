package com.geofile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.geofile.entity.FileHash;
import com.geofile.mapper.FileHashMapper;
import com.geofile.service.FileHashService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FileHashServiceImpl extends ServiceImpl<FileHashMapper, FileHash> implements FileHashService {
    @Override
    public FileHash findByMd5(String md5) {
        return this.getOne(new LambdaQueryWrapper<FileHash>()
                .eq(FileHash::getMd5, md5)
                .eq(FileHash::getStatus, 1)
                .last("LIMIT 1"));
    }

    @Override
    public FileHash findByHash(String fileHash) {
        return this.getOne(new LambdaQueryWrapper<FileHash>()
                .eq(FileHash::getFileHash, fileHash));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean incrementReference(String fileHash) {
        // 使用 SQL 原子自增，防止并发问题
        return this.update(new LambdaUpdateWrapper<FileHash>()
                .eq(FileHash::getFileHash, fileHash)
                .setSql("reference_count = reference_count + 1")
                .set(FileHash::getStatus, 1)
                .set(FileHash::getUpdatedTime, LocalDateTime.now()));

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decrementReference(String fileHash) {
        boolean updated = this.update(new LambdaUpdateWrapper<FileHash>()
                .eq(FileHash::getFileHash, fileHash)
                .gt(FileHash::getReferenceCount, 0)
                .setSql("reference_count = reference_count - 1"));
        if (updated) {
            // 这里可以根据业务决定，如果减完后 count 等于 0，就把 status 设为 0 (标记为可物理清理)
            this.update(new LambdaUpdateWrapper<FileHash>()
                    .eq(FileHash::getFileHash, fileHash)
                    .eq(FileHash::getReferenceCount, 0)
                    .set(FileHash::getStatus, 0));
        }
        return updated;
    }
}
