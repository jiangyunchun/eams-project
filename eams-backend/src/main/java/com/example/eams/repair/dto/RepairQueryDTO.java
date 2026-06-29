package com.example.eams.repair.dto;

import lombok.Data;

/** 维保查询 DTO（PRD 6.8.2/6.8.3） */
@Data
public class RepairQueryDTO {

    private String repairNo;
    private String assetName;
    private String faultType;
    private Integer repairStatus;
    private String applicantName;
    private String beginDate;
    private String endDate;

    private int pageNum = 1;
    private int pageSize = 10;
}
