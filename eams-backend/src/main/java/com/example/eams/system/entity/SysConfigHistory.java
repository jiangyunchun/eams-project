package com.example.eams.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统参数变更历史表
 */
@Data
@TableName("sys_config_history")
public class SysConfigHistory {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long configId;
    private String paramKey;
    private String oldValue;
    private String newValue;
    private String changeReason;

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
