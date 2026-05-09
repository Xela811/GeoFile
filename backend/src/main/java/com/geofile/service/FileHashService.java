package com.geofile.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.geofile.entity.FileHash;

public interface FileHashService extends IService<FileHash> {
    /**
     * 根据 MD5 查找物理文件记录（用于秒传预检）
     */
    FileHash findByMd5(String md5);

    /**
     * 根据 SHA-256 查找物理文件记录（用于秒传精确匹配）
     */
    FileHash findByHash(String fileHash);

    /**
     * 增加引用计数
     */
    boolean incrementReference(String fileHash);

    /**
     * 减少引用计数
     */
    boolean decrementReference(String fileHash);
}
