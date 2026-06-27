package com.example.eams.system.dto;

import lombok.Data;

import javax.validation.constraints.*;

/**
 * 登录请求
 */
@Data
public class LoginDTO {
    @NotBlank(message = "请输入用户名和密码")
    private String username;

    @NotBlank(message = "请输入用户名和密码")
    private String password;

    /** 是否记住我: true-Token有效期7天, false-2小时 */
    private Boolean rememberMe = false;
}
