package com.geofile.mapper;

import com.geofile.entity.File;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
* @author xela
* @description 针对表【t_file(文件信息表)】的数据库操作Mapper
* @createDate 2026-02-10 23:30:13
* @Entity com.geofile.entity.File
*/
public interface FileMapper extends BaseMapper<File> {

    /**
     * 根据条件搜索文件（用于分页查询）
     * @param keyword 关键词（文件名）
     * @param fileType 文件类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param status 状态
     * @param storageType 存储类型
     * @param lat 纬度（用于地理位置筛选）
     * @param lng 经度（用于地理位置筛选）
     * @param radius 半径（用于地理位置筛选）
     * @return 文件列表
     */
    @Select("<script>" +
            "SELECT * FROM t_file " +
            "WHERE deleted = 0 " +
            "AND status = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND file_name LIKE CONCAT('%', #{keyword}, '%') " +
            "</if>" +
            "<if test='fileType != null and fileType != \"\"'>" +
            "AND file_type = #{fileType} " +
            "</if>" +
            "<if test='startTime != null and startTime != \"\"'>" +
            "AND DATE_FORMAT(upload_time, '%Y-%m-%d %H:%i') >= #{startTime} " +
            "</if>" +
            "<if test='endTime != null and endTime != \"\"'>" +
            "AND DATE_FORMAT(upload_time, '%Y-%m-%d %H:%i') &lt;= #{endTime} " +
            "</if>" +
            "<if test='storageType != null and storageType != \"\"'>" +
            "AND storage_type = #{storageType} " +
            "</if>" +
            "<if test='lat != null and lng != null'>" +
            "AND location_lat IS NOT NULL " +
            "AND location_lng IS NOT NULL " +
            "AND location_lat BETWEEN #{lat} - (#{radius}/111000) AND #{lat} + (#{radius}/111000) " +
            "AND location_lng BETWEEN #{lng} - (#{radius}/111000 / COS(RADIANS(#{lat}))) AND #{lng} + (#{radius}/111000 / COS(RADIANS(#{lat}))) " +
            "</if>" +
            "ORDER BY upload_time DESC" +
            "</script>")
    List<File> selectFilesByCondition(
            @Param("keyword") String keyword,
            @Param("fileType") String fileType,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("status") Integer status,
            @Param("storageType") String storageType,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Integer radius
    );

    /**
     * 根据条件统计文件数量
     * @param keyword 关键词（文件名）
     * @param fileType 文件类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param status 状态
     * @param storageType 存储类型
     * @param lat 纬度（用于地理位置筛选）
     * @param lng 经度（用于地理位置筛选）
     * @param radius 半径（用于地理位置筛选）
     * @return 文件数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM t_file " +
            "WHERE deleted = 0 " +
            "AND status = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND file_name LIKE CONCAT('%', #{keyword}, '%') " +
            "</if>" +
            "<if test='fileType != null and fileType != \"\"'>" +
            "AND file_type = #{fileType} " +
            "</if>" +
            "<if test='startTime != null and startTime != \"\"'>" +
            "AND DATE_FORMAT(upload_time, '%Y-%m-%d %H:%i') >= #{startTime} " +
            "</if>" +
            "<if test='endTime != null and endTime != \"\"'>" +
            "AND DATE_FORMAT(upload_time, '%Y-%m-%d %H:%i') &lt;= #{endTime} " +
            "</if>" +
            "<if test='storageType != null and storageType != \"\"'>" +
            "AND storage_type = #{storageType} " +
            "</if>" +
            "<if test='lat != null and lng != null'>" +
            "AND location_lat IS NOT NULL " +
            "AND location_lng IS NOT NULL " +
            "AND location_lat BETWEEN #{lat} - (#{radius}/111000) AND #{lat} + (#{radius}/111000) " +
            "AND location_lng BETWEEN #{lng} - (#{radius}/111000 / COS(RADIANS(#{lat}))) AND #{lng} + (#{radius}/111000 / COS(RADIANS(#{lat}))) " +
            "</if>" +
            "</script>")
    Long countFilesByCondition(
            @Param("keyword") String keyword,
            @Param("fileType") String fileType,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("status") Integer status,
            @Param("storageType") String storageType,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Integer radius
    );
}




