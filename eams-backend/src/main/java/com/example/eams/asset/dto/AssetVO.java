package com.example.eams.asset.dto;

import com.example.eams.asset.entity.AssetDepreciation;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资产详情响应（含折旧信息 + 操作记录）
 */
@Data
public class AssetVO {
    // ---- 基本信息 ----
    private Long id;
    private String assetCode;
    private String assetName;
    private String category;
    private String specification;
    private String snNumber;
    private String procurementNo;
    private BigDecimal originalValue;
    private LocalDate purchaseDate;
    private Integer usefulLife;
    private BigDecimal residualRate;
    private LocalDate scrapDate;
    private String location;
    private Long deptId;
    private String deptName;
    private Long userId;
    private String userName;
    private Integer status;
    private String imageUrl;
    private String remark;
    private Integer version;
    private LocalDateTime createTime;

    // ---- 折旧信息 ----
    private Integer depreciatedMonths;     // 已计提月数
    private BigDecimal monthlyAmount;      // 月折旧额
    private BigDecimal accumulated;        // 累计折旧
    private BigDecimal netValue;           // 净值
    private List<AssetDepreciation> depreciationList;
}
