package com.example.eams.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.eams.procurement.entity.ProcurementOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 采购入库记录 Mapper
 */
public interface ProcurementOrderMapper extends BaseMapper<ProcurementOrder> {

    /**
     * 查询指定类别的最大资产编码流水号
     */
    @Select("SELECT MAX(asset_code) FROM asset_info WHERE asset_code LIKE CONCAT('AS-', #{categoryShort}, '-', #{yyMM}, '%') AND is_deleted = 0")
    String selectMaxAssetCodeByPrefix(@Param("categoryShort") String categoryShort, @Param("yyMM") String yyMM);

    /**
     * 统计供应商关联的采购记录数（未删除）
     */
    @Select("SELECT COUNT(*) FROM proc_order WHERE supplier_id = #{supplierId} AND is_deleted = 0")
    int countBySupplierId(@Param("supplierId") Long supplierId);
}
