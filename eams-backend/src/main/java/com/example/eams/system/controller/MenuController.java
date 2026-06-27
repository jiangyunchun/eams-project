package com.example.eams.system.controller;

import com.example.eams.common.result.Result;
import com.example.eams.security.annotation.RequireRole;
import com.example.eams.system.entity.SysMenu;
import com.example.eams.system.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单权限接口（仅查询，维护在权限配置中）
 */
@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class MenuController {

    private final SysMenuMapper menuMapper;

    /**
     * 获取全量菜单树（权限配置用）
     * GET /api/system/menu/tree
     */
    @GetMapping("/tree")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public Result<List<SysMenu>> tree() {
        List<SysMenu> menus = menuMapper.selectList(null);
        return Result.ok(menus);
    }
}
