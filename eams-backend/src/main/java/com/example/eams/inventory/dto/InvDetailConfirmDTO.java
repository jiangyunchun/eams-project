package com.example.eams.inventory.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 盘点明细确认 DTO（PRD 6.4.2 执行盘点 - 逐项/批量确认）
 */
@Data
public class InvDetailConfirmDTO {

    /** 盘点任务ID */
    @NotNull(message = "盘点任务ID不能为空")
    private Long taskId;

    /** 盘点明细ID列表（支持批量确认） */
    @NotNull(message = "请选择要确认的资产")
    @Size(min = 1, message = "请至少选择一项资产")
    private List<Long> detailIds;

    /** 盘点结果: 0-盘盈, 1-盘亏, 2-正常（PRD 5.5 盘点结果枚举） */
    @NotNull(message = "请选择盘点结果")
    private Integer inventoryResult;

    /** 实盘现场备注（最大200字符，盘亏时必填） */
    @Size(max = 200, message = "实盘备注不能超过200个字符")
    private String remark;
}
