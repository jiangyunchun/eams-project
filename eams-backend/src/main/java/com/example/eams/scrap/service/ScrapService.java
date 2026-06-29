package com.example.eams.scrap.service;

import com.example.eams.common.result.PageResult;
import com.example.eams.scrap.dto.*;
import com.example.eams.scrap.entity.ScrapOrder;

import java.util.List;

/**
 * 报废处置服务接口（PRD 6.9）
 */
public interface ScrapService {

    /**
     * 报废申请（PRD 6.9.1）
     * <p>
     * 权限: 超级管理员/资产管理员（全部）/ 部门管理员（仅本部门资产）
     */
    ScrapOrder apply(ScrapApplyDTO dto);

    /**
     * 分页查询报废审批列表（PRD 6.9.2）
     * <p>
     * 资产管理员看全部待初审单据，超级管理员看全部待终审单据
     */
    PageResult<ScrapOrder> listApproval(ScrapQueryDTO query);

    /**
     * 报废审批：初审/终审通过或驳回（PRD 6.9.2）
     * <p>
     * 初审: ROLE_ASSET_ADMIN
     * 终审: ROLE_SUPER_ADMIN
     */
    void approve(ScrapApprovalDTO dto);

    /**
     * 分页查询待处置单据列表（PRD 6.9.3）
     * <p>
     * 仅展示 status=2 (已通过待处置) 的报废单
     */
    PageResult<ScrapOrder> listDisposal(ScrapQueryDTO query);

    /**
     * 报废处置登记（PRD 6.9.3）
     * <p>
     * 处置完成后: status→已处置，资产归档
     */
    void disposal(ScrapDisposalDTO dto);

    /**
     * 分页查询报废记录（PRD 6.9.4）
     * <p>
     * 超级管理员/资产管理员全部记录，部门管理员本部门记录
     */
    PageResult<ScrapOrder> listRecords(ScrapQueryDTO query);

    /**
     * 报废单详情
     */
    ScrapOrder detail(Long id);

    /**
     * 导出报废记录 Excel（PRD 6.9.4）
     */
    List<ScrapOrder> exportRecords(ScrapQueryDTO query);
}
