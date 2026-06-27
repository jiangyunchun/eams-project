package com.example.eams.system.dto;

import lombok.Data;

import javax.validation.constraints.*;

/**
 * 字典项新增/编辑请求
 */
@Data
public class DictItemDTO {
    private Long id;

    @NotBlank(message = "字典编码不能为空")
    private String dictCode;

    @NotBlank(message = "字典标签为2-50个字符")
    @Size(min = 2, max = 50, message = "字典标签为2-50个字符")
    private String dictLabel;

    @NotBlank(message = "字典值不能为空")
    private String dictValue;

    private String cssClass;

    @NotNull(message = "排序号为0-9999的整数")
    @Max(value = 9999, message = "排序号为0-9999的整数")
    @Min(value = 0, message = "排序号为0-9999的整数")
    private Integer sortOrder;

    private Integer status = 1;
}
