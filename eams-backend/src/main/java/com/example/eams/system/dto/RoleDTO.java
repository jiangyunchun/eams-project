package com.example.eams.system.dto;

import lombok.Data;

import javax.validation.constraints.*;

/**
 * 角色新增/编辑请求
 */
@Data
public class RoleDTO {
    private Long id;

    @NotBlank(message = "角色名称为2-20个字符")
    @Size(min = 2, max = 20, message = "角色名称为2-20个字符")
    private String roleName;

    @NotBlank(message = "角色编码须为大写字母+下划线格式")
    @Pattern(regexp = "^[A-Z_]{1,50}$", message = "角色编码须为大写字母+下划线格式")
    private String roleCode;

    @Size(max = 200, message = "描述不能超过200个字符")
    private String description;
}

