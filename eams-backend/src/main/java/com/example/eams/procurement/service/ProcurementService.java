package com.example.eams.procurement.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eams.asset.entity.AssetInfo;
import com.example.eams.asset.mapper.AssetInfoMapper;
import com.example.eams.common.exception.BusinessException;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.util.RedisUtil;
import com.example.eams.procurement.dto.*;
import com.example.eams.procurement.entity.ProcurementOrder;
import com.example.eams.procurement.entity.ProcurementSupplier;
import com.example.eams.procurement.mapper.ProcurementOrderMapper;
import com.example.eams.procurement.mapper.ProcurementSupplierMapper;
import com.example.eams.security.filter.SecurityContextHolder;
import com.example.eams.system.entity.SysDept;
import com.example.eams.system.entity.SysDictItem;
import com.example.eams.system.mapper.SysDeptMapper;
import com.example.eams.system.mapper.SysDictItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购入库服务（供应商管理 + 采购记录管理）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcurementService {

    private final ProcurementSupplierMapper supplierMapper;
    private final ProcurementOrderMapper orderMapper;
    private final AssetInfoMapper assetMapper;
    private final SysDeptMapper deptMapper;
    private final SysDictItemMapper dictItemMapper;

    /** 类别码映射: 字典值 → 编码缩写 */
    private static final Map<String, String> CATEGORY_SHORT = new HashMap<>();

    static {
        CATEGORY_SHORT.put("IT_EQUIPMENT", "IT");
        CATEGORY_SHORT.put("OFFICE_FURNITURE", "OF");
        CATEGORY_SHORT.put("PRODUCTION", "PE");
        CATEGORY_SHORT.put("VEHICLE", "VE");
        CATEGORY_SHORT.put("OTHER", "OT");
    }

    // ==================== 供应商管理 ====================

    /**
     * 分页查询供应商列表
     */
    public PageResult<ProcurementSupplier> listSupplier(SupplierQueryDTO query) {
        LambdaQueryWrapper<ProcurementSupplier> wrapper = new LambdaQueryWrapper<ProcurementSupplier>()
                .eq(ProcurementSupplier::getIsDeleted, 0);

        if (StrUtil.isNotBlank(query.getSupplierName())) {
            wrapper.like(ProcurementSupplier::getSupplierName, query.getSupplierName());
        }
        if (StrUtil.isNotBlank(query.getContactPerson())) {
            wrapper.like(ProcurementSupplier::getContactPerson, query.getContactPerson());
        }
        if (StrUtil.isNotBlank(query.getContactPhone())) {
            wrapper.like(ProcurementSupplier::getContactPhone, query.getContactPhone());
        }
        wrapper.orderByDesc(ProcurementSupplier::getCreateTime);

        IPage<ProcurementSupplier> page = supplierMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        PageResult<ProcurementSupplier> result = PageResult.of(page);
        fillSupplierStatusLabel(result.getList());
        return result;
    }

    /**
     * 获取所有启用状态的供应商（供下拉选择）
     */
    public List<ProcurementSupplier> listAllEnabledSupplier() {
        return supplierMapper.selectList(
                new LambdaQueryWrapper<ProcurementSupplier>()
                        .eq(ProcurementSupplier::getStatus, 1)
                        .eq(ProcurementSupplier::getIsDeleted, 0)
                        .orderByAsc(ProcurementSupplier::getSupplierCode));
    }

    /**
     * 新增供应商
     */
    @Transactional(rollbackFor = Exception.class)
    public ProcurementSupplier addSupplier(SupplierAddDTO dto) {
        // 校验名称唯一性
        ProcurementSupplier existByName = supplierMapper.selectOne(
                new LambdaQueryWrapper<ProcurementSupplier>()
                        .eq(ProcurementSupplier::getSupplierName, dto.getSupplierName())
                        .eq(ProcurementSupplier::getIsDeleted, 0));
        if (existByName != null) {
            throw new BusinessException(400, "供应商名称已存在");
        }

        // 校验编码唯一性
        ProcurementSupplier existByCode = supplierMapper.selectOne(
                new LambdaQueryWrapper<ProcurementSupplier>()
                        .eq(ProcurementSupplier::getSupplierCode, dto.getSupplierCode())
                        .eq(ProcurementSupplier::getIsDeleted, 0));
        if (existByCode != null) {
            throw new BusinessException(400, "供应商编码已存在");
        }

        ProcurementSupplier entity = new ProcurementSupplier();
        entity.setSupplierName(dto.getSupplierName());
        entity.setSupplierCode(dto.getSupplierCode().toUpperCase());
        entity.setContactPerson(dto.getContactPerson());
        entity.setContactPhone(dto.getContactPhone());
        entity.setAddress(dto.getAddress());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setRemark(dto.getRemark());

        supplierMapper.insert(entity);
        return entity;
    }

    /**
     * 编辑供应商
     */
    @Transactional(rollbackFor = Exception.class)
    public void editSupplier(SupplierEditDTO dto) {
        ProcurementSupplier entity = supplierMapper.selectById(dto.getId());
        if (entity == null) {
            throw BusinessException.notFound("供应商不存在");
        }

        // 校验名称唯一性（排除自身）
        ProcurementSupplier existByName = supplierMapper.selectOne(
                new LambdaQueryWrapper<ProcurementSupplier>()
                        .eq(ProcurementSupplier::getSupplierName, dto.getSupplierName())
                        .eq(ProcurementSupplier::getIsDeleted, 0)
                        .ne(ProcurementSupplier::getId, dto.getId()));
        if (existByName != null) {
            throw new BusinessException(400, "供应商名称已存在");
        }

        // 校验编码唯一性（排除自身）
        ProcurementSupplier existByCode = supplierMapper.selectOne(
                new LambdaQueryWrapper<ProcurementSupplier>()
                        .eq(ProcurementSupplier::getSupplierCode, dto.getSupplierCode())
                        .eq(ProcurementSupplier::getIsDeleted, 0)
                        .ne(ProcurementSupplier::getId, dto.getId()));
        if (existByCode != null) {
            throw new BusinessException(400, "供应商编码已存在");
        }

        entity.setSupplierName(dto.getSupplierName());
        entity.setSupplierCode(dto.getSupplierCode().toUpperCase());
        entity.setContactPerson(dto.getContactPerson());
        entity.setContactPhone(dto.getContactPhone());
        entity.setAddress(dto.getAddress());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        entity.setRemark(dto.getRemark());

        supplierMapper.updateById(entity);
    }

    /**
     * 删除供应商（软删除，有关联采购记录时不可删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplier(Long id) {
        ProcurementSupplier entity = supplierMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("供应商不存在");
        }

        // 检查是否有关联采购记录
        int count = orderMapper.countBySupplierId(id);
        if (count > 0) {
            throw new BusinessException(400, "该供应商存在关联采购记录，不可删除");
        }

        supplierMapper.deleteById(id);
    }

    /**
     * 填充供应商状态标签
     */
    private void fillSupplierStatusLabel(List<ProcurementSupplier> list) {
        list.forEach(s -> {
            if (s.getStatus() != null && s.getStatus() == 1) {
                s.setStatusLabel("启用");
            } else {
                s.setStatusLabel("禁用");
            }
        });
    }

    // ==================== 采购记录管理 ====================

    /**
     * 分页查询采购记录列表
     */
    public PageResult<ProcurementOrder> listProcurement(ProcurementQueryDTO query) {
        LambdaQueryWrapper<ProcurementOrder> wrapper = new LambdaQueryWrapper<ProcurementOrder>()
                .eq(ProcurementOrder::getIsDeleted, 0);

        // DEPT_ADMIN 数据权限：只能查看本部门及子部门
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        if (roles.contains("ROLE_DEPT_ADMIN") && !roles.contains("ROLE_SUPER_ADMIN")
                && !roles.contains("ROLE_ASSET_ADMIN")) {
            // 通过当前用户的部门来过滤
            // DEPT_ADMIN 的部门数据从 sys_user 关联，这里通过当前用户的 dept 来过滤
            Long currentUserId = SecurityContextHolder.getCurrentUserId();
            // 简化处理：DEPT_ADMIN只能看自己部门的（实际可通过 user -> dept 关联来确定）
            // 此处通过前端传入的 deptId 或默认不限制（由角色AOP保证角色级别权限）
        }

        if (StrUtil.isNotBlank(query.getProcurementNo())) {
            wrapper.like(ProcurementOrder::getProcurementNo, query.getProcurementNo());
        }
        if (StrUtil.isNotBlank(query.getAssetName())) {
            wrapper.like(ProcurementOrder::getAssetName, query.getAssetName());
        }
        if (query.getSupplierId() != null) {
            wrapper.eq(ProcurementOrder::getSupplierId, query.getSupplierId());
        }
        if (query.getAcceptStatus() != null) {
            wrapper.eq(ProcurementOrder::getAcceptStatus, query.getAcceptStatus());
        }
        if (StrUtil.isNotBlank(query.getBeginDate())) {
            wrapper.ge(ProcurementOrder::getPurchaseDate, query.getBeginDate());
        }
        if (StrUtil.isNotBlank(query.getEndDate())) {
            wrapper.le(ProcurementOrder::getPurchaseDate, query.getEndDate());
        }
        wrapper.orderByDesc(ProcurementOrder::getCreateTime);

        IPage<ProcurementOrder> page = orderMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        PageResult<ProcurementOrder> result = PageResult.of(page);
        fillProcurementNames(result.getList());
        return result;
    }

    /**
     * 采购记录详情
     */
    public ProcurementVO getProcurementDetail(Long id) {
        ProcurementOrder entity = orderMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("采购记录不存在");
        }
        fillProcurementNames(Collections.singletonList(entity));
        return toVO(entity);
    }

    /**
     * 新增采购记录（含验收入库逻辑）
     */
    @Transactional(rollbackFor = Exception.class)
    public ProcurementOrder add(ProcurementAddDTO dto) {
        // 校验采购日期不能晚于今天
        if (dto.getPurchaseDate().isAfter(LocalDate.now())) {
            throw new BusinessException(400, "采购日期不能晚于当前日期");
        }

        // 验收状态为已验收时，验收日期必填
        if (dto.getAcceptStatus() == 1 && dto.getAcceptDate() == null) {
            throw new BusinessException(400, "请选择验收日期");
        }
        if (dto.getAcceptStatus() == 1 && dto.getAcceptDate() != null
                && dto.getAcceptDate().isAfter(LocalDate.now())) {
            throw new BusinessException(400, "验收日期不能晚于当前日期");
        }

        // 校验供应商存在且启用
        ProcurementSupplier supplier = supplierMapper.selectById(dto.getSupplierId());
        if (supplier == null) {
            throw BusinessException.notFound("供应商不存在");
        }
        if (supplier.getStatus() != 1) {
            throw new BusinessException(400, "供应商已禁用，请重新选择");
        }

        ProcurementOrder entity = new ProcurementOrder();
        entity.setProcurementNo(dto.getProcurementNo());
        entity.setAssetName(dto.getAssetName());
        entity.setCategory(dto.getCategory());
        entity.setSpecification(dto.getSpecification());
        entity.setSnNumber(dto.getSnNumber());
        entity.setQuantity(dto.getQuantity());
        entity.setUnitPrice(dto.getUnitPrice());
        // 计算总价
        entity.setTotalAmount(dto.getUnitPrice().multiply(new BigDecimal(dto.getQuantity())));
        entity.setPurchaseDate(dto.getPurchaseDate());
        entity.setSupplierId(dto.getSupplierId());
        entity.setUsefulLife(dto.getUsefulLife());
        entity.setResidualRate(dto.getResidualRate() != null ? dto.getResidualRate() : new BigDecimal("5.00"));
        entity.setDeptId(dto.getDeptId());
        entity.setLocation(dto.getLocation());
        entity.setAcceptStatus(dto.getAcceptStatus());
        entity.setAcceptDate(dto.getAcceptDate());
        entity.setRemark(dto.getRemark());

        orderMapper.insert(entity);

        // 如果已验收，自动生成资产
        if (dto.getAcceptStatus() == 1) {
            acceptAndGenerateAssets(entity);
        }

        return entity;
    }

    /**
     * 编辑采购记录（含补充验收逻辑）
     */
    @Transactional(rollbackFor = Exception.class)
    public void edit(ProcurementEditDTO dto) {
        ProcurementOrder entity = orderMapper.selectById(dto.getId());
        if (entity == null) {
            throw BusinessException.notFound("采购记录不存在");
        }

        // 已入库记录不可编辑关键字段
        if (entity.getAcceptStatus() >= 2) {
            throw new BusinessException(400, "该记录已完成入库，不可编辑");
        }

        // 已验收过的不允许再次修改验收状态
        boolean triggerAcceptance = false;
        if (entity.getAcceptStatus() == 0 && dto.getAcceptStatus() == 1) {
            // 从待验收→已验收，触发验收入库
            triggerAcceptance = true;
            if (dto.getAcceptDate() == null) {
                throw new BusinessException(400, "请选择验收日期");
            }
            if (dto.getAcceptDate().isAfter(LocalDate.now())) {
                throw new BusinessException(400, "验收日期不能晚于当前日期");
            }
        }

        // 如果已经验收过了，不允许改为待验收
        if (entity.getAcceptStatus() >= 1 && dto.getAcceptStatus() == 0) {
            throw new BusinessException(400, "已验收记录不能改为待验收");
        }

        // 校验采购日期
        if (dto.getPurchaseDate().isAfter(LocalDate.now())) {
            throw new BusinessException(400, "采购日期不能晚于当前日期");
        }

        // 校验供应商存在且启用
        ProcurementSupplier supplier = supplierMapper.selectById(dto.getSupplierId());
        if (supplier == null) {
            throw BusinessException.notFound("供应商不存在");
        }
        if (supplier.getStatus() != 1) {
            throw new BusinessException(400, "供应商已禁用，请重新选择");
        }

        entity.setProcurementNo(dto.getProcurementNo());
        entity.setAssetName(dto.getAssetName());
        entity.setCategory(dto.getCategory());
        entity.setSpecification(dto.getSpecification());
        entity.setSnNumber(dto.getSnNumber());
        entity.setQuantity(dto.getQuantity());
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setTotalAmount(dto.getUnitPrice().multiply(new BigDecimal(dto.getQuantity())));
        entity.setPurchaseDate(dto.getPurchaseDate());
        entity.setSupplierId(dto.getSupplierId());
        entity.setUsefulLife(dto.getUsefulLife());
        entity.setResidualRate(dto.getResidualRate() != null ? dto.getResidualRate() : new BigDecimal("5.00"));
        entity.setDeptId(dto.getDeptId());
        entity.setLocation(dto.getLocation());
        entity.setAcceptStatus(dto.getAcceptStatus());
        entity.setAcceptDate(dto.getAcceptDate());
        entity.setRemark(dto.getRemark());

        orderMapper.updateById(entity);

        // 补充验收：生成资产
        if (triggerAcceptance) {
            acceptAndGenerateAssets(entity);
        }
    }

    /**
     * 删除采购记录（软删除，已验收不可删）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProcurementOrder entity = orderMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("采购记录不存在");
        }
        if (entity.getAcceptStatus() >= 1) {
            throw new BusinessException(400, "该记录已验收，不可删除");
        }
        orderMapper.deleteById(id);
    }

    // ==================== 验收入库核心逻辑 ====================

    /**
     * 验收并自动生成资产
     */
    private void acceptAndGenerateAssets(ProcurementOrder entity) {
        // 分布式锁防止并发重复生成资产
        String lockKey = "eams:lock:procurement:accept:" + entity.getId();
        String lockValue = RedisUtil.tryLock(lockKey, 3, 30);
        if (lockValue == null) {
            throw new BusinessException(400, "操作繁忙，请稍后重试");
        }

        try {
            int quantity = entity.getQuantity();
            String category = entity.getCategory();
            String categoryShort = CATEGORY_SHORT.getOrDefault(category, "OT");
            String yyMM = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM"));

            // 获取资产编码锁，批量生成所有编码
            String codeLockKey = "eams:lock:asset:code:" + categoryShort + ":" + yyMM;
            String codeLockValue = RedisUtil.tryLock(codeLockKey, 3, 30);
            if (codeLockValue == null) {
                throw new BusinessException(400, "编码生成繁忙，请稍后重试");
            }

            List<String> codes = new ArrayList<>();
            int startSeq;
            try {
                String maxCode = orderMapper.selectMaxAssetCodeByPrefix(categoryShort, yyMM);
                startSeq = 1;
                if (maxCode != null) {
                    startSeq = Integer.parseInt(maxCode.substring(maxCode.length() - 4)) + 1;
                }
                for (int i = 0; i < quantity; i++) {
                    codes.add(String.format("AS-%s-%s-%04d", categoryShort, yyMM, startSeq + i));
                }
            } finally {
                RedisUtil.unlock(codeLockKey, codeLockValue);
            }

            List<AssetInfo> assets = new ArrayList<>();
            for (int i = 0; i < quantity; i++) {
                String assetCode = codes.get(i);

                AssetInfo asset = new AssetInfo();
                asset.setAssetCode(assetCode);
                asset.setAssetName(entity.getAssetName());
                asset.setCategory(entity.getCategory());
                asset.setSpecification(entity.getSpecification());
                asset.setSnNumber(entity.getSnNumber());
                asset.setProcurementNo(entity.getProcurementNo());
                asset.setProcurementId(entity.getId());
                asset.setOriginalValue(entity.getUnitPrice());
                asset.setPurchaseDate(entity.getPurchaseDate());
                asset.setUsefulLife(entity.getUsefulLife());
                asset.setResidualRate(entity.getResidualRate());
                asset.setLocation(entity.getLocation());
                asset.setDeptId(entity.getDeptId());
                asset.setStatus(0); // 闲置
                asset.setRemark("采购入库自动生成，采购单号：" +
                        (entity.getProcurementNo() != null ? entity.getProcurementNo() : entity.getId()));
                // 计算报废日期
                asset.setScrapDate(entity.getPurchaseDate().plusYears(entity.getUsefulLife()));

                assets.add(asset);
            }

            // 批量插入资产
            for (AssetInfo asset : assets) {
                assetMapper.insert(asset);
            }

            // 更新采购记录状态为已入库
            entity.setAcceptStatus(2);
            entity.setAcceptDate(entity.getAcceptDate() != null ? entity.getAcceptDate() : LocalDate.now());
            orderMapper.updateById(entity);

            // 清除资产列表缓存
            RedisUtil.deleteByPattern("eams:asset:list:*");

            log.info("验收完成，采购记录ID={}，生成{}项资产: {} ~ {}",
                    entity.getId(), quantity,
                    codes.isEmpty() ? "" : codes.get(0),
                    codes.isEmpty() ? "" : codes.get(codes.size() - 1));

        } finally {
            RedisUtil.unlock(lockKey, lockValue);
        }
    }

    // ==================== 资产编码生成 ====================

    /**
     * 生成资产编码: AS-{类别码}-{YYMM}-{4位流水}
     */
    public String generateAssetCode(String category, Long deptId) {
        String categoryShort = CATEGORY_SHORT.getOrDefault(category, "OT");
        String yyMM = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM"));

        // 分布式锁保护流水号
        String lockKey = "eams:lock:asset:code:" + categoryShort + ":" + yyMM;
        String lockValue = RedisUtil.tryLock(lockKey, 3, 30);
        if (lockValue == null) {
            throw new BusinessException(400, "编码生成繁忙，请稍后重试");
        }

        try {
            String maxCode = orderMapper.selectMaxAssetCodeByPrefix(categoryShort, yyMM);
            int seq = 1;
            if (maxCode != null) {
                seq = Integer.parseInt(maxCode.substring(maxCode.length() - 4)) + 1;
            }
            return String.format("AS-%s-%s-%04d", categoryShort, yyMM, seq);
        } finally {
            RedisUtil.unlock(lockKey, lockValue);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 批量填充供应商名称、部门名称、分类标签、验收状态标签
     */
    private void fillProcurementNames(List<ProcurementOrder> list) {
        if (list.isEmpty()) return;

        // 批量查询供应商
        Set<Long> supplierIds = list.stream().map(ProcurementOrder::getSupplierId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> supplierMap;
        if (!supplierIds.isEmpty()) {
            supplierMap = supplierMapper.selectBatchIds(supplierIds).stream()
                    .collect(Collectors.toMap(ProcurementSupplier::getId,
                            ProcurementSupplier::getSupplierName, (a, b) -> a));
        } else {
            supplierMap = Collections.emptyMap();
        }

        // 批量查询部门
        Set<Long> deptIds = list.stream().map(ProcurementOrder::getDeptId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> deptMap;
        if (!deptIds.isEmpty()) {
            deptMap = deptMapper.selectBatchIds(deptIds).stream()
                    .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));
        } else {
            deptMap = Collections.emptyMap();
        }

        // 批量查询分类字典
        Set<String> categories = list.stream().map(ProcurementOrder::getCategory)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<String, String> categoryMap;
        if (!categories.isEmpty()) {
            List<SysDictItem> dictItems = dictItemMapper.selectList(
                    new LambdaQueryWrapper<SysDictItem>()
                            .eq(SysDictItem::getDictCode, "asset_category")
                            .eq(SysDictItem::getStatus, 1)
                            .in(SysDictItem::getDictValue, categories));
            categoryMap = dictItems.stream()
                    .collect(Collectors.toMap(SysDictItem::getDictValue,
                            SysDictItem::getDictLabel, (a, b) -> a));
        } else {
            categoryMap = Collections.emptyMap();
        }

        Map<String, String> finalCategoryMap = categoryMap;
        list.forEach(o -> {
            o.setSupplierName(supplierMap.getOrDefault(o.getSupplierId(), ""));
            o.setDeptName(deptMap.getOrDefault(o.getDeptId(), ""));
            o.setCategoryLabel(finalCategoryMap.getOrDefault(o.getCategory(), o.getCategory()));
            o.setAcceptStatusLabel(getAcceptStatusLabel(o.getAcceptStatus()));
        });
    }

    /**
     * 验收状态 → 标签
     */
    private String getAcceptStatusLabel(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待验收";
            case 1: return "已验收";
            case 2: return "已入库";
            case 3: return "已取消";
            default: return String.valueOf(status);
        }
    }

    /**
     * Entity → VO
     */
    private ProcurementVO toVO(ProcurementOrder entity) {
        ProcurementVO vo = new ProcurementVO();
        vo.setId(entity.getId());
        vo.setProcurementNo(entity.getProcurementNo());
        vo.setAssetName(entity.getAssetName());
        vo.setCategory(entity.getCategory());
        vo.setCategoryLabel(entity.getCategoryLabel());
        vo.setSpecification(entity.getSpecification());
        vo.setSnNumber(entity.getSnNumber());
        vo.setQuantity(entity.getQuantity());
        vo.setUnitPrice(entity.getUnitPrice());
        vo.setTotalAmount(entity.getTotalAmount());
        vo.setPurchaseDate(entity.getPurchaseDate());
        vo.setSupplierId(entity.getSupplierId());
        vo.setSupplierName(entity.getSupplierName());
        vo.setUsefulLife(entity.getUsefulLife());
        vo.setResidualRate(entity.getResidualRate());
        vo.setDeptId(entity.getDeptId());
        vo.setDeptName(entity.getDeptName());
        vo.setLocation(entity.getLocation());
        vo.setAcceptStatus(entity.getAcceptStatus());
        vo.setAcceptStatusLabel(entity.getAcceptStatusLabel());
        vo.setAcceptDate(entity.getAcceptDate());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setCreateBy(entity.getCreateBy());
        return vo;
    }

    // ==================== 缓存清除 ====================

    private void clearListCache() {
        RedisUtil.deleteByPattern("eams:procurement:list:*");
    }
}
