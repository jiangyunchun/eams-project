package com.example.eams.procurement.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购记录详情 VO
 */
@Data
public class ProcurementVO {

    private Long id;
    private String procurementNo;
    private String assetName;
    private String category;
    private String categoryLabel;
    private String specification;
    private String snNumber;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private LocalDate purchaseDate;
    private Long supplierId;
    private String supplierName;
    private Integer usefulLife;
    private BigDecimal residualRate;
    private Long deptId;
    private String deptName;
    private String location;
    private Integer acceptStatus;
    private String acceptStatusLabel;
    private LocalDate acceptDate;
    private String remark;
    private LocalDateTime createTime;
    private String createBy;
}
