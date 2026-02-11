package com.geofile.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.geofile.entity.FileChunk;
import com.geofile.service.FileChunkService;
import com.geofile.mapper.FileChunkMapper;
import org.springframework.stereotype.Service;

/**
* @author xela
* @description 针对表【t_file_chunk(文件分片表)】的数据库操作Service实现
* @createDate 2026-02-10 23:30:13
*/
@Service
public class FileChunkServiceImpl extends ServiceImpl<FileChunkMapper, FileChunk>
    implements FileChunkService{

}




