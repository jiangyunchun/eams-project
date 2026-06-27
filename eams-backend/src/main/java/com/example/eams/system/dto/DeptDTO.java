package com.example.eams.system.dto;

import lombok.Data;

import javax.validation.constraints.*;

/**
 * 部门新增/编辑请求
 */
@Data
public class DeptDTO {
    private Long id;

    private Long parentId;

    @NotBlank(message = "部门名称为2-20个字符")
    @Size(min = 2, max = 20, message = "部门名称为2-20个字符")
    private String deptName;

    @NotBlank(message = "部门编码格式为 DEPT_XXXX")
    @Pattern(regexp = "^DEPT_[A-Z0-9]+$", message = "部门编码格式为 DEPT_XXXX")
    private String deptCode;

    private Long leaderId;

    @NotNull(message = "排序号为0-9999的整数")
    @Max(value = 9999, message = "排序号为0-9999的整数")
    @Min(value = 0, message = "排序号为0-9999的整数")
    private Integer sortOrder;

    private Integer status = 1;
}
