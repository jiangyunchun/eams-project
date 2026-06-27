package com.example.eams.system.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.result.Result;
import com.example.eams.security.annotation.RequireRole;
import com.example.eams.system.entity.SysOperationLog;
import com.example.eams.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 操作日志查询接口
 */
@RestController
@RequestMapping("/api/system/log")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志
     * GET /api/system/log/list
     */
    @GetMapping("/list")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public Result<PageResult<SysOperationLog>> list(
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        LocalDateTime begin = null;
        LocalDateTime end = null;
        if (StrUtil.isNotBlank(beginTime)) begin = DateUtil.parseLocalDateTime(beginTime);
        if (StrUtil.isNotBlank(endTime)) end = DateUtil.parseLocalDateTime(endTime);

        return Result.ok(operationLogService.list(operator, module, actionType,
                begin, end, pageNum, pageSize));
    }

    /**
     * 日志详情
     * GET /api/system/log/detail/{id}
     */
    @GetMapping("/detail/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public Result<SysOperationLog> detail(@PathVariable Long id) {
        return Result.ok(operationLogService.getDetail(id));
    }
}
