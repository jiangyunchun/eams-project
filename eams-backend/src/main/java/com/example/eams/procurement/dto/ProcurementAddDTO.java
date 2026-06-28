package com.example.eams.procurement.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购登记新增请求（PRD 6.6.1）
 */
@Data
public class ProcurementAddDTO {

    @Size(max = 50, message = "采购单号不能超过50个字符")
    private String procurementNo;

    @NotBlank(message = "资产名称为2-50个字符")
    @Size(min = 2, max = 50, message = "资产名称为2-50个字符")
    private String assetName;

    @NotBlank(message = "请选择资产分类")
    private String category;

    @Size(max = 50, message = "规格型号不能超过50个字符")
    private String specification;

    @Size(max = 50, message = "SN序列号不能超过50个字符")
    private String snNumber;

    @NotNull(message = "采购数量为1-100的整数")
    @Min(value = 1, message = "采购数量为1-100的整数")
    @Max(value = 100, message = "采购数量为1-100的整数")
    private Integer quantity;

    @NotNull(message = "单价须大于0")
    @DecimalMin(value = "0.01", message = "单价须大于0")
    private BigDecimal unitPrice;

    @NotNull(message = "请选择采购日期")
    private LocalDate purchaseDate;

    @NotNull(message = "请选择供应商")
    private Long supplierId;

    @NotNull(message = "使用年限为1-50的整数")
    @Min(value = 1, message = "使用年限为1-50的整数")
    @Max(value = 50, message = "使用年限为1-50的整数")
    private Integer usefulLife;

    @DecimalMin(value = "0", message = "净残值率须在0-100之间")
    @DecimalMax(value = "100", message = "净残值率须在0-100之间")
    private BigDecimal residualRate;

    @NotNull(message = "请选择所属部门")
    private Long deptId;

    @NotBlank(message = "存放地点为2-50个字符")
    @Size(min = 2, max = 50, message = "存放地点为2-50个字符")
    private String location;

    /** 验收状态: 0-待验收, 1-已验收 */
    @NotNull(message = "请选择验收状态")
    @Min(value = 0, message = "验收状态无效")
    @Max(value = 1, message = "验收状态无效")
    private Integer acceptStatus;

    /** 验收日期：acceptStatus=1时必填 */
    private LocalDate acceptDate;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
