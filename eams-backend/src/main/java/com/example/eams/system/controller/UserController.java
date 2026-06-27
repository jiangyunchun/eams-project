package com.example.eams.system.controller;

import com.example.eams.common.result.PageResult;
import com.example.eams.common.result.Result;
import com.example.eams.security.annotation.RequireRole;
import com.example.eams.common.config.OperationLog;
import com.example.eams.common.util.RedisUtil;
import com.example.eams.system.dto.UserAddDTO;
import com.example.eams.system.dto.UserEditDTO;
import com.example.eams.system.dto.UserQueryDTO;
import com.example.eams.system.entity.SysUser;
import com.example.eams.system.service.UserService;
import com.example.eams.system.service.DeptService;
import com.example.eams.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户管理接口
 */
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final DeptService deptService;
    private final RoleService roleService;

    /**
     * 分页查询
     * GET /api/system/user/list
     */
    @GetMapping("/list")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    public Result<PageResult<SysUser>> list(UserQueryDTO query) {
        PageResult<SysUser> page = userService.list(query);
        // 填充部门名称 + 角色名称
        Map<Long, String> roleNameMap = roleService.listAll().stream()
                .collect(Collectors.toMap(com.example.eams.system.entity.SysRole::getId,
                        com.example.eams.system.entity.SysRole::getRoleName));
        page.getList().forEach(u -> {
            if (u.getDeptId() != null) {
                u.setDeptName(deptService.getDeptPathName(u.getDeptId()));
            }
            List<Long> roleIds = userService.getUserRoleIds(u.getId());
            u.setRoleNames(roleIds.stream()
                    .map(id -> roleNameMap.getOrDefault(id, ""))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(",")));
            // 从 Redis 查询锁定状态
            u.setLocked(RedisUtil.exists("eams:login:lock:" + u.getUsername()));
        });
        return Result.ok(page);
    }

    /**
     * 新增用户
     * POST /api/system/user/add
     */
    @PostMapping("/add")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "新增", description = "新增用户【{0}】")
    public Result<?> add(@Valid @RequestBody UserAddDTO dto) {
        userService.add(dto);
        return Result.ok("用户创建成功", null);
    }

    /**
     * 编辑用户
     * PUT /api/system/user/edit
     */
    @PutMapping("/edit")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "编辑", description = "编辑用户【{0}】")
    public Result<?> edit(@Valid @RequestBody UserEditDTO dto) {
        userService.edit(dto);
        return Result.ok("用户信息修改成功", null);
    }

    /**
     * 删除用户
     * DELETE /api/system/user/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "删除", description = "删除用户ID【{0}】")
    public Result<?> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok("用户已删除", null);
    }

    /**
     * 重置密码
     * PUT /api/system/user/reset-pwd/{id}
     */
    @PutMapping("/reset-pwd/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public Result<?> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.ok("密码已重置为默认密码", null);
    }

    /**
     * 修改用户状态
     * PUT /api/system/user/status
     */
    @PutMapping("/status")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public Result<?> toggleStatus(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        userService.toggleStatus(id, status);
        String msg = status == 1 ? "用户已启用" : "用户已禁用";
        return Result.ok(msg, null);
    }

    /**
     * 获取用户详情
     * GET /api/system/user/detail/{id}
     */
    @GetMapping("/detail/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        SysUser user = userService.getUserDetail(id);
        List<Long> roleIds = userService.getUserRoleIds(id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        result.put("roleIds", roleIds);
        return Result.ok(result);
    }

    /**
     * 判断是否存在某字段值的用户（异步校验）
     * GET /api/system/user/check?field=username&value=xxx&excludeId=1
     */
    @GetMapping("/check")
    public Result<Map<String, Boolean>> checkField(
            @RequestParam String field,
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        UserQueryDTO query = new UserQueryDTO();
        if ("username".equals(field)) query.setRealName(value);
        else if ("phone".equals(field)) query.setPhone(value);
        PageResult<SysUser> page = userService.list(query);
        boolean exists = page.getList().stream()
                .anyMatch(u -> !u.getId().equals(excludeId));
        Map<String, Boolean> result = new HashMap<>();
        result.put("exists", exists);
        return Result.ok(result);
    }
}
