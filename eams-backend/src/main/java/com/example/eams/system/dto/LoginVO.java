package com.example.eams.system.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 登录成功响应
 */
@Data
public class LoginVO {
    private String token;
    private UserInfo userInfo;

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String realName;
        private Long deptId;
        private String deptName;
        private Set<String> roles;
        private List<String> permissions;
    }
}
