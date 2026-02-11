package com.geofile.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.geofile.entity.File;
import com.geofile.service.FileService;
import com.geofile.mapper.FileMapper;
import org.springframework.stereotype.Service;

/**
* @author xela
* @description 针对表【t_file(文件信息表)】的数据库操作Service实现
* @createDate 2026-02-10 23:30:13
*/
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File>
    implements FileService{

}




