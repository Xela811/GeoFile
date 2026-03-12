package com.geofile.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 地理位置请求实体
 * 用于接收前端发送的地理信息
 */
@Data
@Schema(description = "地理位置请求")
public class LocationRequest {

    /**
     * 纬度
     * 范围：-90 到 90
     */
    @Schema(description = "纬度", example = "39.9042")
    private Double lat;

    /**
     * 经度
     * 范围：-180 到 180
     */
    @Schema(description = "经度", example = "116.4074")
    private Double lng;

    /**
     * 搜索半径（米）
     * 默认：1000米
     */
    @Schema(description = "搜索半径（米）", example = "1000")
    private Integer radius;

    /**
     * 区域信息
     * 如：朝阳区、浦东新区
     */
    @Schema(description = "区域信息", example = "朝阳区")
    private String region;

    /**
     * 城市
     * 如：北京、上海
     */
    @Schema(description = "城市", example = "北京")
    private String city;

    /**
     * 省份
     * 如：北京市、上海市
     */
    @Schema(description = "省份", example = "北京市")
    private String province;

    /**
     * POI搜索关键词（可选）
     * 用于搜索特定类型的地点
     * 例如：餐厅、银行、加油站、医院等
     */
    @Schema(description = "POI搜索关键词", example = "餐厅")
    private String keyword;

    /**
     * POI搜索类型（可选）
     * 高德地图 POI 类型分类：
     *
     * 商务住宅 (010):
     *   - 写字楼、写字楼大厦、商业综合体、住宅、小区
     *
     * 休闲娱乐 (020):
     *   - 酒吧、KTV、影院、健身房、游乐园、公园、风景区
     *
     * 饮食服务 (030):
     *   - 餐厅、快餐、火锅、中餐、西餐、咖啡厅、甜品店
     *
     * 道路附属设施 (040):
     *   - 公交站、地铁站、停车场、加油站、充电桩
     *
     * 交通设施服务 (050):
     *   - 机场、火车站、汽车站、码头
     *
     * 医疗保健服务 (060):
     *   - 医院、诊所、药店、体检中心
     *
     * 公司企业 (070):
     *   - 公司、企业、政府部门、事业单位
     *
     * 地名地址信息 (080):
     *   - 村庄、街道、道路、建筑物
     */
    @Schema(description = "POI搜索类型", example = "030")
    private String poiType;
}
