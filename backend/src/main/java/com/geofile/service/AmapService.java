package com.geofile.service;

import cn.hutool.core.util.CoordinateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 高德地图 API 服务
 *
 * 高德地图 Web API 使用说明：
 * 1. 开发者需要在高德开放平台注册账号并申请 Key
 * 2. 前往：https://console.amap.com/dev/key/app
 * 3. 选择"Web端（JS API）"或"Web服务 API"密钥
 * 4. 配置 Referer 白名单（可选）
 *
 * 官方文档：
 * - Web 服务 API: https://lbs.amap.com/api/webservice/guide/api
 * - POI 搜索: https://lbs.amap.com/api/webservice/guide/api/search
 * - 地理编码: https://lbs.amap.com/api/webservice/guide/api/geocode
 * - 逆地理编码: https://lbs.amap.com/api/webservice/guide/api/regeo
 */
public interface AmapService {

    /**
     * 使用经纬度逆地理编码（获取地址信息）
     *
     * 说明：
     * 输入经纬度坐标，返回该坐标对应的省市街道信息
     * 这是获取地理位置的详细信息的主要方法
     *
     * @param lat 纬度
     * @param lng 经度
     * @param key 高德地图 API Key
     * @return 地址信息
     */
    AddressInfo geocodeFromLocation(Double lat, Double lng, String key);

    /**
     * 使用地址进行地理编码（获取经纬度）
     *
     * 说明：
     * 输入地址字符串（如"北京市朝阳区"），返回对应的经纬度坐标
     *
     * @param address 地址字符串
     * @param city 城市（可选，如果地址包含城市信息则不需要）
     * @param key 高德地图 API Key
     * @return 经纬度坐标
     */
    CoordinateInfo geocodeFromAddress(String address, String city, String key);

    /**
     * POI 搜索（搜索附近的兴趣点）
     *
     * 说明：
     * 在指定位置搜索特定类型的 POI（Point of Interest）
     * 例如：搜索餐厅、加油站、银行等
     *
     * 参数说明：
     * - keyword: 搜索关键词（必填），如"餐厅"、"加油站"
     * - city: 搜索城市（可选）
     * - location: 中心点经纬度（必填），格式："经度,纬度"
     * - type: POI 类型（可选）
     * - radius: 搜索半径（可选，默认5000米）
     * - offset: 返回结果数量（可选，默认20）
     *
     * @param key 高德地图 API Key
     * @param keyword 搜索关键词
     * @param city 搜索城市
     * @param location 中心点经纬度
     * @param type POI 类型
     * @param radius 搜索半径
     * @return POI 搜索结果
     */
    POISearchResult searchPOI(String key, String keyword, String city, String location,
                              String type, Integer radius);

    /**
     * 根据输入框自动补全地址
     *
     * 说明：
     * 用户输入地址时提供自动补全建议
     * 例如：输入"北京市"会返回"北京市朝阳区"、"北京市海淀区"等
     *
     * @param key 高德地图 API Key
     * @param keyWord 输入的关键词
     * @return 地址补全建议列表
     */
    AddressSuggestion[] autoComplete(String key, String keyWord);

    // ========== 内部数据模型 ==========

    /**
     * 地址信息模型
     */
    @Data
    @Schema(description = "地址信息")
    class AddressInfo {
        /**
         * 地址信息对象
         */
        private AddressComponent addressComponent;
        /**
         * 格式化的地址信息
         */
        private String formattedAddress;
        /**
         * 位置坐标
         */
        private CoordinateUtil.Coordinate coordinate;
    }

    /**
     * 地址组件信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "地址组件信息")
    class AddressComponent {
        /**
         * 省份
         */
        private String province;
        /**
         * 城市
         */
        private String city;
        /**
         * 区县
         */
        private String district;
        /**
         * 街道
         */
        private String township;
        /**
         * 街道门牌号
         */
        private String streetNumber;
        /**
         * 经纬度
         */
        private Double lng;
        /**
         * 纬度
         */
        private Double lat;
    }

    /**
     * 坐标信息模型
     */
    @Data
    @Schema(description = "坐标信息")
    class CoordinateInfo {
        /**
         * 经度
         */
        private Double lng;
        /**
         * 纬度
         */
        private Double lat;
    }

    /**
     * POI 搜索结果
     */
    @Data
    @Schema(description = "POI搜索结果")
    class POISearchResult {
        /**
         * POI 列表
         */
        private POIResult[] pois;
        /**
         * 总数
         */
        private Integer count;
    }

    /**
     * POI 信息
     */
    @Data
    @Schema(description = "POI信息")
    class POIResult {
        /**
         * POI 名称
         */
        private String name;
        /**
         * POI 类型
         */
        private String type;
        /**
         * 地址
         */
        private String address;
        /**
         * 经度
         */
        private Double lng;
        /**
         * 纬度
         */
        private Double lat;
        /**
         * 电话
         */
        private String tel;
        /**
         * 缩略图
         */
        private String thumbPhoto;
    }

    /**
     * 地址补全建议
     */
    @Data
    @Schema(description = "地址补全建议")
    class AddressSuggestion {
        /**
         * 建议地址
         */
        private String address;
        /**
         * 经度
         */
        private Double lng;
        /**
         * 纬度
         */
        private Double lat;
    }
}
