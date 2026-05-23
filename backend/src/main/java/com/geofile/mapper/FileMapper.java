package com.geofile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.geofile.entity.File;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author xela
* @description 针对表【t_file(文件信息表)】的数据库操作Mapper
* @createDate 2026-02-10 23:30:13
* @Entity com.geofile.entity.File
*/
public interface FileMapper extends BaseMapper<File> {
}




