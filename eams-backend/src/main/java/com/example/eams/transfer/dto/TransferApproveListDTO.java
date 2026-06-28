package com.example.eams.transfer.dto;

import lombok.Data;

/**
 * 审批列表查询 DTO（PRD 6.7.2）
 */
@Data
public class TransferApproveListDTO {

    private String transferNo;
    private String fromDeptName;
    private String toDeptName;
    private Integer status;
    private String beginDate;
    private String endDate;

    private int pageNum = 1;
    private int pageSize = 10;
}
