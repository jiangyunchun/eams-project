package com.example.eams.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回体
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 业务状态码: 200-成功, 400-参数错误, 401-未登录, 403-无权限, 404-不存在, 429-限流, 500-服务器错误 */
    private int code;

    /** 提示信息，前端直接展示 */
    private String message;

    /** 返回数据体，无数据时返回 null */
    private T data;

    // ===== 成功快捷方法 =====

    public static <T> Result<T> ok() {
        return new Result<>(200, "操作成功", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(200, message, data);
    }

    // ===== 失败快捷方法 =====

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(400, message, null);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null);
    }

    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message, null);
    }

    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null);
    }

    public static <T> Result<T> rateLimited(String message) {
        return new Result<>(429, message, null);
    }

    public static <T> Result<T> serverError(String message) {
        return new Result<>(500, message, null);
    }
}
