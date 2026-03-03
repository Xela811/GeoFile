package com.geofile.interceptor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 文件上传拦截器
 * 验证文件上传请求
 */
@Component
public class FileUploadInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只拦截POST请求
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":405,\"message\":\"只支持POST方法\"}");
            return false;
        }

        // 检查Content-Type是否为multipart/form-data
        String contentType = request.getContentType();
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":400,\"message\":\"需要multipart/form-data格式\"}");
            return false;
        }

        return true;
    }
}
