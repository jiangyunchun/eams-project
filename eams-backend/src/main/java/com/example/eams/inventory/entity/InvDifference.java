package com.example.eams.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 盘点差异表 (inv_difference)
 * <p>
 * 对应 PRD 6.4.3 盘点差异记录 / 技术方案 4.3.9
 * 差异类型枚举: 0-盘盈, 1-盘亏
 * 处理状态枚举: 0-待处理, 1-已处理
 */
@Data
@TableName("inv_difference")
public class InvDifference {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属盘点任务ID (FK → inv_task.id) */
    private Long taskId;

    /** 盘点明细ID (FK → inv_detail.id) */
    private Long detailId;

    /** 差异类型: 0-盘盈, 1-盘亏 */
    private Integer diffType;

    /** 关联资产ID（盘盈时为新生成资产ID） (FK → asset_info.id) */
    private Long assetId;

    /** 资产名称快照 */
    private String assetName;

    /** 资产编码快照 */
    private String assetCode;

    /** 账面数量 */
    private Integer bookQty;

    /** 实盘数量 */
    private Integer actualQty;

    /** 差异原因说明 */
    private String diffDesc;

    /** 处理状态: 0-待处理, 1-已处理 */
    private Integer handleStatus;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 处理人ID (FK → sys_user.id) */
    private Long handlerId;

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

    /** 盘点任务编号（展示字段） */
    @TableField(exist = false)
    private String taskNo;

    /** 盘点任务名称（展示字段） */
    @TableField(exist = false)
    private String taskName;

    /** 盘点日期（展示字段） */
    @TableField(exist = false)
    private String inventoryDate;

    /** 差异类型标签（展示字段） */
    @TableField(exist = false)
    private String diffTypeLabel;

    /** 处理状态标签（展示字段） */
    @TableField(exist = false)
    private String handleStatusLabel;

    /** 处理人姓名（展示字段） */
    @TableField(exist = false)
    private String handlerName;
}
