package com.example.eams.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.eams.asset.entity.AssetInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 资产信息 Mapper
 */
public interface AssetInfoMapper extends BaseMapper<AssetInfo> {

    /** 查询当天指定类别最大资产编码 */
    @Select("SELECT MAX(asset_code) FROM asset_info WHERE asset_code LIKE CONCAT('AS-', #{categoryShort}, '-', #{yyMM}, '%') AND is_deleted = 0")
    String selectMaxCodeByPrefix(@Param("categoryShort") String categoryShort, @Param("yyMM") String yyMM);
}
