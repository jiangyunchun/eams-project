package com.example.eams.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eams.common.exception.BusinessException;
import com.example.eams.common.util.BCryptUtil;
import com.example.eams.common.util.JwtUtil;
import com.example.eams.common.util.RedisUtil;
import com.example.eams.system.dto.LoginDTO;
import com.example.eams.system.dto.LoginVO;
import com.example.eams.system.entity.*;
import com.example.eams.system.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 登录认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private static final int MAX_FAIL_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 30;
    private static final int TOKEN_EXPIRE_HOURS = 2;
    private static final int REMEMBER_EXPIRE_HOURS = 168; // 7天
    private static final String FAIL_KEY_PREFIX = "eams:login:fail:";
    private static final String LOCK_KEY_PREFIX = "eams:login:lock:";

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysLoginLogMapper loginLogMapper;

    /**
     * 用户登录
     */
    public LoginVO login(LoginDTO dto, HttpServletRequest request) {
        String username = StrUtil.trimToNull(dto.getUsername());
        String password = dto.getPassword();

        // 1. 检查账号锁定
        checkLock(username, request);

        // 2. 查询用户
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getIsDeleted, 0));

        if (user == null) {
            recordLoginFail(username, request);
            throw new BusinessException(400, "用户名或密码错误");
        }

        // 3. 校验密码
        if (!BCryptUtil.matches(password, user.getPassword())) {
            recordLoginFail(username, request);
            throw new BusinessException(400, "用户名或密码错误");
        }

        // 4. 检查账号状态
        if (user.getStatus() == 0) {
            recordLoginLog(username, 0, "账号已禁用", request);
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        // 5. 登录成功：清除失败计数
        RedisUtil.del(FAIL_KEY_PREFIX + username);

        // 6. 生成JWT
        Set<String> roles = getUserRoles(user.getId());
        int expireHours = Boolean.TRUE.equals(dto.getRememberMe()) ? REMEMBER_EXPIRE_HOURS : TOKEN_EXPIRE_HOURS;
        String token = JwtUtil.generate(user.getId(), user.getUsername(), roles, expireHours);

        // 7. 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(getIp(request));
        userMapper.updateById(user);

        // 8. 记录登录日志
        recordLoginLog(username, 1, null, request);

        // 9. 组装响应
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        LoginVO.UserInfo userInfo = new LoginVO.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setDeptId(user.getDeptId());
        userInfo.setRoles(roles);
        userInfo.setPermissions(getUserPermissions(user.getId()));
        vo.setUserInfo(userInfo);

        return vo;
    }

    /**
     * 主动登出
     */
    public void logout(String token) {
        // 将 JTI 加入黑名单，过期时间 = Token剩余有效期
        try {
            String jti = JwtUtil.getJti(token);
            long remainingMs = JwtUtil.getRemainingTime(token);
            if (remainingMs > 0) {
                RedisUtil.set("eams:token:blacklist:" + jti, "1", (int) (remainingMs / 1000));
            }
        } catch (Exception e) {
            log.warn("登出Token处理异常", e);
        }
    }

    /**
     * 获取当前用户信息（用于刷新前端状态）
     */
    public LoginVO getCurrentUserInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        Set<String> roles = getUserRoles(userId);

        LoginVO vo = new LoginVO();
        LoginVO.UserInfo userInfo = new LoginVO.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setDeptId(user.getDeptId());
        userInfo.setRoles(roles);
        userInfo.setPermissions(getUserPermissions(userId));
        vo.setUserInfo(userInfo);
        return vo;
    }

    // ==================== 内部方法 ====================

    private void checkLock(String username, HttpServletRequest request) {
        String lockKey = LOCK_KEY_PREFIX + username;
        if (RedisUtil.exists(lockKey)) {
            recordLoginLog(username, 0, "账号已锁定", request);
            long ttl = RedisUtil.getExpire(lockKey);
            long minutes = Math.max(1, ttl / 60);
            throw new BusinessException(403, "账号已被锁定，请" + minutes + "分钟后重试");
        }
    }

    private void recordLoginFail(String username, HttpServletRequest request) {
        String failKey = FAIL_KEY_PREFIX + username;
        long attempts = RedisUtil.incr(failKey, LOCK_MINUTES * 60);
        log.warn("登录失败 username={}, attempts={}", username, attempts);

        if (attempts >= MAX_FAIL_ATTEMPTS) {
            RedisUtil.set(LOCK_KEY_PREFIX + username, "1", LOCK_MINUTES * 60);
            RedisUtil.del(failKey);
            recordLoginLog(username, 0, "账号已锁定-密码连续错误", request);
            throw new BusinessException(403, "账号已被锁定，请30分钟后重试");
        }
        recordLoginLog(username, 0, "密码错误", request);
    }

    private void recordLoginLog(String username, int status, String failReason, HttpServletRequest request) {
        SysLoginLog logRecord = new SysLoginLog();
        logRecord.setUsername(username);
        logRecord.setLoginStatus(status);
        logRecord.setFailReason(failReason);
        logRecord.setIpAddress(getIp(request));
        logRecord.setUserAgent(request.getHeader("User-Agent"));
        loginLogMapper.insert(logRecord);
    }

    private Set<String> getUserRoles(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        if (roleIds.isEmpty()) return Collections.emptySet();

        return roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toSet());
    }

    private List<String> getUserPermissions(Long userId) {
        // 查询用户拥有的菜单权限标识
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        if (roleIds.isEmpty()) return Collections.emptyList();

        List<Long> menuIds = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>()
                        .in(SysRoleMenu::getRoleId, roleIds))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());

        if (menuIds.isEmpty()) return Collections.emptyList();

        return menuMapper.selectBatchIds(menuIds).stream()
                .filter(m -> m.getPermission() != null && !m.getPermission().isEmpty())
                .map(SysMenu::getPermission)
                .collect(Collectors.toList());
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) return "127.0.0.1";
        return ip;
    }
}
