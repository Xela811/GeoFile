package com.geofile.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.geofile.service.AmapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cn.hutool.core.util.CoordinateUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 高德地图 API 服务实现
 *
 * 使用 Hutool 的 HttpUtil 进行 HTTP 请求
 * 使用 FastJSON2 进行 JSON 解析
 *
 * 配置说明：
 * 在 application.properties 或 application.yml 中配置：
 * amap.key=你的高德地图API Key
 * amap.url=https://restapi.amap.com/v3
 */
@Slf4j
@Service
public class AmapServiceImpl implements AmapService {

    /**
     * 高德地图 Web 服务 API 地址
     */
    @Value("${amap.url:https://restapi.amap.com/v3}")
    private String amapUrl;

    /**
     * 高德地图 API Key
     * 从高德开放平台获取：
     * https://console.amap.com/dev/key/app
     */
    @Value("${amap.key:}")
    private String amapKey;

    // ========== 逆地理编码（经纬度转地址） ==========

    /**
     * 使用经纬度逆地理编码
     *
     * API 接口：https://restapi.amap.com/v3/geocode/regeo
     *
     * 实现步骤：
     * 1. 构造 HTTP 请求 URL
     * 2. 添加必要参数（key, location, radius, extensions）
     * 3. 发送 GET 请求
     * 4. 解析 JSON 响应
     * 5. 提取地址信息
     *
     * @param lat 纬度
     * @param lng 经度
     * @param key 高德地图 API Key
     * @return 地址信息
     */
    @Override
    public AddressInfo geocodeFromLocation(Double lat, Double lng, String key) {
        // 验证参数
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("高德地图 API Key 不能为空");
        }

        if (lat == null || lng == null) {
            throw new IllegalArgumentException("经纬度不能为空");
        }

