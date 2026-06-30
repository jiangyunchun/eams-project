package com.example.eams.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 盘点任务表 (inv_task)
 * <p>
 * 对应 PRD 6.4.1 盘点任务管理 / 技术方案 4.3.9
 * 盘点状态枚举: 0-进行中, 1-已完成, 2-已取消
 */
@Data
@TableName("inv_task")
public class InvTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编号（全局唯一）: PD-YYYYMMDD-XXX */
    private String taskNo;

    /** 任务名称: 2026年Q2全公司资产盘点 */
    private String taskName;

    /** 范围类型: ALL-全公司, DEPT-按部门, CATEGORY-按分类 */
    private String scopeType;

    /** 范围值JSON: {"deptIds":[1,2]} 或 {"categoryCodes":["IT_EQUIPMENT"]} */
    private String scopeValue;

    /** 计划盘点日期 */
    private LocalDate inventoryDate;

    /** 应盘资产总数（任务创建时锁定快照） */
    private Integer totalCount;

    /** 已确认资产数 */
    private Integer checkedCount;

    /** 盘点正常数量 */
    private Integer normalCount;

    /** 盘盈数量 */
    private Integer surplusCount;

    /** 盘亏数量 */
    private Integer shortageCount;

    /** 盘点状态: 0-进行中, 1-已完成, 2-已取消（PRD 5.5 盘点状态枚举） */
    private Integer status;

    /** 任务备注 */
    private String remark;

    /** 任务创建人ID (FK → sys_user.id) */
    private Long creatorId;

    // ---- 审计字段 ----

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

    // ---- 非数据库字段（用于列表展示） ----

    /** 创建人姓名（展示字段） */
    @TableField(exist = false)
    private String creatorName;

    /** 完成率百分比（展示字段）: checkedCount/totalCount*100 */
    @TableField(exist = false)
    private Integer completionRate;

    /** 范围类型标签（展示字段） */
    @TableField(exist = false)
    private String scopeTypeLabel;

    /** 盘点状态标签（展示字段） */
    @TableField(exist = false)
    private String statusLabel;
}
