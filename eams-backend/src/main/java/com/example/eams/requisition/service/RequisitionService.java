package com.example.eams.requisition.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eams.asset.entity.AssetInfo;
import com.example.eams.asset.mapper.AssetInfoMapper;
import com.example.eams.common.exception.BusinessException;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.util.RedisUtil;
import com.example.eams.requisition.dto.*;
import com.example.eams.requisition.entity.RequisitionApprovalLog;
import com.example.eams.requisition.entity.RequisitionOrder;
import com.example.eams.requisition.mapper.RequisitionApprovalLogMapper;
import com.example.eams.requisition.mapper.RequisitionOrderMapper;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 领用归还管理服务（PRD 6.3）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequisitionService {

    private final RequisitionOrderMapper requisitionMapper;
    private final RequisitionApprovalLogMapper approvalLogMapper;
    private final AssetInfoMapper assetMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    // ==================== 领用申请（PRD 6.3.1） ====================

    /**
     * 提交领用申请
     */
    @Transactional(rollbackFor = Exception.class)
    public RequisitionOrder apply(RequisitionApplyDTO dto) {
        Long currentUserId = SecurityContextHolder.getCurrentUserId();
        String currentUsername = SecurityContextHolder.getCurrentUsername();

        // 校验归还日期不能早于今天
        if (dto.getExpectReturnDate().isBefore(LocalDate.now())) {
            throw new BusinessException(400, "归还日期不能早于当前日期");
        }

        // 分布式锁：防止并发重复提交
        String lockKey = "eams:lock:requisition:" + dto.getAssetId() + ":" + currentUserId;
        String lockValue = RedisUtil.tryLock(lockKey, 3, 30);
        if (lockValue == null) {
            throw new BusinessException(400, "操作繁忙，请稍后重试");
        }

        try {
            // 校验资产存在且为闲置状态
            AssetInfo asset = assetMapper.selectById(dto.getAssetId());
            if (asset == null) {
                throw BusinessException.notFound("资产不存在");
            }
            if (asset.getStatus() != 0) {
                throw new BusinessException(400, "该资产当前不可领用");
            }

            // 检查同一资产+同一申请人是否有待审批记录
            int pendingCount = requisitionMapper.countPendingByAssetAndApplicant(
                    dto.getAssetId(), currentUserId);
            if (pendingCount > 0) {
                throw new BusinessException(400,
                        "您已对该资产提交过待审批的领用申请，请勿重复提交");
            }

            // 获取申请人部门ID
            SysUser applicant = userMapper.selectById(currentUserId);

            RequisitionOrder entity = new RequisitionOrder();
            entity.setApplyNo(generateApplyNo());
            entity.setAssetId(dto.getAssetId());
            entity.setApplicantId(currentUserId);
            entity.setApplicantDeptId(applicant != null ? applicant.getDeptId() : null);
            entity.setPurpose(dto.getPurpose());
            entity.setExpectDuration(dto.getExpectDuration());
            entity.setExpectReturnDate(dto.getExpectReturnDate());
            entity.setStatus(0); // 待部门审批
            entity.setRemark(dto.getRemark());

            requisitionMapper.insert(entity);
            log.info("领用申请已提交，申请编号={}，资产ID={}，申请人={}",
                    entity.getApplyNo(), dto.getAssetId(), currentUsername);

            return entity;

        } finally {
            RedisUtil.unlock(lockKey, lockValue);
        }
    }

    /**
     * 我的申请列表
     */
    public PageResult<RequisitionOrder> listMyApply(RequisitionQueryDTO query) {
        Long currentUserId = SecurityContextHolder.getCurrentUserId();

        LambdaQueryWrapper<RequisitionOrder> wrapper = new LambdaQueryWrapper<RequisitionOrder>()
                .eq(RequisitionOrder::getIsDeleted, 0)
                .eq(RequisitionOrder::getApplicantId, currentUserId);

        applyQueryFilters(wrapper, query);
        wrapper.orderByDesc(RequisitionOrder::getCreateTime);

        IPage<RequisitionOrder> page = requisitionMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        PageResult<RequisitionOrder> result = PageResult.of(page);
        fillTransientFields(result.getList());
        return result;
    }

    // ==================== 审批管理（PRD 6.3.2） ====================

    /**
     * 审批列表（按角色过滤数据范围）
     */
    public PageResult<RequisitionOrder> listApproval(ApprovalQueryDTO query) {
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        Long currentUserId = SecurityContextHolder.getCurrentUserId();

        LambdaQueryWrapper<RequisitionOrder> wrapper = new LambdaQueryWrapper<RequisitionOrder>()
                .eq(RequisitionOrder::getIsDeleted, 0);

        // 部门管理员只看本部门（通过 applicant_dept_id 过滤）
        if (roles.contains("ROLE_DEPT_ADMIN") && !roles.contains("ROLE_SUPER_ADMIN")
                && !roles.contains("ROLE_ASSET_ADMIN")) {
            SysUser currentUser = userMapper.selectById(currentUserId);
            if (currentUser != null && currentUser.getDeptId() != null) {
                // 获取本部门及子部门ID
                List<Long> deptIds = getChildDeptIds(currentUser.getDeptId());
                wrapper.in(RequisitionOrder::getApplicantDeptId, deptIds);
            }
        }

        if (StrUtil.isNotBlank(query.getApplyNo())) {
            wrapper.eq(RequisitionOrder::getApplyNo, query.getApplyNo());
        }
        if (StrUtil.isNotBlank(query.getAssetName())) {
            // 先按资产名查 asset_info 获取 ID 列表，再 in 查询
            List<AssetInfo> matchedAssets = assetMapper.selectList(
                    new LambdaQueryWrapper<AssetInfo>()
                            .like(AssetInfo::getAssetName, query.getAssetName())
                            .eq(AssetInfo::getIsDeleted, 0));
            if (!matchedAssets.isEmpty()) {
                List<Long> assetIds = matchedAssets.stream()
                        .map(AssetInfo::getId).collect(Collectors.toList());
                wrapper.in(RequisitionOrder::getAssetId, assetIds);
            } else {
                wrapper.eq(RequisitionOrder::getAssetId, -1L);
            }
        }
        if (query.getStatus() != null) {
            wrapper.eq(RequisitionOrder::getStatus, query.getStatus());
        }
        if (StrUtil.isNotBlank(query.getBeginDate())) {
            wrapper.ge(RequisitionOrder::getCreateTime, query.getBeginDate() + " 00:00:00");
        }
        if (StrUtil.isNotBlank(query.getEndDate())) {
            wrapper.le(RequisitionOrder::getCreateTime, query.getEndDate() + " 23:59:59");
        }

        wrapper.orderByDesc(RequisitionOrder::getCreateTime);

        IPage<RequisitionOrder> page = requisitionMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        PageResult<RequisitionOrder> result = PageResult.of(page);
        fillTransientFields(result.getList());
        return result;
    }

    /**
     * 审批通过
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(ApprovalDTO dto) {
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        Long currentUserId = SecurityContextHolder.getCurrentUserId();

        RequisitionOrder entity = requisitionMapper.selectById(dto.getRequisitionId());
        if (entity == null) {
            throw BusinessException.notFound("领用申请不存在");
        }

        // 判断审批级别
        boolean isDeptAdminOnly = roles.contains("ROLE_DEPT_ADMIN")
                && !roles.contains("ROLE_SUPER_ADMIN")
                && !roles.contains("ROLE_ASSET_ADMIN");
        boolean isAdmin = roles.contains("ROLE_ASSET_ADMIN")
                || roles.contains("ROLE_SUPER_ADMIN");

        int approvalLevel;
        int expectedStatus;

        if (isAdmin) {
            // 管理员可处理两级：status=0 视为部门审批，status=1 视为资产管理员终审
            if (entity.getStatus() == 0) {
                approvalLevel = 1;
                expectedStatus = 0;
            } else if (entity.getStatus() == 1) {
                approvalLevel = 2;
                expectedStatus = 1;
            } else {
                throw new BusinessException(400, "该申请已被处理，无法重复审批");
            }
        } else if (isDeptAdminOnly && entity.getStatus() == 0) {
            approvalLevel = 1;
            expectedStatus = 0;
        } else if (isDeptAdminOnly && entity.getStatus() != 0) {
            throw new BusinessException(400, "该申请已被处理，无法重复审批");
        } else {
            throw new BusinessException(403, "您没有权限审批该申请");
        }

        // 更新状态
        if (approvalLevel == 1) {
            // 部门审批通过 → status 0→1
            entity.setStatus(1);
            requisitionMapper.updateById(entity);
        } else {
            // 资产管理员终审：校验资产仍为闲置
            AssetInfo asset = assetMapper.selectById(entity.getAssetId());
            if (asset == null) {
                throw BusinessException.notFound("资产不存在");
            }
            if (asset.getStatus() != 0) {
                throw new BusinessException(400, "该资产已被其他申请占用，无法重复审批");
            }

            // 资产 status → 在用，绑定使用人
            asset.setStatus(1);
            asset.setUserId(entity.getApplicantId());
            asset.setVersion(asset.getVersion() != null ? asset.getVersion() + 1 : 1);
            assetMapper.updateById(asset);

            // 领用单 status → 已通过
            entity.setStatus(2);
            requisitionMapper.updateById(entity);

            // 清除资产缓存
            RedisUtil.deleteByPattern("eams:asset:list:*");
        }

        // 写入审批日志
        RequisitionApprovalLog logEntry = new RequisitionApprovalLog();
        logEntry.setRequisitionId(entity.getId());
        logEntry.setApproverId(currentUserId);
        logEntry.setApprovalLevel(approvalLevel);
        logEntry.setApprovalResult(1); // 通过
        approvalLogMapper.insert(logEntry);

        log.info("审批通过，申请编号={}，审批级别={}，审批人={}",
                entity.getApplyNo(), approvalLevel, SecurityContextHolder.getCurrentUsername());
    }

    /**
     * 审批驳回
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(ApprovalDTO dto) {
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        Long currentUserId = SecurityContextHolder.getCurrentUserId();

        // 驳回原因必填
        if (StrUtil.isBlank(dto.getRejectReason())) {
            throw new BusinessException(400, "驳回原因为10-200个字符");
        }
        if (dto.getRejectReason().length() < 10 || dto.getRejectReason().length() > 200) {
            throw new BusinessException(400, "驳回原因为10-200个字符");
        }

        RequisitionOrder entity = requisitionMapper.selectById(dto.getRequisitionId());
        if (entity == null) {
            throw BusinessException.notFound("领用申请不存在");
        }

        boolean isDeptAdminOnly = roles.contains("ROLE_DEPT_ADMIN")
                && !roles.contains("ROLE_SUPER_ADMIN")
                && !roles.contains("ROLE_ASSET_ADMIN");
        boolean isAdmin = roles.contains("ROLE_ASSET_ADMIN")
                || roles.contains("ROLE_SUPER_ADMIN");

        int approvalLevel;
        if (isAdmin) {
            if (entity.getStatus() == 0) {
                approvalLevel = 1;
            } else if (entity.getStatus() == 1) {
                approvalLevel = 2;
            } else {
                throw new BusinessException(400, "该申请已被处理，无法重复审批");
            }
        } else if (isDeptAdminOnly && entity.getStatus() == 0) {
            approvalLevel = 1;
        } else if (isDeptAdminOnly && entity.getStatus() != 0) {
            throw new BusinessException(400, "该申请已被处理，无法重复审批");
        } else {
            throw new BusinessException(403, "您没有权限审批该申请");
        }

        // 更新状态为已驳回
        entity.setStatus(3);
        requisitionMapper.updateById(entity);

        // 写入审批日志
        RequisitionApprovalLog logEntry = new RequisitionApprovalLog();
        logEntry.setRequisitionId(entity.getId());
        logEntry.setApproverId(currentUserId);
        logEntry.setApprovalLevel(approvalLevel);
        logEntry.setApprovalResult(0); // 驳回
        logEntry.setRejectReason(dto.getRejectReason());
        approvalLogMapper.insert(logEntry);

        log.info("审批驳回，申请编号={}，驳回原因={}", entity.getApplyNo(), dto.getRejectReason());
    }

    // ==================== 归还登记（PRD 6.3.3） ====================

    /**
     * 归还列表（在用资产 + 已通过的领用单）
     */
    public PageResult<RequisitionOrder> listReturn(ReturnQueryDTO query) {
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        Long currentUserId = SecurityContextHolder.getCurrentUserId();

        LambdaQueryWrapper<RequisitionOrder> wrapper = new LambdaQueryWrapper<RequisitionOrder>()
                .eq(RequisitionOrder::getIsDeleted, 0)
                .eq(RequisitionOrder::getStatus, 2); // 已通过

        // 普通员工只能看自己的
        if (!roles.contains("ROLE_SUPER_ADMIN") && !roles.contains("ROLE_ASSET_ADMIN")) {
            wrapper.eq(RequisitionOrder::getApplicantId, currentUserId);
        }

        if (StrUtil.isNotBlank(query.getAssetCode())) {
            List<AssetInfo> matched = assetMapper.selectList(
                    new LambdaQueryWrapper<AssetInfo>()
                            .eq(AssetInfo::getAssetCode, query.getAssetCode())
                            .eq(AssetInfo::getIsDeleted, 0));
            if (!matched.isEmpty()) {
                wrapper.in(RequisitionOrder::getAssetId,
                        matched.stream().map(AssetInfo::getId).collect(Collectors.toList()));
            } else {
                wrapper.eq(RequisitionOrder::getAssetId, -1L);
            }
        }
        if (StrUtil.isNotBlank(query.getAssetName())) {
            List<AssetInfo> matched = assetMapper.selectList(
                    new LambdaQueryWrapper<AssetInfo>()
                            .like(AssetInfo::getAssetName, query.getAssetName())
                            .eq(AssetInfo::getIsDeleted, 0));
            if (!matched.isEmpty()) {
                wrapper.in(RequisitionOrder::getAssetId,
                        matched.stream().map(AssetInfo::getId).collect(Collectors.toList()));
            } else {
                wrapper.eq(RequisitionOrder::getAssetId, -1L);
            }
        }
        if (StrUtil.isNotBlank(query.getBeginDate())) {
            wrapper.ge(RequisitionOrder::getCreateTime, query.getBeginDate() + " 00:00:00");
        }
        if (StrUtil.isNotBlank(query.getEndDate())) {
            wrapper.le(RequisitionOrder::getCreateTime, query.getEndDate() + " 23:59:59");
        }

        wrapper.orderByDesc(RequisitionOrder::getCreateTime);

        IPage<RequisitionOrder> page = requisitionMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        PageResult<RequisitionOrder> result = PageResult.of(page);
        fillTransientFields(result.getList());
        return result;
    }

    /**
     * 确认归还
     */
    @Transactional(rollbackFor = Exception.class)
    public void returnAsset(ReturnDTO dto) {
        RequisitionOrder entity = requisitionMapper.selectById(dto.getRequisitionId());
        if (entity == null) {
            throw BusinessException.notFound("领用申请不存在");
        }
        if (entity.getStatus() != 2) {
            throw new BusinessException(400, "该申请未通过审批，不可归还");
        }

        // 校验归还日期
        if (dto.getReturnDate().isAfter(LocalDate.now())) {
            throw new BusinessException(400, "归还日期不能晚于当前日期");
        }

        // 校验损坏说明
        if (dto.getReturnAssetStatus() == 1) {
            if (StrUtil.isBlank(dto.getReturnDamageDesc())
                    || dto.getReturnDamageDesc().length() < 10
                    || dto.getReturnDamageDesc().length() > 500) {
                throw new BusinessException(400, "请描述资产损坏情况");
            }
        }

        // 更新领用单
        entity.setReturnDate(dto.getReturnDate());
        entity.setReturnAssetStatus(dto.getReturnAssetStatus());
        entity.setReturnDamageDesc(dto.getReturnDamageDesc());
        entity.setReturnRemark(dto.getReturnRemark());
        entity.setStatus(4); // 已归还
        requisitionMapper.updateById(entity);

        // 更新资产状态
        AssetInfo asset = assetMapper.selectById(entity.getAssetId());
        if (asset != null) {
            if (dto.getReturnAssetStatus() == 0) {
                // 完好 → 闲置
                asset.setStatus(0);
            } else {
                // 有损坏 → 维修
                asset.setStatus(3);
            }
            asset.setUserId(null); // 清空使用人
            asset.setVersion(asset.getVersion() != null ? asset.getVersion() + 1 : 1);
            assetMapper.updateById(asset);
            RedisUtil.deleteByPattern("eams:asset:list:*");
        }

        log.info("资产已归还，申请编号={}，完好状态={}", entity.getApplyNo(), dto.getReturnAssetStatus());
    }

    // ==================== 领用记录（PRD 6.3.4） ====================

    /**
     * 领用记录列表（按角色过滤数据范围）
     */
    public PageResult<RequisitionOrder> listRecords(RequisitionQueryDTO query) {
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        Long currentUserId = SecurityContextHolder.getCurrentUserId();

        LambdaQueryWrapper<RequisitionOrder> wrapper = new LambdaQueryWrapper<RequisitionOrder>()
                .eq(RequisitionOrder::getIsDeleted, 0);

        // 数据范围过滤
        if (!roles.contains("ROLE_SUPER_ADMIN") && !roles.contains("ROLE_ASSET_ADMIN")) {
            if (roles.contains("ROLE_DEPT_ADMIN")) {
                // 部门管理员：只看本部门
                SysUser currentUser = userMapper.selectById(currentUserId);
                if (currentUser != null && currentUser.getDeptId() != null) {
                    List<Long> deptIds = getChildDeptIds(currentUser.getDeptId());
                    wrapper.in(RequisitionOrder::getApplicantDeptId, deptIds);
                }
            } else {
                // 普通员工：只看自己
                wrapper.eq(RequisitionOrder::getApplicantId, currentUserId);
            }
        }

        applyQueryFilters(wrapper, query);
        wrapper.orderByDesc(RequisitionOrder::getCreateTime);

        IPage<RequisitionOrder> page = requisitionMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        PageResult<RequisitionOrder> result = PageResult.of(page);
        fillTransientFields(result.getList());
        return result;
    }

    /**
     * 领用记录详情
     */
    public RequisitionVO getDetail(Long id) {
        RequisitionOrder entity = requisitionMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("领用记录不存在");
        }
        fillTransientFields(Collections.singletonList(entity));

        // 查询审批日志
        List<RequisitionApprovalLog> logs = approvalLogMapper.selectList(
                new LambdaQueryWrapper<RequisitionApprovalLog>()
                        .eq(RequisitionApprovalLog::getRequisitionId, id)
                        .orderByAsc(RequisitionApprovalLog::getApprovalLevel));

        RequisitionVO vo = toVO(entity);
        if (!logs.isEmpty()) {
            RequisitionApprovalLog lastLog = logs.get(logs.size() - 1);
            vo.setApprovalTime(lastLog.getCreateTime());
            // 查询审批人姓名
            SysUser approver = userMapper.selectById(lastLog.getApproverId());
            vo.setApproverName(approver != null ? approver.getRealName() : "");
        }
        return vo;
    }

    // ==================== 数据导出 ====================

    /**
     * 导出领用记录
     */
    public List<RequisitionOrder> exportData(RequisitionQueryDTO query) {
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        Long currentUserId = SecurityContextHolder.getCurrentUserId();

        LambdaQueryWrapper<RequisitionOrder> wrapper = new LambdaQueryWrapper<RequisitionOrder>()
                .eq(RequisitionOrder::getIsDeleted, 0);

        if (!roles.contains("ROLE_SUPER_ADMIN") && !roles.contains("ROLE_ASSET_ADMIN")) {
            if (roles.contains("ROLE_DEPT_ADMIN")) {
                SysUser currentUser = userMapper.selectById(currentUserId);
                if (currentUser != null && currentUser.getDeptId() != null) {
                    List<Long> deptIds = getChildDeptIds(currentUser.getDeptId());
                    wrapper.in(RequisitionOrder::getApplicantDeptId, deptIds);
                }
            } else {
                wrapper.eq(RequisitionOrder::getApplicantId, currentUserId);
            }
        }

        applyQueryFilters(wrapper, query);
        wrapper.orderByDesc(RequisitionOrder::getCreateTime);

        List<RequisitionOrder> list = requisitionMapper.selectList(wrapper);
        if (list.size() > 10000) {
            throw new BusinessException(400, "导出数据超过10000条，请缩小筛选范围后再试");
        }
        fillTransientFields(list);
        return list;
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成申请编号: RY-YYYYMMDD-XXXX
     */
    private String generateApplyNo() {
        String yyMMdd = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String lockKey = "eams:lock:requisition:applyNo:" + yyMMdd;
        String lockValue = RedisUtil.tryLock(lockKey, 3, 30);
        if (lockValue == null) {
            throw new BusinessException(400, "操作繁忙，请稍后重试");
        }
        try {
            String maxNo = requisitionMapper.selectMaxApplyNoByPrefix(yyMMdd);
            int seq = 1;
            if (maxNo != null && maxNo.length() >= 16) {
                seq = Integer.parseInt(maxNo.substring(maxNo.length() - 4)) + 1;
            }
            return String.format("RY-%s-%04d", yyMMdd, seq);
        } finally {
            RedisUtil.unlock(lockKey, lockValue);
        }
    }

    /**
     * 应用通用查询条件
     */
    private void applyQueryFilters(LambdaQueryWrapper<RequisitionOrder> wrapper,
                                    RequisitionQueryDTO query) {
        if (StrUtil.isNotBlank(query.getApplyNo())) {
            wrapper.like(RequisitionOrder::getApplyNo, query.getApplyNo());
        }
        if (query.getStatus() != null) {
            wrapper.eq(RequisitionOrder::getStatus, query.getStatus());
        }
        if (StrUtil.isNotBlank(query.getBeginDate())) {
            wrapper.ge(RequisitionOrder::getCreateTime, query.getBeginDate() + " 00:00:00");
        }
        if (StrUtil.isNotBlank(query.getEndDate())) {
            wrapper.le(RequisitionOrder::getCreateTime, query.getEndDate() + " 23:59:59");
        }
    }

    /**
     * 批量填充瞬态字段（资产信息、申请人、部门）
     */
    private void fillTransientFields(List<RequisitionOrder> list) {
        if (list.isEmpty()) return;

        // 批量查询资产
        Set<Long> assetIds = list.stream().map(RequisitionOrder::getAssetId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<Long, AssetInfo> assetMap;
        if (!assetIds.isEmpty()) {
            assetMap = assetMapper.selectBatchIds(assetIds).stream()
                    .collect(Collectors.toMap(AssetInfo::getId, a -> a, (a, b) -> a));
        } else {
            assetMap = Collections.emptyMap();
        }

        // 批量查询申请人
        Set<Long> userIds = list.stream().map(RequisitionOrder::getApplicantId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<Long, SysUser> userMap;
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        } else {
            userMap = Collections.emptyMap();
        }

        // 批量查询部门
        Set<Long> deptIds = list.stream().map(RequisitionOrder::getApplicantDeptId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<Long, SysDept> deptMap;
        if (!deptIds.isEmpty()) {
            deptMap = deptMapper.selectBatchIds(deptIds).stream()
                    .collect(Collectors.toMap(SysDept::getId, d -> d, (a, b) -> a));
        } else {
            deptMap = Collections.emptyMap();
        }

        list.forEach(r -> {
            // 资产信息
            AssetInfo asset = assetMap.get(r.getAssetId());
            if (asset != null) {
                r.setAssetCode(asset.getAssetCode());
                r.setAssetName(asset.getAssetName());
                r.setCategory(asset.getCategory());
                r.setSpecification(asset.getSpecification());
                r.setLocation(asset.getLocation());
                r.setImageUrl(asset.getImageUrl());
            }

            // 申请人
            SysUser user = userMap.get(r.getApplicantId());
            if (user != null) {
                r.setApplicantName(user.getRealName());
            }

            // 部门
            SysDept dept = deptMap.get(r.getApplicantDeptId());
            if (dept != null) {
                r.setDeptName(dept.getDeptName());
            }

            // 状态标签
            r.setStatusLabel(getStatusLabel(r.getStatus()));
        });
    }

    private String getStatusLabel(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待部门审批";
            case 1: return "待资产管理员审批";
            case 2: return "已通过";
            case 3: return "已驳回";
            case 4: return "已归还";
            default: return String.valueOf(status);
        }
    }

    private RequisitionVO toVO(RequisitionOrder entity) {
        RequisitionVO vo = new RequisitionVO();
        vo.setId(entity.getId());
        vo.setApplyNo(entity.getApplyNo());
        vo.setAssetId(entity.getAssetId());
        vo.setAssetCode(entity.getAssetCode());
        vo.setAssetName(entity.getAssetName());
        vo.setCategory(entity.getCategory());
        vo.setSpecification(entity.getSpecification());
        vo.setLocation(entity.getLocation());
        vo.setImageUrl(entity.getImageUrl());
        vo.setApplicantId(entity.getApplicantId());
        vo.setApplicantName(entity.getApplicantName());
        vo.setApplicantDeptId(entity.getApplicantDeptId());
        vo.setDeptName(entity.getDeptName());
        vo.setPurpose(entity.getPurpose());
        vo.setExpectDuration(entity.getExpectDuration());
        vo.setExpectReturnDate(entity.getExpectReturnDate());
        vo.setStatus(entity.getStatus());
        vo.setStatusLabel(entity.getStatusLabel());
        vo.setRemark(entity.getRemark());
        vo.setReturnDate(entity.getReturnDate());
        vo.setReturnAssetStatus(entity.getReturnAssetStatus());
        vo.setReturnDamageDesc(entity.getReturnDamageDesc());
        vo.setReturnRemark(entity.getReturnRemark());
        vo.setVersion(entity.getVersion());
        vo.setCreateTime(entity.getCreateTime());
        vo.setCreateBy(entity.getCreateBy());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

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
}
