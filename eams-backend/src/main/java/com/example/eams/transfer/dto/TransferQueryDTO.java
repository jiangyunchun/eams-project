package com.example.eams.transfer.dto;

import lombok.Data;

/**
 * 调拨记录查询 DTO（PRD 6.7.3）
 */
@Data
public class TransferQueryDTO {

    private String transferNo;
    private String fromDeptName;
    private String toDeptName;
    private Integer status;
    private String beginDate;
    private String endDate;

    private int pageNum = 1;
    private int pageSize = 10;
}
