package com.geofile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户地理位置表
 * @TableName t_user_location
 */
@TableName(value ="t_user_location")
@Data
public class UserLocation {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID(匿名用户用UUID)
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 纬度
     */
    @TableField(value = "lat")
    private Double lat;

    /**
     * 经度
     */
    @TableField(value = "lng")
    private Double lng;

    /**
     * 区域信息
     */
    @TableField(value = "region")
    private String region;

    /**
     * 城市
     */
    @TableField(value = "city")
    private String city;

    /**
     * 省份
     */
    @TableField(value = "province")
    private String province;

    /**
     * 定位方式: AUTO/IP/GPS
     */
    @TableField(value = "location_type")
    private String locationType;

    /**
     * 
     */
    @TableField(value = "update_time")
    private Date updateTime;
}