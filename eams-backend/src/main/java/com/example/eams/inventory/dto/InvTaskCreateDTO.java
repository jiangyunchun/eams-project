package com.example.eams.inventory.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 盘点任务创建 DTO（PRD 6.4.1 创建任务弹窗）
 */
@Data
public class InvTaskCreateDTO {

    /** 任务名称: 2-50字符 */
    @NotBlank(message = "任务名称为2-50个字符")
    @Size(min = 2, max = 50, message = "任务名称为2-50个字符")
    private String taskName;

    /** 范围类型: ALL-全公司, DEPT-按部门, CATEGORY-按分类 */
    @NotBlank(message = "请选择盘点范围类型")
    private String scopeType;

    /** 盘点部门ID列表（按部门范围时必填） */
    private List<Long> deptIds;

    /** 盘点分类编码列表（按分类范围时必填） */
    private List<String> categoryCodes;

    /** 计划盘点日期: ≥当前日期 */
    @NotNull(message = "请选择盘点日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate inventoryDate;

    /** 任务备注（可选，最大500字符） */
    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
