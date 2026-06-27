package com.example.eams.asset.controller;

import com.example.eams.asset.dto.*;
import com.example.eams.asset.entity.AssetDepreciation;
import com.example.eams.asset.entity.AssetInfo;
import com.example.eams.asset.service.AssetService;
import com.example.eams.common.config.OperationLog;
import com.example.eams.common.exception.BusinessException;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.result.Result;
import com.example.eams.common.util.ExcelUtil;
import com.example.eams.security.annotation.RequireRole;
import com.example.eams.system.entity.SysDept;
import com.example.eams.system.entity.SysDictItem;
import com.example.eams.system.mapper.SysDeptMapper;
import com.example.eams.system.mapper.SysDictItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.poi.ss.util.CellRangeAddressList;

/**
 * 资产台账接口
 */
@RestController
@RequestMapping("/api/asset")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final SysDictItemMapper dictItemMapper;
    private final SysDeptMapper deptMapper;

    /**
     * 分页查询资产列表
     * GET /api/asset/list
     */
    @GetMapping("/list")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    public Result<PageResult<AssetInfo>> list(AssetQueryDTO query) {
        return Result.ok(assetService.list(query));
    }

    /**
     * 资产详情
     * GET /api/asset/detail/{id}
     */
    @GetMapping("/detail/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN", "ROLE_DEPT_ADMIN"})
    public Result<AssetVO> detail(@PathVariable Long id) {
        return Result.ok(assetService.getDetail(id));
    }

    /**
     * 新增资产
     * POST /api/asset/add
     */
    @PostMapping("/add")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "资产台账", actionType = "新增", description = "新增资产【{0}】")
    public Result<?> add(@Valid @RequestBody AssetAddDTO dto) {
        AssetInfo asset = assetService.add(dto);
        return Result.ok("资产【" + dto.getAssetName() + "】创建成功，编码：" + asset.getAssetCode(), null);
    }

    /**
     * 编辑资产
     * PUT /api/asset/edit
     */
    @PutMapping("/edit")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "资产台账", actionType = "编辑", description = "编辑资产【{0}】")
    public Result<?> edit(@Valid @RequestBody AssetEditDTO dto) {
        assetService.edit(dto);
        return Result.ok("资产【" + dto.getAssetName() + "】信息修改成功", null);
    }

    /**
     * 删除资产
     * DELETE /api/asset/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "资产台账", actionType = "删除", description = "删除资产ID【{0}】")
    public Result<?> delete(@PathVariable Long id) {
        assetService.delete(id);
        return Result.ok("资产已删除", null);
    }

    /**
     * 折旧明细
     * GET /api/asset/depreciation/{assetId}
     */
    @GetMapping("/depreciation/{assetId}")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    public Result<List<AssetDepreciation>> depreciation(@PathVariable Long assetId) {
        return Result.ok(assetService.getDepreciationList(assetId));
    }

    /**
     * 批量导入资产
     * POST /api/asset/import
     */
    @PostMapping("/import")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "资产台账", actionType = "导入", description = "批量导入资产")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        List<Map<String, Object>> rows = ExcelUtil.read(file);
        Map<String, Object> result = assetService.importAssets(rows);
        return Result.ok("导入完成，成功 " + result.get("success") + " 条，失败 " + result.get("fail") + " 条", result);
    }

    /**
     * 下载导入错误报告
     * POST /api/asset/import-error-report
     */
    @PostMapping("/import-error-report")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @SuppressWarnings("unchecked")
    public void downloadErrorReport(@RequestBody List<Map<String, Object>> errors,
                                    HttpServletResponse response) throws IOException {
        org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("导入错误报告");

        // 红色背景样式（失败原因列）
        org.apache.poi.ss.usermodel.CellStyle redStyle = wb.createCellStyle();
        redStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.RED.getIndex());
        redStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        org.apache.poi.ss.usermodel.Font whiteFont = wb.createFont();
        whiteFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
        whiteFont.setBold(true);
        redStyle.setFont(whiteFont);

        // 普通表头样式
        org.apache.poi.ss.usermodel.CellStyle headerStyle = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font boldFont = wb.createFont();
        boldFont.setBold(true);
        headerStyle.setFont(boldFont);

        // 11列原始数据 + 1列失败原因
        String[] dataHeaders = {"资产名称", "资产分类", "规格型号", "SN序列号", "采购编号",
                "原值(元)", "采购日期", "使用年限(年)", "存放地点", "所属部门", "备注"};

        // 创建表头行（12列）
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        for (int i = 0; i < dataHeaders.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(dataHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        org.apache.poi.ss.usermodel.Cell reasonHeaderCell = headerRow.createCell(dataHeaders.length);
        reasonHeaderCell.setCellValue("失败原因");
        reasonHeaderCell.setCellStyle(redStyle);

        // 填充错误数据
        for (int i = 0; i < errors.size(); i++) {
            Map<String, Object> err = errors.get(i);
            Map<String, Object> data = (Map<String, Object>) err.get("data");
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(i + 1);
            for (int j = 0; j < dataHeaders.length; j++) {
                String val = data != null && data.get(dataHeaders[j]) != null ? data.get(dataHeaders[j]).toString() : "";
                row.createCell(j).setCellValue(val);
            }
            row.createCell(dataHeaders.length).setCellValue(
                    err.get("reason") != null ? err.get("reason").toString() : "");
        }

        for (int i = 0; i <= dataHeaders.length; i++) sheet.autoSizeColumn(i);

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = java.net.URLEncoder.encode("导入错误报告_" + ts, "UTF-8").replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");
        wb.write(response.getOutputStream());
        wb.close();
    }

    /**
     * 导出资产
     * GET /api/asset/export
     */
    @GetMapping("/export")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    @OperationLog(module = "资产台账", actionType = "导出", description = "导出资产列表")
    public void export(AssetQueryDTO query, HttpServletResponse response) {
        List<AssetInfo> list = assetService.exportData(query);

        // 查询字典映射（value → label）
        List<SysDictItem> dictItems = dictItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysDictItem>()
                        .eq(SysDictItem::getDictCode, "asset_category")
                        .eq(SysDictItem::getStatus, 1));
        Map<String, String> categoryLabelMap = dictItems.stream()
                .collect(java.util.stream.Collectors.toMap(SysDictItem::getDictValue, SysDictItem::getDictLabel, (a, b) -> a));

        LinkedHashMap<String, String> headerAlias = new LinkedHashMap<>();
        headerAlias.put("assetCode", "资产编码");
        headerAlias.put("assetName", "资产名称");
        headerAlias.put("category", "资产分类");
        headerAlias.put("specification", "规格型号");
        headerAlias.put("snNumber", "SN序列号");
        headerAlias.put("status", "资产状态");
        headerAlias.put("deptName", "所属部门");
        headerAlias.put("userName", "使用人");
        headerAlias.put("location", "存放地点");
        headerAlias.put("originalValue", "原值(元)");
        headerAlias.put("purchaseDate", "采购日期");
        headerAlias.put("usefulLife", "使用年限(年)");

        List<Map<String, Object>> dataList = list.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("assetCode", a.getAssetCode());
            m.put("assetName", a.getAssetName());
            m.put("category", categoryLabelMap.getOrDefault(a.getCategory(), a.getCategory()));
            m.put("specification", a.getSpecification());
            m.put("snNumber", a.getSnNumber());
            m.put("status", getStatusLabel(a.getStatus()));
            m.put("deptName", a.getDeptName());
            m.put("userName", a.getUserName());
            m.put("location", a.getLocation());
            m.put("originalValue", a.getOriginalValue());
            m.put("purchaseDate", a.getPurchaseDate() != null ? a.getPurchaseDate().toString() : "");
            m.put("usefulLife", a.getUsefulLife());
            return m;
        }).collect(java.util.stream.Collectors.toList());

        String fileName = "资产台账_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ExcelUtil.export(response, fileName, headerAlias, dataList);
    }

    /**
     * 下载导入模板（PRD 6.2.5 严格对齐）
     * 必填列标黄 + 文本格式 + 资产分类/所属部门下拉框
     * GET /api/asset/template
     */
    @GetMapping("/template")
    @RequireRole({"ROLE_SUPER_ADMIN", "ROLE_ASSET_ADMIN"})
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("资产导入模板");

        // ── 查询字典与部门 ──
        List<SysDictItem> categoryItems = dictItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysDictItem>()
                        .eq(SysDictItem::getDictCode, "asset_category")
                        .eq(SysDictItem::getStatus, 1)
                        .orderByAsc(SysDictItem::getSortOrder));
        List<SysDept> depts = deptMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getStatus, 1)
                        .eq(SysDept::getIsDeleted, 0));

        // 取第一条作为样例
        String sampleCategory = categoryItems.isEmpty() ? "IT设备" : categoryItems.get(0).getDictLabel();
        String sampleDept = depts.isEmpty() ? "技术部" : depts.get(0).getDeptName();

        // ── 单元格格式 ──
        org.apache.poi.ss.usermodel.DataFormat dataFormat = wb.createDataFormat();
        // 文本格式（大部分列用）
        org.apache.poi.ss.usermodel.CellStyle textStyle = wb.createCellStyle();
        textStyle.setDataFormat(dataFormat.getFormat("@"));
        org.apache.poi.ss.usermodel.CellStyle yellowTextStyle = wb.createCellStyle();
        yellowTextStyle.cloneStyleFrom(textStyle);
        yellowTextStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.YELLOW.getIndex());
        yellowTextStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        org.apache.poi.ss.usermodel.Font boldFont = wb.createFont();
        boldFont.setBold(true);
        yellowTextStyle.setFont(boldFont);

        // 数值格式（原值 和 使用年限 使用）
        org.apache.poi.ss.usermodel.CellStyle numberStyle = wb.createCellStyle();
        numberStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));
        org.apache.poi.ss.usermodel.CellStyle yellowNumberStyle = wb.createCellStyle();
        yellowNumberStyle.cloneStyleFrom(numberStyle);
        yellowNumberStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.YELLOW.getIndex());
        yellowNumberStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        yellowNumberStyle.setFont(boldFont);

        // ── 表头定义 ──
        // 列名, 是否必填, 是否下拉, 格式类型(text/number)
        Object[][] columnDefs = {
                {"资产名称", true, false, "text"}, {"资产分类", true, true, "text"},
                {"规格型号", false, false, "text"}, {"SN序列号", false, false, "text"},
                {"采购编号", false, false, "text"}, {"原值(元)", true, false, "number"},
                {"采购日期", true, false, "text"}, {"使用年限(年)", true, false, "number"},
                {"存放地点", true, false, "text"}, {"所属部门", true, true, "text"},
                {"备注", false, false, "text"},
        };

        // ── 创建表头行 ──
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columnDefs.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue((String) columnDefs[i][0]);
            boolean required = (Boolean) columnDefs[i][1];
            boolean isNumber = "number".equals(columnDefs[i][3]);
            cell.setCellStyle(required ? (isNumber ? yellowNumberStyle : yellowTextStyle) : (isNumber ? numberStyle : textStyle));
        }

        // ── 样例数据 ──
        String[] sampleRow = {"示例：ThinkPad X1 Carbon", sampleCategory, "14寸 i7 16G 512G", "", "", "12999.00",
                "2026-01-15", "5", "3楼研发部-301室", sampleDept, ""};
        org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(1);
        for (int i = 0; i < sampleRow.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = dataRow.createCell(i);
            boolean isNumber = "number".equals(columnDefs[i][3]);
            if (isNumber && sampleRow[i] != null && !sampleRow[i].isEmpty()) {
                try { cell.setCellValue(Double.parseDouble(sampleRow[i])); }
                catch (NumberFormatException e) { cell.setCellValue(sampleRow[i]); }
            } else {
                cell.setCellValue(sampleRow[i]);
            }
            cell.setCellStyle(isNumber ? numberStyle : textStyle);
        }

        // ── 下拉框（使用隐藏Sheet + 公式引用，兼容 WPS/Excel）──
        org.apache.poi.ss.usermodel.Sheet hidden = wb.createSheet("OPTIONS");
        wb.setSheetHidden(wb.getSheetIndex("OPTIONS"), true);
        // 资产分类选项写入隐藏Sheet A列
        for (int i = 0; i < categoryItems.size(); i++) {
            hidden.createRow(i).createCell(0).setCellValue(categoryItems.get(i).getDictLabel());
        }
        // 部门名称从 A列续写
        int deptStart = categoryItems.size();
        for (int i = 0; i < depts.size(); i++) {
            hidden.createRow(deptStart + i).createCell(0).setCellValue(depts.get(i).getDeptName());
        }

        // 资产分类下拉（B列，引用隐藏Sheet的A1:A{catCount}）
        if (!categoryItems.isEmpty()) {
            String formula = "OPTIONS!$A$1:$A$" + categoryItems.size();
            CellRangeAddressList addressList = new CellRangeAddressList(1, 999, 1, 1);
            org.apache.poi.ss.usermodel.DataValidationHelper dvHelper = sheet.getDataValidationHelper();
            org.apache.poi.ss.usermodel.DataValidationConstraint dvConstraint =
                    dvHelper.createFormulaListConstraint(formula);
            org.apache.poi.ss.usermodel.DataValidation validation =
                    dvHelper.createValidation(dvConstraint, addressList);
            validation.setSuppressDropDownArrow(false);
            validation.setShowErrorBox(true);
            sheet.addValidationData(validation);
        }

        // 所属部门下拉（J列，引用隐藏Sheet的A{deptStart+1}:A{end}）
        if (!depts.isEmpty()) {
            int startRow = deptStart + 1;
            int endRow = deptStart + depts.size();
            String formula = "OPTIONS!$A$" + startRow + ":$A$" + endRow;
            CellRangeAddressList addressList = new CellRangeAddressList(1, 999, 9, 9);
            org.apache.poi.ss.usermodel.DataValidationHelper dvHelper = sheet.getDataValidationHelper();
            org.apache.poi.ss.usermodel.DataValidationConstraint dvConstraint =
                    dvHelper.createFormulaListConstraint(formula);
            org.apache.poi.ss.usermodel.DataValidation validation =
                    dvHelper.createValidation(dvConstraint, addressList);
            validation.setSuppressDropDownArrow(false);
            validation.setShowErrorBox(true);
            sheet.addValidationData(validation);
        }

        // ── 自动列宽 ──
        for (int i = 0; i < columnDefs.length; i++) sheet.autoSizeColumn(i);

        // ── 写入响应（含时间戳） ──
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = java.net.URLEncoder.encode("资产导入模板_" + ts, "UTF-8").replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");
        wb.write(response.getOutputStream());
        wb.close();
    }

    private String getStatusLabel(Integer status) {
        if (status == null) return "";
        String[] labels = {"闲置", "在用", "借用", "维修", "报废", "盘点中"};
        return status >= 0 && status < labels.length ? labels[status] : String.valueOf(status);
    }
}
