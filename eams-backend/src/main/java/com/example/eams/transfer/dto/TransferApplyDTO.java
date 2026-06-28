package com.example.eams.transfer.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 调拨申请 DTO（PRD 6.7.1）
 */
@Data
public class TransferApplyDTO {

    @NotNull(message = "请选择调拨资产")
    private Long assetId;

    @NotNull(message = "请选择调入部门")
    private Long toDeptId;

    /** 调入使用人（可选） */
    private Long toUserId;

    @NotBlank(message = "调入地点为2-50个字符")
    @Size(min = 2, max = 50, message = "调入地点为2-50个字符")
    private String toLocation;

    @NotBlank(message = "调拨原因为10-500个字符")
    @Size(min = 10, max = 500, message = "调拨原因为10-500个字符")
    private String transferReason;

    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;
}
