package com.example.eams.security.filter;

import cn.hutool.json.JSONUtil;
import com.example.eams.common.result.Result;
import com.example.eams.common.util.JwtUtil;
import com.example.eams.common.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

/**
 * JWT 认证过滤器
 * <p>
 * 拦截所有业务请求，校验 Token 有效性。
 * 白名单: /api/login（登录接口不拦截）
 */
@Slf4j
@Component
@Order(1)
public class JwtAuthenticationFilter implements Filter {

    /** Token 请求头 */
    private static final String HEADER_AUTH = "Authorization";

    /** Token 前缀 */
    private static final String TOKEN_PREFIX = "Bearer ";

    /** 白名单路径 */
    private static final String[] WHITE_LIST = {
            "/api/login",
            "/api/health",
            "/api/system/dict/all",
            "/api/system/dict/item/cached",
            "/uploads",
    };

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // 白名单放行
        if (isWhiteList(request.getRequestURI())) {
            chain.doFilter(req, res);
            return;
        }

        try {
            // 1. 提取 Token
            String token = extractToken(request);
            if (token == null) {
                write401(response, "未登录");
                return;
            }

            // 2. 校验 Token 黑名单（主动登出）
            String jti = JwtUtil.getJti(token);
            if (RedisUtil.exists("eams:token:blacklist:" + jti)) {
                write401(response, "登录已过期，请重新登录");
                return;
            }

            // 3. 校验 Token 有效性
            if (!JwtUtil.verify(token)) {
                write401(response, "登录已过期，请重新登录");
                return;
            }

            // 4. 设置安全上下文
            Long userId = JwtUtil.getUserId(token);
            String username = JwtUtil.getUsername(token);
            Set<String> roles = JwtUtil.getRoles(token);
            SecurityContextHolder.set(userId, username, roles);

            // 5. 放行
            chain.doFilter(req, res);
        } finally {
            // 请求结束后清除 ThreadLocal，防止内存泄漏
            SecurityContextHolder.clear();
        }
    }

    // ==================== 内部方法 ====================

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HEADER_AUTH);
        if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            return authHeader.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    private boolean isWhiteList(String uri) {
        for (String path : WHITE_LIST) {
            if (uri.equals(path) || uri.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    private void write401(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(JSONUtil.toJsonStr(Result.unauthorized(message)));
        }
    }
}
