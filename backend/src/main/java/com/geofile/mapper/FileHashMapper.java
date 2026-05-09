package com.geofile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.geofile.entity.FileHash;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileHashMapper extends BaseMapper<FileHash> {
}
