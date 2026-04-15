package com.geofile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.geofile.entity.VerificationCode;
import com.geofile.service.VerificationCodeService;
import com.geofile.mapper.VerificationCodeMapper;
import com.geofile.util.JwtUtil;
import com.geofile.util.RedisUtil;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现类
 * 与FileUploadServiceImpl配合，提供验证码的生成、验证和绑定功能
 * @author xela
 * @createDate 2026-02-10 23:30:13
 */
@Service
public class VerificationCodeServiceImpl extends ServiceImpl<VerificationCodeMapper, VerificationCode>
    implements VerificationCodeService {

    @Autowired
    private RedisUtil redisUtil;

    // 验证码常量
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 5;
    private static final int CAPTCHA_LENGTH = 4;
    private static final int CAPTCHA_WIDTH = 120;
    private static final int CAPTCHA_HEIGHT = 40;
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;
    private static final int DOWNLOAD_CODE_EXPIRE_MINUTES = 30;

    // Redis前缀
    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String FILE_DOWNLOAD_PREFIX = "file:download:";

    @Override
    public Map<String, Object> generateCaptcha() {
        // 1. 定义验证码所使用的字符范围
        String codeBase = CHARACTERS;
        RandomGenerator generator = new RandomGenerator(codeBase, CAPTCHA_LENGTH);

        // 2. 创建验证码对象（以线段干扰为例）
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(CAPTCHA_WIDTH, CAPTCHA_HEIGHT);
        captcha.setGenerator(generator); // 设置自定义字符生成器

        // 3. 获取验证码内容
        String code = captcha.getCode();
        String captchaKey = UUID.randomUUID().toString();

        // 4. 存入 Redis
        String redisKey = CAPTCHA_PREFIX + captchaKey;
        redisUtil.set(redisKey, code, CAPTCHA_EXPIRE_MINUTES * 60, TimeUnit.SECONDS);

        // 5. 获取 Base64 编码
        String base64 = captcha.getImageBase64Data();

        // 6. 组装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("captchaKey", captchaKey);
        result.put("image", base64);
        result.put("expireMinutes", CAPTCHA_EXPIRE_MINUTES);

        return result;
    }

    @Override
    public boolean verifyCaptcha(String captchaKey, String code) {
        String redisKey = CAPTCHA_PREFIX + captchaKey;
        String correctCode = (String) redisUtil.get(redisKey);

        if (correctCode == null) {
            return false;
        }

        boolean isValid = correctCode.equalsIgnoreCase(code); // 不区分大小写
        if (isValid) {
            redisUtil.del(redisKey);
        }

        return isValid;
    }

    @Override
    public String generateDownloadCode() {
        // 生成5位字母数字验证码
        return generateRandomCode(CODE_LENGTH);
    }

    @Override
    public boolean saveDownloadCode(String code, int expireMinutes) {
        try {
            // 计算过期时间
            Date expireTime = new Date(System.currentTimeMillis() + expireMinutes * 60 * 1000L);

            // 保存到数据库
            VerificationCode verificationCode = new VerificationCode();
            verificationCode.setCode(code);
            verificationCode.setType("DOWNLOAD");
            verificationCode.setExpireTime(expireTime);
            verificationCode.setMaxAttempts(0); // 下载验证码不限制尝试次数
            verificationCode.setAttemptCount(0);
            verificationCode.setIsUsed(0);
            verificationCode.setCreatedTime(new Date());
            this.save(verificationCode);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean verifyDownloadCode(String code) {
        LambdaQueryWrapper<VerificationCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VerificationCode::getCode, code)
                   .eq(VerificationCode::getType, "DOWNLOAD")
                   .eq(VerificationCode::getIsUsed, 0)
                   .gt(VerificationCode::getExpireTime, new Date());

        VerificationCode verificationCode = this.getOne(queryWrapper);

        if (verificationCode != null) {
            // 标记为已使用
            verificationCode.setIsUsed(1);
            this.updateById(verificationCode);
            return true;
        }

        return false;
    }

    @Override
    public String getUploadTokenByCode(String code) {
        String redisKey = FILE_DOWNLOAD_PREFIX + code;
        return (String) redisUtil.get(redisKey);
    }

    @Override
    public String findUploadTokenByCode(String code) {
        return getUploadTokenByCode(code);
    }

    @Override
    public String getCodeByUploadToken(String uploadToken) {
        String redisKey = "upload:files:" + uploadToken;
        return (String) redisUtil.get(redisKey);
    }

    @Override
    public void deleteCode(String code) {
        // 从数据库中标记为已使用
        LambdaQueryWrapper<VerificationCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VerificationCode::getCode, code)
                   .eq(VerificationCode::getType, "DOWNLOAD");

        VerificationCode verificationCode = this.getOne(queryWrapper);
        if (verificationCode != null) {
            verificationCode.setIsUsed(1);
            this.updateById(verificationCode);
        }

        // 从Redis中删除
        String redisKey = FILE_DOWNLOAD_PREFIX + code;
        redisUtil.del(redisKey);
    }

    @Override
    public boolean isCodeValid(String code) {
        LambdaQueryWrapper<VerificationCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VerificationCode::getCode, code)
                   .eq(VerificationCode::getType, "DOWNLOAD")
                   .eq(VerificationCode::getIsUsed, 0)
                   .gt(VerificationCode::getExpireTime, new Date());

        return this.getOne(queryWrapper) != null;
    }

    /**
     * 生成随机验证码
     */
    private String generateRandomCode(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }

        return sb.toString();
    }
}




