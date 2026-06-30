package com.example.eams.inventory.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 盘盈资产登记 DTO（PRD 6.4.2 盘盈登记弹窗）
 * <p>
 * 实盘发现账外资产时，登记资产信息自动入库生成资产台账
 */
@Data
public class InvSurplusAssetDTO {

    /** 所属盘点任务ID */
    @NotNull(message = "盘点任务ID不能为空")
    private Long taskId;

    /** 资产名称: 2-50字符 */
    @NotBlank(message = "资产名称为2-50个字符")
    @Size(min = 2, max = 50, message = "资产名称为2-50个字符")
    private String assetName;

    /** 资产分类（字典值）: IT_EQUIPMENT */
    @NotBlank(message = "请选择资产分类")
    private String category;

    /** 规格型号（可选，最大50字符） */
    @Size(max = 50, message = "规格型号不能超过50个字符")
    private String specification;

    /** SN序列号（可选，最大50字符） */
    @Size(max = 50, message = "SN序列号不能超过50个字符")
    private String snNumber;

    /** 原值(元): >0，最多2位小数 */
    @NotNull(message = "原值须大于0")
    @DecimalMin(value = "0.01", message = "原值须大于0")
    @DecimalMax(value = "99999999.99", message = "原值超出允许范围")
    private BigDecimal originalValue;

    /** 采购日期: ≤当前日期 */
    @NotNull(message = "请选择采购日期")
    private LocalDate purchaseDate;

    /** 使用年限(年): 1-50整数 */
    @NotNull(message = "使用年限为1-50的整数")
    @Min(value = 1, message = "使用年限为1-50的整数")
    @Max(value = 50, message = "使用年限为1-50的整数")
    private Integer usefulLife;

    /** 净残值率(%): 0-100，默认5 */
    @DecimalMin(value = "0", message = "净残值率须在0-100之间")
    @DecimalMax(value = "100", message = "净残值率须在0-100之间")
    private BigDecimal residualRate;

    /** 存放地点: 2-50字符 */
    @NotBlank(message = "存放地点为2-50个字符")
    @Size(min = 2, max = 50, message = "存放地点为2-50个字符")
    private String location;

    /** 所属部门ID（必填） */
    @NotNull(message = "请选择所属部门")
    private Long deptId;

    /** 使用人ID（可选） */
    private Long userId;

    /** 实盘现场备注（最大200字符） */
    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;
}
