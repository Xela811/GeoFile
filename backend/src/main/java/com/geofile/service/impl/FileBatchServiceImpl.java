package com.geofile.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.geofile.entity.FileBatch;
import com.geofile.mapper.FileBatchMapper;
import com.geofile.service.FileBatchService;
import org.springframework.stereotype.Service;

/**
 * 文件批次管理服务实现类
 */
@Service
public class FileBatchServiceImpl extends ServiceImpl<FileBatchMapper, FileBatch> implements FileBatchService {
    // 继承 ServiceImpl 后，你已经拥有了 save, getOne, list, update 等所有 MP 内置方法
}