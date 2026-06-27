package com.example.eams.asset.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eams.asset.dto.*;
import com.example.eams.asset.entity.AssetDepreciation;
import com.example.eams.asset.entity.AssetInfo;
import com.example.eams.asset.mapper.AssetDepreciationMapper;
import com.example.eams.asset.mapper.AssetInfoMapper;
import com.example.eams.common.exception.BusinessException;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.util.RedisUtil;
import com.example.eams.system.entity.SysDept;
import com.example.eams.system.entity.SysDictItem;
import com.example.eams.system.entity.SysUser;
import com.example.eams.system.mapper.SysDeptMapper;
import com.example.eams.system.mapper.SysDictItemMapper;
import com.example.eams.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 资产台账服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetInfoMapper assetMapper;
    private final AssetDepreciationMapper depreciationMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
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

    // ==================== 列表查询 ====================

    /**
     * 分页查询资产列表（含Redis缓存）
     */
    public PageResult<AssetInfo> list(AssetQueryDTO query) {
        // 构建查询条件
        LambdaQueryWrapper<AssetInfo> wrapper = new LambdaQueryWrapper<AssetInfo>()
                .eq(AssetInfo::getIsDeleted, 0);

        if (StrUtil.isNotBlank(query.getAssetCode())) {
            wrapper.eq(AssetInfo::getAssetCode, query.getAssetCode());
        }
        if (StrUtil.isNotBlank(query.getAssetName())) {
            wrapper.like(AssetInfo::getAssetName, query.getAssetName());
        }
        if (StrUtil.isNotBlank(query.getCategory())) {
            wrapper.eq(AssetInfo::getCategory, query.getCategory());
        }
        if (query.getStatus() != null) {
            wrapper.eq(AssetInfo::getStatus, query.getStatus());
        }
        if (query.getDeptId() != null) {
            // 含子部门
            List<Long> deptIds = getChildDeptIds(query.getDeptId());
            wrapper.in(AssetInfo::getDeptId, deptIds);
        }
        if (StrUtil.isNotBlank(query.getUserName())) {
            // 先查询 sys_user 表获取匹配的用户ID，再按用户ID筛选资产
            List<SysUser> matchedUsers = userMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getRealName, query.getUserName())
                            .eq(SysUser::getIsDeleted, 0));
            if (!matchedUsers.isEmpty()) {
                List<Long> matchedUserIds = matchedUsers.stream().map(SysUser::getId).collect(Collectors.toList());
                wrapper.in(AssetInfo::getUserId, matchedUserIds);
            } else {
                wrapper.eq(AssetInfo::getUserId, -1); // 无匹配用户，返回空结果
            }
        }
        if (StrUtil.isNotBlank(query.getLocation())) {
            wrapper.like(AssetInfo::getLocation, query.getLocation());
        }
        if (StrUtil.isNotBlank(query.getBeginPurchaseDate())) {
            wrapper.ge(AssetInfo::getPurchaseDate, query.getBeginPurchaseDate());
        }
        if (StrUtil.isNotBlank(query.getEndPurchaseDate())) {
            wrapper.le(AssetInfo::getPurchaseDate, query.getEndPurchaseDate());
        }

        wrapper.orderByDesc(AssetInfo::getCreateTime);

        // 分页查询
        IPage<AssetInfo> page = assetMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        // 填充部门名称和使用人姓名
        PageResult<AssetInfo> result = PageResult.of(page);
        fillNames(result.getList());

        return result;
    }

    /**
     * 填充部门名称和使用人姓名
     */
    private void fillNames(List<AssetInfo> list) {
        // 批量查询部门
        Set<Long> deptIds = list.stream().map(AssetInfo::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<Long, String> deptMap;
        if (!deptIds.isEmpty()) {
            deptMap = deptMapper.selectBatchIds(deptIds).stream()
                    .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));
        } else {
            deptMap = Collections.emptyMap();
        }

        // 批量查询用户
        Set<Long> userIds = list.stream().map(AssetInfo::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<Long, String> userMap;
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
        } else {
            userMap = Collections.emptyMap();
        }

        list.forEach(a -> {
            a.setDeptName(deptMap.getOrDefault(a.getDeptId(), ""));
            if (a.getUserId() != null) {
                a.setUserName(userMap.getOrDefault(a.getUserId(), ""));
            }
        });
    }

    /**
     * 获取部门及其所有子部门ID
     */
    private List<Long> getChildDeptIds(Long deptId) {
        List<SysDept> all = deptMapper.selectList(new LambdaQueryWrapper<>());
        List<Long> ids = new ArrayList<>();
        ids.add(deptId);
        collectChildren(ids, all, deptId);
        return ids;
    }

    private void collectChildren(List<Long> ids, List<SysDept> all, Long parentId) {
        for (SysDept d : all) {
            if (Objects.equals(d.getParentId(), parentId) && d.getIsDeleted() == 0) {
                ids.add(d.getId());
                collectChildren(ids, all, d.getId());
            }
        }
    }

    // ==================== 新增 ====================

    /**
     * 新增资产
     */
    @Transactional(rollbackFor = Exception.class)
    public AssetInfo add(AssetAddDTO dto) {
        AssetInfo asset = new AssetInfo();
        asset.setAssetName(dto.getAssetName());
        asset.setCategory(dto.getCategory());
        asset.setSpecification(dto.getSpecification());
        asset.setSnNumber(dto.getSnNumber());
        asset.setProcurementNo(dto.getProcurementNo());
        asset.setOriginalValue(dto.getOriginalValue());
        asset.setPurchaseDate(dto.getPurchaseDate());
        asset.setUsefulLife(dto.getUsefulLife());
        // 净残值率默认5%
        asset.setResidualRate(dto.getResidualRate() != null ? dto.getResidualRate() : new BigDecimal("5.00"));
        asset.setLocation(dto.getLocation());
        asset.setDeptId(dto.getDeptId());
        asset.setUserId(dto.getUserId());
        asset.setStatus(dto.getStatus());
        asset.setImageUrl(dto.getImageUrl());
        asset.setRemark(dto.getRemark());
        // 计算报废日期
        asset.setScrapDate(dto.getPurchaseDate().plusYears(dto.getUsefulLife()));
        // 生成资产编码
        asset.setAssetCode(generateAssetCode(dto.getCategory()));

        assetMapper.insert(asset);
        clearListCache();
        return asset;
    }

    // ==================== 编辑 ====================

    /**
     * 编辑资产
     */
    @Transactional(rollbackFor = Exception.class)
    public void edit(AssetEditDTO dto) {
        AssetInfo asset = assetMapper.selectById(dto.getId());
        if (asset == null) {
            throw BusinessException.notFound("资产不存在");
        }

        // 盘点中不可编辑
        if (asset.getStatus() == 5) {
            throw new BusinessException(400, "该资产正在盘点中，请等待盘点结束后再操作");
        }

        // 乐观锁校验
        if (dto.getVersion() != null && !dto.getVersion().equals(asset.getVersion())) {
            throw new BusinessException(400, "数据已被他人修改，请刷新后重试");
        }

        asset.setAssetName(dto.getAssetName());
        asset.setCategory(dto.getCategory());
        asset.setSpecification(dto.getSpecification());
        asset.setSnNumber(dto.getSnNumber());
        asset.setProcurementNo(dto.getProcurementNo());
        asset.setOriginalValue(dto.getOriginalValue());
        asset.setPurchaseDate(dto.getPurchaseDate());
        asset.setUsefulLife(dto.getUsefulLife());
        if (dto.getResidualRate() != null) {
            asset.setResidualRate(dto.getResidualRate());
        }
        asset.setLocation(dto.getLocation());
        asset.setDeptId(dto.getDeptId());

        // 从闲置→在用，须校验使用人
        if (dto.getStatus() == 1 && asset.getStatus() == 0 && dto.getUserId() == null) {
            throw new BusinessException(400, "状态变更为【在用】时，请指定使用人");
        }
        // 闲置状态清空使用人
        if (dto.getStatus() == 0) {
            asset.setUserId(null);
        } else {
            asset.setUserId(dto.getUserId());
        }
        asset.setStatus(dto.getStatus());
        asset.setImageUrl(dto.getImageUrl());
        asset.setRemark(dto.getRemark());
        asset.setScrapDate(dto.getPurchaseDate().plusYears(dto.getUsefulLife()));
        asset.setVersion(asset.getVersion() + 1); // 乐观锁+1

        assetMapper.updateById(asset);
        clearListCache();
        RedisUtil.del("eams:asset:detail:" + dto.getId());
    }

    // ==================== 删除 ====================

    /**
     * 删除资产（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AssetInfo asset = assetMapper.selectById(id);
        if (asset == null) {
            throw BusinessException.notFound("资产不存在");
        }
        // 在用/借用状态不可删除
        if (asset.getStatus() == 1) {
            throw new BusinessException(400, "该资产当前为【在用】状态，请先归还后再删除");
        }
        if (asset.getStatus() == 2) {
            throw new BusinessException(400, "该资产当前为【借用】状态，请先归还后再删除");
        }
        if (asset.getStatus() == 5) {
            throw new BusinessException(400, "该资产正在盘点中，请等待盘点结束后再操作");
        }
        assetMapper.deleteById(id);
        clearListCache();
        RedisUtil.del("eams:asset:detail:" + id);
    }

    // ==================== 详情 ====================

    /**
     * 资产详情（含折旧信息）
     */
    public AssetVO getDetail(Long id) {
        AssetInfo asset = assetMapper.selectById(id);
        if (asset == null) {
            throw BusinessException.notFound("资产不存在");
        }
        fillNames(Collections.singletonList(asset));

        AssetVO vo = new AssetVO();
        // 复制基本信息
        vo.setId(asset.getId());
        vo.setAssetCode(asset.getAssetCode());
        vo.setAssetName(asset.getAssetName());
        vo.setCategory(asset.getCategory());
        vo.setSpecification(asset.getSpecification());
        vo.setSnNumber(asset.getSnNumber());
        vo.setProcurementNo(asset.getProcurementNo());
        vo.setOriginalValue(asset.getOriginalValue());
        vo.setPurchaseDate(asset.getPurchaseDate());
        vo.setUsefulLife(asset.getUsefulLife());
        vo.setResidualRate(asset.getResidualRate());
        vo.setScrapDate(asset.getScrapDate());
        vo.setLocation(asset.getLocation());
        vo.setDeptId(asset.getDeptId());
        vo.setDeptName(asset.getDeptName());
        vo.setUserId(asset.getUserId());
        vo.setUserName(asset.getUserName());
        vo.setStatus(asset.getStatus());
        vo.setImageUrl(asset.getImageUrl());
        vo.setRemark(asset.getRemark());
        vo.setVersion(asset.getVersion());
        vo.setCreateTime(asset.getCreateTime());

        // 计算折旧信息
        calculateDepreciation(vo);

        return vo;
    }

    /**
     * 计算折旧信息
     */
    private void calculateDepreciation(AssetVO vo) {
        if (vo.getOriginalValue() == null || vo.getPurchaseDate() == null) return;

        BigDecimal rate = vo.getResidualRate() != null ? vo.getResidualRate() : new BigDecimal("5");
        int life = vo.getUsefulLife() != null ? vo.getUsefulLife() : 1;

        // 月折旧额 = 原值 × (1 - 残值率%) ÷ (年限 × 12)
        BigDecimal monthly = vo.getOriginalValue()
                .multiply(BigDecimal.ONE.subtract(rate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)))
                .divide(new BigDecimal(life * 12), 2, RoundingMode.HALF_UP);
        vo.setMonthlyAmount(monthly);

        // 已提月数（从采购次月到当前月）
        LocalDate now = LocalDate.now();
        LocalDate startMonth = vo.getPurchaseDate().plusMonths(1).withDayOfMonth(1);
        int months = 0;
        if (now.isAfter(startMonth) || now.isEqual(startMonth)) {
            months = (int) ChronoUnit.MONTHS.between(startMonth.withDayOfMonth(1), now.withDayOfMonth(1));
        }
        vo.setDepreciatedMonths(months);

        // 累计折旧
        BigDecimal accumulated = monthly.multiply(new BigDecimal(months));
        BigDecimal residualValue = vo.getOriginalValue().multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        // 净值 ≤ 残值时停止
        BigDecimal netValue = vo.getOriginalValue().subtract(accumulated);
        if (netValue.compareTo(residualValue) <= 0) {
            netValue = residualValue;
            accumulated = vo.getOriginalValue().subtract(residualValue);
        }
        vo.setAccumulated(accumulated);
        vo.setNetValue(netValue);
    }

    // ==================== 折旧明细 ====================

    /**
     * 查询折旧明细
     */
    public List<AssetDepreciation> getDepreciationList(Long assetId) {
        return depreciationMapper.selectList(
                new LambdaQueryWrapper<AssetDepreciation>()
                        .eq(AssetDepreciation::getAssetId, assetId)
                        .orderByAsc(AssetDepreciation::getDepreciationMonth));
    }

    // ==================== 资产编码生成 ====================

    /**
     * 生成资产编码: AS-{类别码}-{YYMM}-{4位流水}
     */
    public String generateAssetCode(String category) {
        String categoryShort = CATEGORY_SHORT.getOrDefault(category, "OT");
        String yyMM = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM"));

        // 分布式锁保护流水号
        String lockKey = "eams:lock:asset:code:" + categoryShort + ":" + yyMM;
        String lockValue = RedisUtil.tryLock(lockKey, 3, 30);
        if (lockValue == null) {
            throw new BusinessException(400, "编码生成繁忙，请稍后重试");
        }
        try {
            String maxCode = assetMapper.selectMaxCodeByPrefix(categoryShort, yyMM);
            int seq = 1;
            if (maxCode != null) {
                seq = Integer.parseInt(maxCode.substring(maxCode.length() - 4)) + 1;
            }
            return String.format("AS-%s-%s-%04d", categoryShort, yyMM, seq);
        } finally {
            RedisUtil.unlock(lockKey, lockValue);
        }
    }

    // ==================== Excel 导入 ====================

    /**
     * 批量导入资产
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importAssets(List<Map<String, Object>> rows) {
        int success = 0;
        int fail = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            try {
                AssetInfo asset = parseRowToAsset(row, i + 1);
                asset.setAssetCode(generateAssetCode(asset.getCategory()));
                asset.setScrapDate(asset.getPurchaseDate().plusYears(asset.getUsefulLife()));
                assetMapper.insert(asset);
                success++;
            } catch (Exception e) {
                fail++;
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("reason", e.getMessage());
                // 包含原始行数据（用于生成包含全量字段的错误报告）
                err.put("data", row);
                errors.add(err);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("fail", fail);
        result.put("errors", errors);
        if (success > 0) clearListCache();
        return result;
    }

    /**
     * 解析Excel行为资产对象
     */
    private AssetInfo parseRowToAsset(Map<String, Object> row, int rowNum) {
        String assetName = getStr(row, "资产名称");
        if (StrUtil.isBlank(assetName) || assetName.length() < 2 || assetName.length() > 50) {
            throw new BusinessException(400, "第" + rowNum + "行：资产名称格式错误（2-50字符）");
        }

        String category = getStr(row, "资产分类");
        if (StrUtil.isBlank(category)) {
            throw new BusinessException(400, "第" + rowNum + "行：资产分类不能为空");
        }
        // 支持按字典名称导入：将 dict_label 转为 dict_value
        SysDictItem dictItem = dictItemMapper.selectOne(
                new LambdaQueryWrapper<SysDictItem>()
                        .eq(SysDictItem::getDictCode, "asset_category")
                        .eq(SysDictItem::getDictValue, category)
                        .eq(SysDictItem::getStatus, 1));
        if (dictItem == null) {
            // 按 label 查
            dictItem = dictItemMapper.selectOne(
                    new LambdaQueryWrapper<SysDictItem>()
                            .eq(SysDictItem::getDictCode, "asset_category")
                            .eq(SysDictItem::getDictLabel, category)
                            .eq(SysDictItem::getStatus, 1));
            if (dictItem == null) {
                throw new BusinessException(400, "第" + rowNum + "行：资产分类【" + category + "】不存在，请使用正确的分类名称");
            }
            category = dictItem.getDictValue();
        }

        String valueStr = getStr(row, "原值(元)");
        BigDecimal value;
        try {
            value = new BigDecimal(valueStr);
            if (value.compareTo(BigDecimal.ZERO) <= 0) throw new Exception();
        } catch (Exception e) {
            throw new BusinessException(400, "第" + rowNum + "行：原值格式错误");
        }

        LocalDate purchaseDate;
        try {
            purchaseDate = LocalDate.parse(getStr(row, "采购日期"), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            throw new BusinessException(400, "第" + rowNum + "行：采购日期格式错误（yyyy-MM-dd）");
        }

        int usefulLife;
        try {
            usefulLife = Integer.parseInt(getStr(row, "使用年限(年)"));
            if (usefulLife < 1 || usefulLife > 50) throw new Exception();
        } catch (Exception e) {
            throw new BusinessException(400, "第" + rowNum + "行：使用年限格式错误（1-50整数）");
        }

        String location = getStr(row, "存放地点");
        if (StrUtil.isBlank(location) || location.length() < 2 || location.length() > 50) {
            throw new BusinessException(400, "第" + rowNum + "行：存放地点格式错误（2-50字符）");
        }

        // 查找部门
        String deptName = getStr(row, "所属部门");
        SysDept dept = deptMapper.selectOne(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeptName, deptName)
                .eq(SysDept::getIsDeleted, 0));
        if (dept == null) {
            throw new BusinessException(400, "第" + rowNum + "行：部门【" + deptName + "】不存在");
        }

        AssetInfo asset = new AssetInfo();
        asset.setAssetName(assetName);
        asset.setCategory(category);
        asset.setSpecification(getStr(row, "规格型号"));
        asset.setSnNumber(getStr(row, "SN序列号"));
        asset.setProcurementNo(getStr(row, "采购编号"));
        asset.setOriginalValue(value);
        asset.setPurchaseDate(purchaseDate);
        asset.setUsefulLife(usefulLife);
        asset.setResidualRate(new BigDecimal("5.00"));
        asset.setLocation(location);
        asset.setDeptId(dept.getId());
        asset.setStatus(0);
        asset.setRemark(getStr(row, "备注"));
        return asset;
    }

    private String getStr(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v != null ? v.toString().trim() : "";
    }

    // ==================== Excel 导出 ====================

    /**
     * 按筛选条件导出
     */
    public List<AssetInfo> exportData(AssetQueryDTO query) {
        LambdaQueryWrapper<AssetInfo> wrapper = new LambdaQueryWrapper<AssetInfo>()
                .eq(AssetInfo::getIsDeleted, 0);

        if (StrUtil.isNotBlank(query.getAssetCode())) {
            wrapper.eq(AssetInfo::getAssetCode, query.getAssetCode());
        }
        if (StrUtil.isNotBlank(query.getAssetName())) {
            wrapper.like(AssetInfo::getAssetName, query.getAssetName());
        }
        if (StrUtil.isNotBlank(query.getCategory())) {
            wrapper.eq(AssetInfo::getCategory, query.getCategory());
        }
        if (query.getStatus() != null) {
            wrapper.eq(AssetInfo::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(AssetInfo::getCreateTime);

        List<AssetInfo> list = assetMapper.selectList(wrapper);
        if (list.size() > 10000) {
            throw new BusinessException(400, "导出数据超过10000条，请缩小筛选范围后再试");
        }
        fillNames(list);
        return list;
    }

    // ==================== 缓存清除 ====================

    private void clearListCache() {
        RedisUtil.deleteByPattern("eams:asset:list:*");
    }
}
