package com.geofile.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.geofile.pojo.UserLocation;
import com.geofile.service.UserLocationService;
import com.geofile.mapper.UserLocationMapper;
import org.springframework.stereotype.Service;

/**
* @author xela
* @description 针对表【t_user_location(用户地理位置表)】的数据库操作Service实现
* @createDate 2026-02-10 23:30:13
*/
@Service
public class UserLocationServiceImpl extends ServiceImpl<UserLocationMapper, UserLocation>
    implements UserLocationService{

}