        try {
            // 1. 构造请求 URL
            // 参数说明：
            // - key: API 密钥
            // - location: 经纬度坐标，格式为"经度,纬度"
            // - radius: 搜索半径，默认5000米
            // - extensions: 返回详细程度，all 返回详细地址信息
            String url = String.format("%s/geocode/regeo?key=%s&location=%s,%s&radius=5000&extensions=all",
                    amapUrl, key, lng, lat);

            log.debug("高德地图逆地理编码请求: {}", url);

            // 2. 发送 GET 请求
            // 使用 Hutool 的 HttpUtil
            String response = cn.hutool.http.HttpUtil.get(url);

            log.debug("高德地图逆地理编码响应: {}", response);

            // 3. 解析 JSON 响应
            JSONObject jsonResponse = JSON.parseObject(response);

            // 4. 检查响应状态
            if (!"1".equals(jsonResponse.getString("status"))) {
                String info = jsonResponse.getString("info");
                String infocode = jsonResponse.getString("infocode");
                log.error("高德地图逆地理编码失败: info={}, infocode={}", info, infocode);
                throw new RuntimeException("逆地理编码失败: " + info + " (" + infocode + ")");
            }

            // 5. 提取地址信息
            AddressInfo addressInfo = new AddressInfo();

            JSONObject regeocode = jsonResponse.getJSONObject("regeocode");

// 修复点 1: 字段名映射。高德 API 返回的是下划线格式 "formatted_address"
            addressInfo.setFormattedAddress(regeocode.getString("formatted_address"));

// 修复点 2: 简化地址组件解析
            JSONObject addressComponent = regeocode.getJSONObject("addressComponent");
            AddressComponent component = new AddressComponent();
            component.setProvince(addressComponent.getString("province"));

// 重点：处理直辖市。北京/上海等城市 city 字段会返回空数组 []
            Object cityObj = addressComponent.get("city");
            component.setCity(cityObj instanceof String ? (String) cityObj : "");

            component.setDistrict(addressComponent.getString("district"));
            component.setTownship(addressComponent.getString("township"));

// 修复点 3: 街道门牌号解析。streetNumber 在高德返回中是一个对象，不是字符串
            JSONObject streetNumberObj = addressComponent.getJSONObject("streetNumber");
            if (streetNumberObj != null) {
                String sn = streetNumberObj.getString("street") + streetNumberObj.getString("number");
                component.setStreetNumber(sn);
            }

// 修复点 4: 直接使用传入的经纬度，不要去 JSON 里找
            addressInfo.setAddressComponent(component);
            addressInfo.setCoordinate(new CoordinateUtil.Coordinate(lng, lat));

            log.info("逆地理编码成功: {}", addressInfo.getFormattedAddress());
            return addressInfo;

        } catch (Exception e) {
            log.error("逆地理编码异常", e);
            throw new RuntimeException("逆地理编码失败: " + e.getMessage(), e);
        }
    }

    // ========== 地理编码（地址转经纬度） ==========

    /**
     * 使用地址进行地理编码
     *
     * API 接口：https://restapi.amap.com/v3/geocode/geo
     *
     * @param address 地址字符串
     * @param city 城市（可选）
     * @param key 高德地图 API Key
     * @return 经纬度坐标
     */
    @Override
    public CoordinateInfo geocodeFromAddress(String address, String city, String key) {
        // 验证参数
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("高德地图 API Key 不能为空");
        }

        if (StrUtil.isBlank(address)) {
            throw new IllegalArgumentException("地址不能为空");
        }

        try {
            // 构造请求 URL
            // 参数说明：
            // - key: API 密钥
            // - address: 地址（必填）
            // - city: 城市（可选），如果 address 包含城市信息则不需要
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(amapUrl).append("/geocode/geo?key=").append(key).append("&address=").append(address);

            if (StrUtil.isNotBlank(city)) {
                urlBuilder.append("&city=").append(city);
            }

            String url = urlBuilder.toString();
            log.debug("高德地图地理编码请求: {}", url);

            // 发送 GET 请求
            String response = cn.hutool.http.HttpUtil.get(url);
            log.debug("高德地图地理编码响应: {}", response);

            // 解析 JSON 响应
            JSONObject jsonResponse = JSON.parseObject(response);

            // 检查响应状态
            if (!"1".equals(jsonResponse.getString("status"))) {
                String info = jsonResponse.getString("info");
                log.error("高德地图地理编码失败: {}", info);
                throw new RuntimeException("地理编码失败: " + info);
            }

            // 提取第一个结果的经纬度
            JSONArray geocodes = jsonResponse.getJSONArray("geocodes");
            if (geocodes == null || geocodes.isEmpty()) {
                throw new RuntimeException("未找到匹配的地址");
            }

            JSONObject firstResult = geocodes.getJSONObject(0);
            JSONObject location = firstResult.getJSONObject("location");

            CoordinateInfo coordinateInfo = new CoordinateInfo();
            coordinateInfo.setLng(location.getDouble("lng"));
            coordinateInfo.setLat(location.getDouble("lat"));

            log.info("地理编码成功: {} -> {}", address, coordinateInfo);

            return coordinateInfo;

        } catch (Exception e) {
            log.error("地理编码异常", e);
            throw new RuntimeException("地理编码失败: " + e.getMessage(), e);
        }
    }

    // ========== POI 搜索 ==========

    /**
     * POI 搜索
     *
     * API 接口：https://restapi.amap.com/v3/place/text
     *
     * @param key 高德地图 API Key
     * @param keyword 搜索关键词
     * @param city 搜索城市
     * @param location 中心点经纬度
     * @param type POI 类型
     * @param radius 搜索半径
     * @return POI 搜索结果
     */
    @Override
    public POISearchResult searchPOI(String key, String keyword, String city, String location,
                                    String type, Integer radius) {
        // 验证参数
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("高德地图 API Key 不能为空");
        }

        if (StrUtil.isBlank(keyword)) {
            throw new IllegalArgumentException("搜索关键词不能为空");
        }

        if (StrUtil.isBlank(location)) {
            throw new IllegalArgumentException("搜索位置不能为空");
        }

        try {
            // 构造请求 URL
            // 参数说明：
            // - key: API 密钥
            // - keywords: 搜索关键词
            // - city: 搜索城市
            // - location: 中心点经纬度
            // - type: POI 类型
            // - radius: 搜索半径
            // - offset: 返回结果数量
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(amapUrl)
                    .append("/place/text?key=").append(key)
                    .append("&keywords=").append(keyword)
                    .append("&location=").append(location)
                    .append("&offset=20");

            if (StrUtil.isNotBlank(city)) {
                urlBuilder.append("&city=").append(city);
            }

            if (StrUtil.isNotBlank(type)) {
                urlBuilder.append("&type=").append(type);
            }

            if (radius != null && radius > 0) {
                urlBuilder.append("&radius=").append(radius);
            }

            String url = urlBuilder.toString();
            log.debug("高德地图 POI 搜索请求: {}", url);

            // 发送 GET 请求
            String response = cn.hutool.http.HttpUtil.get(url);
            log.debug("高德地图 POI 搜索响应: {}", response);

            // 解析 JSON 响应
            JSONObject jsonResponse = JSON.parseObject(response);

            // 检查响应状态
            if (!"1".equals(jsonResponse.getString("status"))) {
                String info = jsonResponse.getString("info");
                log.warn("高德地图 POI 搜索失败: {}", info);
                // 不抛出异常，返回空结果
            }

            // 提取 POI 列表
            POISearchResult result = new POISearchResult();
            result.setCount(jsonResponse.getInteger("count"));

            JSONArray pois = jsonResponse.getJSONArray("pois");
            if (pois != null && !pois.isEmpty()) {
                List<POIResult> poiList = new ArrayList<>();
                for (int i = 0; i < pois.size(); i++) {
                    JSONObject poi = pois.getJSONObject(i);
                    POIResult poiResult = new POIResult();
                    poiResult.setName(poi.getString("name"));
                    poiResult.setType(poi.getString("type"));
                    poiResult.setAddress(poi.getString("address"));
                    poiResult.setLng(poi.getJSONObject("location").getDouble("lng"));
                    poiResult.setLat(poi.getJSONObject("location").getDouble("lat"));
                    poiResult.setTel(poi.getString("tel"));
                    poiResult.setThumbPhoto(poi.getString("thumbPhoto"));
                    poiList.add(poiResult);
                }
                result.setPois(poiList.toArray(new POIResult[0]));
            }

            log.info("POI 搜索成功，找到 {} 个结果", result.getCount());
            return result;

        } catch (Exception e) {
            log.error("POI 搜索异常", e);
            throw new RuntimeException("POI 搜索失败: " + e.getMessage(), e);
        }
    }

    // ========== 地址自动补全 ==========

    /**
     * 地址自动补全
     *
     * API 接口：https://restapi.amap.com/v3/assistant/inputtip
     *
     * @param key 高德地图 API Key
     * @param keyWord 输入的关键词
     * @return 地址补全建议列表
     */
    @Override
    public AddressSuggestion[] autoComplete(String key, String keyWord) {
        // 验证参数
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("高德地图 API Key 不能为空");
        }

        if (StrUtil.isBlank(keyWord)) {
            return new AddressSuggestion[0];
        }

        try {
            // 构造请求 URL
            String url = String.format("%s/assistant/inputtip?key=%s&keywords=%s",
                    amapUrl, key, keyWord);

            log.debug("高德地图地址自动补全请求: {}", url);

            // 发送 GET 请求
            String response = cn.hutool.http.HttpUtil.get(url);
            log.debug("高德地图地址自动补全响应: {}", response);

            // 解析 JSON 响应
            JSONObject jsonResponse = JSON.parseObject(response);

            // 检查响应状态
            if (!"1".equals(jsonResponse.getString("status"))) {
                log.warn("高德地图地址自动补全失败");
                return new AddressSuggestion[0];
            }

            // 提取建议列表
            JSONArray tips = jsonResponse.getJSONArray("tips");
            if (tips == null || tips.isEmpty()) {
                return new AddressSuggestion[0];
            }

            List<AddressSuggestion> suggestionList = new ArrayList<>();
            for (int i = 0; i < tips.size(); i++) {
                JSONObject tip = tips.getJSONObject(i);
                AddressSuggestion suggestion = new AddressSuggestion();
                suggestion.setAddress(tip.getString("adm1") + tip.getString("adm2") + tip.getString("address"));
                suggestion.setLng(tip.getJSONObject("location").getDouble("lng"));
                suggestion.setLat(tip.getJSONObject("location").getDouble("lat"));
                suggestionList.add(suggestion);
            }

            log.info("地址自动补全成功，找到 {} 个建议", suggestionList.size());
            return suggestionList.toArray(new AddressSuggestion[0]);

        } catch (Exception e) {
            log.error("地址自动补全异常", e);
            throw new RuntimeException("地址自动补全失败: " + e.getMessage(), e);
        }
    }

    // ========== 便捷方法：搜索特定类型的 POI ==========

    /**
     * 搜索周边地铁站点
     */
    @Override
    public POIResult[] searchSubwayStations(String key, String location, Integer radius) {
        POISearchResult result = searchPOI(key, "地铁站", null, location, "080201", radius != null ? radius : 5000);
        return result.getPois();
    }

    /**
     * 搜索周边医院
     */
    @Override
    public POIResult[] searchHospitals(String key, String location, Integer radius) {
        POISearchResult result = searchPOI(key, "医院", null, location, "060000", radius != null ? radius : 5000);
        return result.getPois();
    }

    /**
     * 搜索周边餐厅
     */
    @Override
    public POIResult[] searchRestaurants(String key, String location, Integer radius) {
        POISearchResult result = searchPOI(key, "餐厅", null, location, "030000", radius != null ? radius : 5000);
        return result.getPois();
    }
}
