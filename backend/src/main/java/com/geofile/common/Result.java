package com.geofile.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "统一响应结果")
public class Result<T> implements Serializable {

    @Schema(description = "状态码", example = "200")
    private Integer code;

    @Schema(description = "返回信息", example = "操作成功")
    private String message;

    @Schema(description = "数据")
    private T data;

    // --- 构造方法 ---

    public Result() {}

    /**
     * 全参数方法
     */
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // --- 成功返回系列 ---

    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> success(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> success(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(String message) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, null);
    }

    // --- 失败返回系列 ---

    public static <T> Result<T> error() {
        return new Result<>(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage(), null);
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(String message, T data) {
        return new Result<>(ResultCode.ERROR.getCode(), message, data);
    }

    public static <T> Result<T> error(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }

    public static <T> Result<T> error(T data) {
        return new Result<>(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage(), data);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message, null);
    }
}