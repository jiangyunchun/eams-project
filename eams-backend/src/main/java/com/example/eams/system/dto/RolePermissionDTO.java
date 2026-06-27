package com.example.eams.system.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.util.Set;

/**
 * 角色权限配置请求
 */
@Data
public class RolePermissionDTO {
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @NotEmpty(message = "请选择至少一个权限")
    private Set<Long> menuIds;
}
