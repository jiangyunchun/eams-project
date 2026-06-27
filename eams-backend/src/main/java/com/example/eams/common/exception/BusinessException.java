package com.example.eams.common.exception;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 抛出此异常后由 {@link GlobalExceptionHandler} 统一捕获并返回 Result。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务状态码 */
    private final int code;

    /**
     * @param code    业务状态码
     * @param message 提示信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 默认 code=400 参数错误
     */
    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    /**
     * 无权限
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }

    /**
     * 未登录
     */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, message);
    }

    /**
     * 资源不存在
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }
}
