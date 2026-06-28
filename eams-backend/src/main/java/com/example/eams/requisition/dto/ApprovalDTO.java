package com.example.eams.requisition.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 审批操作 DTO（PRD 6.3.2）
 */
@Data
public class ApprovalDTO {

    @NotNull(message = "领用申请ID不能为空")
    private Long requisitionId;

    /** 审批结果: 1-通过, 0-驳回（Controller 层自动赋值，无需前端传） */
    private Integer approvalResult;

    /** 驳回原因（驳回时必填） */
    @Size(min = 10, max = 200, message = "驳回原因为10-200个字符")
    private String rejectReason;
}
