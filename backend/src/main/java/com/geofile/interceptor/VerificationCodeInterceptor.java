package com.geofile.interceptor;

import com.geofile.util.JwtUtil;
import com.geofile.util.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 验证码拦截器
 * 拦截需要验证码验证的接口
 */
@Component
public class VerificationCodeInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    private static final String VERIFICATION_PREFIX = "verification:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求路径
        String uri = request.getRequestURI();

        // 检查是否是公开接口（已由SecurityConfig处理）
        // 这里只需要处理需要验证码的接口

        // 从请求头获取Token
        String token = request.getHeader("Authorization");

        if (token == null || token.isEmpty()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"请先获取验证码\"}");
            return false;
        }

        // 验证Token
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"验证码无效或已过期\"}");
            return false;
        }

        // 验证Token是否过期
        if (jwtUtil.isTokenExpired(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"验证码已过期\"}");
            return false;
        }

        // 从Token获取验证码
        String code = jwtUtil.getCodeFromToken(token);

        // 从Redis获取正确的验证码
        String redisKey = VERIFICATION_PREFIX + code;
        String correctCode = (String) redisUtil.get(redisKey);

        if (correctCode == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"验证码已失效\"}");
            return false;
        }

        // 验证码使用后删除（一次性验证码）
        redisUtil.del(redisKey);

        // 放行
        return true;
    }
}
