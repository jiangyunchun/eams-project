package com.example.eams.scrap.controller;

import com.example.eams.common.config.OperationLog;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.result.Result;
import com.example.eams.common.util.ExcelUtil;
import com.example.eams.scrap.dto.*;
import com.example.eams.scrap.entity.ScrapOrder;
import com.example.eams.scrap.service.ScrapService;
import com.example.eams.security.annotation.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报废处置接口（PRD 6.9）
 * <p>
 * 报废全流程: POST 申请 → PUT 审批(初审/终审) → PUT 处置登记 → GET 记录查询
 * 权限对齐 PRD 4.2 权限矩阵
 */
@RestController
@RequestMapping("/api/scrap")
@RequiredArgsConstructor
public class ScrapController {

    private final ScrapService scrapService;

    // ==================== 6.9.1 报废申请 ====================

    /**
     * 报废申请
     * POST /api/scrap/apply
     * <p>
     * 权限: 超级管理员/资产管理员/部门管理员（仅本部门资产）
     * PRD 6.9.1 报废申请
     */
    @PostMapping("/apply")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    @OperationLog(module = "报废处置", actionType = "新增", description = "报废申请: 资产ID={0.assetId}")
    public Result<?> apply(@Valid @RequestBody ScrapApplyDTO dto) {
        ScrapOrder order = scrapService.apply(dto);
        return Result.ok("报废申请已提交，请等待审批", order.getScrapNo());
    }

    // ==================== 6.9.2 报废审批 ====================

    /**
     * 报废审批列表（初审/终审）
     * GET /api/scrap/approval/list
     * <p>
     * 资产管理员: 查看待初审单据 (status=0)
     * 超级管理员: 查看待初审+待终审单据 (status=0,1)
     * PRD 6.9.2 报废审批
     */
    @GetMapping("/approval/list")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    public Result<PageResult<ScrapOrder>> listApproval(ScrapQueryDTO query) {
        return Result.ok(scrapService.listApproval(query));
    }

    /**
     * 报废审批：通过或驳回
     * PUT /api/scrap/approve
     * <p>
     * 初审: 资产管理员 → 状态 0→1(通过) 或 0→3(驳回)
     * 终审: 超级管理员 → 状态 1→2(通过) 或 1→3(驳回)
     * PRD 6.9.2 审批操作
     */
    @PutMapping("/approve")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "报废处置", actionType = "审批", description = "报废审批: 报废单ID={0.scrapId}, 结果={0.approvalResult}")
    public Result<?> approve(@Valid @RequestBody ScrapApprovalDTO dto) {
        scrapService.approve(dto);
        if (dto.getApprovalResult() == 1) {
            // 根据当前状态返回不同提示
            ScrapOrder order = scrapService.detail(dto.getScrapId());
            if (order.getStatus() == 2) {
                return Result.ok("报废审批通过，资产已标记为报废，请执行处置", null);
            } else if (order.getStatus() == 1) {
                return Result.ok("初审通过，待超级管理员终审", null);
            }
            return Result.ok("审批通过", null);
        } else {
            return Result.ok("已驳回该报废申请", null);
        }
    }

    // ==================== 6.9.3 报废处置登记 ====================

    /**
     * 待处置单据列表
     * GET /api/scrap/disposal/list
     * <p>
     * 仅展示 status=2 (已通过待处置) 的报废单
     * 权限: 超级管理员/资产管理员
     * PRD 6.9.3 报废处置记录
     */
    @GetMapping("/disposal/list")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    public Result<PageResult<ScrapOrder>> listDisposal(ScrapQueryDTO query) {
        return Result.ok(scrapService.listDisposal(query));
    }

    /**
     * 报废处置登记
     * PUT /api/scrap/disposal
     * <p>
     * 处置完成后: status→已处置(4)，资产归档
     * 权限: 超级管理员/资产管理员
     * PRD 6.9.3 处置登记
     */
    @PutMapping("/disposal")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "报废处置", actionType = "编辑", description = "报废处置登记: 报废单ID={0.scrapId}, 处置方式={0.disposalMethod}")
    public Result<?> disposal(@Valid @RequestBody ScrapDisposalDTO dto) {
        scrapService.disposal(dto);
        return Result.ok("报废处置登记完成，资产已归档", null);
    }

    // ==================== 6.9.4 报废记录 ====================

    /**
     * 报废记录列表
     * GET /api/scrap/record/list
     * <p>
     * 超级管理员/资产管理员: 全部记录
     * 部门管理员: 本部门资产报废记录
     * PRD 6.9 报废记录查询
     */
    @GetMapping("/record/list")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    public Result<PageResult<ScrapOrder>> listRecords(ScrapQueryDTO query) {
        return Result.ok(scrapService.listRecords(query));
    }

    /**
     * 报废单详情
     * GET /api/scrap/record/detail/{id}
     */
    @GetMapping("/record/detail/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    public Result<ScrapOrder> detail(@PathVariable Long id) {
        return Result.ok(scrapService.detail(id));
    }

    /**
     * 导出报废记录 Excel
     * GET /api/scrap/record/export
     * <p>
     * 按当前筛选条件导出 .xlsx 文件
     * PRD 6.9.4 报废记录导出
     */
    @GetMapping("/record/export")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "报废处置", actionType = "导出", description = "导出报废记录Excel")
    public void exportRecords(ScrapQueryDTO query, HttpServletResponse response) {
        List<ScrapOrder> list = scrapService.exportRecords(query);

        LinkedHashMap<String, String> headerAlias = new LinkedHashMap<>();
        headerAlias.put("scrapNo", "报废编号");
        headerAlias.put("assetCode", "资产编码");
        headerAlias.put("assetName", "资产名称");
        headerAlias.put("category", "资产分类");
        headerAlias.put("specification", "规格型号");
        headerAlias.put("deptName", "所属部门");
        headerAlias.put("originalValue", "原值(元)");
        headerAlias.put("netValue", "净值(元)");
        headerAlias.put("scrapReason", "报废原因");
        headerAlias.put("statusLabel", "报废状态");
        headerAlias.put("applicantName", "申请人");
        headerAlias.put("createTime", "申请时间");
        headerAlias.put("disposalMethod", "处置方式");
        headerAlias.put("disposalDate", "处置日期");

        DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<Map<String, Object>> dataList = list.stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("scrapNo", o.getScrapNo());
            m.put("assetCode", o.getAssetCode());
            m.put("assetName", o.getAssetName());
            m.put("category", o.getCategory());
            m.put("specification", o.getSpecification());
            m.put("deptName", o.getDeptName());
            m.put("originalValue", o.getOriginalValue() != null
                    ? o.getOriginalValue().setScale(2, BigDecimal.ROUND_HALF_UP) : null);
            m.put("netValue", o.getNetValue() != null
                    ? o.getNetValue().setScale(2, BigDecimal.ROUND_HALF_UP) : null);
            m.put("scrapReason", o.getScrapReasonLabel() != null ? o.getScrapReasonLabel() : o.getScrapReason());
            m.put("statusLabel", o.getStatusLabel());
            m.put("applicantName", o.getApplicantName());
            m.put("createTime", o.getCreateTime() != null ? o.getCreateTime().format(dateTimeFmt) : "");
            m.put("disposalMethod", o.getDisposalMethod());
            m.put("disposalDate", o.getDisposalDate() != null ? o.getDisposalDate().format(dateFmt) : "");
            return m;
        }).collect(Collectors.toList());

        String fileName = "报废记录_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ExcelUtil.export(response, fileName, headerAlias, dataList);
    }
}
