package com.geofile.service;

import com.geofile.entity.File;
import com.geofile.entity.FileVO;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 文件地理位置服务
 * 处理基于地理位置的文件搜索功能
 */
public interface FileLocationService extends IService<File> {

    /**
     * 根据地理位置搜索附近文件
     * @param lat 纬度
     * @param lng 经度
     * @param radius 半径(米)，默认1000米
     * @param fileId 排除的文件ID（用于搜索时排除自己）
     * @return 附近文件列表
     */
    List<FileVO> searchNearbyFiles(Double lat, Double lng, Integer radius, Long fileId);

    /**
     * 更新文件地理位置信息
     * @param fileId 文件ID
     * @param lat 纬度
     * @param lng 经度
     * @param radius 半径
     */
    void updateFileLocation(Long fileId, Double lat, Double lng, Integer radius);
}
