package com.geofile.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.geofile.pojo.FileTransfer;
import com.geofile.service.FileTransferService;
import com.geofile.mapper.FileTransferMapper;
import org.springframework.stereotype.Service;

/**
* @author xela
* @description 针对表【t_file_transfer(文件传输记录表)】的数据库操作Service实现
* @createDate 2026-02-10 23:30:13
*/
@Service
public class FileTransferServiceImpl extends ServiceImpl<FileTransferMapper, FileTransfer>
    implements FileTransferService{

}




