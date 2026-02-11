package com.geofile.mapper;
import org.apache.ibatis.annotations.Param;

import com.geofile.entity.UserLocation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author xela
* @description 针对表【t_user_location(用户地理位置表)】的数据库操作Mapper
* @createDate 2026-02-10 23:30:13
* @Entity com.geofile.entity.UserLocation
*/
public interface UserLocationMapper extends BaseMapper<UserLocation> {

    int deleteByCity(@Param("city") String city);
}




