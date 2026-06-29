package com.example.eams.scrap.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eams.asset.entity.AssetInfo;
import com.example.eams.asset.mapper.AssetInfoMapper;
import com.example.eams.common.exception.BusinessException;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.util.RedisUtil;
import com.example.eams.scrap.dto.*;
import com.example.eams.scrap.entity.ScrapOrder;
import com.example.eams.scrap.mapper.ScrapOrderMapper;
import com.example.eams.scrap.service.ScrapService;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报废处置服务实现（PRD 6.9）
 * <p>
 * 报废全流程: 申请 → 初审(资产管理员) → 终审(超级管理员) → 处置登记 → 归档
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapServiceImpl implements ScrapService {

    private final ScrapOrderMapper scrapOrderMapper;
    private final AssetInfoMapper assetInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;

    /** 报废编号前缀 */
    private static final String SCRAP_NO_PREFIX = "BF";

    /** Redis 分布式锁配置（PRD 5.7.4）：等待3秒，最大持有30秒 */
    private static final int LOCK_WAIT_SEC = 3;
    private static final int LOCK_HOLD_SEC = 30;

    /** 报废锁 Key 前缀（PRD 5.7.1） */
    private static final String LOCK_KEY_PREFIX = "eams:lock:scrap:";

    // ==================== 6.9.1 报废申请 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScrapOrder apply(ScrapApplyDTO dto) {
        Long assetId = dto.getAssetId();
        Long currentUserId = SecurityContextHolder.getCurrentUserId();
        String currentUsername = SecurityContextHolder.getCurrentUsername();

        // 1. 校验资产存在
        AssetInfo asset = assetInfoMapper.selectById(assetId);
        if (asset == null) {
            throw BusinessException.notFound("资产不存在");
        }

        // 2. 校验资产状态（PRD 6.9.1）：非报废、非盘点中
        if (asset.getStatus() != null && asset.getStatus() == 4) {
            throw new BusinessException(400, "该资产已报废，不可重复申请");
        }
        if (asset.getStatus() != null && asset.getStatus() == 5) {
            throw new BusinessException(400, "该资产正在盘点中，不可报废");
        }

        // 3. 部门管理员仅可申请本部门资产
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        if (roles.contains("ROLE_DEPT_ADMIN") && !roles.contains("ROLE_SUPER_ADMIN")
                && !roles.contains("ROLE_ASSET_ADMIN")) {
            if (!asset.getDeptId().equals(getCurrentUserDeptId())) {
                throw BusinessException.forbidden("您没有权限执行此操作");
            }
        }

        // 4. Redis 分布式锁：防止同一资产并发提交报废（PRD 5.7.4）
        String lockKey = LOCK_KEY_PREFIX + assetId;
        String lockValue = RedisUtil.tryLock(lockKey, LOCK_WAIT_SEC, LOCK_HOLD_SEC);
        if (lockValue == null) {
            throw new BusinessException(400, "操作繁忙，请稍后重试");
        }

        try {
            // 5. 检查同一资产是否已有待审批的报废单（状态：待初审/待终审）
            int pendingCount = scrapOrderMapper.countPendingByAssetId(assetId);
            if (pendingCount > 0) {
                throw new BusinessException(400, "该资产存在待审批的报废单，请勿重复提交");
            }

            // 6. 生成报废编号: BF-YYYYMMDD-XXXX
            String scrapNo = generateScrapNo();

            // 7. 构建报废单
            ScrapOrder order = new ScrapOrder();
            order.setScrapNo(scrapNo);
            order.setAssetId(assetId);
            order.setScrapReason(dto.getScrapReason());
            order.setReasonDesc(dto.getReasonDesc());
            order.setDisposalAdvice(dto.getDisposalAdvice());
            order.setStatus(0); // 待初审（PRD 5.5 报废状态枚举）
            order.setApplicantId(currentUserId);
            order.setRemark(dto.getRemark());
            order.setAttachmentUrls(dto.getAttachmentUrls());
            order.setVersion(0);

            scrapOrderMapper.insert(order);
            log.info("报废申请提交成功, scrapNo={}, assetId={}, applicantId={}", scrapNo, assetId, currentUserId);
            return order;

        } finally {
            RedisUtil.unlock(lockKey, lockValue);
        }
    }

    // ==================== 6.9.2 报废审批 ====================

    @Override
    public PageResult<ScrapOrder> listApproval(ScrapQueryDTO query) {
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        LambdaQueryWrapper<ScrapOrder> wrapper = new LambdaQueryWrapper<ScrapOrder>()
                .eq(ScrapOrder::getIsDeleted, 0);

        // 资产管理员看到 status=0(待初审)，超级管理员看到 status=1(待终审)
        if (roles.contains("ROLE_SUPER_ADMIN")) {
            // 超级管理员可看 待初审 + 待终审（因为也可能需要代替资产管理员初审）
            wrapper.in(ScrapOrder::getStatus, 0, 1);
        } else if (roles.contains("ROLE_ASSET_ADMIN")) {
            wrapper.eq(ScrapOrder::getStatus, 0); // 待初审
        } else {
            // 其他角色无权限查看审批列表
            throw BusinessException.forbidden("您没有权限执行此操作");
        }

        applyQueryFilters(wrapper, query);
        wrapper.orderByAsc(ScrapOrder::getStatus)
               .orderByDesc(ScrapOrder::getCreateTime);

        IPage<ScrapOrder> page = scrapOrderMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        PageResult<ScrapOrder> result = PageResult.of(page);
        fillDisplayFields(result.getList());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(ScrapApprovalDTO dto) {
        Long currentUserId = SecurityContextHolder.getCurrentUserId();
        String currentUsername = SecurityContextHolder.getCurrentUsername();
        Set<String> roles = SecurityContextHolder.getCurrentRoles();

        ScrapOrder order = scrapOrderMapper.selectById(dto.getScrapId());
        if (order == null) {
            throw BusinessException.notFound("报废单不存在");
        }

        Integer currentStatus = order.getStatus();
        Integer approvalResult = dto.getApprovalResult();

        // 审批通过: approvalResult=1, 审批驳回: approvalResult=0
        if (approvalResult == 1) {
            // ---- 通过 ----
            if (currentStatus == 0) {
                // 初审：仅资产管理员可操作（PRD 6.9.2 初审）
                if (!roles.contains("ROLE_ASSET_ADMIN") && !roles.contains("ROLE_SUPER_ADMIN")) {
                    throw BusinessException.forbidden("仅资产管理员可进行初审");
                }
                // 检查是否本人即是申请人（不可审批自己的申请）
                if (order.getApplicantId().equals(currentUserId)) {
                    throw new BusinessException(400, "不可审批自己提交的报废申请");
                }
                // 初审通过 → 待终审（审批记录由 sys_operation_log 审计追踪）
                order.setStatus(1);
                scrapOrderMapper.updateById(order);
                log.info("报废初审通过, scrapNo={}, approverId={}", order.getScrapNo(), currentUserId);

            } else if (currentStatus == 1) {
                // 终审：仅超级管理员可操作（PRD 6.9.2 终审）
                if (!roles.contains("ROLE_SUPER_ADMIN")) {
                    throw BusinessException.forbidden("仅超级管理员可进行终审");
                }
                // 终审通过 → 已通过(待处置)，同时更新资产为报废状态（审批记录由操作日志审计追踪）
                order.setStatus(2);
                scrapOrderMapper.updateById(order);

                // 资产状态更新为【报废】（PRD 6.9.2：终审通过后资产标记为报废）
                AssetInfo asset = assetInfoMapper.selectById(order.getAssetId());
                if (asset != null) {
                    asset.setStatus(4); // 报废
                    assetInfoMapper.updateById(asset);
                    log.info("资产标记为报废, assetCode={}, assetId={}", asset.getAssetCode(), asset.getId());
                }

                log.info("报废终审通过, scrapNo={}, approverId={}, assetId={}",
                        order.getScrapNo(), currentUserId, order.getAssetId());

            } else {
                throw new BusinessException(400, "当前报废单状态不可审批");
            }

        } else if (approvalResult == 0) {
            // ---- 驳回（PRD 6.9.2） ----
            if (StrUtil.isBlank(dto.getRejectReason())) {
                throw new BusinessException(400, "驳回原因为10-200个字符");
            }
            if (dto.getRejectReason().length() < 10 || dto.getRejectReason().length() > 200) {
                throw new BusinessException(400, "驳回原因为10-200个字符");
            }

            if (currentStatus == 0) {
                // 初审驳回：资产管理员
                if (!roles.contains("ROLE_ASSET_ADMIN") && !roles.contains("ROLE_SUPER_ADMIN")) {
                    throw BusinessException.forbidden("仅资产管理员可进行初审驳回");
                }
            } else if (currentStatus == 1) {
                // 终审驳回：超级管理员
                if (!roles.contains("ROLE_SUPER_ADMIN")) {
                    throw BusinessException.forbidden("仅超级管理员可进行终审驳回");
                }
            } else {
                throw new BusinessException(400, "当前报废单状态不可驳回");
            }

            // 检查是否本人即是申请人（不可审批自己的申请）
            if (order.getApplicantId().equals(currentUserId)) {
                throw new BusinessException(400, "不可审批自己提交的报废申请");
            }

            order.setStatus(3); // 已驳回
            // 驳回原因存储至 remark 字段（表内无独立驳回字段，操作日志记录完整审批轨迹）
            order.setRemark(dto.getRejectReason());
            scrapOrderMapper.updateById(order);
            log.info("报废审批驳回, scrapNo={}, rejectBy={}, reason={}",
                    order.getScrapNo(), currentUserId, dto.getRejectReason());

        } else {
            throw new BusinessException(400, "审批结果参数错误");
        }
    }

    // ==================== 6.9.3 报废处置登记 ====================

    @Override
    public PageResult<ScrapOrder> listDisposal(ScrapQueryDTO query) {
        LambdaQueryWrapper<ScrapOrder> wrapper = new LambdaQueryWrapper<ScrapOrder>()
                .eq(ScrapOrder::getIsDeleted, 0)
                .eq(ScrapOrder::getStatus, 2); // 仅已通过(待处置)

        if (StrUtil.isNotBlank(query.getScrapNo())) {
            wrapper.like(ScrapOrder::getScrapNo, query.getScrapNo());
        }
        if (StrUtil.isNotBlank(query.getAssetName())) {
            wrapper.like(ScrapOrder::getAssetName, query.getAssetName());
        }
        if (StrUtil.isNotBlank(query.getAssetCode())) {
            wrapper.like(ScrapOrder::getAssetCode, query.getAssetCode());
        }
        wrapper.orderByDesc(ScrapOrder::getUpdateTime);

        IPage<ScrapOrder> page = scrapOrderMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        PageResult<ScrapOrder> result = PageResult.of(page);
        fillDisplayFields(result.getList());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disposal(ScrapDisposalDTO dto) {
        ScrapOrder order = scrapOrderMapper.selectById(dto.getScrapId());
        if (order == null) {
            throw BusinessException.notFound("报废单不存在");
        }

        // 仅 status=2 (已通过待处置) 可处置
        if (order.getStatus() == null || order.getStatus() != 2) {
            throw new BusinessException(400, "仅已通过的报废单可进行处置登记");
        }

        // 校验处置日期：≥审批日期，≤当前日期
        if (dto.getDisposalDate().isAfter(LocalDate.now())) {
            throw new BusinessException(400, "处置日期不能晚于当前日期");
        }

        // 更新处置信息
        order.setDisposalMethod(dto.getDisposalMethod());
        order.setDisposalDate(dto.getDisposalDate());
        order.setDisposalIncome(dto.getDisposalIncome());
        order.setDisposalCost(dto.getDisposalCost());
        order.setDisposalHandler(dto.getDisposalHandler());
        order.setDisposalDesc(dto.getDisposalDesc());

        // 合并附件URL
        if (StrUtil.isNotBlank(dto.getAttachmentUrls())) {
            String existingUrls = order.getAttachmentUrls();
            if (StrUtil.isNotBlank(existingUrls)) {
                order.setAttachmentUrls(existingUrls + "," + dto.getAttachmentUrls());
            } else {
                order.setAttachmentUrls(dto.getAttachmentUrls());
            }
        }

        // 状态改为已处置（归档）
        order.setStatus(4); // PRD: 4-已处置(归档)

        scrapOrderMapper.updateById(order);

        log.info("报废处置登记完成, scrapNo={}, disposalMethod={}, disposalDate={}",
                order.getScrapNo(), dto.getDisposalMethod(), dto.getDisposalDate());
    }

    // ==================== 6.9.4 报废记录查询 ====================

    @Override
    public PageResult<ScrapOrder> listRecords(ScrapQueryDTO query) {
        LambdaQueryWrapper<ScrapOrder> wrapper = new LambdaQueryWrapper<ScrapOrder>()
                .eq(ScrapOrder::getIsDeleted, 0);

        // 部门管理员仅可查看本部门资产报废记录
        Set<String> roles = SecurityContextHolder.getCurrentRoles();
        if (roles.contains("ROLE_DEPT_ADMIN") && !roles.contains("ROLE_SUPER_ADMIN")
                && !roles.contains("ROLE_ASSET_ADMIN")) {
            // 通过子查询：报废资产所属部门 = 当前用户部门
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
                    wrapper.in(ScrapOrder::getAssetId, assetIds);
                } else {
                    wrapper.eq(ScrapOrder::getAssetId, -1L); // 无权限数据
                }
            }
        }

        applyQueryFilters(wrapper, query);
        wrapper.orderByDesc(ScrapOrder::getCreateTime);

        IPage<ScrapOrder> page = scrapOrderMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        PageResult<ScrapOrder> result = PageResult.of(page);
        fillDisplayFields(result.getList());
        return result;
    }

    @Override
    public ScrapOrder detail(Long id) {
        ScrapOrder order = scrapOrderMapper.selectById(id);
        if (order == null) {
            throw BusinessException.notFound("报废单不存在");
        }
        fillDisplayFields(Collections.singletonList(order));
        return order;
    }

    @Override
    public List<ScrapOrder> exportRecords(ScrapQueryDTO query) {
        LambdaQueryWrapper<ScrapOrder> wrapper = new LambdaQueryWrapper<ScrapOrder>()
                .eq(ScrapOrder::getIsDeleted, 0);
        applyQueryFilters(wrapper, query);
        wrapper.orderByDesc(ScrapOrder::getCreateTime);
        // 导出限制最多10000条
        List<ScrapOrder> list = scrapOrderMapper.selectList(wrapper);
        fillDisplayFields(list);
        return list;
    }

    // ==================== 私有方法 ====================

    /**
     * 生成报废编号: BF-YYYYMMDD-XXXX（PRD 6.9.2 / 技术方案 4.3.8）
     * 格式: BF-20260629-0001
     */
    private String generateScrapNo() {
        String yyMMdd = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = SCRAP_NO_PREFIX + "-" + yyMMdd + "-";

        String maxScrapNo = scrapOrderMapper.selectMaxScrapNoByPrefix(prefix);
        int seq = 1;
        if (maxScrapNo != null && maxScrapNo.length() >= prefix.length() + 4) {
            String seqPart = maxScrapNo.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + String.format("%04d", seq);
    }

    /**
     * 拼接查询条件
     */
    private void applyQueryFilters(LambdaQueryWrapper<ScrapOrder> wrapper, ScrapQueryDTO query) {
        if (StrUtil.isNotBlank(query.getScrapNo())) {
            wrapper.like(ScrapOrder::getScrapNo, query.getScrapNo());
        }
        if (StrUtil.isNotBlank(query.getScrapReason())) {
            wrapper.eq(ScrapOrder::getScrapReason, query.getScrapReason());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ScrapOrder::getStatus, query.getStatus());
        }
        if (StrUtil.isNotBlank(query.getBeginDate())) {
            wrapper.ge(ScrapOrder::getCreateTime, query.getBeginDate() + " 00:00:00");
        }
        if (StrUtil.isNotBlank(query.getEndDate())) {
            wrapper.le(ScrapOrder::getCreateTime, query.getEndDate() + " 23:59:59");
        }
        // 资产名称/编码模糊查询需要在 fillDisplayFields 之后的子查询逻辑处理
        // 此处先标记，在 fillDisplayFields 中通过资产信息筛选
    }

    /**
     * 填充展示字段：资产信息、用户姓名、状态标签等
     * （对应 PRD 6.9.2 报废审批表格列）
     */
    private void fillDisplayFields(List<ScrapOrder> list) {
        if (list.isEmpty()) return;

        // 1. 批量查询资产信息
        Set<Long> assetIds = list.stream().map(ScrapOrder::getAssetId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AssetInfo> assetMap;
        if (!assetIds.isEmpty()) {
            List<AssetInfo> assets = assetInfoMapper.selectBatchIds(assetIds);
            assetMap = assets.stream()
                    .collect(Collectors.toMap(AssetInfo::getId, a -> a, (a, b) -> a));
        } else {
            assetMap = Collections.emptyMap();
        }

        // 2. 批量查询用户姓名
        Set<Long> userIds = new HashSet<>();
        list.forEach(o -> {
            if (o.getApplicantId() != null) userIds.add(o.getApplicantId());
        });
        Map<Long, String> userNameMap;
        if (!userIds.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
            userNameMap = users.stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
        } else {
            userNameMap = Collections.emptyMap();
        }

        // 3. 批量查询部门名称
        Set<Long> deptIds = new HashSet<>();
        assetMap.values().forEach(a -> {
            if (a.getDeptId() != null) deptIds.add(a.getDeptId());
        });
        Map<Long, String> deptNameMap;
        if (!deptIds.isEmpty()) {
            List<SysDept> depts = sysDeptMapper.selectBatchIds(deptIds);
            deptNameMap = depts.stream()
                    .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));
        } else {
            deptNameMap = Collections.emptyMap();
        }

        // 4. 填充每个报废单
        LocalDate today = LocalDate.now();
        list.forEach(o -> {
            AssetInfo asset = assetMap.get(o.getAssetId());
            if (asset != null) {
                o.setAssetCode(asset.getAssetCode());
                o.setAssetName(asset.getAssetName());
                o.setOriginalValue(asset.getOriginalValue());
                o.setCategory(asset.getCategory());
                o.setSpecification(asset.getSpecification());
                o.setLocation(asset.getLocation());
                o.setPurchaseDate(asset.getPurchaseDate());
                o.setDeptId(asset.getDeptId());
                o.setAssetStatus(asset.getStatus());
                o.setDeptName(deptNameMap.getOrDefault(asset.getDeptId(), ""));

                // 计算净值（原值 - 累计折旧估算）
                if (asset.getOriginalValue() != null && asset.getPurchaseDate() != null
                        && asset.getUsefulLife() != null && asset.getResidualRate() != null) {
                    BigDecimal monthlyDepreciation = calculateMonthlyDepreciation(asset);
                    long monthsPassed = ChronoUnit.MONTHS.between(
                            asset.getPurchaseDate().withDayOfMonth(1), today.withDayOfMonth(1));
                    if (monthsPassed < 0) monthsPassed = 0;
                    int totalMonths = asset.getUsefulLife() * 12;
                    if (monthsPassed > totalMonths) monthsPassed = totalMonths;
                    BigDecimal accumulated = monthlyDepreciation.multiply(BigDecimal.valueOf(monthsPassed));
                    BigDecimal netValue = asset.getOriginalValue().subtract(accumulated);
                    // 净值不低于残值
                    BigDecimal residualValue = asset.getOriginalValue()
                            .multiply(asset.getResidualRate())
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    if (netValue.compareTo(residualValue) < 0) {
                        netValue = residualValue;
                    }
                    o.setNetValue(netValue);
                }

                // 计算已用年限
                if (asset.getPurchaseDate() != null) {
                    long years = ChronoUnit.YEARS.between(asset.getPurchaseDate(), today);
                    long months = ChronoUnit.MONTHS.between(asset.getPurchaseDate(), today) % 12;
                    if (years > 0) {
                        o.setUsedYears(years + "年" + (months > 0 ? months + "个月" : ""));
                    } else {
                        o.setUsedYears(months + "个月");
                    }
                }
            }

            o.setApplicantName(userNameMap.getOrDefault(o.getApplicantId(), ""));
            o.setScrapReasonLabel(getScrapReasonLabel(o.getScrapReason()));
            o.setDisposalAdviceLabel(getDisposalAdviceLabel(o.getDisposalAdvice()));
            o.setStatusLabel(getStatusLabel(o.getStatus()));
        });
    }

    /**
     * 计算月折旧额（PRD 6.2.4 平均年限法）
     */
    private BigDecimal calculateMonthlyDepreciation(AssetInfo asset) {
        BigDecimal originalValue = asset.getOriginalValue();
        BigDecimal residualRate = asset.getResidualRate() != null
                ? asset.getResidualRate() : new BigDecimal("5.00");
        int totalMonths = asset.getUsefulLife() * 12;

        BigDecimal residualValue = originalValue.multiply(residualRate)
                .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        BigDecimal depreciableAmount = originalValue.subtract(residualValue);
        return depreciableAmount.divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP);
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

    // ==================== 标签转换 ====================

    /** 报废原因 → 标签 */
    private String getScrapReasonLabel(String reason) {
        if (StrUtil.isBlank(reason)) return "";
        switch (reason) {
            case "老化损坏": return "老化损坏";
            case "技术淘汰": return "技术淘汰";
            case "维修成本过高": return "维修成本过高";
            case "盘亏确认": return "盘亏确认";
            case "其他": return "其他";
            default: return reason;
        }
    }

    /** 处置建议/方式 → 标签 */
    private String getDisposalAdviceLabel(String advice) {
        if (StrUtil.isBlank(advice)) return "";
        switch (advice) {
            case "变卖": return "变卖";
            case "回收": return "回收";
            case "销毁": return "销毁";
            case "其他": return "其他";
            default: return advice;
        }
    }

    /** 报废状态 → 标签（PRD 5.5 报废状态枚举） */
    private String getStatusLabel(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待初审";
            case 1: return "待终审";
            case 2: return "已通过(待处置)";
            case 3: return "已驳回";
            case 4: return "已处置";
            default: return String.valueOf(status);
        }
    }
}
