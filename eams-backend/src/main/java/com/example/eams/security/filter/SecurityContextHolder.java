package com.example.eams.security.filter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 安全上下文持有者（ThreadLocal）
 * <p>
 * 在 JwtAuthenticationFilter 中设置，在请求结束后清除。
 * 提供当前请求的用户身份信息。
 */
public class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> CONTEXT = new ThreadLocal<>();

    /**
     * 设置当前请求的用户上下文
     */
    public static void set(Long userId, String username, Set<String> roles) {
        CONTEXT.set(new SecurityContext(userId, username, roles));
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        SecurityContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getUserId() : null;
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        SecurityContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getUsername() : null;
    }

    /**
     * 获取当前用户角色集合
     */
    public static Set<String> getCurrentRoles() {
        SecurityContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getRoles() : Collections.emptySet();
    }

    /**
     * 清除上下文（请求结束后必须调用，防止内存泄漏）
     */
    public static void clear() {
        CONTEXT.remove();
    }

    // ==================== 内部类 ====================

    static class SecurityContext {
        private final Long userId;
        private final String username;
        private final Set<String> roles;

        SecurityContext(Long userId, String username, Set<String> roles) {
            this.userId = userId;
            this.username = username;
            this.roles = roles;
        }

        Long getUserId() { return userId; }
        String getUsername() { return username; }
        Set<String> getRoles() { return roles; }
    }
}
