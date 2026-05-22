package com.geofile.controller;

import com.geofile.common.Result;
import com.geofile.entity.LocationRequest;
import com.geofile.service.FileLocationService;
import com.geofile.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
}
