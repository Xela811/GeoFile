package com.geofile.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.geofile.entity.VerificationCode;

import java.util.Map;

/**
 * 验证码服务接口
 * 提供验证码的生成、验证、绑定等功能
 * @author xela
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
     * 生成5位字母数字下载验证码
     * @return 验证码字符串
     */
    String generateDownloadCode();

    /**
     * 保存下载验证码到数据库和Redis
     * @param code 验证码
     * @param expireMinutes 过期时间（分钟）
     * @return 保存是否成功
     */
    boolean saveDownloadCode(String code, int expireMinutes);

    /**
     * 验证下载验证码
     * @param code 验证码
     * @return 验证结果
     */
    boolean verifyDownloadCode(String code);

    /**
     * 根据验证码获取文件信息
     * @param code 验证码
     * @return uploadToken，用于查找文件
     */
    String getUploadTokenByCode(String code);

    /**
     * 通过验证码查找文件列表
     * @param code 验证码
     * @return 验证码对应的uploadToken
     */
    String findUploadTokenByCode(String code);

    /**
     * 根据上传令牌获取验证码
     * @param uploadToken 上传令牌
     * @return 下载验证码
     */
    String getCodeByUploadToken(String uploadToken);

    /**
     * 删除验证码（使用后失效）
     * @param code 验证码
     */
    void deleteCode(String code);

    /**
     * 检查验证码是否有效
     * @param code 验证码
     * @return 是否有效
     */
    boolean isCodeValid(String code);

}
