package com.example.eams.repair.controller;

import com.example.eams.common.config.OperationLog;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.result.Result;
import com.example.eams.common.util.ExcelUtil;
import com.example.eams.repair.dto.*;
import com.example.eams.repair.entity.RepairOrder;
import com.example.eams.repair.service.RepairService;
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
 * 维保报修接口（PRD 6.8）
 */
@RestController
@RequestMapping("/api/repair")
@RequiredArgsConstructor
public class RepairController {

    private final RepairService repairService;

    // ==================== 报修登记（PRD 6.8.1） ====================

    @PostMapping("/apply")
    @OperationLog(module = "维保报修", actionType = "新增", description = "提交报修【{0}】")
    public Result<?> apply(@Valid @RequestBody RepairApplyDTO dto) {
        repairService.apply(dto);
        return Result.ok("报修申请已提交，维修人员将尽快处理", null);
    }

    @GetMapping("/apply/list")
    public Result<PageResult<RepairOrder>> listMyApply(RepairQueryDTO q) {
        return Result.ok(repairService.listMyApply(q));
    }

    // ==================== 维修处理（PRD 6.8.2） ====================

    @GetMapping("/handle/list")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    public Result<PageResult<RepairOrder>> listHandle(RepairQueryDTO q) {
        return Result.ok(repairService.listHandle(q));
    }

    @PutMapping("/handle/accept")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "维保报修", actionType = "处理", description = "接单【{0}】")
    public Result<?> accept(@Valid @RequestBody RepairHandleDTO dto) {
        dto.setAction("accept");
        repairService.accept(dto);
        return Result.ok("已接单，开始维修", null);
    }

    @PutMapping("/handle/complete")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "维保报修", actionType = "处理", description = "标记已修复【{0}】")
    public Result<?> complete(@Valid @RequestBody RepairHandleDTO dto) {
        dto.setAction("complete");
        repairService.complete(dto);
        return Result.ok("维修完成，资产已恢复可用", null);
    }

    @PutMapping("/handle/unfixable")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "维保报修", actionType = "处理", description = "标记无法修复【{0}】")
    public Result<?> unfixable(@Valid @RequestBody RepairHandleDTO dto) {
        dto.setAction("unfixable");
        repairService.unfixable(dto);
        return Result.ok("已标记为无法修复，请评估是否报废", null);
    }

    // ==================== 维保记录（PRD 6.8.3） ====================

    @GetMapping("/record/list")
    public Result<PageResult<RepairOrder>> listRecords(RepairQueryDTO q) {
        return Result.ok(repairService.listRecords(q));
    }

    @GetMapping("/record/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(repairService.getDetail(id));
    }

    @GetMapping("/record/export")
    @OperationLog(module = "维保报修", actionType = "导出", description = "导出维保记录")
    public void export(RepairQueryDTO q, HttpServletResponse response) {
        List<RepairOrder> list = repairService.exportData(q);
        LinkedHashMap<String, String> h = new LinkedHashMap<>();
        h.put("repairNo", "报修编号"); h.put("assetCode", "资产编码"); h.put("assetName", "资产名称");
        h.put("applicantName", "报修人"); h.put("faultType", "故障类型"); h.put("urgencyLabel", "紧急程度");
        h.put("faultDesc", "故障描述"); h.put("statusLabel", "维修状态");
        h.put("repairPerson", "维修人员"); h.put("repairFee", "维修费用");
        h.put("faultReason", "故障原因"); h.put("solution", "处理措施");
        h.put("createTime", "报修时间");

        List<Map<String, Object>> data = list.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("repairNo", r.getRepairNo()); m.put("assetCode", r.getAssetCode());
            m.put("assetName", r.getAssetName()); m.put("applicantName", r.getApplicantName());
            m.put("faultType", r.getFaultType()); m.put("urgencyLabel", r.getUrgencyLabel());
            m.put("faultDesc", r.getFaultDesc()); m.put("statusLabel", r.getStatusLabel());
            m.put("repairPerson", r.getRepairPerson()); m.put("repairFee", r.getRepairFee());
            m.put("faultReason", r.getFaultReason()); m.put("solution", r.getSolution());
            m.put("createTime", r.getCreateTime() != null ? r.getCreateTime().toString() : "");
            return m;
        }).collect(Collectors.toList());

        String fn = "维保记录_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ExcelUtil.export(response, fn, h, data);
    }
}
