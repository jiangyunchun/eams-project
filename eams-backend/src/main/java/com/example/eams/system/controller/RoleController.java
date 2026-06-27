package com.example.eams.system.controller;

import com.example.eams.common.result.PageResult;
import com.example.eams.common.result.Result;
import com.example.eams.security.annotation.RequireRole;
import com.example.eams.common.config.OperationLog;
import com.example.eams.system.dto.RoleDTO;
import com.example.eams.system.dto.RolePermissionDTO;
import com.example.eams.system.entity.SysRole;
import com.example.eams.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;

/**
 * 角色管理接口
 */
@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 分页查询
     * GET /api/system/role/list
     */
    @GetMapping("/list")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public Result<PageResult<SysRole>> list(
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleCode,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(roleService.list(roleName, roleCode, pageNum, pageSize));
    }

    /**
     * 获取全量角色列表（下拉框用）
     * GET /api/system/role/all
     */
    @GetMapping("/all")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public Result<List<SysRole>> all() {
        return Result.ok(roleService.listAll());
    }

    /**
     * 新增角色
     * POST /api/system/role/add
     */
    @PostMapping("/add")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "新增", description = "新增角色【{0}】")
    public Result<?> add(@Valid @RequestBody RoleDTO dto) {
        roleService.add(dto);
        return Result.ok("角色创建成功", null);
    }

    /**
     * 编辑角色
     * PUT /api/system/role/edit
     */
    @PutMapping("/edit")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "编辑", description = "编辑角色【{0}】")
    public Result<?> edit(@Valid @RequestBody RoleDTO dto) {
        roleService.edit(dto);
        return Result.ok("角色信息修改成功", null);
    }

    /**
     * 删除角色
     * DELETE /api/system/role/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "删除", description = "删除角色ID【{0}】")
    public Result<?> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok("角色已删除", null);
    }

    /**
     * 保存角色权限配置
     * PUT /api/system/role/permission
     */
    @PutMapping("/permission")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "编辑", description = "配置角色权限【{0}】")
    public Result<?> assignPermission(@Valid @RequestBody RolePermissionDTO dto) {
        roleService.assignPermission(dto);
        return Result.ok("权限配置保存成功", null);
    }

    /**
     * 获取角色已有菜单ID
     * GET /api/system/role/menu-ids/{roleId}
     */
    @GetMapping("/menu-ids/{roleId}")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public Result<List<Long>> getMenuIds(@PathVariable Long roleId) {
        return Result.ok(roleService.getMenuIds(roleId));
    }
}
