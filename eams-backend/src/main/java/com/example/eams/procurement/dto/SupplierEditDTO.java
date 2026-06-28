package com.example.eams.procurement.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 供应商编辑请求
 */
@Data
public class SupplierEditDTO {

    @NotNull(message = "ID不能为空")
    private Long id;

    @NotNull(message = "供应商名称为2-50个字符")
    @javax.validation.constraints.Size(min = 2, max = 50, message = "供应商名称为2-50个字符")
    private String supplierName;

    @NotNull(message = "编码格式为 SUP-XXXX")
    @javax.validation.constraints.Pattern(regexp = "SUP-\\d{4}", message = "编码格式为 SUP-XXXX")
    private String supplierCode;

    @javax.validation.constraints.Size(max = 20, message = "联系人不能超过20个字符")
    private String contactPerson;

    @javax.validation.constraints.Pattern(regexp = "1[3-9]\\d{9}", message = "请输入正确的手机号")
    private String contactPhone;

    @javax.validation.constraints.Size(max = 200, message = "地址不能超过200个字符")
    private String address;

    private Integer status = 1;

    @javax.validation.constraints.Size(max = 200, message = "备注不能超过200个字符")
    private String remark;

    /** 乐观锁版本号 */
    private Integer version;
}
