package com.example.eams.requisition.dto;

import lombok.Data;

/**
 * 审批列表查询 DTO（PRD 6.3.2）
 */
@Data
public class ApprovalQueryDTO {

    /** 申请编号 */
    private String applyNo;

    /** 申请人 */
    private String applicantName;

    /** 资产名称 */
    private String assetName;

    /** 状态 */
    private Integer status;

    /** 申请时间起 */
    private String beginDate;

    /** 申请时间止 */
    private String endDate;

    private int pageNum = 1;

    private int pageSize = 10;
}
