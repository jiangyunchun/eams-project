package com.example.eams.system.dto;

import lombok.Data;

import javax.validation.constraints.*;

/**
 * 字典类型新增/编辑请求
 */
@Data
public class DictTypeDTO {
    private Long id;

    @NotBlank(message = "字典名称为2-20个字符")
    @Size(min = 2, max = 20, message = "字典名称为2-20个字符")
    private String dictName;

    @NotBlank(message = "字典编码须为小写字母+下划线格式")
    @Pattern(regexp = "^[a-z_]{2,50}$", message = "字典编码须为小写字母+下划线格式")
    private String dictCode;

    private Integer status = 1;

    @Size(max = 200, message = "描述不能超过200个字符")
    private String description;
}
