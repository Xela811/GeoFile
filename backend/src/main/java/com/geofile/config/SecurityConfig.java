package com.geofile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security配置 - 免登录版本
 * 只开放验证码和文件相关接口，其他接口需要验证码验证
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（前后端分离不需要）
            .csrf(AbstractHttpConfigurer::disable)
            // 禁用CORS（通过配置类控制）
            .cors(AbstractHttpConfigurer::disable)
            // 配置授权规则
            .authorizeHttpRequests(auth -> auth
                // 公开接口（不需要验证）
                .requestMatchers(
                    "/api/verification/**",           // 验证码相关接口
                    "/api/verification/captcha",       // 获取验证码图片
                    "/api/verification/send",          // 发送验证码
                    "/api/file/list",                  // 文件列表
                    "/api/file/nearby",                // 附近文件
                    "/api/file/download/**",           // 文件下载
                    "/ws/**",                          // WebSocket连接
                    "/actuator/**"                     // 健康检查
                ).permitAll()
                // 其他接口都需要认证（将通过验证码验证）
                .anyRequest().authenticated()
            )
            // 无需Session
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            );

        return http.build();
    }
}
