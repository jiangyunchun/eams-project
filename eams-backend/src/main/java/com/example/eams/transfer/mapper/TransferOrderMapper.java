package com.example.eams.transfer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.eams.transfer.entity.TransferOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 资产调拨 Mapper（PRD 6.7）
 */
public interface TransferOrderMapper extends BaseMapper<TransferOrder> {

    /** 查询当天最大调拨编号 */
    @Select("SELECT MAX(transfer_no) FROM trans_order WHERE transfer_no LIKE CONCAT('DB-', #{yyMMdd}, '%') AND is_deleted = 0")
    String selectMaxTransferNoByPrefix(@Param("yyMMdd") String yyMMdd);

    /** 检查同一资产是否有在途调拨单 */
    @Select("SELECT COUNT(*) FROM trans_order WHERE asset_id = #{assetId} AND status IN (0, 1) AND is_deleted = 0")
    int countPendingByAssetId(@Param("assetId") Long assetId);
}
