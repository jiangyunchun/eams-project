package com.example.eams.system.controller;

import com.example.eams.common.result.PageResult;
import com.example.eams.common.result.Result;
import com.example.eams.security.annotation.RequireRole;
import com.example.eams.common.config.OperationLog;
import com.example.eams.system.entity.SysConfig;
import com.example.eams.system.entity.SysConfigHistory;
import com.example.eams.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 系统参数配置接口
 */
@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    /**
     * 分页查询参数
     * GET /api/system/config/list
     */
    @GetMapping("/list")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public Result<PageResult<SysConfig>> list(
            @RequestParam(required = false) String paramKey,
            @RequestParam(required = false) String paramName,
            @RequestParam(required = false) String paramGroup,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(configService.list(paramKey, paramName, paramGroup, pageNum, pageSize));
    }

    /**
     * 新增参数
     * POST /api/system/config/add
     */
    @PostMapping("/add")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "新增", description = "新增参数【{0}】")
    public Result<?> add(@Valid @RequestBody SysConfig config) {
        configService.add(config);
        return Result.ok("参数配置创建成功", null);
    }

    @PutMapping("/edit")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "编辑", description = "编辑参数【{0}】")
    public Result<?> edit(@Valid @RequestBody SysConfig config) {
        configService.edit(config);
        return Result.ok("参数【" + config.getParamName() + "】已更新为 " + config.getParamValue(), null);
    }

    /**
     * 删除参数
     * DELETE /api/system/config/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "删除", description = "删除参数ID【{0}】")
    public Result<?> delete(@PathVariable Long id) {
        configService.delete(id);
        return Result.ok("参数已删除", null);
    }

    /**
     * 重置为默认值
     * PUT /api/system/config/reset/{id}
     */
    @PutMapping("/reset/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public Result<?> reset(@PathVariable Long id) {
        configService.resetToDefault(id);
        return Result.ok("参数已重置为默认值", null);
    }

    /**
     * 查看变更历史
     * GET /api/system/config/history/{configId}
     */
    @GetMapping("/history/{configId}")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public Result<List<SysConfigHistory>> history(@PathVariable Long configId) {
        return Result.ok(configService.getHistory(configId));
    }
}
