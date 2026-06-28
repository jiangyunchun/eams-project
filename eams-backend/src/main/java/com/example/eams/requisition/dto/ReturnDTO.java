package com.example.eams.requisition.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 归还登记 DTO（PRD 6.3.3）
 */
@Data
public class ReturnDTO {

    @NotNull(message = "领用申请ID不能为空")
    private Long requisitionId;

    @NotNull(message = "请选择归还日期")
    private LocalDate returnDate;

    /** 归还时资产完好: 0-完好, 1-有损坏 */
    @NotNull(message = "请选择资产完好状态")
    private Integer returnAssetStatus;

    /** 损坏说明（有损坏时必填） */
    @Size(min = 10, max = 500, message = "请描述资产损坏情况")
    private String returnDamageDesc;

    @Size(max = 200, message = "备注不能超过200个字符")
    private String returnRemark;
}
