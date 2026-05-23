package com.geofile.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. 捕获参数校验异常 (Validation)
     * 对应 Day 6 的文件验证场景
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public Result<String> handleValidationException(Exception e) {
        log.warn("参数校验失败: {}", e.getMessage());
        return Result.error(400, "参数格式不正确");
    }

    /**
     * 2. 捕获数据库访问异常
     * 对应 Day 2/3 的数据库操作场景
     */
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public Result<String> handleDatabaseException(Exception e) {
        log.error("数据库操作异常: ", e);
        return Result.error(500, "数据库访问失败，请检查数据库服务");
    }

    /**
     * 3. 兜底捕获所有未知异常 (Exception.class)
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleAllException(Exception e) {
        // 这里的 log 非常重要，否则你不知道后台出了什么错
        log.error("【系统未知错误】: ", e);
        return Result.error(500, "服务器繁忙，请稍后再试");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<String> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return Result.error("文件太大了！单文件不能超过限制大小。");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<?>> handleIllegalArgumentException(IllegalArgumentException e) {
        // 返回 400 Bad Request，强制前端 axios 进入 catch 块
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(e.getMessage()));
    }
}