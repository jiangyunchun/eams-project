package com.example.eams.scrap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.eams.scrap.entity.ScrapOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 报废单 Mapper
 * <p>
 * 对应 scrap_order 表（PRD 6.9 / 技术方案 4.3.8）
 */
public interface ScrapOrderMapper extends BaseMapper<ScrapOrder> {

    /**
     * 查询指定前缀的最大报废编号
     * 用于生成新编号: BF-YYYYMMDD-XXXX
     */
    @Select("SELECT MAX(scrap_no) FROM scrap_order WHERE scrap_no LIKE CONCAT(#{prefix}, '%') AND is_deleted = 0")
    String selectMaxScrapNoByPrefix(@Param("prefix") String prefix);

    /**
     * 检查指定资产是否存在待审批的报废单
     * (status = 0 待初审 或 status = 1 待终审)
     */
    @Select("SELECT COUNT(*) FROM scrap_order WHERE asset_id = #{assetId} AND status IN (0, 1) AND is_deleted = 0")
    int countPendingByAssetId(@Param("assetId") Long assetId);
}
