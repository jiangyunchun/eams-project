package com.example.eams.procurement.controller;

import com.example.eams.common.config.OperationLog;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.result.Result;
import com.example.eams.procurement.dto.*;
import com.example.eams.procurement.entity.ProcurementOrder;
import com.example.eams.procurement.entity.ProcurementSupplier;
import com.example.eams.procurement.service.ProcurementService;
import com.example.eams.security.annotation.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 采购入库接口
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementService procurementService;

    // ==================== 供应商管理 ====================

    /**
     * 分页查询供应商列表
     * GET /api/procurement-supplier/list
     */
    @GetMapping("/procurement-supplier/list")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    public Result<PageResult<ProcurementSupplier>> listSupplier(SupplierQueryDTO query) {
        return Result.ok(procurementService.listSupplier(query));
    }

    /**
     * 获取所有启用供应商（下拉选择用）
     * GET /api/procurement-supplier/all
     */
    @GetMapping("/procurement-supplier/all")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    public Result<List<ProcurementSupplier>> allEnabled() {
        return Result.ok(procurementService.listAllEnabledSupplier());
    }

    /**
     * 新增供应商
     * POST /api/procurement-supplier/add
     */
    @PostMapping("/procurement-supplier/add")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "供应商管理", actionType = "新增", description = "新增供应商【{0}】")
    public Result<?> addSupplier(@Valid @RequestBody SupplierAddDTO dto) {
        ProcurementSupplier entity = procurementService.addSupplier(dto);
        return Result.ok("供应商创建成功，编码：" + entity.getSupplierCode(), null);
    }

    /**
     * 编辑供应商
     * PUT /api/procurement-supplier/edit
     */
    @PutMapping("/procurement-supplier/edit")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "供应商管理", actionType = "编辑", description = "编辑供应商【{0}】")
    public Result<?> editSupplier(@Valid @RequestBody SupplierEditDTO dto) {
        procurementService.editSupplier(dto);
        return Result.ok("供应商信息修改成功", null);
    }

    /**
     * 删除供应商
     * DELETE /api/procurement-supplier/delete/{id}
     */
    @DeleteMapping("/procurement-supplier/delete/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "供应商管理", actionType = "删除", description = "删除供应商ID【{0}】")
    public Result<?> deleteSupplier(@PathVariable Long id) {
        procurementService.deleteSupplier(id);
        return Result.ok("供应商已删除", null);
    }

    // ==================== 采购记录管理 ====================

    /**
     * 分页查询采购记录列表
     * GET /api/procurement/record/list
     */
    @GetMapping("/procurement/record/list")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    public Result<PageResult<ProcurementOrder>> list(ProcurementQueryDTO query) {
        return Result.ok(procurementService.listProcurement(query));
    }

    /**
     * 采购记录详情
     * GET /api/procurement/record/detail/{id}
     */
    @GetMapping("/procurement/record/detail/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    public Result<ProcurementVO> detail(@PathVariable Long id) {
        return Result.ok(procurementService.getProcurementDetail(id));
    }

    /**
     * 新增采购记录（采购登记）
     * POST /api/procurement/add
     */
    @PostMapping("/procurement/add")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "采购入库", actionType = "新增", description = "采购登记【{0}】")
    public Result<?> add(@Valid @RequestBody ProcurementAddDTO dto) {
        ProcurementOrder entity = procurementService.add(dto);
        if (entity.getAcceptStatus() != null && entity.getAcceptStatus() >= 2) {
            return Result.ok("验收完成，已生成" + entity.getQuantity() + "项资产", null);
        }
        return Result.ok("采购记录保存成功，验收后资产将自动入库", null);
    }

    /**
     * 编辑采购记录
     * PUT /api/procurement/edit
     */
    @PutMapping("/procurement/edit")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "采购入库", actionType = "编辑", description = "编辑采购记录【{0}】")
    public Result<?> edit(@Valid @RequestBody ProcurementEditDTO dto) {
        procurementService.edit(dto);
        // 判断是否触发了验收
        return Result.ok("采购记录修改成功", null);
    }

    /**
     * 删除采购记录
     * DELETE /api/procurement/delete/{id}
     */
    @DeleteMapping("/procurement/delete/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "采购入库", actionType = "删除", description = "删除采购记录ID【{0}】")
    public Result<?> delete(@PathVariable Long id) {
        procurementService.delete(id);
        return Result.ok("采购记录已删除", null);
    }
}
