package com.example.eams.inventory.controller;

import com.example.eams.common.config.OperationLog;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.result.Result;
import com.example.eams.common.util.ExcelUtil;
import com.example.eams.inventory.dto.*;
import com.example.eams.inventory.entity.InvDetail;
import com.example.eams.inventory.entity.InvDifference;
import com.example.eams.inventory.entity.InvTask;
import com.example.eams.inventory.service.InventoryService;
import com.example.eams.security.annotation.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 盘点管理接口（PRD 6.4）
 * <p>
 * 盘点全流程: POST 创建任务 → GET 明细列表 → PUT 确认明细 → POST 盘盈登记 → PUT 完成盘点
 * 差异管理: GET 差异列表 → PUT 批量处理 → GET 导出Excel
 * 权限对齐 PRD 4.2 权限矩阵: 超级管理员/资产管理员 → 全部; 部门管理员 → 仅查看本部门
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // ==================== 6.4.1 盘点任务管理 ====================

    /**
     * 盘点任务列表（分页）
     * GET /api/inventory/task/list
     * <p>
     * 权限: 超级管理员/资产管理员(全部) / 部门管理员(仅本部门任务)
     * PRD 6.4.1 盘点任务管理
     */
    @GetMapping("/task/list")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    public Result<PageResult<InvTask>> listTasks(InvTaskQueryDTO query) {
        return Result.ok(inventoryService.listTasks(query));
    }

    /**
     * 盘点任务详情
     * GET /api/inventory/task/detail/{id}
     * <p>
     * 权限: 超级管理员/资产管理员/部门管理员
     */
    @GetMapping("/task/detail/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    public Result<InvTask> getTaskDetail(@PathVariable Long id) {
        return Result.ok(inventoryService.getTaskDetail(id));
    }

    /**
     * 创建盘点任务（PRD 6.4.1 创建任务弹窗）
     * POST /api/inventory/task/add
     * <p>
     * 权限: 超级管理员/资产管理员（部门管理员无创建权限）
     * 业务: Redis分布式锁防重复创建 + 批量更新资产状态为盘点中
     */
    @PostMapping("/task/add")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "盘点管理", actionType = "新增", description = "创建盘点任务: {0.taskName}, 范围={0.scopeType}")
    public Result<InvTask> createTask(@Valid @RequestBody InvTaskCreateDTO dto) {
        InvTask task = inventoryService.createTask(dto);
        return Result.ok("盘点任务【" + task.getTaskName() + "】创建成功，范围内共" + task.getTotalCount() + "项资产", task);
    }

    /**
     * 取消盘点任务（PRD 6.4.1 取消任务）
     * PUT /api/inventory/task/cancel/{id}
     * <p>
     * 权限: 超级管理员/资产管理员
     * 业务: 恢复所有资产原有状态，任务状态→已取消
     */
    @PutMapping("/task/cancel/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "盘点管理", actionType = "编辑", description = "取消盘点任务: ID={0}")
    public Result<?> cancelTask(@PathVariable Long id) {
        inventoryService.cancelTask(id);
        return Result.ok("盘点任务已取消", null);
    }

    /**
     * 导出盘点任务 Excel
     * GET /api/inventory/task/export
     */
    @GetMapping("/task/export")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "盘点管理", actionType = "导出", description = "导出盘点任务Excel")
    public void exportTasks(InvTaskQueryDTO query, HttpServletResponse response) {
        List<InvTask> list = inventoryService.exportTasks(query);

        LinkedHashMap<String, String> headerAlias = new LinkedHashMap<>();
        headerAlias.put("taskNo", "任务编号");
        headerAlias.put("taskName", "任务名称");
        headerAlias.put("scopeTypeLabel", "盘点范围");
        headerAlias.put("inventoryDate", "盘点日期");
        headerAlias.put("totalCount", "应盘数量");
        headerAlias.put("checkedCount", "已盘数量");
        headerAlias.put("normalCount", "正常数量");
        headerAlias.put("surplusCount", "盘盈数量");
        headerAlias.put("shortageCount", "盘亏数量");
        headerAlias.put("statusLabel", "状态");
        headerAlias.put("creatorName", "创建人");
        headerAlias.put("createTime", "创建时间");

        DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<Map<String, Object>> dataList = list.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskNo", t.getTaskNo());
            m.put("taskName", t.getTaskName());
            m.put("scopeTypeLabel", t.getScopeTypeLabel());
            m.put("inventoryDate", t.getInventoryDate() != null ? t.getInventoryDate().format(dateFmt) : "");
            m.put("totalCount", t.getTotalCount());
            m.put("checkedCount", t.getCheckedCount());
            m.put("normalCount", t.getNormalCount());
            m.put("surplusCount", t.getSurplusCount());
            m.put("shortageCount", t.getShortageCount());
            m.put("statusLabel", t.getStatusLabel());
            m.put("creatorName", t.getCreatorName());
            m.put("createTime", t.getCreateTime() != null ? t.getCreateTime().format(dateTimeFmt) : "");
            return m;
        }).collect(Collectors.toList());

        String fileName = "盘点任务_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ExcelUtil.export(response, fileName, headerAlias, dataList);
    }

    // ==================== 6.4.2 执行盘点 ====================

    /**
     * 获取盘点任务明细列表（执行盘点页）
     * GET /api/inventory/execute/details/{taskId}
     * <p>
     * 权限: 超级管理员/资产管理员
     * 返回: 任务摘要信息 + 所有盘点明细（含资产账面快照）
     */
    @GetMapping("/execute/details/{taskId}")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    public Result<List<InvDetail>> getTaskDetails(@PathVariable Long taskId) {
        return Result.ok(inventoryService.getTaskDetails(taskId));
    }

    /**
     * 确认盘点明细（逐项/批量确认）（PRD 6.4.2）
     * PUT /api/inventory/execute/confirm
     * <p>
     * 权限: 超级管理员/资产管理员
     * Redis分布式锁防并发编辑同一盘点任务
     */
    @PutMapping("/execute/confirm")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "盘点管理", actionType = "编辑", description = "确认盘点明细: 任务ID={0.taskId}, 结果={0.inventoryResult}, 数量={0.detailIds.size}")
    public Result<?> confirmDetails(@Valid @RequestBody InvDetailConfirmDTO dto) {
        inventoryService.confirmDetails(dto);
        String resultLabel = dto.getInventoryResult() == 0 ? "盘盈" : dto.getInventoryResult() == 1 ? "盘亏" : "正常";
        return Result.ok("已确认" + dto.getDetailIds().size() + "项资产，结果: " + resultLabel, null);
    }

    /**
     * 盘盈资产登记入库（PRD 6.4.2 盘盈登记弹窗）
     * POST /api/inventory/execute/surplus-asset
     * <p>
     * 权限: 超级管理员/资产管理员
     * 业务: 自动生成资产编码 → 资产入库 → 新增盘点明细
     */
    @PostMapping("/execute/surplus-asset")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "盘点管理", actionType = "新增", description = "盘盈资产登记: {0.assetName}, 分类={0.category}")
    public Result<InvDetail> registerSurplusAsset(@Valid @RequestBody InvSurplusAssetDTO dto) {
        InvDetail detail = inventoryService.registerSurplusAsset(dto);
        return Result.ok("盘盈资产登记成功，已自动入库，编码: " + detail.getAssetCode(), detail);
    }

    /**
     * 完成盘点（PRD 6.4.2 完成盘点）
     * PUT /api/inventory/task/complete/{id}
     * <p>
     * 权限: 超级管理员/资产管理员
     * 前置校验: 所有资产已确认 + 前端确认弹窗
     * 业务: 汇总数据 + 盘盈生成差异 + 盘亏生成差异 + 任务状态→已完成
     */
    @PutMapping("/task/complete/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "盘点管理", actionType = "编辑", description = "完成盘点: 任务ID={0}")
    public Result<?> completeTask(@PathVariable Long id) {
        inventoryService.completeTask(id);
        return Result.ok("盘点任务已完成，盘点报告已生成", null);
    }

    // ==================== 6.4.3 盘点差异记录 ====================

    /**
     * 盘点差异列表（分页）
     * GET /api/inventory/difference/list
     * <p>
     * 权限: 超级管理员/资产管理员(全部差异) / 部门管理员(仅本部门)
     * PRD 6.4.3 盘点差异记录
     */
    @GetMapping("/difference/list")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    public Result<PageResult<InvDifference>> listDifferences(InvDifferenceQueryDTO query) {
        return Result.ok(inventoryService.listDifferences(query));
    }

    /**
     * 差异详情
     * GET /api/inventory/difference/detail/{id}
     */
    @GetMapping("/difference/detail/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    public Result<InvDifference> getDifferenceDetail(@PathVariable Long id) {
        return Result.ok(inventoryService.getDifferenceDetail(id));
    }

    /**
     * 批量标记差异已处理（PRD 6.4.3 标记已处理）
     * PUT /api/inventory/difference/handle
     * <p>
     * 权限: 超级管理员/资产管理员（部门管理员无处理权限）
     */
    @PutMapping("/difference/handle")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "盘点管理", actionType = "编辑", description = "批量处理差异: {0.ids}")
    public Result<?> handleDifferences(@Valid @RequestBody InvDifferenceHandleDTO dto) {
        inventoryService.handleDifferences(dto);
        return Result.ok("已批量处理" + dto.getIds().size() + "条差异", null);
    }

    /**
     * 导出差异记录 Excel
     * GET /api/inventory/difference/export
     * <p>
     * 权限: 超级管理员/资产管理员
     * PRD 6.4.3 支持差异记录Excel导出
     */
    @GetMapping("/difference/export")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "盘点管理", actionType = "导出", description = "导出差异记录Excel")
    public void exportDifferences(InvDifferenceQueryDTO query, HttpServletResponse response) {
        List<InvDifference> list = inventoryService.exportDifferences(query);

        LinkedHashMap<String, String> headerAlias = new LinkedHashMap<>();
        headerAlias.put("taskNo", "任务编号");
        headerAlias.put("assetCode", "资产编码");
        headerAlias.put("assetName", "资产名称");
        headerAlias.put("diffTypeLabel", "差异类型");
        headerAlias.put("bookQty", "账面数量");
        headerAlias.put("actualQty", "实盘数量");
        headerAlias.put("diffDesc", "差异说明");
        headerAlias.put("handleStatusLabel", "处理状态");
        headerAlias.put("handlerName", "处理人");
        headerAlias.put("inventoryDate", "盘点日期");

        DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<Map<String, Object>> dataList = list.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskNo", d.getTaskNo());
            m.put("assetCode", d.getAssetCode());
            m.put("assetName", d.getAssetName());
            m.put("diffTypeLabel", d.getDiffTypeLabel());
            m.put("bookQty", d.getBookQty());
            m.put("actualQty", d.getActualQty());
            m.put("diffDesc", d.getDiffDesc());
            m.put("handleStatusLabel", d.getHandleStatusLabel());
            m.put("handlerName", d.getHandlerName());
            m.put("inventoryDate", d.getInventoryDate());
            return m;
        }).collect(Collectors.toList());

        String fileName = "盘点差异_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ExcelUtil.export(response, fileName, headerAlias, dataList);
    }
}
