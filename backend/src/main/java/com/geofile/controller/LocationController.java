package com.geofile.controller;

import com.geofile.common.Result;
import com.geofile.entity.FileVO;
import com.geofile.entity.LocationRequest;
import com.geofile.service.FileLocationService;
import com.geofile.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 地理位置服务Controller
 * 使用HTML5 Geolocation API获取用户地理位置
 */
@Slf4j
@RestController
@RequestMapping("/api/location")
@CrossOrigin
@Tag(name = "地理位置服务", description = "基于HTML5 Geolocation API的地理位置服务")
public class LocationController {

    @Autowired
    private FileLocationService fileLocationService;

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * 获取当前用户位置
     * 前端通过navigator.geolocation.getCurrentPosition调用此接口
     */
    @PostMapping("/current")
    @Operation(summary = "获取当前用户位置", description = "接收前端传来的地理位置信息并保存")
    public Result<Map<String, Object>> getCurrentLocation(
//            @Parameter(description = "纬度") @RequestParam Double lat,
//            @Parameter(description = "经度") @RequestParam Double lng,
//            @Parameter(description = "半径(米)") @RequestParam(defaultValue = "1000") Integer radius,
//            @Parameter(description = "区域信息") @RequestParam(required = false) String region,
//            @Parameter(description = "城市") @RequestParam(required = false) String city,
//            @Parameter(description = "省份") @RequestParam(required = false) String province
    @RequestBody LocationRequest request) {

        try {
            //log.info("保存用户位置: lat={}, lng={}, radius={}, region={}, city={}, province={}",
            //        lat, lng, radius, region, city, province);

            // 从对象中获取参数
            Double lat = request.getLat();
            Double lng = request.getLng();
            Integer radius = request.getRadius() != null ? request.getRadius() : 1000;

            log.info("保存用户位置: lat={}, lng={}, radius={}, region={}, city={}, province={}",
                    lat, lng, radius, request.getRegion(), request.getCity(), request.getProvince());

            // 创建匿名用户ID (使用时间戳)
            Long userId = System.currentTimeMillis();

            // 保存位置信息到UserLocation表（可选）
            // 这里可以选择性地保存位置信息到数据库

            // 返回位置信息
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("userId", userId);
            locationData.put("lat", lat);
            locationData.put("lng", lng);
            locationData.put("radius", radius);
            locationData.put("region", request.getRegion());
            locationData.put("city", request.getCity());
            locationData.put("province", request.getProvince());
            locationData.put("message", "位置信息保存成功");

            return Result.success(locationData);

        } catch (Exception e) {
            log.error("保存位置信息失败", e);
            return Result.error("保存位置信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取附近文件
     */
    @GetMapping("/nearby")
    @Operation(summary = "获取附近文件", description = "根据用户地理位置搜索附近的文件")
    public Result<Map<String, Object>> getNearbyFiles(
            @Parameter(description = "纬度") @RequestParam Double lat,
            @Parameter(description = "经度") @RequestParam Double lng,
            @Parameter(description = "半径(米)，默认1000米") @RequestParam(defaultValue = "1000") Integer radius,
            @Parameter(description = "排除的文件ID") @RequestParam(required = false) Long excludeFileId) {

        try {
            log.info("搜索附近文件: lat={}, lng={}, radius={}, excludeFileId={}", lat, lng, radius, excludeFileId);

            // 先保存用户位置
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("lat", lat);
            locationData.put("lng", lng);
            locationData.put("radius", radius);

            // 保存位置信息
            Long userId = System.currentTimeMillis();
            locationData.put("userId", userId);

            // 搜索附近文件
            List<FileVO> files = fileLocationService.searchNearbyFiles(lat, lng, radius, excludeFileId);

            Map<String, Object> result = new HashMap<>();
            result.put("location", locationData);
            result.put("files", files);
            result.put("count", files.size());

            return Result.success(result);

        } catch (Exception e) {
            log.error("搜索附近文件失败", e);
            return Result.error("搜索附近文件失败: " + e.getMessage());
        }
    }

    /**
     * 估算文件距离
     */
    @GetMapping("/distance")
    @Operation(summary = "估算文件距离", description = "计算用户位置与文件位置的距离")
    public Result<Double> calculateDistance(
            @Parameter(description = "用户纬度") @RequestParam Double userLat,
            @Parameter(description = "用户经度") @RequestParam Double userLng,
            @Parameter(description = "文件纬度") @RequestParam Double fileLat,
            @Parameter(description = "文件经度") @RequestParam Double fileLng) {

        try {
            // 使用Haversine公式计算距离
            double distance = haversineDistance(userLat, userLng, fileLat, fileLng);
            return Result.success(distance);
        } catch (Exception e) {
            log.error("计算距离失败", e);
            return Result.error("计算距离失败: " + e.getMessage());
        }
    }

    /**
     * 计算两点之间的距离（米）
     * 使用Haversine公式
     */
    private double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        final int EARTH_RADIUS = 6371000; // 地球半径(米)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }
}
