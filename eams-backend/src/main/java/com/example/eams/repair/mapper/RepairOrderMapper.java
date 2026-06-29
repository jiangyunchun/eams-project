package com.example.eams.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.eams.repair.entity.RepairOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface RepairOrderMapper extends BaseMapper<RepairOrder> {

    @Select("SELECT MAX(repair_no) FROM repair_order WHERE repair_no LIKE CONCAT('WX-', #{yyMMdd}, '%') AND is_deleted = 0")
    String selectMaxRepairNoByPrefix(@Param("yyMMdd") String yyMMdd);

    @Select("SELECT COUNT(*) FROM repair_order WHERE asset_id = #{assetId} AND repair_status IN (0, 1) AND is_deleted = 0")
    int countPendingByAssetId(@Param("assetId") Long assetId);
}
