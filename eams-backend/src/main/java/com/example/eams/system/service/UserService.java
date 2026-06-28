package com.example.eams.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eams.common.exception.BusinessException;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.util.BCryptUtil;
import com.example.eams.security.filter.SecurityContextHolder;
import com.example.eams.system.dto.UserAddDTO;
import com.example.eams.system.dto.UserEditDTO;
import com.example.eams.system.dto.UserQueryDTO;
import com.example.eams.system.entity.SysUser;
import com.example.eams.system.entity.SysUserRole;
import com.example.eams.system.mapper.SysUserMapper;
import com.example.eams.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;

    /** 默认密码 */
    private static final String DEFAULT_PASSWORD = "Eams@123456";

    /**
     * 分页查询用户列表
     */
    public PageResult<SysUser> list(UserQueryDTO query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getIsDeleted, 0);

        if (StrUtil.isNotBlank(query.getRealName())) {
            wrapper.like(SysUser::getRealName, query.getRealName());
        }
        if (query.getDeptId() != null) {
            wrapper.eq(SysUser::getDeptId, query.getDeptId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        if (StrUtil.isNotBlank(query.getPhone())) {
            wrapper.eq(SysUser::getPhone, query.getPhone());
        }
        if (query.getRoleId() != null) {
            List<Long> userIds = userRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, query.getRoleId()))
                    .stream().map(SysUserRole::getUserId).collect(Collectors.toList());
            if (!userIds.isEmpty()) {
                wrapper.in(SysUser::getId, userIds);
            } else {
                wrapper.eq(SysUser::getId, -1L);
            }
        }

        wrapper.orderByDesc(SysUser::getCreateTime);

        IPage<SysUser> page = userMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page);
    }

    /**
     * 新增用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void add(UserAddDTO dto) {
        // 唯一性校验
        checkUnique(dto.getUsername(), null, dto.getPhone(), dto.getEmail());

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(BCryptUtil.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setDeptId(dto.getDeptId());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(dto.getStatus());
        userMapper.insert(user);

        // 保存角色关联
        saveUserRoles(user.getId(), dto.getRoleIds());
    }

    /**
     * 编辑用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void edit(UserEditDTO dto) {
        SysUser user = userMapper.selectById(dto.getId());
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        // 唯一性校验（排除自身）
        checkUnique(dto.getUsername(), dto.getId(), dto.getPhone(), dto.getEmail());

        user.setUsername(dto.getUsername());
        user.setRealName(dto.getRealName());
        user.setDeptId(dto.getDeptId());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(dto.getStatus());
        userMapper.updateById(user);

        // 更新角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, dto.getId()));
        saveUserRoles(dto.getId(), dto.getRoleIds());
    }

    /**
     * 删除用户（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 不允许删除自己
        if (id.equals(SecurityContextHolder.getCurrentUserId())) {
            throw new BusinessException(400, "不能删除自己的账号");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        userMapper.deleteById(id); // MyBatis-Plus 逻辑删除
        // 同步清除角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, id));
    }

    /**
     * 重置密码
     */
    public void resetPassword(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        user.setPassword(BCryptUtil.encode(DEFAULT_PASSWORD));
        userMapper.updateById(user);
    }

    /**
     * 切换用户状态（启用/禁用）
     */
    public void toggleStatus(Long id, Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    /**
     * 获取用户详情（含角色ID列表）
     */
    public SysUser getUserDetail(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }

    /**
     * 获取用户角色ID列表
     */
    public List<Long> getUserRoleIds(Long userId) {
        return userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>()
                                .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
    }

    // ==================== 内部方法 ====================

    private void checkUnique(String username, Long excludeId, String phone, String email) {
        // 用户名唯一
        LambdaQueryWrapper<SysUser> usernameQw = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getIsDeleted, 0);
        if (excludeId != null) usernameQw.ne(SysUser::getId, excludeId);
        if (userMapper.selectCount(usernameQw) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 手机号唯一
        if (StrUtil.isNotBlank(phone)) {
            LambdaQueryWrapper<SysUser> phoneQw = new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getPhone, phone)
                    .eq(SysUser::getIsDeleted, 0);
            if (excludeId != null) phoneQw.ne(SysUser::getId, excludeId);
            if (userMapper.selectCount(phoneQw) > 0) {
                throw new BusinessException(400, "手机号已被使用");
            }
        }

        // 邮箱唯一
        if (StrUtil.isNotBlank(email)) {
            LambdaQueryWrapper<SysUser> emailQw = new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getEmail, email)
                    .eq(SysUser::getIsDeleted, 0);
            if (excludeId != null) emailQw.ne(SysUser::getId, excludeId);
            if (userMapper.selectCount(emailQw) > 0) {
                throw new BusinessException(400, "邮箱已被使用");
            }
        }
    }

    private void saveUserRoles(Long userId, java.util.Set<Long> roleIds) {
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }
}
