package com.example.eams.scrap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 报废申请单表 (scrap_order)
 * <p>
 * 对应 PRD 6.9 报废处置模块 / 技术方案 4.3.8
 * 报废状态枚举: 0-待初审, 1-待终审, 2-已通过(待处置), 3-已驳回, 4-已处置(归档)
 */
@Data
@TableName("scrap_order")
public class ScrapOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报废编号（全局唯一）: BF-YYYYMMDD-XXXX */
    private String scrapNo;

    /** 报废资产ID (FK → asset_info.id) */
    private Long assetId;

    /** 报废原因: 老化损坏/技术淘汰/维修成本过高/盘亏确认/其他 */
    private String scrapReason;

    /** 报废原因详细说明: 10-500字符 */
    private String reasonDesc;

    /** 处置方式建议: 变卖/回收/销毁/其他 */
    private String disposalAdvice;

    /** 报废状态: 0-待初审, 1-待终审, 2-已通过(待处置), 3-已驳回, 4-已处置(归档) */
    private Integer status;

    /** 报废申请人ID (FK → sys_user.id) */
    private Long applicantId;

    /** 申请备注（驳回时用于存储驳回原因） */
    private String remark;

    /** 实际处置方式: 变卖/回收/销毁/其他 */
    private String disposalMethod;

    /** 实际处置日期 */
    private LocalDate disposalDate;

    /** 处置收入（元），变卖时填写 */
    private BigDecimal disposalIncome;

    /** 处置费用（元） */
    private BigDecimal disposalCost;

    /** 处置经办人姓名 */
    private String disposalHandler;

    /** 处置过程说明 */
    private String disposalDesc;

    /** 报废/处置相关附件URL（逗号分隔） */
    private String attachmentUrls;

    /** 乐观锁版本号 */
    private Integer version;

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

    /** 资产原值（展示字段） */
    @TableField(exist = false)
    private BigDecimal originalValue;

    /** 资产净值（展示字段） */
    @TableField(exist = false)
    private BigDecimal netValue;

    /** 采购日期（展示字段，用于计算已用年限） */
    @TableField(exist = false)
    private LocalDate purchaseDate;

    /** 资产分类（展示字段） */
    @TableField(exist = false)
    private String category;

    /** 规格型号（展示字段） */
    @TableField(exist = false)
    private String specification;

    /** 存放地点（展示字段） */
    @TableField(exist = false)
    private String location;

    /** 所属部门ID（展示字段） */
    @TableField(exist = false)
    private Long deptId;

    /** 所属部门名称（展示字段） */
    @TableField(exist = false)
    private String deptName;

    /** 资产当前状态（展示字段） */
    @TableField(exist = false)
    private Integer assetStatus;

    /** 申请人姓名（展示字段） */
    @TableField(exist = false)
    private String applicantName;

    /** 报废原因标签（展示字段） */
    @TableField(exist = false)
    private String scrapReasonLabel;

    /** 处置建议标签（展示字段） */
    @TableField(exist = false)
    private String disposalAdviceLabel;

    /** 报废状态标签（展示字段） */
    @TableField(exist = false)
    private String statusLabel;

    /** 已用年限（展示字段） */
    @TableField(exist = false)
    private String usedYears;
}
