package com.example.eams.inventory.service;

import com.example.eams.common.result.PageResult;
import com.example.eams.inventory.dto.*;
import com.example.eams.inventory.entity.InvDetail;
import com.example.eams.inventory.entity.InvDifference;
import com.example.eams.inventory.entity.InvTask;

import java.util.List;

/**
 * 盘点管理服务接口（PRD 6.4）
 * <p>
 * 盘点全流程: 创建任务 → 执行盘点(逐项确认/盘盈登记) → 完成盘点(生成差异) → 差异处理
 */
public interface InventoryService {

    // ==================== 6.4.1 盘点任务管理 ====================

    /**
     * 盘点任务列表（分页）
     * <p>
     * 权限: 超级管理员/资产管理员(全部) / 部门管理员(仅本部门任务)
     */
    PageResult<InvTask> listTasks(InvTaskQueryDTO query);

    /**
     * 盘点任务详情
     */
    InvTask getTaskDetail(Long taskId);

    /**
     * 创建盘点任务（PRD 6.4.1 创建任务弹窗）
     * <p>
     * 1. Redis分布式锁防重复创建
     * 2. 检查同范围是否有进行中任务
     * 3. 批量更新范围内资产状态为"盘点中"
     * 4. 批量插入盘点明细
     * 5. 清除资产列表缓存
     */
    InvTask createTask(InvTaskCreateDTO dto);

    /**
     * 取消盘点任务（PRD 6.4.1 取消任务）
     * <p>
     * 1. 恢复所有资产原有状态（从inv_detail的book_status）
     * 2. 任务状态→已取消
     * 3. 清除资产列表缓存
     */
    void cancelTask(Long taskId);

    // ==================== 6.4.2 执行盘点 ====================

    /**
     * 获取盘点任务的明细列表（执行盘点页）
     * <p>
     * 包含任务摘要信息 + 资产明细（账面信息快照）
     */
    List<InvDetail> getTaskDetails(Long taskId);

    /**
     * 确认盘点明细（逐项/批量确认）（PRD 6.4.2）
     * <p>
     * Redis分布式锁防并发编辑同一任务
     * 更新明细的盘点结果、备注、确认状态
     */
    void confirmDetails(InvDetailConfirmDTO dto);

    /**
     * 盘盈资产登记入库（PRD 6.4.2 盘盈登记弹窗）
     * <p>
     * 1. Redis编码锁生成资产编码
     * 2. 资产信息入库（asset_info）
     * 3. 新增盘点明细（inventory_result=0盘盈）
     * 4. 更新任务已确认数
     */
    InvDetail registerSurplusAsset(InvSurplusAssetDTO dto);

    /**
     * 完成盘点（PRD 6.4.2 完成盘点）
     * <p>
     * 1. 校验所有资产已确认
     * 2. 汇总正常/盘盈/盘亏数量
     * 3. 盘盈资产已入库（在confirmDetails中处理）
     * 4. 盘亏记录生成差异（inv_difference）
     * 5. 正常资产恢复原状态
     * 6. 任务状态→已完成
     */
    void completeTask(Long taskId);

    // ==================== 6.4.3 盘点差异记录 ====================

    /**
     * 盘点差异列表（分页）
     * <p>
     * 超级管理员/资产管理员: 查看全部差异
     * 部门管理员: 仅查看本部门盘点任务的差异
     */
    PageResult<InvDifference> listDifferences(InvDifferenceQueryDTO query);

    /**
     * 差异详情
     */
    InvDifference getDifferenceDetail(Long id);

    /**
     * 批量标记差异已处理（PRD 6.4.3 标记已处理）
     */
    void handleDifferences(InvDifferenceHandleDTO dto);

    /**
     * 导出差异记录Excel
     */
    List<InvDifference> exportDifferences(InvDifferenceQueryDTO query);

    /**
     * 导出盘点任务Excel
     */
    List<InvTask> exportTasks(InvTaskQueryDTO query);
}
