package com.geofile.service.impl;

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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
* @author xela
* @description 针对表【t_verification_code(验证码表)】的数据库操作Service实现
* @createDate 2026-02-10 23:30:13
*/
@Service
public class VerificationCodeServiceImpl extends ServiceImpl<VerificationCodeMapper, VerificationCode>
    implements VerificationCodeService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Value("${jwt.secret}")
    private String secret;

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final int CAPTCHA_LENGTH = 4;
    private static final int CAPTCHA_WIDTH = 120;
    private static final int CAPTCHA_HEIGHT = 40;
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;

//    @Override
//    public Map<String, Object> generateCaptcha() {
//        SpecCaptcha captcha = new SpecCaptcha(CAPTCHA_WIDTH, CAPTCHA_HEIGHT, CAPTCHA_LENGTH);
//        captcha.setCharType(com.wf.captcha.CaptchaTypeProperty.defaultProperty);
//        captcha.setCharSpace(3);
//        captcha.setLen(CAPTCHA_LENGTH);
//        captcha.setCharFactory(new com.wf.captcha.impl.DefaultCharFactory() {
//            @Override
//            protected char[] randomChars() {
//                String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
//                Random random = new Random();
//                char[] result = new char[CAPTCHA_LENGTH];
//                for (int i = 0; i < CAPTCHA_LENGTH; i++) {
//                    result[i] = chars.charAt(random.nextInt(chars.length()));
//                }
//                return result;
//            }
//        });
//
//        String code = captcha.text().trim();
//        String captchaKey = UUID.randomUUID().toString();
//
//        String redisKey = CAPTCHA_PREFIX + captchaKey;
//        redisUtil.set(redisKey, code, CAPTCHA_EXPIRE_MINUTES * 60);
//
//        String base64 = "data:image/gif;base64," + captcha.toBase64();
//
//        Map<String, Object> result = new HashMap<>();
//        result.put("captchaKey", captchaKey);
//        result.put("image", base64);
//        result.put("expireMinutes", CAPTCHA_EXPIRE_MINUTES);
//
//        return result;
//    }
@Override
public Map<String, Object> generateCaptcha() {
    // 1. 定义验证码所使用的字符范围（对应你原代码中的 chars）
    String codeBase = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    RandomGenerator generator = new RandomGenerator(codeBase, CAPTCHA_LENGTH);

    // 2. 创建验证码对象（以线段干扰为例，参数：宽、高、字符数、干扰线数）
    // 你也可以选择 CaptchaUtil.createShearCaptcha (扭曲效果)
    LineCaptcha captcha = CaptchaUtil.createLineCaptcha(CAPTCHA_WIDTH, CAPTCHA_HEIGHT);
    captcha.setGenerator(generator); // 设置自定义字符生成器

    // 3. 获取验证码内容（用于存入 Redis）
    String code = captcha.getCode();
    String captchaKey = UUID.randomUUID().toString();

    // 4. 存入 Redis
    String redisKey = CAPTCHA_PREFIX + captchaKey;
    redisUtil.set(redisKey, code, CAPTCHA_EXPIRE_MINUTES * 60, TimeUnit.SECONDS);

    // 5. 获取 Base64 编码
    // Hutool 的 getImageBase64Data() 已经包含了 "data:image/png;base64," 前缀
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

        boolean isValid = correctCode.equals(code);
        if (isValid) {
            redisUtil.del(redisKey);
        }

        return isValid;
    }

    @Override
    public String generateDownloadToken() {
        String code = generateRandomCode(6);
        String token = jwtUtil.generateToken(code);
        String redisKey = CAPTCHA_PREFIX + "download:" + code;
        redisUtil.set(redisKey, code, CAPTCHA_EXPIRE_MINUTES * 60,TimeUnit.SECONDS);
        return token;
    }

    @Override
    public boolean verifyDownloadToken(String token) {
        String code = jwtUtil.getCodeFromToken(token);
        if (code == null) {
            return false;
        }

        String redisKey = CAPTCHA_PREFIX + "download:" + code;
        String correctCode = (String) redisUtil.get(redisKey);

        if (correctCode == null) {
            return false;
        }

        redisUtil.del(redisKey);
        return correctCode.equals(code);
    }

    private String generateRandomCode(int length) {
        String chars = "0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}




