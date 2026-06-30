package com.example.eams.inventory.dto;

import lombok.Data;

/**
 * 盘点差异查询 DTO（PRD 6.4.3 查询区）
 */
@Data
public class InvDifferenceQueryDTO {

    /** 页码（默认1） */
    private Integer pageNum = 1;

    /** 每页条数（默认10，可选10/20/50/100） */
    private Integer pageSize = 10;

    /** 盘点任务ID（精确） */
    private Long taskId;

    /** 任务编号（模糊） */
    private String taskNo;

    /** 差异类型: 0-盘盈, 1-盘亏 */
    private Integer diffType;

    /** 资产编码（模糊） */
    private String assetCode;

    /** 处理状态: 0-待处理, 1-已处理 */
    private Integer handleStatus;

    /** 盘点开始日期 (YYYY-MM-DD) */
    private String beginDate;

    /** 盘点结束日期 (YYYY-MM-DD) */
    private String endDate;
}
