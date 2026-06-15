package com.geofile.config;

import com.geofile.interceptor.FileUploadInterceptor;
import com.geofile.interceptor.VerificationCodeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web配置类
 * 配置CORS跨域、拦截器等
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final VerificationCodeInterceptor verificationCodeInterceptor;
    private final FileUploadInterceptor fileUploadInterceptor;

    public WebConfig(
            VerificationCodeInterceptor verificationCodeInterceptor,
            FileUploadInterceptor fileUploadInterceptor
    ) {
        this.verificationCodeInterceptor = verificationCodeInterceptor;
        this.fileUploadInterceptor = fileUploadInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 验证码拦截器（拦截需要验证码的接口）
        registry.addInterceptor(verificationCodeInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/verification/**",           // 排除验证码接口
                    "/api/file/**",                   // 排除所有文件接口（不需要验证码）
                        "/api/location/**",
                    "/api/amap/**",
                    "/ws/**"                           // 排除WebSocket接口
                );

        // 文件上传拦截器（拦截文件上传接口）
        registry.addInterceptor(fileUploadInterceptor)
                .addPathPatterns("/api/file/upload")
                .excludePathPatterns("/api/file/upload/chunk"); // 分片上传接口单独处理
    }
}
