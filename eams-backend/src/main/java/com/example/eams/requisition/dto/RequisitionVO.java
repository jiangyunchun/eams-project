package com.example.eams.requisition.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 领用记录详情 VO（PRD 6.3.4）
 */
@Data
public class RequisitionVO {

    private Long id;
    private String applyNo;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private String category;
    private String specification;
    private String location;
    private String imageUrl;
    private Long applicantId;
    private String applicantName;
    private Long applicantDeptId;
    private String deptName;
    private String purpose;
    private String expectDuration;
    private LocalDate expectReturnDate;
    private Integer status;
    private String statusLabel;
    private String remark;
    private LocalDate returnDate;
    private Integer returnAssetStatus;
    private String returnDamageDesc;
    private String returnRemark;
    private Integer version;

    // 时间线字段
    private LocalDateTime createTime;
    private String createBy;
    private LocalDateTime approvalTime;
    private String approverName;
    private LocalDateTime updateTime;
}
