package com.example.eams.repair.dto;

import lombok.Data;
import javax.validation.constraints.*;

/** 报修申请 DTO（PRD 6.8.1） */
@Data
public class RepairApplyDTO {

    @NotNull(message = "请选择报修资产")
    private Long assetId;

    @NotBlank(message = "请选择故障类型")
    private String faultType;

    /** 紧急程度: 0-普通, 1-紧急 */
    @NotNull(message = "请选择紧急程度")
    private Integer urgency;

    @NotBlank(message = "故障描述为10-500个字符")
    @Size(min = 10, max = 500, message = "故障描述为10-500个字符")
    private String faultDesc;

    /** 故障图片URL逗号分隔 */
    private String faultImages;

    @NotBlank(message = "请输入正确的联系电话")
    @Pattern(regexp = "1[3-9]\\d{9}", message = "请输入正确的联系电话")
    private String contactPhone;

    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;
}
