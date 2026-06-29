package com.example.eams.repair.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eams.asset.entity.AssetInfo;
import com.example.eams.asset.mapper.AssetInfoMapper;
import com.example.eams.common.exception.BusinessException;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.util.RedisUtil;
import com.example.eams.repair.dto.*;
import com.example.eams.repair.entity.RepairOrder;
import com.example.eams.repair.entity.RepairRecord;
import com.example.eams.repair.mapper.RepairOrderMapper;
import com.example.eams.repair.mapper.RepairRecordMapper;
import com.example.eams.security.filter.SecurityContextHolder;
import com.example.eams.system.entity.SysDept;
import com.example.eams.system.entity.SysUser;
import com.example.eams.system.mapper.SysDeptMapper;
import com.example.eams.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 维保报修服务（PRD 6.8）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepairService {

    private final RepairOrderMapper repairOrderMapper;
    private final RepairRecordMapper repairRecordMapper;
    private final AssetInfoMapper assetMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    // ==================== 报修登记（PRD 6.8.1） ====================

    @Transactional(rollbackFor = Exception.class)
    public RepairOrder apply(RepairApplyDTO dto) {
        Long currentUserId = SecurityContextHolder.getCurrentUserId();
        String lockKey = "eams:lock:repair:" + dto.getAssetId();
        String lockValue = RedisUtil.tryLock(lockKey, 3, 30);
        if (lockValue == null) throw new BusinessException(400, "操作繁忙，请稍后重试");

        try {
            AssetInfo asset = assetMapper.selectById(dto.getAssetId());
            if (asset == null) throw BusinessException.notFound("资产不存在");
            if (asset.getStatus() == 3) throw new BusinessException(400, "该资产已处于维修状态，请勿重复报修");
            if (asset.getStatus() == 4) throw new BusinessException(400, "该资产已报废，不可报修");
            if (asset.getStatus() == 5) throw new BusinessException(400, "该资产正在盘点中，不可报修");

            // 同资产无未完成报修
            if (repairOrderMapper.countPendingByAssetId(dto.getAssetId()) > 0) {
                throw new BusinessException(400, "该资产已处于维修状态，请勿重复报修");
            }

            RepairOrder entity = new RepairOrder();
            entity.setRepairNo(generateRepairNo());
            entity.setAssetId(dto.getAssetId());
            entity.setApplicantId(currentUserId);
            entity.setFaultType(dto.getFaultType());
            entity.setUrgency(dto.getUrgency() != null ? dto.getUrgency() : 0);
            entity.setFaultDesc(dto.getFaultDesc());
            entity.setFaultImages(dto.getFaultImages());
            entity.setContactPhone(dto.getContactPhone());
            entity.setRepairStatus(0); // 待维修
            entity.setPreRepairStatus(asset.getStatus()); // 快照
            entity.setRemark(dto.getRemark());
            repairOrderMapper.insert(entity);

            // 资产→维修
            asset.setStatus(3);
            asset.setVersion(asset.getVersion() != null ? asset.getVersion() + 1 : 1);
            assetMapper.updateById(asset);
            RedisUtil.deleteByPattern("eams:asset:list:*");

            log.info("报修已提交，编号={}", entity.getRepairNo());
            return entity;
        } finally {
            RedisUtil.unlock(lockKey, lockValue);
        }
    }

    /** 我的报修 */
    public PageResult<RepairOrder> listMyApply(RepairQueryDTO q) {
        LambdaQueryWrapper<RepairOrder> w = new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getIsDeleted, 0)
                .eq(RepairOrder::getApplicantId, SecurityContextHolder.getCurrentUserId());
        applyFilters(w, q); w.orderByDesc(RepairOrder::getCreateTime);
        return queryAndFill(w, q.getPageNum(), q.getPageSize());
    }

    // ==================== 维修处理（PRD 6.8.2） ====================

    /** 处理列表（仅管理员） */
    public PageResult<RepairOrder> listHandle(RepairQueryDTO q) {
        LambdaQueryWrapper<RepairOrder> w = new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getIsDeleted, 0);
        applyFilters(w, q); w.orderByDesc(RepairOrder::getCreateTime);
        PageResult<RepairOrder> r = queryAndFill(w, q.getPageNum(), q.getPageSize());
        // 填充维修记录
        fillRepairRecords(r.getList());
        return r;
    }

    /** 接单 */
    @Transactional(rollbackFor = Exception.class)
    public void accept(RepairHandleDTO dto) {
        RepairOrder order = repairOrderMapper.selectById(dto.getRepairOrderId());
        if (order == null) throw BusinessException.notFound("报修单不存在");
        if (order.getRepairStatus() != 0) throw new BusinessException(400, "该报修单已处理，无法接单");

        // 写入维修记录
        RepairRecord record = fillRecord(new RepairRecord(), dto);
        record.setRepairOrderId(order.getId());
        repairRecordMapper.insert(record);

        order.setRepairStatus(1); // 维修中
        repairOrderMapper.updateById(order);
        log.info("接单成功，编号={}", order.getRepairNo());
    }

    /** 已修复 */
    @Transactional(rollbackFor = Exception.class)
    public void complete(RepairHandleDTO dto) {
        RepairOrder order = repairOrderMapper.selectById(dto.getRepairOrderId());
        if (order == null) throw BusinessException.notFound("报修单不存在");
        if (order.getRepairStatus() != 1) throw new BusinessException(400, "该报修单状态不正确，无法标记修复");

        if (dto.getFinishDate() == null) throw new BusinessException(400, "请选择修复日期");

        // 更新维修记录
        RepairRecord record = getOrCreateRecord(dto.getRepairOrderId());
        fillRecord(record, dto);
        record.setFinishDate(dto.getFinishDate());
        repairRecordMapper.updateById(record);

        // 恢复资产状态
        AssetInfo asset = assetMapper.selectById(order.getAssetId());
        if (asset != null) {
            asset.setStatus(order.getPreRepairStatus() != null ? order.getPreRepairStatus() : 0);
            asset.setVersion(asset.getVersion() != null ? asset.getVersion() + 1 : 1);
            assetMapper.updateById(asset);
            RedisUtil.deleteByPattern("eams:asset:list:*");
        }

        order.setRepairStatus(2);
        repairOrderMapper.updateById(order);
        log.info("维修完成，编号={}", order.getRepairNo());
    }

    /** 无法修复 */
    @Transactional(rollbackFor = Exception.class)
    public void unfixable(RepairHandleDTO dto) {
        RepairOrder order = repairOrderMapper.selectById(dto.getRepairOrderId());
        if (order == null) throw BusinessException.notFound("报修单不存在");
        if (order.getRepairStatus() != 1) throw new BusinessException(400, "该报修单状态不正确，无法标记");

        RepairRecord record = getOrCreateRecord(dto.getRepairOrderId());
        fillRecord(record, dto);
        repairRecordMapper.updateById(record);

        order.setRepairStatus(3); // 无法修复，资产保持维修状态
        repairOrderMapper.updateById(order);
        log.info("标记无法修复，编号={}", order.getRepairNo());
    }

    // ==================== 维保记录（PRD 6.8.3） ====================

    public PageResult<RepairOrder> listRecords(RepairQueryDTO q) {
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        Long userId = SecurityContextHolder.getCurrentUserId();

        LambdaQueryWrapper<RepairOrder> w = new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getIsDeleted, 0);

        if (!roles.contains("ROLE_SUPER_ADMIN") && !roles.contains("ROLE_ASSET_ADMIN")) {
            if (roles.contains("ROLE_DEPT_ADMIN")) {
                SysUser u = userMapper.selectById(userId);
                if (u != null && u.getDeptId() != null) {
                    List<Long> deptAssetIds = assetMapper.selectList(
                            new LambdaQueryWrapper<AssetInfo>().eq(AssetInfo::getDeptId, u.getDeptId())
                                    .eq(AssetInfo::getIsDeleted, 0))
                            .stream().map(AssetInfo::getId).collect(Collectors.toList());
                    if (!deptAssetIds.isEmpty()) w.in(RepairOrder::getAssetId, deptAssetIds);
                    else w.eq(RepairOrder::getAssetId, -1L);
                }
            } else {
                w.eq(RepairOrder::getApplicantId, userId);
            }
        }

        applyFilters(w, q); w.orderByDesc(RepairOrder::getCreateTime);
        PageResult<RepairOrder> r = queryAndFill(w, q.getPageNum(), q.getPageSize());
        fillRepairRecords(r.getList());
        return r;
    }

    public Map<String, Object> getDetail(Long id) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) throw BusinessException.notFound("报修单不存在");
        fillTransient(Collections.singletonList(order));
        fillRepairRecords(Collections.singletonList(order));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        RepairRecord record = repairRecordMapper.selectOne(
                new LambdaQueryWrapper<RepairRecord>().eq(RepairRecord::getRepairOrderId, id));
        result.put("record", record);
        return result;
    }

    public List<RepairOrder> exportData(RepairQueryDTO q) {
        LambdaQueryWrapper<RepairOrder> w = new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getIsDeleted, 0);
        applyFilters(w, q); w.orderByDesc(RepairOrder::getCreateTime);
        List<RepairOrder> list = repairOrderMapper.selectList(w);
        if (list.size() > 10000) throw new BusinessException(400, "导出数据超过10000条，请缩小筛选范围后再试");
        fillTransient(list); fillRepairRecords(list);
        return list;
    }

    // ==================== 辅助 ====================

    private String generateRepairNo() {
        String yyMMdd = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String lk = "eams:lock:repair:no:" + yyMMdd;
        String lv = RedisUtil.tryLock(lk, 3, 30);
        if (lv == null) throw new BusinessException(400, "操作繁忙，请稍后重试");
        try {
            String max = repairOrderMapper.selectMaxRepairNoByPrefix(yyMMdd);
            int seq = 1;
            if (max != null && max.length() >= 16) seq = Integer.parseInt(max.substring(max.length() - 4)) + 1;
            return String.format("WX-%s-%04d", yyMMdd, seq);
        } finally { RedisUtil.unlock(lk, lv); }
    }

    private void applyFilters(LambdaQueryWrapper<RepairOrder> w, RepairQueryDTO q) {
        if (StrUtil.isNotBlank(q.getRepairNo())) w.like(RepairOrder::getRepairNo, q.getRepairNo());
        if (StrUtil.isNotBlank(q.getFaultType())) w.eq(RepairOrder::getFaultType, q.getFaultType());
        if (q.getRepairStatus() != null) w.eq(RepairOrder::getRepairStatus, q.getRepairStatus());
        if (StrUtil.isNotBlank(q.getAssetName())) {
            List<Long> ids = assetMapper.selectList(
                    new LambdaQueryWrapper<AssetInfo>().like(AssetInfo::getAssetName, q.getAssetName())
                            .eq(AssetInfo::getIsDeleted, 0))
                    .stream().map(AssetInfo::getId).collect(Collectors.toList());
            if (!ids.isEmpty()) w.in(RepairOrder::getAssetId, ids);
            else w.eq(RepairOrder::getAssetId, -1L);
        }
        if (StrUtil.isNotBlank(q.getBeginDate())) w.ge(RepairOrder::getCreateTime, q.getBeginDate() + " 00:00:00");
        if (StrUtil.isNotBlank(q.getEndDate())) w.le(RepairOrder::getCreateTime, q.getEndDate() + " 23:59:59");
    }

    private PageResult<RepairOrder> queryAndFill(LambdaQueryWrapper<RepairOrder> w, int pn, int ps) {
        IPage<RepairOrder> page = repairOrderMapper.selectPage(new Page<>(pn, ps), w);
        PageResult<RepairOrder> r = PageResult.of(page);
        fillTransient(r.getList());
        return r;
    }

    private void fillTransient(List<RepairOrder> list) {
        if (list.isEmpty()) return;
        Set<Long> assetIds = list.stream().map(RepairOrder::getAssetId).filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<Long, AssetInfo> am;
        if (!assetIds.isEmpty()) am = assetMapper.selectBatchIds(assetIds).stream().collect(Collectors.toMap(AssetInfo::getId, a -> a, (a, b) -> a));
        else am = Collections.emptyMap();

        Set<Long> userIds = list.stream().map(RepairOrder::getApplicantId).filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<Long, SysUser> um;
        if (!userIds.isEmpty()) um = userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        else um = Collections.emptyMap();

        list.forEach(r -> {
            AssetInfo a = am.get(r.getAssetId());
            if (a != null) { r.setAssetCode(a.getAssetCode()); r.setAssetName(a.getAssetName()); r.setCategory(a.getCategory()); r.setSpecification(a.getSpecification()); r.setLocation(a.getLocation()); r.setImageUrl(a.getImageUrl()); }
            SysUser u = um.get(r.getApplicantId());
            if (u != null) r.setApplicantName(u.getRealName());
            r.setStatusLabel(getStatusLabel(r.getRepairStatus()));
            r.setUrgencyLabel(r.getUrgency() != null && r.getUrgency() == 1 ? "紧急" : "普通");
        });
    }

    private void fillRepairRecords(List<RepairOrder> list) {
        if (list.isEmpty()) return;
        Set<Long> ids = list.stream().map(RepairOrder::getId).collect(Collectors.toSet());
        List<RepairRecord> records = repairRecordMapper.selectList(
                new LambdaQueryWrapper<RepairRecord>().in(RepairRecord::getRepairOrderId, ids));
        Map<Long, RepairRecord> rm = records.stream().collect(Collectors.toMap(RepairRecord::getRepairOrderId, r -> r, (a, b) -> a));
        list.forEach(o -> {
            RepairRecord r = rm.get(o.getId());
            if (r != null) {
                o.setRepairMethod(r.getRepairMethod()); o.setRepairPerson(r.getRepairPerson());
                o.setRepairFee(r.getRepairFee()); o.setFaultReason(r.getFaultReason());
                o.setSolution(r.getSolution()); o.setRepairFiles(r.getRepairFiles());
                if (r.getStartDate() != null) o.setStartDate(r.getStartDate().atStartOfDay());
                if (r.getFinishDate() != null) o.setFinishDate(r.getFinishDate().atStartOfDay());
            }
        });
    }

    private RepairRecord fillRecord(RepairRecord r, RepairHandleDTO d) {
        r.setRepairMethod(d.getRepairMethod()); r.setRepairPerson(d.getRepairPerson());
        r.setRepairFee(d.getRepairFee()); r.setStartDate(d.getStartDate());
        r.setFaultReason(d.getFaultReason()); r.setSolution(d.getSolution());
        r.setRepairFiles(d.getRepairFiles()); r.setRemark(d.getRemark());
        return r;
    }

    private RepairRecord getOrCreateRecord(Long orderId) {
        RepairRecord r = repairRecordMapper.selectOne(
                new LambdaQueryWrapper<RepairRecord>().eq(RepairRecord::getRepairOrderId, orderId));
        if (r == null) { r = new RepairRecord(); r.setRepairOrderId(orderId); repairRecordMapper.insert(r); }
        return r;
    }

    private String getStatusLabel(Integer s) {
        if (s == null) return "";
        switch (s) { case 0: return "待维修"; case 1: return "维修中"; case 2: return "已修复"; case 3: return "无法修复"; default: return String.valueOf(s); }
    }
}
