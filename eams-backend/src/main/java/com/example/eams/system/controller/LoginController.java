package com.example.eams.system.controller;

import com.example.eams.common.result.Result;
import com.example.eams.common.config.OperationLog;
import com.example.eams.common.util.RedisUtil;
import com.example.eams.security.annotation.RequireRole;
import com.example.eams.security.filter.SecurityContextHolder;
import com.example.eams.system.dto.LoginDTO;
import com.example.eams.system.dto.LoginVO;
import com.example.eams.system.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 登录认证接口
 */
@RestController
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    /**
     * 登录
     * POST /api/login
     */
    @PostMapping("/api/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        LoginVO vo = loginService.login(dto, request);
        return Result.ok("登录成功", vo);
    }

    /**
     * 登出
     * POST /api/logout
     */
    @PostMapping("/api/logout")
    @OperationLog(module = "系统管理", actionType = "登录", description = "退出登录")
    public Result<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            loginService.logout(authHeader.substring(7));
        }
        return Result.ok("已退出登录", null);
    }

    /**
     * 解锁账号（仅超级管理员）
     * PUT /api/login/unlock?username=xxx
     */
    @PutMapping("/api/system/user/unlock")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "编辑", description = "解锁账号【{0}】")
    public Result<?> unlock(@RequestParam String username) {
        RedisUtil.del("eams:login:lock:" + username);
        RedisUtil.del("eams:login:fail:" + username);
        return Result.ok("账号【" + username + "】已解锁", null);
    }

    /**
     * 获取当前用户信息
     * GET /api/user/info
     */
    @GetMapping("/api/user/info")
    public Result<LoginVO> userInfo() {
        Long userId = SecurityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("未登录");
        }
        LoginVO vo = loginService.getCurrentUserInfo(userId);
        return Result.ok(vo);
    }
}
