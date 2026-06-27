package com.example.eams.common.util;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * JWT 工具类
 * <p>
 * 生成/校验/解析 JWT Token。
 * 签名算法: HS256，密钥通过配置注入。
 */
@Slf4j
public class JwtUtil {

    /** JWT签名密钥（由 application.yml 注入） */
    private static String SECRET;

    /** Token签发者 */
    private static final String ISSUER = "EAMS";

    /**
     * Spring 容器启动时由配置类调用注入
     */
    public static void setSecret(String secret) {
        JwtUtil.SECRET = Base64.getEncoder().encodeToString(secret.getBytes());
    }

    // ==================== 生成 Token ====================

    /**
     * 生成JWT Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param roles    角色编码集合
     * @param expireHours 过期时间（小时）
     * @return JWT Token 字符串
     */
    public static String generate(Long userId, String username, Set<String> roles, int expireHours) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireHours * 3600_000L);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())          // jti: Token唯一标识
                .setSubject(String.valueOf(userId))            // sub: 用户ID
                .setIssuer(ISSUER)
                .claim("username", username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    // ==================== 校验 Token ====================

    /**
     * 校验Token有效性（签名 + 过期时间）
     *
     * @param token JWT Token
     * @return true-有效, false-无效
     */
    public static boolean verify(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token已过期: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.debug("Token无效: {}", e.getMessage());
            return false;
        }
    }

    // ==================== 解析 Token ====================

    /**
     * 获取 Token 唯一标识 (jti)
     */
    public static String getJti(String token) {
        return parseClaims(token).getId();
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    /**
     * 获取用户名
     */
    public static String getUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    /**
     * 获取角色编码集合
     */
    @SuppressWarnings("unchecked")
    public static Set<String> getRoles(String token) {
        List<String> roleList = parseClaims(token).get("roles", List.class);
        return roleList != null ? new HashSet<>(roleList) : Collections.emptySet();
    }

    /**
     * 获取Token剩余有效时间（毫秒）
     */
    public static long getRemainingTime(String token) {
        return parseClaims(token).getExpiration().getTime() - System.currentTimeMillis();
    }

    // ==================== 内部方法 ====================

    private static Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody();
    }
}
