package com.example.eams.requisition.dto;

import lombok.Data;

/**
 * 领用记录查询 DTO（PRD 6.3.4）
 */
@Data
public class RequisitionQueryDTO {

    /** 申请编号 */
    private String applyNo;

    /** 申请人 */
    private String applicantName;

    /** 资产编码 */
    private String assetCode;

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
