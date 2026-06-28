package com.example.eams.procurement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 供应商新增请求
 */
@Data
public class SupplierAddDTO {

    @NotBlank(message = "供应商名称为2-50个字符")
    @Size(min = 2, max = 50, message = "供应商名称为2-50个字符")
    private String supplierName;

    @NotBlank(message = "编码格式为 SUP-XXXX")
    @Pattern(regexp = "SUP-\\d{4}", message = "编码格式为 SUP-XXXX")
    private String supplierCode;

    @Size(max = 20, message = "联系人不能超过20个字符")
    private String contactPerson;

    @Pattern(regexp = "1[3-9]\\d{9}", message = "请输入正确的手机号")
    private String contactPhone;

    @Size(max = 200, message = "地址不能超过200个字符")
    private String address;

    private Integer status = 1;

    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;
}
