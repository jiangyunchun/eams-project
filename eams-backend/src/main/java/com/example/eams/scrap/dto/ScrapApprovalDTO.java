package com.example.eams.scrap.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 报废审批 DTO（PRD 6.9.2）
 * <p>
 * 用于初审和终审的通过/驳回操作。
 * approvalResult: 1-通过, 0-驳回
 */
@Data
public class ScrapApprovalDTO {

    /** 报废单ID */
    @NotNull(message = "报废单ID不能为空")
    private Long scrapId;

    /** 审批结果: 1-通过, 0-驳回 */
    @NotNull(message = "审批结果不能为空")
    private Integer approvalResult;

    /** 驳回原因（驳回时必填，10-200字符） */
    @Size(min = 10, max = 200, message = "驳回原因为10-200个字符")
    private String rejectReason;

    /** 审批意见（通过时可选） */
    @Size(max = 500, message = "审批意见不能超过500个字符")
    private String approveOpinion;
}
