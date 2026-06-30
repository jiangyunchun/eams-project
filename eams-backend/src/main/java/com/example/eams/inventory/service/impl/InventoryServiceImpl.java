package com.example.eams.inventory.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eams.asset.entity.AssetInfo;
import com.example.eams.asset.mapper.AssetInfoMapper;
import com.example.eams.common.exception.BusinessException;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.util.RedisUtil;
import com.example.eams.inventory.dto.*;
import com.example.eams.inventory.entity.InvDetail;
import com.example.eams.inventory.entity.InvDifference;
import com.example.eams.inventory.entity.InvTask;
import com.example.eams.inventory.mapper.InvDetailMapper;
import com.example.eams.inventory.mapper.InvDifferenceMapper;
import com.example.eams.inventory.mapper.InvTaskMapper;
import com.example.eams.inventory.service.InventoryService;
import com.example.eams.security.filter.SecurityContextHolder;
import com.example.eams.system.entity.SysDept;
import com.example.eams.system.entity.SysUser;
import com.example.eams.system.mapper.SysDeptMapper;
import com.example.eams.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 盘点管理服务实现（PRD 6.4）
 * <p>
 * 盘点全流程: 创建任务 → 执行盘点(逐项确认/盘盈登记) → 完成盘点(生成差异) → 差异处理
 * 权限对齐 PRD 4.2 权限矩阵: 超级管理员/资产管理员 → 全部; 部门管理员 → 仅本部门查看
 * 分布式锁对齐 PRD 5.7 Redis缓存规范
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InvTaskMapper invTaskMapper;
    private final InvDetailMapper invDetailMapper;
    private final InvDifferenceMapper invDifferenceMapper;
    private final AssetInfoMapper assetInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;

    /** 盘点任务编号前缀（PRD 6.4.1） */
    private static final String TASK_NO_PREFIX = "PD";

    /** 资产编码前缀（PRD 5.5） */
    private static final String ASSET_CODE_PREFIX = "AS";

    /** 分布式锁配置（PRD 5.7.4）：等待3秒，最大持有30秒 */
    private static final int LOCK_WAIT_SEC = 3;
    private static final int LOCK_HOLD_SEC = 30;

    /** 盘点任务创建锁 Key（PRD 5.7.1） */
    private static final String LOCK_INVENTORY_CREATE = "eams:lock:inventory:create:";

    /** 盘点执行锁 Key（PRD 5.7.1） */
    private static final String LOCK_INVENTORY = "eams:lock:inventory:";

    /** 资产编码锁 Key（PRD 5.7.1） */
    private static final String LOCK_ASSET_CODE = "eams:lock:asset:code:";

    // ==================== 6.4.1 盘点任务管理 ====================

    @Override
    public PageResult<InvTask> listTasks(InvTaskQueryDTO query) {
        LambdaQueryWrapper<InvTask> wrapper = new LambdaQueryWrapper<InvTask>()
                .eq(InvTask::getIsDeleted, 0);

        // 部门管理员仅可查看本部门盘点任务（PRD 4.2 权限矩阵）
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        if (roles.contains("ROLE_DEPT_ADMIN") && !roles.contains("ROLE_SUPER_ADMIN")
                && !roles.contains("ROLE_ASSET_ADMIN")) {
            Long deptId = getCurrentUserDeptId();
            if (deptId != null) {
                // 部门管理员查看范围为本部门的盘点任务（通过scope_value判断）
                Set<Long> deptIds = getDeptAndChildIds(deptId);
                // 全公司范围的任务部门管理员也可查看
                wrapper.and(w -> w.eq(InvTask::getScopeType, "ALL")
                        .or().like(InvTask::getScopeValue, String.valueOf(deptId)));
            }
        }

        applyTaskQueryFilters(wrapper, query);
        wrapper.orderByDesc(InvTask::getCreateTime);

        IPage<InvTask> page = invTaskMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        PageResult<InvTask> result = PageResult.of(page);
        fillTaskDisplayFields(result.getList());
        return result;
    }

    @Override
    public InvTask getTaskDetail(Long taskId) {
        InvTask task = invTaskMapper.selectById(taskId);
        if (task == null) {
            throw BusinessException.notFound("盘点任务不存在");
        }
        fillTaskDisplayFields(Collections.singletonList(task));
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvTask createTask(InvTaskCreateDTO dto) {
        Long currentUserId = SecurityContextHolder.getCurrentUserId();
        String currentUsername = SecurityContextHolder.getCurrentUsername();

        // 1. 构建范围值JSON并获取分布式锁（PRD 5.7.4）
        String scopeValue = buildScopeValue(dto);
        String lockKey = LOCK_INVENTORY_CREATE + dto.getScopeType() + ":" + scopeValue;
        String lockValue = RedisUtil.tryLock(lockKey, LOCK_WAIT_SEC, LOCK_HOLD_SEC);
        if (lockValue == null) {
            throw new BusinessException(400, "操作繁忙，请稍后重试");
        }

        try {
            // 2. 检查同范围是否有进行中的任务（PRD 6.4.1 业务拦截）
            int inProgressCount = invTaskMapper.countInProgressByScope(dto.getScopeType(), scopeValue);
            if (inProgressCount > 0) {
                throw new BusinessException(400, "该范围内已存在进行中的盘点任务");
            }

            // 3. 校验盘点日期（≥当前日期）
            if (dto.getInventoryDate().isBefore(LocalDate.now())) {
                throw new BusinessException(400, "盘点日期不能早于当前日期");
            }

            // 4. 查询范围内资产列表
            List<AssetInfo> scopeAssets = queryScopeAssets(dto);

            // 5. 生成任务编号: PD-YYYYMMDD-XXX
            String taskNo = generateTaskNo();

            // 6. 创建盘点任务
            InvTask task = new InvTask();
            task.setTaskNo(taskNo);
            task.setTaskName(dto.getTaskName());
            task.setScopeType(dto.getScopeType());
            task.setScopeValue(scopeValue);
            task.setInventoryDate(dto.getInventoryDate());
            task.setTotalCount(scopeAssets.size());
            task.setCheckedCount(0);
            task.setNormalCount(0);
            task.setSurplusCount(0);
            task.setShortageCount(0);
            task.setStatus(0); // 进行中（PRD 5.5 盘点状态枚举）
            task.setRemark(dto.getRemark());
            task.setCreatorId(currentUserId);

            invTaskMapper.insert(task);

            // 7. 批量更新范围内资产状态为"盘点中"（status=5），并记录原状态快照
            if (!scopeAssets.isEmpty()) {
                for (AssetInfo asset : scopeAssets) {
                    // 创建盘点明细，记录账面快照（PRD 6.4.2）
                    InvDetail detail = new InvDetail();
                    detail.setTaskId(task.getId());
                    detail.setAssetId(asset.getId());
                    detail.setBookUserName(getUserName(asset.getUserId()));
                    detail.setBookStatus(asset.getStatus());
                    detail.setInventoryResult(2); // 默认正常
                    detail.setIsConfirmed(0); // 未确认
                    invDetailMapper.insert(detail);

                    // 更新资产状态为"盘点中"
                    asset.setStatus(5);
                    assetInfoMapper.updateById(asset);
                }
            }

            // 8. 清除资产列表缓存（PRD 5.7.3）
            RedisUtil.deleteByPattern("eams:asset:list:*");

            log.info("盘点任务创建成功, taskNo={}, taskName={}, totalCount={}, creatorId={}",
                    taskNo, dto.getTaskName(), scopeAssets.size(), currentUserId);
            return task;

        } finally {
            RedisUtil.unlock(lockKey, lockValue);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long taskId) {
        InvTask task = invTaskMapper.selectById(taskId);
        if (task == null) {
            throw BusinessException.notFound("盘点任务不存在");
        }
        if (task.getStatus() != 0) {
            throw new BusinessException(400, "仅进行中的盘点任务可取消");
        }

        // 1. 恢复所有资产原有状态（PRD 6.4.1 取消逻辑）
        List<InvDetail> details = invDetailMapper.selectList(
                new LambdaQueryWrapper<InvDetail>()
                        .eq(InvDetail::getTaskId, taskId)
                        .eq(InvDetail::getIsDeleted, 0));

        for (InvDetail detail : details) {
            // 盘盈资产（新入库的）在取消时直接删除
            if (detail.getInventoryResult() == 0 && detail.getIsConfirmed() == 1) {
                // 删除盘盈新增的资产
                assetInfoMapper.deleteById(detail.getAssetId());
                continue;
            }
            // 恢复原有状态
            AssetInfo asset = assetInfoMapper.selectById(detail.getAssetId());
            if (asset != null && asset.getStatus() == 5) {
                asset.setStatus(detail.getBookStatus() != null ? detail.getBookStatus() : 0);
                assetInfoMapper.updateById(asset);
            }
        }

        // 2. 任务状态→已取消
        task.setStatus(2); // 已取消（PRD 5.5 盘点状态枚举）
        invTaskMapper.updateById(task);

        // 3. 清除资产列表缓存
        RedisUtil.deleteByPattern("eams:asset:list:*");

        log.info("盘点任务已取消, taskNo={}, taskId={}", task.getTaskNo(), taskId);
    }

    // ==================== 6.4.2 执行盘点 ====================

    @Override
    public List<InvDetail> getTaskDetails(Long taskId) {
        InvTask task = invTaskMapper.selectById(taskId);
        if (task == null) {
            throw BusinessException.notFound("盘点任务不存在");
        }

        List<InvDetail> details = invDetailMapper.selectList(
                new LambdaQueryWrapper<InvDetail>()
                        .eq(InvDetail::getTaskId, taskId)
                        .eq(InvDetail::getIsDeleted, 0)
                        .orderByAsc(InvDetail::getIsConfirmed)
                        .orderByAsc(InvDetail::getId));

        fillDetailDisplayFields(details);
        return details;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDetails(InvDetailConfirmDTO dto) {
        Long taskId = dto.getTaskId();
        String currentUsername = SecurityContextHolder.getCurrentUsername();

        // 1. Redis分布式锁：防止多人同时编辑同一盘点任务（PRD 5.7.1）
        String lockKey = LOCK_INVENTORY + taskId;
        String lockValue = RedisUtil.tryLock(lockKey, LOCK_WAIT_SEC, LOCK_HOLD_SEC);
        if (lockValue == null) {
            throw new BusinessException(400, "操作繁忙，请稍后重试");
        }

        try {
            InvTask task = invTaskMapper.selectById(taskId);
            if (task == null) {
                throw BusinessException.notFound("盘点任务不存在");
            }
            if (task.getStatus() != 0) {
                throw new BusinessException(400, "仅进行中的盘点任务可执行盘点");
            }

            // 2. 盘亏时校验备注必填（PRD 6.4.2）
            if (dto.getInventoryResult() == 1 && StrUtil.isBlank(dto.getRemark())) {
                throw new BusinessException(400, "请填写盘亏原因");
            }

            // 3. 批量更新明细
            for (Long detailId : dto.getDetailIds()) {
                InvDetail detail = invDetailMapper.selectById(detailId);
                if (detail == null || !detail.getTaskId().equals(taskId)) {
                    throw new BusinessException(400, "盘点明细不属于当前任务");
                }

                detail.setInventoryResult(dto.getInventoryResult());
                detail.setRemark(dto.getRemark());
                detail.setIsConfirmed(1);
                detail.setConfirmTime(LocalDateTime.now());
                invDetailMapper.updateById(detail);

                // 盘亏时立即更新资产状态为"待处理"（保持盘点中直到任务完成）
                if (dto.getInventoryResult() == 1) {
                    AssetInfo asset = assetInfoMapper.selectById(detail.getAssetId());
                    if (asset != null) {
                        // 盘亏资产暂保持盘点中状态，完成盘点时统一处理
                    }
                }
            }

            // 4. 更新任务已确认数
            Long totalConfirmed = invDetailMapper.selectCount(
                    new LambdaQueryWrapper<InvDetail>()
                            .eq(InvDetail::getTaskId, taskId)
                            .eq(InvDetail::getIsConfirmed, 1)
                            .eq(InvDetail::getIsDeleted, 0));
            task.setCheckedCount(totalConfirmed.intValue());
            invTaskMapper.updateById(task);

            log.info("盘点确认完成, taskId={}, detailIds={}, result={}, operator={}",
                    taskId, dto.getDetailIds(), dto.getInventoryResult(), currentUsername);

        } finally {
            RedisUtil.unlock(lockKey, lockValue);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvDetail registerSurplusAsset(InvSurplusAssetDTO dto) {
        Long taskId = dto.getTaskId();

        // 1. Redis分布式锁
        String lockKey = LOCK_INVENTORY + taskId;
        String lockValue = RedisUtil.tryLock(lockKey, LOCK_WAIT_SEC, LOCK_HOLD_SEC);
        if (lockValue == null) {
            throw new BusinessException(400, "操作繁忙，请稍后重试");
        }

        try {
            InvTask task = invTaskMapper.selectById(taskId);
            if (task == null) {
                throw BusinessException.notFound("盘点任务不存在");
            }
            if (task.getStatus() != 0) {
                throw new BusinessException(400, "仅进行中的盘点任务可登记盘盈");
            }

            // 2. 生成资产编码（PRD 5.5 + Redis编码锁）
            String assetCode = generateAssetCode(dto.getCategory());

            // 3. 盘盈资产入库（asset_info）
            AssetInfo asset = new AssetInfo();
            asset.setAssetCode(assetCode);
            asset.setAssetName(dto.getAssetName());
            asset.setCategory(dto.getCategory());
            asset.setSpecification(dto.getSpecification());
            asset.setSnNumber(dto.getSnNumber());
            asset.setOriginalValue(dto.getOriginalValue());
            asset.setPurchaseDate(dto.getPurchaseDate());
            asset.setUsefulLife(dto.getUsefulLife());
            asset.setResidualRate(dto.getResidualRate() != null ? dto.getResidualRate() : new BigDecimal("5.00"));
            asset.setLocation(dto.getLocation());
            asset.setDeptId(dto.getDeptId());
            asset.setUserId(dto.getUserId());
            asset.setStatus(0); // 闲置（新盘盈资产默认闲置）
            asset.setVersion(0);

            // 计算预计报废日期
            if (dto.getPurchaseDate() != null && dto.getUsefulLife() != null) {
                asset.setScrapDate(dto.getPurchaseDate().plusYears(dto.getUsefulLife()));
            }

            assetInfoMapper.insert(asset);

            // 4. 新增盘点明细（盘盈，已确认）
            InvDetail detail = new InvDetail();
            detail.setTaskId(taskId);
            detail.setAssetId(asset.getId());
            detail.setBookUserName(null); // 盘盈资产无账面使用人
            detail.setBookStatus(null);   // 盘盈资产无账面状态
            detail.setInventoryResult(0); // 盘盈（PRD 5.5 盘点结果枚举）
            detail.setRemark(dto.getRemark());
            detail.setIsConfirmed(1);
            detail.setConfirmTime(LocalDateTime.now());
            invDetailMapper.insert(detail);

            // 5. 更新任务已确认数
            Long totalConfirmed = invDetailMapper.selectCount(
                    new LambdaQueryWrapper<InvDetail>()
                            .eq(InvDetail::getTaskId, taskId)
                            .eq(InvDetail::getIsConfirmed, 1)
                            .eq(InvDetail::getIsDeleted, 0));
            task.setCheckedCount(totalConfirmed.intValue());
            invTaskMapper.updateById(task);

            // 6. 清除资产列表缓存
            RedisUtil.deleteByPattern("eams:asset:list:*");

            log.info("盘盈资产登记成功, assetCode={}, assetName={}, taskId={}, detailId={}",
                    assetCode, dto.getAssetName(), taskId, detail.getId());

            // 填充展示字段
            detail.setAssetCode(assetCode);
            detail.setAssetName(dto.getAssetName());
            detail.setCategory(dto.getCategory());

            return detail;

        } finally {
            RedisUtil.unlock(lockKey, lockValue);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId) {
        String currentUsername = SecurityContextHolder.getCurrentUsername();

        // 1. Redis分布式锁
        String lockKey = LOCK_INVENTORY + taskId;
        String lockValue = RedisUtil.tryLock(lockKey, LOCK_WAIT_SEC, LOCK_HOLD_SEC);
        if (lockValue == null) {
            throw new BusinessException(400, "操作繁忙，请稍后重试");
        }

        try {
            InvTask task = invTaskMapper.selectById(taskId);
            if (task == null) {
                throw BusinessException.notFound("盘点任务不存在");
            }
            if (task.getStatus() != 0) {
                throw new BusinessException(400, "仅进行中的盘点任务可完成");
            }

            // 2. 校验所有资产已确认（PRD 6.4.2 完成前置校验）
            int unconfirmedCount = invDetailMapper.countUnconfirmedByTaskId(taskId);
            if (unconfirmedCount > 0) {
                throw new BusinessException(400,
                        "还有" + unconfirmedCount + "项资产未确认，请确认后再完成盘点");
            }

            // 3. 统计各盘点结果数量（PRD 6.4.2 完成盘点后处理）
            int normalCount = invDetailMapper.countByTaskIdAndResult(taskId, 2);
            int surplusCount = invDetailMapper.countByTaskIdAndResult(taskId, 0);
            int shortageCount = invDetailMapper.countByTaskIdAndResult(taskId, 1);

            task.setNormalCount(normalCount);
            task.setSurplusCount(surplusCount);
            task.setShortageCount(shortageCount);

            // 4. 处理盘点结果（PRD 6.4.2 完成盘点后处理）
            List<InvDetail> details = invDetailMapper.selectList(
                    new LambdaQueryWrapper<InvDetail>()
                            .eq(InvDetail::getTaskId, taskId)
                            .eq(InvDetail::getIsDeleted, 0));

            for (InvDetail detail : details) {
                AssetInfo asset = assetInfoMapper.selectById(detail.getAssetId());
                if (asset == null) continue;

                if (detail.getInventoryResult() == 2) {
                    // 正常：恢复原状态
                    if (asset.getStatus() == 5) {
                        asset.setStatus(detail.getBookStatus() != null ? detail.getBookStatus() : 0);
                        assetInfoMapper.updateById(asset);
                    }
                } else if (detail.getInventoryResult() == 0) {
                    // 盘盈：新入库资产已是闲置状态；若为范围资产误标盘盈，恢复原状态
                    if (asset.getStatus() == 5) {
                        asset.setStatus(detail.getBookStatus() != null ? detail.getBookStatus() : 0);
                        assetInfoMapper.updateById(asset);
                    }
                } else if (detail.getInventoryResult() == 1) {
                    // 盘亏：生成差异记录 + 恢复资产原状态（PRD 6.4.2）
                    // 差异记录标记为待处理，后续可通过报废处置模块以"盘亏确认"原因报废
                    InvDifference diff = new InvDifference();
                    diff.setTaskId(taskId);
                    diff.setDetailId(detail.getId());
                    diff.setDiffType(1); // 盘亏
                    diff.setAssetId(detail.getAssetId());
                    diff.setAssetName(asset.getAssetName() != null ? asset.getAssetName() : "未知资产");
                    diff.setAssetCode(asset.getAssetCode() != null ? asset.getAssetCode() : "");
                    diff.setBookQty(1);
                    diff.setActualQty(0);
                    diff.setDiffDesc(detail.getRemark());
                    diff.setHandleStatus(0); // 待处理
                    invDifferenceMapper.insert(diff);

                    // 盘亏资产恢复原状态（差异已记录在inv_difference中）
                    if (asset.getStatus() == 5) {
                        asset.setStatus(detail.getBookStatus() != null ? detail.getBookStatus() : 0);
                        assetInfoMapper.updateById(asset);
                    }
                }
            }

            // 5. 盘盈记录生成差异（汇总记录）
            if (surplusCount > 0) {
                List<InvDetail> surplusDetails = details.stream()
                        .filter(d -> d.getInventoryResult() == 0)
                        .collect(Collectors.toList());
                for (InvDetail sd : surplusDetails) {
                    AssetInfo sa = assetInfoMapper.selectById(sd.getAssetId());
                    if (sa != null) {
                        InvDifference diff = new InvDifference();
                        diff.setTaskId(taskId);
                        diff.setDetailId(sd.getId());
                        diff.setDiffType(0); // 盘盈
                        diff.setAssetId(sd.getAssetId());
                        diff.setAssetName(sa.getAssetName());
                        diff.setAssetCode(sa.getAssetCode());
                        diff.setBookQty(0);
                        diff.setActualQty(1);
                        diff.setDiffDesc(sd.getRemark());
                        diff.setHandleStatus(0); // 待处理
                        invDifferenceMapper.insert(diff);
                    }
                }
            }

            // 6. 任务状态→已完成
            task.setStatus(1); // 已完成（PRD 5.5 盘点状态枚举）
            invTaskMapper.updateById(task);

            // 7. 清除资产列表缓存
            RedisUtil.deleteByPattern("eams:asset:list:*");

            log.info("盘点任务已完成, taskNo={}, normal={}, surplus={}, shortage={}, operator={}",
                    task.getTaskNo(), normalCount, surplusCount, shortageCount, currentUsername);

        } finally {
            RedisUtil.unlock(lockKey, lockValue);
        }
    }

    // ==================== 6.4.3 盘点差异记录 ====================

    @Override
    public PageResult<InvDifference> listDifferences(InvDifferenceQueryDTO query) {
        LambdaQueryWrapper<InvDifference> wrapper = new LambdaQueryWrapper<InvDifference>()
                .eq(InvDifference::getIsDeleted, 0);

        // 部门管理员仅可查看本部门盘点任务的差异（PRD 4.2 权限矩阵）
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        if (roles.contains("ROLE_DEPT_ADMIN") && !roles.contains("ROLE_SUPER_ADMIN")
                && !roles.contains("ROLE_ASSET_ADMIN")) {
            Long deptId = getCurrentUserDeptId();
            if (deptId != null) {
                Set<Long> deptIds = getDeptAndChildIds(deptId);
                // 查询这些部门的资产ID
                List<AssetInfo> deptAssets = assetInfoMapper.selectList(
                        new LambdaQueryWrapper<AssetInfo>()
                                .in(AssetInfo::getDeptId, deptIds)
                                .eq(AssetInfo::getIsDeleted, 0));
                Set<Long> assetIds = deptAssets.stream()
                        .map(AssetInfo::getId).collect(Collectors.toSet());
                if (!assetIds.isEmpty()) {
                    wrapper.in(InvDifference::getAssetId, assetIds);
                } else {
                    wrapper.eq(InvDifference::getAssetId, -1L);
                }
            }
        }

        applyDiffQueryFilters(wrapper, query);
        wrapper.orderByAsc(InvDifference::getHandleStatus)
               .orderByDesc(InvDifference::getCreateTime);

        IPage<InvDifference> page = invDifferenceMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        PageResult<InvDifference> result = PageResult.of(page);
        fillDiffDisplayFields(result.getList());
        return result;
    }

    @Override
    public InvDifference getDifferenceDetail(Long id) {
        InvDifference diff = invDifferenceMapper.selectById(id);
        if (diff == null) {
            throw BusinessException.notFound("差异记录不存在");
        }
        fillDiffDisplayFields(Collections.singletonList(diff));
        return diff;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleDifferences(InvDifferenceHandleDTO dto) {
        Long currentUserId = SecurityContextHolder.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        for (Long id : dto.getIds()) {
            InvDifference diff = invDifferenceMapper.selectById(id);
            if (diff == null) {
                throw BusinessException.notFound("差异记录不存在");
            }
            if (diff.getHandleStatus() == 1) {
                continue; // 已处理的跳过
            }
            diff.setHandleStatus(1); // 已处理
            diff.setHandleTime(now);
            diff.setHandlerId(currentUserId);
            invDifferenceMapper.updateById(diff);
        }

        log.info("差异批量处理完成, ids={}, handlerId={}", dto.getIds(), currentUserId);
    }

    @Override
    public List<InvDifference> exportDifferences(InvDifferenceQueryDTO query) {
        LambdaQueryWrapper<InvDifference> wrapper = new LambdaQueryWrapper<InvDifference>()
                .eq(InvDifference::getIsDeleted, 0);
        applyDiffQueryFilters(wrapper, query);
        wrapper.orderByDesc(InvDifference::getCreateTime);
        List<InvDifference> list = invDifferenceMapper.selectList(wrapper);
        fillDiffDisplayFields(list);
        return list;
    }

    @Override
    public List<InvTask> exportTasks(InvTaskQueryDTO query) {
        LambdaQueryWrapper<InvTask> wrapper = new LambdaQueryWrapper<InvTask>()
                .eq(InvTask::getIsDeleted, 0);
        applyTaskQueryFilters(wrapper, query);
        wrapper.orderByDesc(InvTask::getCreateTime);
        List<InvTask> list = invTaskMapper.selectList(wrapper);
        fillTaskDisplayFields(list);
        return list;
    }

    // ==================== 私有方法 ====================

    /**
     * 构建范围值JSON（PRD 6.4.1）
     */
    private String buildScopeValue(InvTaskCreateDTO dto) {
        if ("DEPT".equals(dto.getScopeType())) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("deptIds", dto.getDeptIds());
            return JSONUtil.toJsonStr(map);
        } else if ("CATEGORY".equals(dto.getScopeType())) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("categoryCodes", dto.getCategoryCodes());
            return JSONUtil.toJsonStr(map);
        }
        return "ALL";
    }

    /**
     * 查询范围内资产列表（PRD 6.4.1 创建任务时锁定快照）
     */
    private List<AssetInfo> queryScopeAssets(InvTaskCreateDTO dto) {
        LambdaQueryWrapper<AssetInfo> wrapper = new LambdaQueryWrapper<AssetInfo>()
                .eq(AssetInfo::getIsDeleted, 0)
                .ne(AssetInfo::getStatus, 4)  // 排除已报废
                .ne(AssetInfo::getStatus, 5); // 排除已在其他盘点中的资产

        if ("DEPT".equals(dto.getScopeType())) {
            // 按部门范围：包含子部门
            Set<Long> deptIds = new HashSet<>();
            for (Long deptId : dto.getDeptIds()) {
                deptIds.addAll(getDeptAndChildIds(deptId));
            }
            wrapper.in(AssetInfo::getDeptId, deptIds);
        } else if ("CATEGORY".equals(dto.getScopeType())) {
            // 按分类范围
            wrapper.in(AssetInfo::getCategory, dto.getCategoryCodes());
        }
        // ALL: 全公司，不加额外过滤

        return assetInfoMapper.selectList(wrapper);
    }

    /**
     * 生成盘点任务编号: PD-YYYYMMDD-XXX（PRD 6.4.1 / 技术方案 4.3.9）
     */
    private String generateTaskNo() {
        String yyMMdd = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = TASK_NO_PREFIX + "-" + yyMMdd + "-";

        String maxTaskNo = invTaskMapper.selectMaxTaskNoByPrefix(prefix);
        int seq = 1;
        if (maxTaskNo != null && maxTaskNo.length() >= prefix.length() + 3) {
            String seqPart = maxTaskNo.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + String.format("%03d", seq);
    }

    /**
     * 生成资产编码: AS-{类别码}-{YYMM}-{4位流水}（PRD 5.5 / 技术方案 6.3）
     */
    private String generateAssetCode(String categoryCode) {
        String categoryShort = getCategoryShort(categoryCode);
        String yyMM = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM"));
        String lockKey = LOCK_ASSET_CODE + categoryShort + ":" + yyMM;
        String lockValue = RedisUtil.tryLock(lockKey, LOCK_WAIT_SEC, LOCK_HOLD_SEC);
        if (lockValue == null) {
            throw new BusinessException(400, "编码生成繁忙，请稍后重试");
        }
        try {
            String prefix = ASSET_CODE_PREFIX + "-" + categoryShort + "-" + yyMM + "-";
            // 查询当日该类别最大流水号（复用AssetInfoMapper已有方法）
            String maxCode = assetInfoMapper.selectMaxCodeByPrefix(categoryShort, yyMM);
            int seq = 1;
            if (maxCode != null && maxCode.length() >= prefix.length() + 4) {
                String seqPart = maxCode.substring(prefix.length());
                try {
                    seq = Integer.parseInt(seqPart) + 1;
                } catch (NumberFormatException e) {
                    seq = 1;
                }
            }
            return prefix + String.format("%04d", seq);
        } finally {
            RedisUtil.unlock(lockKey, lockValue);
        }
    }

    /**
     * 获取资产分类的简短编码
     */
    private String getCategoryShort(String categoryCode) {
        if (StrUtil.isBlank(categoryCode)) return "OT";
        switch (categoryCode) {
            case "IT_EQUIPMENT": return "IT";
            case "OFFICE_FURNITURE": return "OF";
            case "PRODUCTION": return "PD";
            case "VEHICLE": return "VE";
            default: return "OT";
        }
    }

    /**
     * 获取用户名（根据userId）
     */
    private String getUserName(Long userId) {
        if (userId == null) return null;
        SysUser user = sysUserMapper.selectById(userId);
        return user != null ? user.getRealName() : null;
    }

    /**
     * 获取当前用户所属部门ID
     */
    private Long getCurrentUserDeptId() {
        Long userId = SecurityContextHolder.getCurrentUserId();
        if (userId == null) return null;
        SysUser user = sysUserMapper.selectById(userId);
        return user != null ? user.getDeptId() : null;
    }

    /**
     * 获取部门及其所有子部门ID集合
     */
    private Set<Long> getDeptAndChildIds(Long deptId) {
        Set<Long> result = new HashSet<>();
        result.add(deptId);
        List<SysDept> children = sysDeptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getParentId, deptId)
                        .eq(SysDept::getIsDeleted, 0));
        for (SysDept child : children) {
            result.addAll(getDeptAndChildIds(child.getId()));
        }
        return result;
    }

    /**
     * 拼接任务查询条件
     */
    private void applyTaskQueryFilters(LambdaQueryWrapper<InvTask> wrapper, InvTaskQueryDTO query) {
        if (StrUtil.isNotBlank(query.getTaskNo())) {
            wrapper.like(InvTask::getTaskNo, query.getTaskNo());
        }
        if (StrUtil.isNotBlank(query.getTaskName())) {
            wrapper.like(InvTask::getTaskName, query.getTaskName());
        }
        if (query.getStatus() != null) {
            wrapper.eq(InvTask::getStatus, query.getStatus());
        }
        if (StrUtil.isNotBlank(query.getBeginDate())) {
            wrapper.ge(InvTask::getInventoryDate, query.getBeginDate());
        }
        if (StrUtil.isNotBlank(query.getEndDate())) {
            wrapper.le(InvTask::getInventoryDate, query.getEndDate());
        }
        if (StrUtil.isNotBlank(query.getCreatorName())) {
            // 创建人模糊查询需要先查用户表获取ID
            List<SysUser> users = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getRealName, query.getCreatorName())
                            .eq(SysUser::getIsDeleted, 0));
            Set<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toSet());
            if (!userIds.isEmpty()) {
                wrapper.in(InvTask::getCreatorId, userIds);
            } else {
                wrapper.eq(InvTask::getCreatorId, -1L);
            }
        }
    }

    /**
     * 拼接差异查询条件
     */
    private void applyDiffQueryFilters(LambdaQueryWrapper<InvDifference> wrapper, InvDifferenceQueryDTO query) {
        if (query.getTaskId() != null) {
            wrapper.eq(InvDifference::getTaskId, query.getTaskId());
        }
        if (StrUtil.isNotBlank(query.getTaskNo())) {
            // taskNo是@TableField(exist=false)展示字段，需先查inv_task获取taskId再过滤
            List<InvTask> matchedTasks = invTaskMapper.selectList(
                    new LambdaQueryWrapper<InvTask>()
                            .like(InvTask::getTaskNo, query.getTaskNo())
                            .eq(InvTask::getIsDeleted, 0));
            Set<Long> taskIds = matchedTasks.stream().map(InvTask::getId).collect(Collectors.toSet());
            if (!taskIds.isEmpty()) {
                wrapper.in(InvDifference::getTaskId, taskIds);
            } else {
                wrapper.eq(InvDifference::getTaskId, -1L);
            }
        }
        if (query.getDiffType() != null) {
            wrapper.eq(InvDifference::getDiffType, query.getDiffType());
        }
        if (StrUtil.isNotBlank(query.getAssetCode())) {
            wrapper.like(InvDifference::getAssetCode, query.getAssetCode());
        }
        if (query.getHandleStatus() != null) {
            wrapper.eq(InvDifference::getHandleStatus, query.getHandleStatus());
        }
        if (StrUtil.isNotBlank(query.getBeginDate())) {
            wrapper.ge(InvDifference::getCreateTime, query.getBeginDate() + " 00:00:00");
        }
        if (StrUtil.isNotBlank(query.getEndDate())) {
            wrapper.le(InvDifference::getCreateTime, query.getEndDate() + " 23:59:59");
        }
    }

    /**
     * 填充任务展示字段
     */
    private void fillTaskDisplayFields(List<InvTask> list) {
        if (list.isEmpty()) return;

        // 批量查询创建人姓名
        Set<Long> creatorIds = list.stream().map(InvTask::getCreatorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> userNameMap;
        if (!creatorIds.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectBatchIds(creatorIds);
            userNameMap = users.stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
        } else {
            userNameMap = Collections.emptyMap();
        }

        list.forEach(task -> {
            task.setCreatorName(userNameMap.getOrDefault(task.getCreatorId(), ""));
            task.setScopeTypeLabel(getScopeTypeLabel(task.getScopeType()));
            task.setStatusLabel(getTaskStatusLabel(task.getStatus()));
            // 计算完成率
            if (task.getTotalCount() != null && task.getTotalCount() > 0) {
                int rate = (int) Math.round(task.getCheckedCount() * 100.0 / task.getTotalCount());
                task.setCompletionRate(Math.min(rate, 100));
            } else {
                task.setCompletionRate(0);
            }
        });
    }

    /**
     * 填充明细展示字段
     */
    private void fillDetailDisplayFields(List<InvDetail> list) {
        if (list.isEmpty()) return;

        // 批量查询资产信息
        Set<Long> assetIds = list.stream().map(InvDetail::getAssetId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AssetInfo> assetMap;
        if (!assetIds.isEmpty()) {
            List<AssetInfo> assets = assetInfoMapper.selectBatchIds(assetIds);
            assetMap = assets.stream()
                    .collect(Collectors.toMap(AssetInfo::getId, a -> a, (a, b) -> a));
        } else {
            assetMap = Collections.emptyMap();
        }

        // 批量查询部门名称
        Set<Long> deptIds = assetMap.values().stream()
                .map(AssetInfo::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> deptNameMap;
        if (!deptIds.isEmpty()) {
            List<SysDept> depts = sysDeptMapper.selectBatchIds(deptIds);
            deptNameMap = depts.stream()
                    .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));
        } else {
            deptNameMap = Collections.emptyMap();
        }

        list.forEach(detail -> {
            AssetInfo asset = assetMap.get(detail.getAssetId());
            if (asset != null) {
                detail.setAssetCode(asset.getAssetCode());
                detail.setAssetName(asset.getAssetName());
                detail.setCategory(asset.getCategory());
                detail.setLocation(asset.getLocation());
                detail.setDeptName(deptNameMap.getOrDefault(asset.getDeptId(), ""));
            }
            detail.setBookStatusLabel(getAssetStatusLabel(detail.getBookStatus()));
            detail.setResultLabel(getResultLabel(detail.getInventoryResult()));
        });
    }

    /**
     * 填充差异展示字段
     */
    private void fillDiffDisplayFields(List<InvDifference> list) {
        if (list.isEmpty()) return;

        // 批量查询任务信息
        Set<Long> taskIds = list.stream().map(InvDifference::getTaskId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, InvTask> taskMap;
        if (!taskIds.isEmpty()) {
            List<InvTask> tasks = invTaskMapper.selectBatchIds(taskIds);
            taskMap = tasks.stream()
                    .collect(Collectors.toMap(InvTask::getId, t -> t, (a, b) -> a));
        } else {
            taskMap = Collections.emptyMap();
        }

        // 批量查询处理人姓名
        Set<Long> handlerIds = list.stream().map(InvDifference::getHandlerId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> handlerNameMap;
        if (!handlerIds.isEmpty()) {
            List<SysUser> handlers = sysUserMapper.selectBatchIds(handlerIds);
            handlerNameMap = handlers.stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
        } else {
            handlerNameMap = Collections.emptyMap();
        }

        list.forEach(diff -> {
            InvTask task = taskMap.get(diff.getTaskId());
            if (task != null) {
                diff.setTaskNo(task.getTaskNo());
                diff.setTaskName(task.getTaskName());
                diff.setInventoryDate(task.getInventoryDate() != null
                        ? task.getInventoryDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "");
            }
            diff.setDiffTypeLabel(getDiffTypeLabel(diff.getDiffType()));
            diff.setHandleStatusLabel(getHandleStatusLabel(diff.getHandleStatus()));
            diff.setHandlerName(handlerNameMap.getOrDefault(diff.getHandlerId(), ""));
        });
    }

    // ==================== 标签转换 ====================

    /** 范围类型 → 标签 */
    private String getScopeTypeLabel(String scopeType) {
        if (StrUtil.isBlank(scopeType)) return "";
        switch (scopeType) {
            case "ALL": return "全公司";
            case "DEPT": return "按部门";
            case "CATEGORY": return "按分类";
            default: return scopeType;
        }
    }

    /** 盘点任务状态 → 标签（PRD 5.5 盘点状态枚举） */
    private String getTaskStatusLabel(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "进行中";
            case 1: return "已完成";
            case 2: return "已取消";
            default: return String.valueOf(status);
        }
    }

    /** 资产状态 → 标签（PRD 5.5 资产状态枚举） */
    private String getAssetStatusLabel(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "闲置";
            case 1: return "在用";
            case 2: return "借用";
            case 3: return "维修";
            case 4: return "报废";
            default: return String.valueOf(status);
        }
    }

    /** 盘点结果 → 标签（PRD 5.5 盘点结果枚举） */
    private String getResultLabel(Integer result) {
        if (result == null) return "";
        switch (result) {
            case 0: return "盘盈";
            case 1: return "盘亏";
            case 2: return "正常";
            default: return String.valueOf(result);
        }
    }

    /** 差异类型 → 标签 */
    private String getDiffTypeLabel(Integer diffType) {
        if (diffType == null) return "";
        switch (diffType) {
            case 0: return "盘盈";
            case 1: return "盘亏";
            default: return String.valueOf(diffType);
        }
    }

    /** 处理状态 → 标签 */
    private String getHandleStatusLabel(Integer handleStatus) {
        if (handleStatus == null) return "";
        switch (handleStatus) {
            case 0: return "待处理";
            case 1: return "已处理";
            default: return String.valueOf(handleStatus);
        }
    }
}
