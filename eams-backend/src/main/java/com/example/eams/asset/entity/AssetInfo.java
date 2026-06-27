package com.example.eams.asset.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产信息表（核心主表）
 */
@Data
@TableName("asset_info")
public class AssetInfo {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资产编码: AS-{类别码}-{YYMM}-{4位流水} */
    private String assetCode;

    /** 资产名称 */
    private String assetName;

    /** 资产分类（字典值）: IT_EQUIPMENT */
    private String category;

    /** 规格型号 */
    private String specification;

    /** SN序列号 */
    private String snNumber;

    /** 关联采购单号 */
    private String procurementNo;

    /** 关联采购记录ID */
    private Long procurementId;

    /** 原值（元） */
    private BigDecimal originalValue;

    /** 采购日期 */
    private LocalDate purchaseDate;

    /** 使用年限（年）: 1-50 */
    private Integer usefulLife;

    /** 净残值率(%): 默认5% */
    private BigDecimal residualRate;

    /** 预计报废日期（后端自动计算） */
    private LocalDate scrapDate;

    /** 存放地点 */
    private String location;

    /** 资产归属部门ID */
    private Long deptId;

    /** 当前使用人ID，闲置为NULL */
    private Long userId;

    /** 资产状态: 0-闲置,1-在用,2-借用,3-维修,4-报废,5-盘点中 */
    private Integer status;

    /** 资产图片URL */
    private String imageUrl;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    private Integer version;

    // ---- transient fields ----

    /** 部门名称（非数据库字段） */
    @TableField(exist = false)
    private String deptName;

    /** 使用人姓名（非数据库字段） */
    @TableField(exist = false)
    private String userName;

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
