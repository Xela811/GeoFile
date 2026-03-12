package com.geofile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.geofile.entity.File;
import com.geofile.entity.FileVO;
import com.geofile.mapper.FileMapper;
import com.geofile.service.FileLocationService;
import com.geofile.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件地理位置服务实现
 */
@Slf4j
@Service
public class FileLocationServiceImpl extends ServiceImpl<FileMapper,File> implements FileLocationService {

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private FileService fileService;

    /**
     * 根据地理位置搜索附近文件
     * 使用简单的经纬度范围查询实现
     */
    @Override
    public List<FileVO> searchNearbyFiles(Double lat, Double lng, Integer radius, Long fileId) {
        if (lat == null || lng == null || radius == null) {
            return new ArrayList<>();
        }

        log.info("搜索附近文件: lat={}, lng={}, radius={}, excludeFileId={}", lat, lng, radius, fileId);

        // 计算经纬度范围
        // 1度纬度 ≈ 111km (111000米)
        // 1度经度 ≈ 111km * cos(lat) (米)
        double latDelta = radius / 111000.0;
        double lngDelta = radius / (111000.0 * Math.cos(lat * Math.PI / 180.0));

        double minLat = lat - latDelta;
        double maxLat = lat + latDelta;
        double minLng = lng - lngDelta;
        double maxLng = lng + lngDelta;

        // 构建查询条件
        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<File>()
            .between(File::getLocationLat, minLat, maxLat)
            .between(File::getLocationLng, minLng, maxLng)
            .eq(File::getStatus, 1)  // 只查询有效文件
            .isNotNull(File::getLocationLat)
            .isNotNull(File::getLocationLng);

        // 排除自己
        if (fileId != null) {
            queryWrapper.ne(File::getId, fileId);
        }

        // 按距离排序
        // 距离计算公式: distance = sqrt((lat2-lat1)^2 + (lng2-lng1)^2 * cos(lat1)^2)
        List<File> files = fileMapper.selectList(queryWrapper);

        // 计算距离并排序
        List<FileVO> result = new ArrayList<>();
        for (File file : files) {
            double distance = calculateDistance(lat, lng, file.getLocationLat(), file.getLocationLng());
            if (distance <= radius) {
                FileVO vo = convertToFileVO(file);
                vo.setDistance(distance);
                result.add(vo);
            }
        }

        // 按距离排序
        result.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));

        return result;
    }

    /**
     * 更新文件地理位置信息
     */
    @Override
    public void updateFileLocation(Long fileId, Double lat, Double lng, Integer radius) {
        File file = fileService.getById(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在: " + fileId);
        }

        file.setLocationLat(lat);
        file.setLocationLng(lng);
        file.setLocationRadius(radius);
        fileService.updateById(file);

        log.info("更新文件地理位置: fileId={}, lat={}, lng={}, radius={}", fileId, lat, lng, radius);
    }

    /**
     * 计算两点之间的距离（米）
     * 使用Haversine公式
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int EARTH_RADIUS = 6371000; // 地球半径(米)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * 转换为FileVO
     */
    private FileVO convertToFileVO(File file) {
        FileVO vo = new FileVO();
        BeanUtils.copyProperties(file, vo);

        // 计算距离（如果提供了用户位置）
        // 这个方法在调用时会被覆盖，所以这里返回null
        vo.setDistance(null);

        return vo;
    }
}
