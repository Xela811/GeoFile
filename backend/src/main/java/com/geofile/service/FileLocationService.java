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
     * @param pageNum 当前页码
     * @param pageSize 每页大小
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @param keyword 搜索关键词
     * @param fileType 文件类型
     * @return 附近文件列表
     */
    List<FileVO> searchNearbyFiles(Double lat, Double lng, Integer radius, Long fileId,
                                    Integer pageNum, Integer pageSize,
                                    String sortBy, String sortOrder, String keyword, String fileType);

    /**
     * 更新文件地理位置信息
     * @param fileId 文件ID
     * @param lat 纬度
     * @param lng 经度
     * @param radius 半径
     */
    void updateFileLocation(Long fileId, Double lat, Double lng, Integer radius);
}
