package com.example.eams.procurement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购入库记录表
 */
@Data
@TableName("proc_order")
public class ProcurementOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 采购单号（外部采购单据号） */
    private String procurementNo;

    /** 采购资产名称 */
    private String assetName;

    /** 资产分类（字典值） */
    private String category;

    /** 规格型号 */
    private String specification;

    /** SN序列号 */
    private String snNumber;

    /** 采购数量: 1-100 */
    private Integer quantity;

    /** 单价（元） */
    private BigDecimal unitPrice;

    /** 采购总价（元）= 单价 × 数量 */
    private BigDecimal totalAmount;

    /** 采购日期 */
    private LocalDate purchaseDate;

    /** 供应商ID */
    private Long supplierId;

    /** 预计使用年限（年）: 1-50 */
    private Integer usefulLife;

    /** 净残值率(%): 默认5% */
    private BigDecimal residualRate;

    /** 资产归属部门ID */
    private Long deptId;

    /** 存放地点 */
    private String location;

    /** 验收状态: 0-待验收, 1-已验收, 2-已入库, 3-已取消 */
    private Integer acceptStatus;

    /** 验收日期 */
    private LocalDate acceptDate;

    /** 备注 */
    private String remark;

    // ---- transient fields ----

    /** 供应商名称（非数据库字段） */
    @TableField(exist = false)
    private String supplierName;

    /** 部门名称（非数据库字段） */
    @TableField(exist = false)
    private String deptName;

    /** 分类标签（非数据库字段） */
    @TableField(exist = false)
    private String categoryLabel;

    /** 验收状态标签（非数据库字段） */
    @TableField(exist = false)
    private String acceptStatusLabel;

    // ---- audit fields ----

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
