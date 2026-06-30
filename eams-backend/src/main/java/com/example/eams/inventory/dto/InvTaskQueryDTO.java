package com.example.eams.inventory.dto;

import lombok.Data;

/**
 * 盘点任务查询 DTO（PRD 6.4.1 查询区）
 */
@Data
public class InvTaskQueryDTO {

    /** 页码（默认1） */
    private Integer pageNum = 1;

    /** 每页条数（默认10，可选10/20/50/100） */
    private Integer pageSize = 10;

    /** 任务编号（模糊） */
    private String taskNo;

    /** 任务名称（模糊） */
    private String taskName;

    /** 盘点状态: 0-进行中, 1-已完成, 2-已取消 */
    private Integer status;

    /** 盘点开始日期 (YYYY-MM-DD) */
    private String beginDate;

    /** 盘点结束日期 (YYYY-MM-DD) */
    private String endDate;

    /** 创建人（模糊） */
    private String creatorName;
}
