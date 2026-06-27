package com.example.eams.security.annotation;

import java.lang.annotation.*;

/**
 * 角色权限校验注解
 * <p>
 * 标注在 Controller 方法上，通过 AOP 切面校验当前用户是否拥有指定角色。
 * 满足任一角色即可通过（OR 逻辑）。
 *
 * <pre>
 * &#064;RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
 * &#064;PostMapping("/add")
 * public Result<?> add() { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /** 允许的角色编码集合 */
    String[] value();
}
