package com.example.eams.scrap.dto;

import lombok.Data;

/**
 * 报废单查询 DTO（PRD 6.9.2 查询区 + 6.9.4 记录查询）
 */
@Data
public class ScrapQueryDTO {

    /** 页码（默认1） */
    private Integer pageNum = 1;

    /** 每页条数（默认10，可选10/20/50/100） */
    private Integer pageSize = 10;

    /** 报废编号（精确） */
    private String scrapNo;

    /** 资产名称（模糊） */
    private String assetName;

    /** 资产编码（精确） */
    private String assetCode;

    /** 报废原因 */
    private String scrapReason;

    /** 报废状态: 0-待初审, 1-待终审, 2-已通过(待处置), 3-已驳回, 4-已处置 */
    private Integer status;

    /** 申请人（模糊） */
    private String applicantName;

    /** 申请开始时间 (YYYY-MM-DD) */
    private String beginDate;

    /** 申请结束时间 (YYYY-MM-DD) */
    private String endDate;
}
