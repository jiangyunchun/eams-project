package com.example.eams.security.aspect;

import com.example.eams.common.exception.BusinessException;
import com.example.eams.security.annotation.RequireRole;
import com.example.eams.security.filter.SecurityContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/**
 * 角色权限校验 AOP 切面
 * <p>
 * 拦截 @RequireRole 注解的方法，校验当前登录用户角色。
 */
@Aspect
@Component
public class RoleAspect {

    /**
     * 环绕通知：校验 @RequireRole 注解
     */
    @Around("@annotation(com.example.eams.security.annotation.RequireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        String[] requiredRoles = requireRole.value();

        // 2. 获取当前用户角色
        Set<String> userRoles = SecurityContextHolder.getCurrentRoles();

        // 3. 校验（满足任一角色即可）
        boolean hasRole = Arrays.stream(requiredRoles).anyMatch(userRoles::contains);
        if (!hasRole) {
            throw BusinessException.forbidden("您没有权限执行此操作");
        }

        // 4. 放行
        return joinPoint.proceed();
    }
}
