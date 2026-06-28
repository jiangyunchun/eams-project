package com.example.eams.requisition.controller;

import com.example.eams.common.config.OperationLog;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.result.Result;
import com.example.eams.common.util.ExcelUtil;
import com.example.eams.requisition.dto.*;
import com.example.eams.requisition.entity.RequisitionOrder;
import com.example.eams.requisition.service.RequisitionService;
import com.example.eams.security.annotation.RequireRole;
import com.example.eams.system.entity.SysUser;
import com.example.eams.system.mapper.SysUserMapper;
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
 * 领用归还管理接口（PRD 6.3）
 */
@RestController
@RequestMapping("/api/requisition")
@RequiredArgsConstructor
public class RequisitionController {

    private final RequisitionService requisitionService;
    private final SysUserMapper userMapper;

    // ==================== 领用申请（PRD 6.3.1） ====================

    /**
     * 提交领用申请
     * POST /api/requisition/apply
     */
    @PostMapping("/apply")
    @OperationLog(module = "领用管理", actionType = "新增", description = "提交领用申请【{0}】")
    public Result<?> apply(@Valid @RequestBody RequisitionApplyDTO dto) {
        RequisitionOrder entity = requisitionService.apply(dto);
        return Result.ok("领用申请已提交，请等待审批", null);
    }

    /**
     * 我的申请列表
     * GET /api/requisition/apply/list
     */
    @GetMapping("/apply/list")
    public Result<PageResult<RequisitionOrder>> listMyApply(RequisitionQueryDTO query) {
        return Result.ok(requisitionService.listMyApply(query));
    }

    // ==================== 审批管理（PRD 6.3.2） ====================

    /**
     * 审批列表
     * GET /api/requisition/approval/list
     */
    @GetMapping("/approval/list")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    public Result<PageResult<RequisitionOrder>> listApproval(ApprovalQueryDTO query) {
        return Result.ok(requisitionService.listApproval(query));
    }

    /**
     * 审批通过
     * PUT /api/requisition/approval/pass
     */
    @PutMapping("/approval/pass")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    @OperationLog(module = "领用管理", actionType = "审批", description = "审批通过领用申请【{0}】")
    public Result<?> approve(@Valid @RequestBody ApprovalDTO dto) {
        dto.setApprovalResult(1);
        requisitionService.approve(dto);
        return Result.ok("审批通过", null);
    }

    /**
     * 审批驳回
     * PUT /api/requisition/approval/reject
     */
    @PutMapping("/approval/reject")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    @OperationLog(module = "领用管理", actionType = "审批", description = "驳回领用申请【{0}】")
    public Result<?> reject(@Valid @RequestBody ApprovalDTO dto) {
        dto.setApprovalResult(0);
        requisitionService.reject(dto);
        return Result.ok("已驳回该申请", null);
    }

    // ==================== 归还登记（PRD 6.3.3） ====================

    /**
     * 归还列表
     * GET /api/requisition/return/list
     */
    @GetMapping("/return/list")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    public Result<PageResult<RequisitionOrder>> listReturn(ReturnQueryDTO query) {
        return Result.ok(requisitionService.listReturn(query));
    }

    /**
     * 确认归还
     * PUT /api/requisition/return
     */
    @PutMapping("/return")
    @OperationLog(module = "领用管理", actionType = "归还", description = "归还资产【{0}】")
    public Result<?> returnAsset(@Valid @RequestBody ReturnDTO dto) {
        requisitionService.returnAsset(dto);
        return Result.ok("资产已归还", null);
    }

    // ==================== 领用记录（PRD 6.3.4） ====================

    /**
     * 领用记录列表
     * GET /api/requisition/record/list
     */
    @GetMapping("/record/list")
    public Result<PageResult<RequisitionOrder>> listRecords(RequisitionQueryDTO query) {
        return Result.ok(requisitionService.listRecords(query));
    }

    /**
     * 领用记录详情
     * GET /api/requisition/record/detail/{id}
     */
    @GetMapping("/record/detail/{id}")
    public Result<RequisitionVO> detail(@PathVariable Long id) {
        return Result.ok(requisitionService.getDetail(id));
    }

    /**
     * 导出领用记录
     * GET /api/requisition/record/export
     */
    @GetMapping("/record/export")
    @OperationLog(module = "领用管理", actionType = "导出", description = "导出领用记录")
    public void export(RequisitionQueryDTO query, HttpServletResponse response) {
        List<RequisitionOrder> list = requisitionService.exportData(query);

        LinkedHashMap<String, String> headerAlias = new LinkedHashMap<>();
        headerAlias.put("applyNo", "申请编号");
        headerAlias.put("assetCode", "资产编码");
        headerAlias.put("assetName", "资产名称");
        headerAlias.put("applicantName", "申请人");
        headerAlias.put("deptName", "部门");
        headerAlias.put("purpose", "领用用途");
        headerAlias.put("expectDuration", "预计时长");
        headerAlias.put("expectReturnDate", "预计归还日期");
        headerAlias.put("statusLabel", "状态");
        headerAlias.put("createTime", "申请时间");
        headerAlias.put("returnDate", "归还日期");

        List<Map<String, Object>> dataList = list.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("applyNo", r.getApplyNo());
            m.put("assetCode", r.getAssetCode());
            m.put("assetName", r.getAssetName());
            m.put("applicantName", r.getApplicantName());
            m.put("deptName", r.getDeptName());
            m.put("purpose", r.getPurpose());
            m.put("expectDuration", r.getExpectDuration());
            m.put("expectReturnDate", r.getExpectReturnDate() != null ? r.getExpectReturnDate().toString() : "");
            m.put("statusLabel", r.getStatusLabel());
            m.put("createTime", r.getCreateTime() != null ? r.getCreateTime().toString() : "");
            m.put("returnDate", r.getReturnDate() != null ? r.getReturnDate().toString() : "");
            return m;
        }).collect(Collectors.toList());

        String fileName = "领用记录_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ExcelUtil.export(response, fileName, headerAlias, dataList);
    }
}
