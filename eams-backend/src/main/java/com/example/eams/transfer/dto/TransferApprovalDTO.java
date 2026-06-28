package com.example.eams.transfer.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 调拨审批操作 DTO（PRD 6.7.2）
 */
@Data
public class TransferApprovalDTO {

    @NotNull(message = "调拨申请ID不能为空")
    private Long transferId;

    /** 审批结果: 1-通过, 0-驳回（Controller 自动赋值） */
    private Integer approvalResult;

    /** 驳回原因: 10-200字符 */
    @Size(min = 10, max = 200, message = "驳回原因为10-200个字符")
    private String rejectReason;
}
