package com.example.eams.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户表
 */
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String realName;
    private Long deptId;
    private String phone;
    private String email;

    @TableField("`status`")
    private Integer status;

    private LocalDateTime lastLoginTime;
    private String lastLoginIp;

    /** 部门名称（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private String deptName;

    /** 角色名称列表（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private String roleNames;

    /** 是否被锁定（非数据库字段，查询时从 Redis 读取） */
    @TableField(exist = false)
    private Boolean locked;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
