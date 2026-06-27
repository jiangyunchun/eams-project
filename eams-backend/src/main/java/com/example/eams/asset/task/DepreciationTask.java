package com.example.eams.asset.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eams.asset.entity.AssetDepreciation;
import com.example.eams.asset.entity.AssetInfo;
import com.example.eams.asset.mapper.AssetDepreciationMapper;
import com.example.eams.asset.mapper.AssetInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 折旧计提定时任务
 * 每月1日凌晨2:00执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DepreciationTask {

    private final AssetInfoMapper assetMapper;
    private final AssetDepreciationMapper depreciationMapper;

    @Scheduled(cron = "0 0 2 1 * ?")
    @Transactional(rollbackFor = Exception.class)
    public void executeMonthlyDepreciation() {
        log.info("折旧计提任务开始执行...");

        // 查询需计提折旧的资产（非报废、非盘点中、未提满）
        List<AssetInfo> assets = assetMapper.selectList(
                new LambdaQueryWrapper<AssetInfo>()
                        .eq(AssetInfo::getIsDeleted, 0)
                        .ne(AssetInfo::getStatus, 4)  // 非报废
                        .ne(AssetInfo::getStatus, 5)); // 非盘点中

        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        int count = 0;

        for (AssetInfo asset : assets) {
            try {
                // 检查是否已计提
                Long exists = depreciationMapper.selectCount(
                        new LambdaQueryWrapper<AssetDepreciation>()
                                .eq(AssetDepreciation::getAssetId, asset.getId())
                                .eq(AssetDepreciation::getDepreciationMonth, currentMonth));
                if (exists > 0) continue;

                // 从采购次月开始计提
                LocalDate startMonth = asset.getPurchaseDate().plusMonths(1).withDayOfMonth(1);
                if (LocalDate.now().isBefore(startMonth)) continue;

                // 计算月折旧额
                BigDecimal rate = asset.getResidualRate() != null ? asset.getResidualRate() : new BigDecimal("5");
                int life = asset.getUsefulLife() != null ? asset.getUsefulLife() : 1;
                BigDecimal monthlyAmount = asset.getOriginalValue()
                        .multiply(BigDecimal.ONE.subtract(rate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)))
                        .divide(new BigDecimal(life * 12), 2, RoundingMode.HALF_UP);

                // 查已有折旧记录数
                long executedMonths = depreciationMapper.selectCount(
                        new LambdaQueryWrapper<AssetDepreciation>()
                                .eq(AssetDepreciation::getAssetId, asset.getId()));

                BigDecimal accumulated = monthlyAmount.multiply(new BigDecimal(executedMonths + 1));
                BigDecimal netValue = asset.getOriginalValue().subtract(accumulated);
                BigDecimal residualValue = asset.getOriginalValue()
                        .multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                // 净值 <= 残值时停止
                if (netValue.compareTo(residualValue) <= 0) continue;

                AssetDepreciation record = new AssetDepreciation();
                record.setAssetId(asset.getId());
                record.setDepreciationMonth(currentMonth);
                record.setMonthlyAmount(monthlyAmount);
                record.setAccumulated(accumulated);
                record.setNetValue(netValue);
                record.setStatus(1);
                depreciationMapper.insert(record);
                count++;
            } catch (Exception e) {
                log.error("折旧计提失败 assetId={}", asset.getId(), e);
            }
        }

        log.info("折旧计提任务完成，共处理 {} 项资产", count);
    }
}
