package com.example.eams.transfer.controller;

import com.example.eams.common.config.OperationLog;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.result.Result;
import com.example.eams.common.util.ExcelUtil;
import com.example.eams.security.annotation.RequireRole;
import com.example.eams.transfer.dto.*;
import com.example.eams.transfer.entity.TransferOrder;
import com.example.eams.transfer.service.TransferService;
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
 * 资产调拨接口（PRD 6.7）
 */
@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    // ==================== 调拨申请（PRD 6.7.1） ====================

    @PostMapping("/apply")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    @OperationLog(module = "资产调拨", actionType = "新增", description = "提交调拨申请【{0}】")
    public Result<?> apply(@Valid @RequestBody TransferApplyDTO dto) {
        TransferOrder entity = transferService.apply(dto);
        return Result.ok("调拨申请已提交，请等待调入部门确认", null);
    }

    @GetMapping("/apply/list")
    public Result<PageResult<TransferOrder>> listMyApply(TransferQueryDTO query) {
        return Result.ok(transferService.listMyApply(query));
    }

    // ==================== 调拨审批（PRD 6.7.2） ====================

    @GetMapping("/approval/list")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    public Result<PageResult<TransferOrder>> listApproval(TransferApproveListDTO query) {
        return Result.ok(transferService.listApproval(query));
    }

    @PutMapping("/approval/pass")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    @OperationLog(module = "资产调拨", actionType = "审批", description = "通过调拨申请【{0}】")
    public Result<?> approve(@Valid @RequestBody TransferApprovalDTO dto) {
        dto.setApprovalResult(1);
        transferService.approve(dto);
        // 区分确认调入 vs 终审通过（前端自行判断状态变化）
        return Result.ok("审批通过", null);
    }

    @PutMapping("/approval/reject")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    @OperationLog(module = "资产调拨", actionType = "审批", description = "驳回调拨申请【{0}】")
    public Result<?> reject(@Valid @RequestBody TransferApprovalDTO dto) {
        dto.setApprovalResult(0);
        transferService.reject(dto);
        return Result.ok("已驳回该调拨申请", null);
    }

    // ==================== 调拨记录（PRD 6.7.3） ====================

    @GetMapping("/record/list")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    public Result<PageResult<TransferOrder>> listRecords(TransferQueryDTO query) {
        return Result.ok(transferService.listRecords(query));
    }

    @GetMapping("/record/detail/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    public Result<TransferVO> detail(@PathVariable Long id) {
        return Result.ok(transferService.getDetail(id));
    }

    @GetMapping("/record/export")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    @OperationLog(module = "资产调拨", actionType = "导出", description = "导出调拨记录")
    public void export(TransferQueryDTO query, HttpServletResponse response) {
        List<TransferOrder> list = transferService.exportData(query);

        LinkedHashMap<String, String> headerAlias = new LinkedHashMap<>();
        headerAlias.put("transferNo", "调拨编号");
        headerAlias.put("assetCode", "资产编码");
        headerAlias.put("assetName", "资产名称");
        headerAlias.put("fromDeptName", "调出部门");
        headerAlias.put("toDeptName", "调入部门");
        headerAlias.put("toLocation", "调入地点");
        headerAlias.put("toUserName", "调入使用人");
        headerAlias.put("applicantName", "申请人");
        headerAlias.put("transferReason", "调拨原因");
        headerAlias.put("statusLabel", "状态");
        headerAlias.put("createTime", "申请时间");

        List<Map<String, Object>> dataList = list.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("transferNo", r.getTransferNo());
            m.put("assetCode", r.getAssetCode());
            m.put("assetName", r.getAssetName());
            m.put("fromDeptName", r.getFromDeptName());
            m.put("toDeptName", r.getToDeptName());
            m.put("toLocation", r.getToLocation());
            m.put("toUserName", r.getToUserName() != null ? r.getToUserName() : "");
            m.put("applicantName", r.getApplicantName());
            m.put("transferReason", r.getTransferReason());
            m.put("statusLabel", r.getStatusLabel());
            m.put("createTime", r.getCreateTime() != null ? r.getCreateTime().toString() : "");
            return m;
        }).collect(Collectors.toList());

        String fileName = "调拨记录_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ExcelUtil.export(response, fileName, headerAlias, dataList);
    }
}
