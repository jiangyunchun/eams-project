package com.example.eams.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.result.Result;
import com.example.eams.security.annotation.RequireRole;
import com.example.eams.system.entity.SysLoginLog;
import com.example.eams.system.mapper.SysLoginLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 登录日志查询接口
 */
@RestController
@RequestMapping("/api/system/login-log")
@RequiredArgsConstructor
public class LoginLogController {

    private final SysLoginLogMapper loginLogMapper;

    @GetMapping("/list")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    public Result<PageResult<SysLoginLog>> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer loginStatus,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<SysLoginLog>()
                .eq(SysLoginLog::getIsDeleted, 0);
        if (username != null && !username.isEmpty()) {
            wrapper.like(SysLoginLog::getUsername, username);
        }
        if (loginStatus != null) {
            wrapper.eq(SysLoginLog::getLoginStatus, loginStatus);
        }
        wrapper.orderByDesc(SysLoginLog::getCreateTime);

        IPage<SysLoginLog> page = loginLogMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);
        return Result.ok(PageResult.of(page));
    }
}
