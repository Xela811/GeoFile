package com.geofile.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.geofile.entity.VerificationCode;
import com.geofile.service.VerificationCodeService;
import com.geofile.mapper.VerificationCodeMapper;
import org.springframework.stereotype.Service;

/**
* @author xela
* @description 针对表【t_verification_code(验证码表)】的数据库操作Service实现
* @createDate 2026-02-10 23:30:13
*/
@Service
public class VerificationCodeServiceImpl extends ServiceImpl<VerificationCodeMapper, VerificationCode>
    implements VerificationCodeService{

}




