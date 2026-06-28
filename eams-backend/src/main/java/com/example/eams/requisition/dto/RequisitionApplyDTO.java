package com.example.eams.requisition.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 领用申请提交 DTO（PRD 6.3.1）
 */
@Data
public class RequisitionApplyDTO {

    @NotNull(message = "请选择领用资产")
    private Long assetId;

    @NotBlank(message = "领用用途为10-500个字符")
    @Size(min = 10, max = 500, message = "领用用途为10-500个字符")
    private String purpose;

    @NotBlank(message = "请选择预计领用时长")
    private String expectDuration;

    @NotNull(message = "请选择预计归还日期")
    private LocalDate expectReturnDate;

    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;
}
