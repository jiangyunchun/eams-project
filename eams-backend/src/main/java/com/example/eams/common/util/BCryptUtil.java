package com.example.eams.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 密码加密工具类
 * <p>
 * 基于 Spring Security Crypto 模块的 BCryptPasswordEncoder。
 * 工作因子 cost=10，每次加密自动生成随机盐。
 */
public class BCryptUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(10);

    /**
     * 加密密码
     *
     * @param rawPassword 明文密码
     * @return BCrypt密文（60字符）
     */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 校验密码
     *
     * @param rawPassword     明文密码
     * @param encodedPassword BCrypt密文
     * @return true-匹配, false-不匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return ENCODER.matches(rawPassword, encodedPassword);
    }
}
