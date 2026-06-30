package com.example.eams.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 盘点明细表 (inv_detail)
 * <p>
 * 对应 PRD 6.4.2 执行盘点 / 技术方案 4.3.9
 * 盘点结果枚举: 0-盘盈(账外资产), 1-盘亏(实物缺失), 2-正常
 */
@Data
@TableName("inv_detail")
public class InvDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属盘点任务ID (FK → inv_task.id) */
    private Long taskId;

    /** 资产ID (FK → asset_info.id) */
    private Long assetId;

    /** 账面使用人快照（盘点时刻记录） */
    private String bookUserName;

    /** 账面资产状态快照: 0-闲置,1-在用,2-借用,3-维修,4-报废 */
    private Integer bookStatus;

    /** 盘点结果: 0-盘盈(账外资产), 1-盘亏(实物缺失), 2-正常（PRD 5.5 盘点结果枚举） */
    private Integer inventoryResult;

    /** 实盘现场备注 */
    private String remark;

    /** 是否已确认: 0-未确认, 1-已确认 */
    private Integer isConfirmed;

    /** 确认时间 */
    private LocalDateTime confirmTime;

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

    /** 资产编码（展示字段） */
    @TableField(exist = false)
    private String assetCode;

    /** 资产名称（展示字段） */
    @TableField(exist = false)
    private String assetName;

    /** 资产分类（展示字段） */
    @TableField(exist = false)
    private String category;

    /** 存放地点（展示字段） */
    @TableField(exist = false)
    private String location;

    /** 所属部门名称（展示字段） */
    @TableField(exist = false)
    private String deptName;

    /** 账面状态标签（展示字段） */
    @TableField(exist = false)
    private String bookStatusLabel;

    /** 盘点结果标签（展示字段） */
    @TableField(exist = false)
    private String resultLabel;
}
