package com.geofile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.geofile.entity.File;
import com.geofile.entity.FileVO;
import com.geofile.service.FileService;
import com.geofile.mapper.FileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
* @author xela
* @description 针对表【t_file(文件信息表)】的数据库操作Service实现
* @createDate 2026-02-10 23:30:13
*/
@Slf4j
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File>
    implements FileService{

    @Override
    public List<FileVO> searchNearbyFiles(Double lat, Double lng, Integer radius, Long excludeFileId) {
        try {
            log.info("搜索附近文件: lat={}, lng={}, radius={}, excludeFileId={}", lat, lng, radius, excludeFileId);

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
            if (excludeFileId != null) {
                queryWrapper.ne(File::getId, excludeFileId);
            }

            // 按距离排序
            // 距离计算公式: distance = sqrt((lat2-lat1)^2 + (lng2-lng1)^2 * cos(lat1)^2)
            List<File> files = list(queryWrapper);

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

        } catch (Exception e) {
            log.error("搜索附近文件失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void updateFileLocation(Long fileId, Double lat, Double lng, Integer radius) {
        try {
            File file = getById(fileId);
            if (file == null) {
                throw new RuntimeException("文件不存在: " + fileId);
            }

            file.setLocationLat(lat);
            file.setLocationLng(lng);
            file.setLocationRadius(radius);
            updateById(file);

            log.info("更新文件地理位置: fileId={}, lat={}, lng={}, radius={}", fileId, lat, lng, radius);
        } catch (Exception e) {
            log.error("更新文件地理位置失败", e);
            throw e;
        }
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
        vo.setUploadTime(file.getUploadTime().toString());
        vo.setExpireTime(file.getExpireTime().toString());
        vo.setStatusText(file.getStatus() == 1 ? "正常" : "已删除");
        return vo;
    }
}




