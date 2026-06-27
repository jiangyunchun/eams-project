package com.example.eams.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eams.common.exception.BusinessException;
import com.example.eams.common.result.PageResult;
import com.example.eams.system.dto.RoleDTO;
import com.example.eams.system.dto.RolePermissionDTO;
import com.example.eams.system.entity.SysRole;
import com.example.eams.system.entity.SysRoleMenu;
import com.example.eams.system.entity.SysUserRole;
import com.example.eams.system.mapper.SysRoleMapper;
import com.example.eams.system.mapper.SysRoleMenuMapper;
import com.example.eams.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 角色管理服务
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    /** 系统预置角色编码（不可删除） */
    private static final Set<String> SYSTEM_ROLES = new java.util.HashSet<>(Arrays.asList(
            "ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN", "ROLE_EMPLOYEE"));

    /**
     * 分页查询
     */
    public PageResult<SysRole> list(String roleName, String roleCode, int pageNum, int pageSize) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getIsDeleted, 0);
        if (StrUtil.isNotBlank(roleName)) wrapper.like(SysRole::getRoleName, roleName);
        if (StrUtil.isNotBlank(roleCode)) wrapper.eq(SysRole::getRoleCode, roleCode);
        wrapper.orderByDesc(SysRole::getCreateTime);

        IPage<SysRole> page = roleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    /**
     * 查询所有角色（全量）
     */
    public List<SysRole> listAll() {
        return roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getIsDeleted, 0)
                        .orderByAsc(SysRole::getId));
    }

    /**
     * 新增角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void add(RoleDTO dto) {
        LambdaQueryWrapper<SysRole> checkQw = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, dto.getRoleCode())
                .eq(SysRole::getIsDeleted, 0);
        if (roleMapper.selectCount(checkQw) > 0) {
            throw new BusinessException(400, "角色编码已存在");
        }

        SysRole role = new SysRole();
        role.setRoleName(dto.getRoleName());
        role.setRoleCode(dto.getRoleCode());
        role.setDescription(dto.getDescription());
        roleMapper.insert(role);
    }

    /**
     * 编辑角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void edit(RoleDTO dto) {
        SysRole role = roleMapper.selectById(dto.getId());
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }
        // 角色编码不可修改
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        roleMapper.updateById(role);
    }

    /**
     * 删除角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }
        if (role.getIsSystem() == 1 || SYSTEM_ROLES.contains(role.getRoleCode())) {
            throw new BusinessException(400, "系统预置角色不可删除");
        }

        // 检查是否有用户关联
        Long userCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, id));
        if (userCount > 0) {
            throw new BusinessException(400,
                    "该角色下存在" + userCount + "个用户，请先解除关联后再删除");
        }

        roleMapper.deleteById(id);
    }

    /**
     * 配置角色权限
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignPermission(RolePermissionDTO dto) {
        SysRole role = roleMapper.selectById(dto.getRoleId());
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }

        // 先清空原有权限
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, dto.getRoleId()));

        // 保存新权限
        for (Long menuId : dto.getMenuIds()) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(dto.getRoleId());
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    /**
     * 获取角色已有的菜单ID列表
     */
    public List<Long> getMenuIds(Long roleId) {
        return roleMenuMapper.selectList(
                        new LambdaQueryWrapper<SysRoleMenu>()
                                .eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(java.util.stream.Collectors.toList());
    }
}
