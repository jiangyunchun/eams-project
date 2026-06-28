package com.example.eams.requisition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.eams.requisition.entity.RequisitionOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 领用申请单 Mapper（PRD 6.3）
 */
public interface RequisitionOrderMapper extends BaseMapper<RequisitionOrder> {

    /**
     * 查询当天最大申请编号
     */
    @Select("SELECT MAX(apply_no) FROM req_order WHERE apply_no LIKE CONCAT('RY-', #{yyMMdd}, '%') AND is_deleted = 0")
    String selectMaxApplyNoByPrefix(@Param("yyMMdd") String yyMMdd);

    /**
     * 检查同一资产+同一申请人是否有待审批记录
     */
    @Select("SELECT COUNT(*) FROM req_order WHERE asset_id = #{assetId} AND applicant_id = #{applicantId} AND status IN (0, 1) AND is_deleted = 0")
    int countPendingByAssetAndApplicant(@Param("assetId") Long assetId, @Param("applicantId") Long applicantId);
}
