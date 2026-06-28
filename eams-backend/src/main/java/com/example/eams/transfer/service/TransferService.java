package com.example.eams.transfer.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eams.asset.entity.AssetInfo;
import com.example.eams.asset.mapper.AssetInfoMapper;
import com.example.eams.common.exception.BusinessException;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.util.RedisUtil;
import com.example.eams.security.filter.SecurityContextHolder;
import com.example.eams.system.entity.SysDept;
import com.example.eams.system.entity.SysUser;
import com.example.eams.system.mapper.SysDeptMapper;
import com.example.eams.system.mapper.SysUserMapper;
import com.example.eams.transfer.dto.*;
import com.example.eams.transfer.entity.TransferOrder;
import com.example.eams.transfer.mapper.TransferOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 资产调拨服务（PRD 6.7）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferOrderMapper transferMapper;
    private final AssetInfoMapper assetMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    // ==================== 调拨申请（PRD 6.7.1） ====================

    @Transactional(rollbackFor = Exception.class)
    public TransferOrder apply(TransferApplyDTO dto) {
        Long currentUserId = SecurityContextHolder.getCurrentUserId();
        String currentUsername = SecurityContextHolder.getCurrentUsername();

        // 分布式锁防并发
        String lockKey = "eams:lock:transfer:" + dto.getAssetId();
        String lockValue = RedisUtil.tryLock(lockKey, 3, 30);
        if (lockValue == null) {
            throw new BusinessException(400, "操作繁忙，请稍后重试");
        }

        try {
            // 校验资产存在
            AssetInfo asset = assetMapper.selectById(dto.getAssetId());
            if (asset == null) {
                throw BusinessException.notFound("资产不存在");
            }
            // 盘点中不可调拨
            if (asset.getStatus() == 5) {
                throw new BusinessException(400, "该资产正在盘点中，不可调拨");
            }
            // 仅闲置/在用可调拨
            if (asset.getStatus() != 0 && asset.getStatus() != 1) {
                throw new BusinessException(400, "该资产当前不可调拨");
            }

            // 调入部门不能与调出部门相同
            if (dto.getToDeptId().equals(asset.getDeptId())) {
                throw new BusinessException(400, "调入部门不能与调出部门相同");
            }

            // 同一资产无在途调拨单
            int pending = transferMapper.countPendingByAssetId(dto.getAssetId());
            if (pending > 0) {
                throw new BusinessException(400, "该资产已有在途调拨申请，请勿重复提交");
            }

            TransferOrder entity = new TransferOrder();
            entity.setTransferNo(generateTransferNo());
            entity.setAssetId(dto.getAssetId());
            entity.setFromDeptId(asset.getDeptId());
            entity.setToDeptId(dto.getToDeptId());
            entity.setToUserId(dto.getToUserId());
            entity.setToLocation(dto.getToLocation());
            entity.setTransferReason(dto.getTransferReason());
            entity.setStatus(0); // 待调入确认
            entity.setRemark(dto.getRemark());
            entity.setApplicantId(currentUserId);

            transferMapper.insert(entity);
            log.info("调拨申请已提交，编号={}，资产ID={}，申请人={}", entity.getTransferNo(), dto.getAssetId(), currentUsername);
            return entity;

        } finally {
            RedisUtil.unlock(lockKey, lockValue);
        }
    }

    /** 我的申请列表 */
    public PageResult<TransferOrder> listMyApply(TransferQueryDTO query) {
        Long currentUserId = SecurityContextHolder.getCurrentUserId();
        LambdaQueryWrapper<TransferOrder> wrapper = new LambdaQueryWrapper<TransferOrder>()
                .eq(TransferOrder::getIsDeleted, 0)
                .eq(TransferOrder::getApplicantId, currentUserId);
        applyQueryFilters(wrapper, query);
        wrapper.orderByDesc(TransferOrder::getCreateTime);
        return queryAndFill(wrapper, query);
    }

    // ==================== 调拨审批（PRD 6.7.2） ====================

    /** 审批列表 */
    public PageResult<TransferOrder> listApproval(TransferApproveListDTO query) {
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        Long currentUserId = SecurityContextHolder.getCurrentUserId();

        LambdaQueryWrapper<TransferOrder> wrapper = new LambdaQueryWrapper<TransferOrder>()
                .eq(TransferOrder::getIsDeleted, 0);

        // 部门管理员只看涉及本部门的
        if (roles.contains("ROLE_DEPT_ADMIN") && !roles.contains("ROLE_SUPER_ADMIN")
                && !roles.contains("ROLE_ASSET_ADMIN")) {
            SysUser user = userMapper.selectById(currentUserId);
            if (user != null && user.getDeptId() != null) {
                wrapper.and(w -> w.eq(TransferOrder::getFromDeptId, user.getDeptId())
                        .or().eq(TransferOrder::getToDeptId, user.getDeptId()));
            }
        }

        if (StrUtil.isNotBlank(query.getTransferNo())) {
            wrapper.eq(TransferOrder::getTransferNo, query.getTransferNo());
        }
        if (query.getStatus() != null) {
            wrapper.eq(TransferOrder::getStatus, query.getStatus());
        }
        if (StrUtil.isNotBlank(query.getBeginDate())) {
            wrapper.ge(TransferOrder::getCreateTime, query.getBeginDate() + " 00:00:00");
        }
        if (StrUtil.isNotBlank(query.getEndDate())) {
            wrapper.le(TransferOrder::getCreateTime, query.getEndDate() + " 23:59:59");
        }
        wrapper.orderByDesc(TransferOrder::getCreateTime);
        return queryAndFill(wrapper, query.getPageNum(), query.getPageSize());
    }

    /** 审批通过（两级） */
    @Transactional(rollbackFor = Exception.class)
    public void approve(TransferApprovalDTO dto) {
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        Long currentUserId = SecurityContextHolder.getCurrentUserId();

        TransferOrder entity = transferMapper.selectById(dto.getTransferId());
        if (entity == null) {
            throw BusinessException.notFound("调拨申请不存在");
        }

        boolean isDeptAdminOnly = roles.contains("ROLE_DEPT_ADMIN")
                && !roles.contains("ROLE_SUPER_ADMIN") && !roles.contains("ROLE_ASSET_ADMIN");
        boolean isAdmin = roles.contains("ROLE_ASSET_ADMIN") || roles.contains("ROLE_SUPER_ADMIN");

        int approvalLevel;
        if (isAdmin) {
            if (entity.getStatus() == 0) {
                approvalLevel = 1; // Level 1: 确认调入
            } else if (entity.getStatus() == 1) {
                approvalLevel = 2; // Level 2: 终审
            } else {
                throw new BusinessException(400, "该申请已被处理，无法重复审批");
            }
        } else if (isDeptAdminOnly && entity.getStatus() == 0) {
            approvalLevel = 1;
        } else if (isDeptAdminOnly) {
            throw new BusinessException(400, "该申请已被处理，无法重复审批");
        } else {
            throw new BusinessException(403, "您没有权限审批该申请");
        }

        if (approvalLevel == 1) {
            // 确认调入：status 0→1
            entity.setStatus(1);
            transferMapper.updateById(entity);
        } else {
            // 资产管理员终审：status 1→2，资产划转
            AssetInfo asset = assetMapper.selectById(entity.getAssetId());
            if (asset == null) {
                throw BusinessException.notFound("资产不存在");
            }
            // 更新资产归属
            asset.setDeptId(entity.getToDeptId());
            asset.setUserId(entity.getToUserId());
            asset.setLocation(entity.getToLocation());
            asset.setVersion(asset.getVersion() != null ? asset.getVersion() + 1 : 1);
            assetMapper.updateById(asset);

            entity.setStatus(2);
            transferMapper.updateById(entity);

            RedisUtil.deleteByPattern("eams:asset:list:*");
        }

        log.info("调拨审批通过，编号={}，级别={}", entity.getTransferNo(), approvalLevel);
    }

    /** 审批驳回 */
    @Transactional(rollbackFor = Exception.class)
    public void reject(TransferApprovalDTO dto) {
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        Long currentUserId = SecurityContextHolder.getCurrentUserId();

        if (StrUtil.isBlank(dto.getRejectReason())
                || dto.getRejectReason().length() < 10
                || dto.getRejectReason().length() > 200) {
            throw new BusinessException(400, "驳回原因为10-200个字符");
        }

        TransferOrder entity = transferMapper.selectById(dto.getTransferId());
        if (entity == null) {
            throw BusinessException.notFound("调拨申请不存在");
        }

        boolean isDeptAdminOnly = roles.contains("ROLE_DEPT_ADMIN")
                && !roles.contains("ROLE_SUPER_ADMIN") && !roles.contains("ROLE_ASSET_ADMIN");
        boolean isAdmin = roles.contains("ROLE_ASSET_ADMIN") || roles.contains("ROLE_SUPER_ADMIN");

        if (isAdmin) {
            if (entity.getStatus() != 0 && entity.getStatus() != 1) {
                throw new BusinessException(400, "该申请已被处理，无法重复审批");
            }
        } else if (isDeptAdminOnly && entity.getStatus() == 0) {
            // OK
        } else if (isDeptAdminOnly) {
            throw new BusinessException(400, "该申请已被处理，无法重复审批");
        } else {
            throw new BusinessException(403, "您没有权限审批该申请");
        }

        entity.setStatus(3);
        transferMapper.updateById(entity);

        log.info("调拨驳回，编号={}，原因={}", entity.getTransferNo(), dto.getRejectReason());
    }

    // ==================== 调拨记录（PRD 6.7.3） ====================

    public PageResult<TransferOrder> listRecords(TransferQueryDTO query) {
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        Long currentUserId = SecurityContextHolder.getCurrentUserId();

        LambdaQueryWrapper<TransferOrder> wrapper = new LambdaQueryWrapper<TransferOrder>()
                .eq(TransferOrder::getIsDeleted, 0);

        if (roles.contains("ROLE_DEPT_ADMIN") && !roles.contains("ROLE_SUPER_ADMIN")
                && !roles.contains("ROLE_ASSET_ADMIN")) {
            SysUser user = userMapper.selectById(currentUserId);
            if (user != null && user.getDeptId() != null) {
                wrapper.and(w -> w.eq(TransferOrder::getFromDeptId, user.getDeptId())
                        .or().eq(TransferOrder::getToDeptId, user.getDeptId()));
            }
        }
        applyQueryFilters(wrapper, query);
        wrapper.orderByDesc(TransferOrder::getCreateTime);
        return queryAndFill(wrapper, query);
    }

    public TransferVO getDetail(Long id) {
        TransferOrder entity = transferMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("调拨记录不存在");
        }
        fillTransientFields(Collections.singletonList(entity));
        return toVO(entity);
    }

    public List<TransferOrder> exportData(TransferQueryDTO query) {
        LambdaQueryWrapper<TransferOrder> wrapper = new LambdaQueryWrapper<TransferOrder>()
                .eq(TransferOrder::getIsDeleted, 0);
        applyQueryFilters(wrapper, query);
        wrapper.orderByDesc(TransferOrder::getCreateTime);
        List<TransferOrder> list = transferMapper.selectList(wrapper);
        if (list.size() > 10000) {
            throw new BusinessException(400, "导出数据超过10000条，请缩小筛选范围后再试");
        }
        fillTransientFields(list);
        return list;
    }

    // ==================== 辅助方法 ====================

    private String generateTransferNo() {
        String yyMMdd = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String lockKey = "eams:lock:transfer:no:" + yyMMdd;
        String lockValue = RedisUtil.tryLock(lockKey, 3, 30);
        if (lockValue == null) throw new BusinessException(400, "操作繁忙，请稍后重试");
        try {
            String maxNo = transferMapper.selectMaxTransferNoByPrefix(yyMMdd);
            int seq = 1;
            if (maxNo != null && maxNo.length() >= 16) {
                seq = Integer.parseInt(maxNo.substring(maxNo.length() - 4)) + 1;
            }
            return String.format("DB-%s-%04d", yyMMdd, seq);
        } finally {
            RedisUtil.unlock(lockKey, lockValue);
        }
    }

    private void applyQueryFilters(LambdaQueryWrapper<TransferOrder> wrapper, TransferQueryDTO q) {
        if (StrUtil.isNotBlank(q.getTransferNo())) wrapper.like(TransferOrder::getTransferNo, q.getTransferNo());
        if (q.getStatus() != null) wrapper.eq(TransferOrder::getStatus, q.getStatus());
        if (StrUtil.isNotBlank(q.getBeginDate())) wrapper.ge(TransferOrder::getCreateTime, q.getBeginDate() + " 00:00:00");
        if (StrUtil.isNotBlank(q.getEndDate())) wrapper.le(TransferOrder::getCreateTime, q.getEndDate() + " 23:59:59");
    }

    private PageResult<TransferOrder> queryAndFill(LambdaQueryWrapper<TransferOrder> w, TransferQueryDTO q) {
        return queryAndFill(w, q.getPageNum(), q.getPageSize());
    }

    private PageResult<TransferOrder> queryAndFill(LambdaQueryWrapper<TransferOrder> w, int pageNum, int pageSize) {
        IPage<TransferOrder> page = transferMapper.selectPage(new Page<>(pageNum, pageSize), w);
        PageResult<TransferOrder> result = PageResult.of(page);
        fillTransientFields(result.getList());
        return result;
    }

    private void fillTransientFields(List<TransferOrder> list) {
        if (list.isEmpty()) return;

        // 资产
        Set<Long> assetIds = list.stream().map(TransferOrder::getAssetId).filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<Long, AssetInfo> assetMap;
        if (!assetIds.isEmpty()) {
            assetMap = assetMapper.selectBatchIds(assetIds).stream()
                    .collect(Collectors.toMap(AssetInfo::getId, a -> a, (a, b) -> a));
        } else { assetMap = Collections.emptyMap(); }

        // 用户
        Set<Long> userIds = new HashSet<>();
        list.forEach(r -> {
            if (r.getApplicantId() != null) userIds.add(r.getApplicantId());
            if (r.getToUserId() != null) userIds.add(r.getToUserId());
        });
        final Map<Long, SysUser> userMap;
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        } else { userMap = Collections.emptyMap(); }

        // 部门
        Set<Long> deptIds = new HashSet<>();
        list.forEach(r -> {
            if (r.getFromDeptId() != null) deptIds.add(r.getFromDeptId());
            if (r.getToDeptId() != null) deptIds.add(r.getToDeptId());
        });
        final Map<Long, SysDept> deptMap;
        if (!deptIds.isEmpty()) {
            deptMap = deptMapper.selectBatchIds(deptIds).stream()
                    .collect(Collectors.toMap(SysDept::getId, d -> d, (a, b) -> a));
        } else { deptMap = Collections.emptyMap(); }

        list.forEach(r -> {
            AssetInfo a = assetMap.get(r.getAssetId());
            if (a != null) {
                r.setAssetCode(a.getAssetCode());
                r.setAssetName(a.getAssetName());
                r.setCategory(a.getCategory());
                r.setSpecification(a.getSpecification());
                r.setImageUrl(a.getImageUrl());
            }
            SysUser applicant = userMap.get(r.getApplicantId());
            if (applicant != null) r.setApplicantName(applicant.getRealName());
            SysUser toUser = userMap.get(r.getToUserId());
            if (toUser != null) r.setToUserName(toUser.getRealName());
            SysDept fromDept = deptMap.get(r.getFromDeptId());
            if (fromDept != null) r.setFromDeptName(fromDept.getDeptName());
            SysDept toDept = deptMap.get(r.getToDeptId());
            if (toDept != null) r.setToDeptName(toDept.getDeptName());
            r.setStatusLabel(getStatusLabel(r.getStatus()));
        });
    }

    private String getStatusLabel(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待调入确认";
            case 1: return "待资产管理员审批";
            case 2: return "已通过";
            case 3: return "已驳回";
            default: return String.valueOf(status);
        }
    }

    private TransferVO toVO(TransferOrder e) {
        TransferVO vo = new TransferVO();
        vo.setId(e.getId()); vo.setTransferNo(e.getTransferNo());
        vo.setAssetId(e.getAssetId()); vo.setAssetCode(e.getAssetCode());
        vo.setAssetName(e.getAssetName()); vo.setCategory(e.getCategory());
        vo.setSpecification(e.getSpecification());
        vo.setFromDeptId(e.getFromDeptId()); vo.setFromDeptName(e.getFromDeptName());
        vo.setToDeptId(e.getToDeptId()); vo.setToDeptName(e.getToDeptName());
        vo.setToUserId(e.getToUserId()); vo.setToUserName(e.getToUserName());
        vo.setToLocation(e.getToLocation()); vo.setTransferReason(e.getTransferReason());
        vo.setStatus(e.getStatus()); vo.setStatusLabel(e.getStatusLabel());
        vo.setRemark(e.getRemark()); vo.setApplicantId(e.getApplicantId());
        vo.setApplicantName(e.getApplicantName()); vo.setVersion(e.getVersion());
        vo.setCreateTime(e.getCreateTime()); vo.setCreateBy(e.getCreateBy());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
