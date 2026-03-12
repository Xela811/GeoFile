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
            // 禁用默认CORS（使用自定义CORS配置）
            .cors(AbstractHttpConfigurer::disable)
            // 配置授权规则
            .authorizeHttpRequests(auth -> auth
                // 公开接口（不需要验证）
                .requestMatchers(
                    "/api/verification/**",           // 验证码相关接口
                    "/api/verification/captcha",       // 获取验证码图片
                    "/api/verification/send",          // 发送验证码
                    "/api/file/**",                    // 所有文件相关接口（包括上传、下载、列表等）
                    "/ws/**",                          // WebSocket连接
                    "/actuator/**",                    // 健康检查
                    "/doc.html", "/webjars/**", "/v3/api-docs/**", "/swagger-resources/**",
                        "/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**","/api/location/**","/api/amap/**"
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
