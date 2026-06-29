package com.example.eams.repair.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 维修处理 DTO（PRD 6.8.2 - 接单/完成/无法修复共用） */
@Data
public class RepairHandleDTO {

    @NotNull(message = "报修单ID不能为空")
    private Long repairOrderId;

    /** 操作类型: accept/complete/unfixable（Controller 自动赋值） */
    private String action;

    // ---- 维修记录字段（accept及之后都需填） ----
    @NotBlank(message = "请选择维修方式")
    private String repairMethod;

    @NotBlank(message = "请填写维修人员姓名")
    @Size(min = 2, max = 20, message = "请填写维修人员姓名")
    private String repairPerson;

    @NotNull(message = "维修费用不能为负数")
    @DecimalMin(value = "0", message = "维修费用不能为负数")
    private BigDecimal repairFee;

    @NotNull(message = "请选择开始维修日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /** 修复日期（complete时必填） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate finishDate;

    @NotBlank(message = "故障原因为10-500个字符")
    @Size(min = 10, max = 500, message = "故障原因为10-500个字符")
    private String faultReason;

    @NotBlank(message = "处理措施为10-500个字符")
    @Size(min = 10, max = 500, message = "处理措施为10-500个字符")
    private String solution;

    private String repairFiles;

    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;
}
