package com.example.eams.system.controller;

import com.example.eams.common.result.Result;
import com.example.eams.security.annotation.RequireRole;
import com.example.eams.common.config.OperationLog;
import com.example.eams.system.dto.DeptDTO;
import com.example.eams.system.entity.SysDept;
import com.example.eams.system.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 部门管理接口
 */
@RestController
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    /**
     * 获取部门树（全量）
     * GET /api/system/dept/tree
     */
    @GetMapping("/tree")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    public Result<List<SysDept>> tree() {
        return Result.ok(deptService.listAll());
    }

    /**
     * 获取部门详情
     * GET /api/system/dept/detail/{id}
     */
    @GetMapping("/detail/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public Result<SysDept> detail(@PathVariable Long id) {
        return Result.ok(deptService.getDetail(id));
    }

    /**
     * 新增部门
     * POST /api/system/dept/add
     */
    @PostMapping("/add")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "新增", description = "新增部门【{0}】")
    public Result<?> add(@Valid @RequestBody DeptDTO dto) {
        deptService.add(dto);
        return Result.ok("部门创建成功", null);
    }

    @PutMapping("/edit")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "编辑", description = "编辑部门【{0}】")
    public Result<?> edit(@Valid @RequestBody DeptDTO dto) {
        deptService.edit(dto);
        return Result.ok("部门信息修改成功", null);
    }

    @DeleteMapping("/delete/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "删除", description = "删除部门ID【{0}】")
    public Result<?> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.ok("部门已删除", null);
    }
}
