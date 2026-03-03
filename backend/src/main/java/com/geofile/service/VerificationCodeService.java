package com.geofile.service;

import com.geofile.entity.VerificationCode;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
* @author xela
* @description 针对表【t_verification_code(验证码表)】的数据库操作Service
* @createDate 2026-02-10 23:30:13
*/
public interface VerificationCodeService extends IService<VerificationCode> {

    /**
     * 生成图形验证码
     * @return 验证码信息（包含图片Base64和captchaKey）
     */
    Map<String, Object> generateCaptcha();

    /**
     * 验证图形验证码
     * @param captchaKey 验证码Key
     * @param code 验证码
     * @return 验证结果
     */
    boolean verifyCaptcha(String captchaKey, String code);

    /**
     * 生成下载验证码Token
     * @return JWT Token
     */
    String generateDownloadToken();

    /**
     * 验证下载Token
     * @param token JWT Token
     * @return 验证结果
     */
    boolean verifyDownloadToken(String token);

}
