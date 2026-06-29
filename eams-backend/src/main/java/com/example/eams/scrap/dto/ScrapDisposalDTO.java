package com.example.eams.scrap.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 报废处置登记 DTO（PRD 6.9.3）
 */
@Data
public class ScrapDisposalDTO {

    /** 报废单ID */
    @NotNull(message = "报废单ID不能为空")
    private Long scrapId;

    /** 处置方式: 变卖/回收/销毁/其他 */
    @NotBlank(message = "请选择处置方式")
    private String disposalMethod;

    /** 处置日期 */
    @NotNull(message = "请选择处置日期")
    private LocalDate disposalDate;

    /** 处置收入（元），≥0 */
    @NotNull(message = "处置收入不能为负数")
    @DecimalMin(value = "0.00", message = "处置收入不能为负数")
    private BigDecimal disposalIncome;

    /** 处置费用（元），≥0 */
    @NotNull(message = "处置费用不能为负数")
    @DecimalMin(value = "0.00", message = "处置费用不能为负数")
    private BigDecimal disposalCost;

    /** 经办人姓名 */
    @NotBlank(message = "请填写经办人姓名")
    @Size(min = 2, max = 20, message = "经办人姓名为2-20个字符")
    private String disposalHandler;

    /** 处置过程说明（最大500字符） */
    @Size(max = 500, message = "处置说明不能超过500个字符")
    private String disposalDesc;

    /** 处置附件URL（逗号分隔，可选） */
    private String attachmentUrls;
}
