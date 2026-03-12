package com.geofile.service;

import com.geofile.entity.File;
import com.geofile.entity.FileVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author xela
* @description 针对表【t_file(文件信息表)】的数据库操作Service
* @createDate 2026-02-10 23:30:13
*/
public interface FileService extends IService<File> {

    /**
     * 根据地理位置搜索附近文件
     * @param lat 纬度
     * @param lng 经度
     * @param radius 半径(米)
     * @param excludeFileId 排除的文件ID
     * @return 附近文件列表
     */
    List<FileVO> searchNearbyFiles(Double lat, Double lng, Integer radius, Long excludeFileId);

    /**
     * 更新文件地理位置信息
     * @param fileId 文件ID
     * @param lat 纬度
     * @param lng 经度
     * @param radius 半径
     */
    void updateFileLocation(Long fileId, Double lat, Double lng, Integer radius);
}
