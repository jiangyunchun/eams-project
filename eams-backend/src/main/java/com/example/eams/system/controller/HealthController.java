package com.example.eams.system.controller;

import com.example.eams.common.result.Result;
import com.example.eams.common.util.AIClientUtil;
import com.example.eams.common.util.BCryptUtil;
import com.example.eams.common.util.JwtUtil;
import com.example.eams.common.util.RedisUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 健康检查 / 脚手架验证 Controller
 */
@RestController
public class HealthController {

    /**
     * 综合健康检查
     * GET /api/health
     */
    @GetMapping("/api/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();

        // 1. 基本状态
        status.put("app", "EAMS Backend V1.0");
        status.put("time", new Date().toString());

        // 2. Redis 检查
        try {
            RedisUtil.set("eams:health:check", "ok", 60);
            String redisVal = RedisUtil.get("eams:health:check");
            status.put("redis", "ok".equals(redisVal) ? "connected" : "error");
        } catch (Exception e) {
            status.put("redis", "unavailable: " + e.getMessage());
        }

        // 3. JWT 检查
        try {
            Set<String> roles = new HashSet<>(Arrays.asList("ROLE_TEST"));
            String token = JwtUtil.generate(1L, "healthcheck", roles, 1);
            boolean valid = JwtUtil.verify(token);
            status.put("jwt", valid ? "working" : "error");
        } catch (Exception e) {
            status.put("jwt", "error: " + e.getMessage());
        }

        // 4. BCrypt 检查
        try {
            String hash = BCryptUtil.encode("test");
            boolean match = BCryptUtil.matches("test", hash);
            status.put("bcrypt", match ? "working" : "error");
        } catch (Exception e) {
            status.put("bcrypt", "error: " + e.getMessage());
        }

        // 5. DeepSeek API Key 状态（不实际调用）
        status.put("deepseek", "configured (verify with /api/health/ai-probe)");

        return Result.ok(status);
    }

    /**
     * AI 服务探测
     * GET /api/health/ai-probe
     */
    @GetMapping("/api/health/ai-probe")
    public Result<Map<String, String>> aiProbe() {
        Map<String, String> result = new HashMap<>();
        boolean recovered = AIClientUtil.probe();
        result.put("status", recovered ? "recovered" : "still_degraded");
        return Result.ok(result);
    }

    /**
     * JWT Token 生成测试（开发环境）
     * GET /api/health/gen-token
     */
    @GetMapping("/api/health/gen-token")
    public Result<Map<String, String>> genToken() {
        Set<String> roles = new HashSet<>(Arrays.asList("ROLE_SUPER_ADMIN"));
        String token = JwtUtil.generate(1L, "admin", roles, 2);
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("expire", "2 hours");
        result.put("usage", "Authorization: Bearer " + token);
        return Result.ok(result);
    }
}
