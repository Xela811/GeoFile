package com.geofile.controller;

import com.geofile.common.Result;
import com.geofile.service.AmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 高德地图 API Controller
 *
 * 功能：
 * - 逆地理编码（经纬度转地址）
 * - 地理编码（地址转经纬度）
 * - POI 搜索
 * - 地址自动补全
 * - 搜索周边的餐厅、医院、地铁等
 *
 * 注意事项：
 * 1. API Key 需要在 application.yml 中配置
 * 2. 高德地图 API 有调用次数限制
 * 3. Web 服务 API 每日限额 60000 次调用
 */
@Slf4j
@RestController
@RequestMapping("/api/amap")
@CrossOrigin
@Tag(name = "高德地图API", description = "高德地图 Web 服务 API 接口")
public class AmapController {

    @Autowired
    private AmapService amapService;

    /**
     * 高德地图 API Key
     * 从 application.yml 注入
     */
    @Value("${amap.key:}")
    private String amapKey;

    /**
     * 使用经纬度逆地理编码
     *
     * 接口：GET /api/amap/geocode-from-location?lat=39.9042&lng=116.4074
     * 参数：lat（纬度）、lng（经度）
     * 返回：详细的地址信息
     *
     * 使用示例：
     * GET /api/amap/geocode-from-location?lat=39.9042&lng=116.4074
     *
     * @param lat 纬度
     * @param lng 经度
     * @returns 地址信息
     */
    @GetMapping("/geocode-from-location")
    @Operation(summary = "逆地理编码", description = "将经纬度坐标转换为详细的地址信息")
    public Result<Map<String, Object>> geocodeFromLocation(
            @Parameter(description = "纬度", example = "39.9042") @RequestParam Double lat,
            @Parameter(description = "经度", example = "116.4074") @RequestParam Double lng) {

        try {
            log.info("逆地理编码请求: lat={}, lng={}", lat, lng);

            // 调用高德地图服务
            AmapService.AddressInfo addressInfo = amapService.geocodeFromLocation(lat, lng, amapKey);

            // 封装返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("province", addressInfo.getAddressComponent().getProvince());
            result.put("city", addressInfo.getAddressComponent().getCity());
            result.put("district", addressInfo.getAddressComponent().getDistrict());
            result.put("township", addressInfo.getAddressComponent().getTownship());
            result.put("formattedAddress", addressInfo.getFormattedAddress());
            result.put("lat", lat);
            result.put("lng", lng);

            log.info("逆地理编码成功: {}", addressInfo.getFormattedAddress());
            return Result.success(result);

        } catch (Exception e) {
            log.error("逆地理编码失败", e);
            return Result.error("逆地理编码失败: " + e.getMessage());
        }
    }

    /**
     * 使用地址进行地理编码
     *
     * 接口：GET /api/amap/geocode-from-address?address=北京市朝阳区&city=北京
     * 参数：address（地址）、city（城市，可选）
     * 返回：经纬度坐标
     *
     * 使用示例：
     * GET /api/amap/geocode-from-address?address=北京市朝阳区&city=北京
     *
     * @param address 地址字符串
     * @param city 城市（可选）
     * @returns 坐标信息
     */
    @GetMapping("/geocode-from-address")
    @Operation(summary = "地理编码", description = "将地址字符串转换为经纬度坐标")
    public Result<Map<String, Object>> geocodeFromAddress(
            @Parameter(description = "地址", example = "北京市朝阳区") @RequestParam String address,
            @Parameter(description = "城市（可选）", example = "北京") @RequestParam(required = false) String city) {

        try {
            log.info("地理编码请求: address={}, city={}", address, city);

            // 调用高德地图服务
            AmapService.CoordinateInfo coordinateInfo = amapService.geocodeFromAddress(address, city, amapKey);

            // 封装返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("lat", coordinateInfo.getLat());
            result.put("lng", coordinateInfo.getLng());
            result.put("address", address);

            log.info("地理编码成功: {} -> ({}, {})", address, coordinateInfo.getLng(), coordinateInfo.getLat());
            return Result.success(result);

        } catch (Exception e) {
            log.error("地理编码失败", e);
            return Result.error("地理编码失败: " + e.getMessage());
        }
    }

    /**
     * POI 搜索
     *
     * 接口：GET /api/amap/search-poi?keywords=餐厅&location=116.4074,39.9042&radius=1000
     * 参数：keywords（关键词）、city（城市）、location（经纬度）、type（类型）、radius（半径）
     * 返回：POI 搜索结果
     *
     * 使用示例：
     * GET /api/amap/search-poi?keywords=餐厅&location=116.4074,39.9042&radius=1000
     *
     * @param keywords 搜索关键词
     * @param city 搜索城市（可选）
     * @param location 中心点经纬度
     * @param type POI 类型（可选）
     * @param radius 搜索半径（可选）
     * @returns POI 搜索结果
     */
    @GetMapping("/search-poi")
    @Operation(summary = "POI 搜索", description = "搜索附近的兴趣点")
    public Result<Map<String, Object>> searchPOI(
            @Parameter(description = "搜索关键词", example = "餐厅") @RequestParam String keywords,
            @Parameter(description = "搜索城市（可选）", example = "北京") @RequestParam(required = false) String city,
            @Parameter(description = "中心点经纬度", example = "116.4074,39.9042") @RequestParam String location,
            @Parameter(description = "POI 类型（可选）", example = "030000") @RequestParam(required = false) String type,
            @Parameter(description = "搜索半径（可选）", example = "1000") @RequestParam(required = false) Integer radius) {

        try {
            log.info("POI 搜索请求: keywords={}, city={}, location={}, type={}, radius={}",
                    keywords, city, location, type, radius);

            // 调用高德地图服务
            AmapService.POISearchResult searchResult = amapService.searchPOI(amapKey, keywords, city, location, type, radius);

            // 封装返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("count", searchResult.getCount());
            result.put("pois", searchResult.getPois());

            log.info("POI 搜索成功，找到 {} 个结果", searchResult.getCount());
            return Result.success(result);

        } catch (Exception e) {
            log.error("POI 搜索失败", e);
            return Result.error("POI 搜索失败: " + e.getMessage());
        }
    }

    /**
     * 地址自动补全
     *
     * 接口：GET /api/amap/auto-complete?keywords=北京
     * 参数：keywords（关键词）
     * 返回：地址补全建议列表
     *
     * 使用示例：
     * GET /api/amap/auto-complete?keywords=北京
     *
     * @param keywords 输入的关键词
     * @returns 地址补全建议列表
     */
    @GetMapping("/auto-complete")
    @Operation(summary = "地址自动补全", description = "根据输入的关键词提供地址补全建议")
    public Result<Map<String, Object>> autoComplete(
            @Parameter(description = "关键词", example = "北京") @RequestParam String keywords) {

        try {
            log.info("地址自动补全请求: keywords={}", keywords);

            // 调用高德地图服务
            AmapService.AddressSuggestion[] suggestions = amapService.autoComplete(amapKey, keywords);

            // 封装返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("count", suggestions != null ? suggestions.length : 0);
            result.put("suggestions", suggestions);

            log.info("地址自动补全成功，找到 {} 个建议", suggestions != null ? suggestions.length : 0);
            return Result.success(result);

        } catch (Exception e) {
            log.error("地址自动补全失败", e);
            return Result.error("地址自动补全失败: " + e.getMessage());
        }
    }
}
