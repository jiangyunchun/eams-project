package com.example.eams.asset.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 编辑资产请求
 */
@Data
public class AssetEditDTO {
    @NotNull(message = "资产ID不能为空")
    private Long id;

    @NotBlank(message = "资产名称为2-50个字符")
    @Size(min = 2, max = 50, message = "资产名称为2-50个字符")
    private String assetName;

    @NotBlank(message = "请选择资产分类")
    private String category;

    @Size(max = 50, message = "规格型号不能超过50个字符")
    private String specification;

    @Size(max = 50, message = "SN序列号不能超过50个字符")
    private String snNumber;

    @Size(max = 50, message = "采购编号不能超过50个字符")
    private String procurementNo;

    @NotNull(message = "原值须大于0")
    @DecimalMin(value = "0.01", message = "原值须大于0")
    private BigDecimal originalValue;

    @NotNull(message = "请选择采购日期")
    private LocalDate purchaseDate;

    @NotNull(message = "使用年限为1-50的整数")
    @Min(value = 1, message = "使用年限为1-50的整数")
    @Max(value = 50, message = "使用年限为1-50的整数")
    private Integer usefulLife;

    private BigDecimal residualRate;

    @NotBlank(message = "存放地点为2-50个字符")
    @Size(min = 2, max = 50, message = "存放地点为2-50个字符")
    private String location;

    @NotNull(message = "请选择所属部门")
    private Long deptId;

    private Long userId;

    private Integer status;

    private String imageUrl;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    /** 乐观锁版本号 */
    private Integer version;
}
