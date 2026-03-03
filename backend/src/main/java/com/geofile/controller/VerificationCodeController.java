package com.geofile.controller;

import com.geofile.common.Result;
import com.geofile.service.VerificationCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 验证码Controller
 * 提供简洁的验证码验证接口
 */
@Tag(name = "验证码接口", description = "验证码生成和验证")
@RestController
@RequestMapping("/api/verification")
@CrossOrigin
public class VerificationCodeController {

    @Autowired
    private VerificationCodeService verificationCodeService;

    /**
     * 获取图形验证码
     * @return 验证码信息（包含图片Base64和captchaKey）
     */
    @Operation(summary = "获取图形验证码", description = "生成验证码图片和captchaKey")
    @GetMapping("/captcha")
    public Result<Map<String, Object>> getCaptcha() {
        Map<String, Object> captcha = verificationCodeService.generateCaptcha();
        return Result.success(captcha);
    }

    /**
     * 验证验证码
     * 直接传入验证码进行验证，无需其他参数
     * @param captchaKey 图形验证码Key
     * @param code 验证码
     * @return 验证结果
     */
    @Operation(summary = "验证图形验证码", description = "验证用户输入的验证码")
    @PostMapping("/verify")
    public Result<Boolean> verify(@RequestParam String captchaKey, @RequestParam String code) {
        boolean isValid = verificationCodeService.verifyCaptcha(captchaKey, code);
        return Result.success(isValid);
    }

    /**
     * 生成下载验证码Token（用于文件下载）
     * 前端使用此Token作为Authorization header
     * @return JWT Token
     */
    @Operation(summary = "获取下载Token", description = "生成用于文件下载的JWT Token")
    @GetMapping("/token/download")
    public Result<String> getDownloadToken() {
        String token = verificationCodeService.generateDownloadToken();
        return Result.success(token);
    }

    /**
     * 验证下载Token
     * @param token JWT Token
     * @return 验证结果
     */
    @Operation(summary = "验证下载Token", description = "验证JWT Token是否有效")
    @PostMapping("/token/verify")
    public Result<Boolean> verifyDownloadToken(@RequestHeader("Authorization") String token) {
        boolean isValid = verificationCodeService.verifyDownloadToken(token);
        return Result.success(isValid);
    }
}
