package com.geofile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.geofile.entity.FileBatch;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileBatchMapper extends BaseMapper<FileBatch> {
    // 继承 BaseMapper 后，基本的增删改查都已经有了
}