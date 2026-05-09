package com.geofile.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.geofile.entity.FileBatch;

/**
 * 文件批次管理服务接口
 */
public interface FileBatchService extends IService<FileBatch> {
    // 如果后续需要自定义复杂的批次业务逻辑（例如清理特定IP的批次），可以在这里定义方法
}