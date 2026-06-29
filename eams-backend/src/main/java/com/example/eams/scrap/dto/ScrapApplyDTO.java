package com.example.eams.scrap.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 报废申请 DTO（PRD 6.9.1 步骤2）
 */
@Data
public class ScrapApplyDTO {

    /** 报废资产ID */
    @NotNull(message = "请选择报废资产")
    private Long assetId;

    /** 报废原因: 老化损坏/技术淘汰/维修成本过高/盘亏确认/其他 */
    @NotBlank(message = "请选择报废原因")
    private String scrapReason;

    /** 报废原因详细说明: 10-500字符 */
    @NotBlank(message = "原因说明为10-500个字符")
    @Size(min = 10, max = 500, message = "原因说明为10-500个字符")
    private String reasonDesc;

    /** 处置方式建议: 变卖/回收/销毁/其他 */
    @NotBlank(message = "请选择处置建议")
    private String disposalAdvice;

    /** 附件URL（逗号分隔，可选） */
    private String attachmentUrls;

    /** 申请备注（可选，最大200字符） */
    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;
}
