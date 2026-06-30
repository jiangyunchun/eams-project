package com.example.eams.inventory.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 盘点差异处理 DTO（PRD 6.4.3 标记已处理/批量标记）
 */
@Data
public class InvDifferenceHandleDTO {

    /** 差异记录ID列表（支持批量处理） */
    @NotNull(message = "请选择要处理的差异记录")
    @Size(min = 1, message = "请至少选择一条差异记录")
    private List<Long> ids;
}
