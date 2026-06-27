package com.example.eams.system.controller;

import com.example.eams.common.result.PageResult;
import com.example.eams.common.result.Result;
import com.example.eams.security.annotation.RequireRole;
import com.example.eams.common.config.OperationLog;
import com.example.eams.system.dto.DictItemDTO;
import com.example.eams.system.dto.DictTypeDTO;
import com.example.eams.system.entity.SysDictItem;
import com.example.eams.system.entity.SysDictType;
import com.example.eams.system.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 数据字典接口
 */
@RestController
@RequestMapping("/api/system/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    // ==================== 字典类型 ====================

    /**
     * 分页查询字典类型
     * GET /api/system/dict/type/list
     */
    @GetMapping("/type/list")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public Result<PageResult<SysDictType>> listTypes(
            @RequestParam(required = false) String dictName,
            @RequestParam(required = false) String dictCode,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(dictService.listTypes(dictName, dictCode, pageNum, pageSize));
    }

    /**
     * 新增字典类型
     * POST /api/system/dict/type/add
     */
    @PostMapping("/type/add")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "新增", description = "新增字典类型【{0}】")
    public Result<?> addType(@Valid @RequestBody DictTypeDTO dto) {
        dictService.addType(dto);
        return Result.ok("字典类型创建成功", null);
    }

    /**
     * 编辑字典类型
     * PUT /api/system/dict/type/edit
     */
    @PutMapping("/type/edit")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "编辑", description = "编辑字典类型【{0}】")
    public Result<?> editType(@Valid @RequestBody DictTypeDTO dto) {
        dictService.editType(dto);
        return Result.ok("字典类型修改成功", null);
    }

    /**
     * 删除字典类型
     * DELETE /api/system/dict/type/delete/{id}
     */
    @DeleteMapping("/type/delete/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public Result<?> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return Result.ok("字典类型已删除", null);
    }

    // ==================== 字典项 ====================

    /**
     * 查询指定类型的字典项列表
     * GET /api/system/dict/item/list?dictCode=asset_category
     */
    @GetMapping("/item/list")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public Result<List<SysDictItem>> listItems(@RequestParam String dictCode) {
        return Result.ok(dictService.listItems(dictCode));
    }

    /**
     * 新增字典项
     * POST /api/system/dict/item/add
     */
    @PostMapping("/item/add")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "新增", description = "新增字典项【{0}】")
    public Result<?> addItem(@Valid @RequestBody DictItemDTO dto) {
        dictService.addItem(dto);
        return Result.ok("字典项创建成功", null);
    }

    /**
     * 编辑字典项
     * PUT /api/system/dict/item/edit
     */
    @PutMapping("/item/edit")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "编辑", description = "编辑字典项【{0}】")
    public Result<?> editItem(@Valid @RequestBody DictItemDTO dto) {
        dictService.editItem(dto);
        return Result.ok("字典项修改成功", null);
    }

    /**
     * 删除字典项
     * DELETE /api/system/dict/item/delete/{id}
     */
    @DeleteMapping("/item/delete/{id}")
    @RequireRole({"ROLE_SUPER_ADMIN"})
    @OperationLog(module = "系统管理", actionType = "删除", description = "删除字典项ID【{0}】")
    public Result<?> deleteItem(@PathVariable Long id) {
        dictService.deleteItem(id);
        return Result.ok("字典项已删除", null);
    }

    // ==================== 缓存接口（前端调用） ====================

    /**
     * 获取全量字典缓存（前端下拉框使用，无需登录）
     * GET /api/system/dict/all
     */
    @GetMapping("/all")
    public Result<Map<String, List<Map<String, Object>>>> getAllDict() {
        return Result.ok(dictService.getAllDictCached());
    }

    /**
     * 按字典编码获取字典项列表（缓存）
     * GET /api/system/dict/item/cached?dictCode=asset_category
     */
    @GetMapping("/item/cached")
    public Result<List<SysDictItem>> getDictItemsCached(@RequestParam String dictCode) {
        return Result.ok(dictService.getDictItemsCached(dictCode));
    }
}
