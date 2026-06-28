package com.example.eams.procurement.dto;

import lombok.Data;

/**
 * 采购记录查询请求
 */
@Data
public class ProcurementQueryDTO {

    /** 采购单号（模糊搜索） */
    private String procurementNo;

    /** 资产名称（模糊搜索） */
    private String assetName;

    /** 供应商ID */
    private Long supplierId;

    /** 验收状态: 0-待验收, 1-已验收, 2-已入库, 3-已取消 */
    private Integer acceptStatus;

    /** 采购日期起 */
    private String beginDate;

    /** 采购日期止 */
    private String endDate;

    private int pageNum = 1;

    private int pageSize = 10;
}
