package com.example.eams.requisition.dto;

import lombok.Data;

/**
 * 归还列表查询 DTO（PRD 6.3.3）
 */
@Data
public class ReturnQueryDTO {

    /** 资产编码 */
    private String assetCode;

    /** 资产名称 */
    private String assetName;

    /** 使用人 */
    private String userName;

    /** 领用日期起 */
    private String beginDate;

    /** 领用日期止 */
    private String endDate;

    private int pageNum = 1;

    private int pageSize = 10;
}
