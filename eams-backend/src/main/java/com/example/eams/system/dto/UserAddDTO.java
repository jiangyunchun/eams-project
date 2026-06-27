package com.example.eams.system.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.util.Set;

/**
 * 新增用户请求
 */
@Data
public class UserAddDTO {
    @NotBlank(message = "用户名为4-20位，字母开头，仅支持字母、数字、下划线")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{3,19}$", message = "用户名为4-20位，字母开头，仅支持字母、数字、下划线")
    private String username;

    @NotBlank(message = "姓名为2-20个字符")
    @Size(min = 2, max = 20, message = "姓名为2-20个字符")
    private String realName;

    @NotBlank(message = "密码为6-20位，须包含字母和数字")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{6,20}$", message = "密码为6-20位，须包含字母和数字")
    private String password;

    @NotNull(message = "请选择所属部门")
    private Long deptId;

    @NotEmpty(message = "请至少选择一个角色")
    private Set<Long> roleIds;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "请输入正确的手机号")
    private String phone;

    @Email(message = "请输入正确的邮箱地址")
    private String email;

    private Integer status = 1;
}
