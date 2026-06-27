package com.example.eams.asset.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资产折旧明细表
 */
@Data
@TableName("asset_depreciation")
public class AssetDepreciation {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资产ID */
    private Long assetId;

    /** 计提月份: 2026-06 */
    private String depreciationMonth;

    /** 月折旧额（元） */
    private BigDecimal monthlyAmount;

    /** 累计折旧（元） */
    private BigDecimal accumulated;

    /** 资产净值（元）= 原值 - 累计折旧 */
    private BigDecimal netValue;

    /** 计提状态: 0-待计提, 1-已计提 */
    private Integer status;

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
