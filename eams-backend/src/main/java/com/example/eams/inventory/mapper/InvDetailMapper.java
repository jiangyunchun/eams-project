package com.example.eams.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.eams.inventory.entity.InvDetail;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 盘点明细 Mapper
 * <p>
 * 对应 inv_detail 表（PRD 6.4.2 / 技术方案 4.3.9）
 */
public interface InvDetailMapper extends BaseMapper<InvDetail> {

    /**
     * 统计某盘点任务下未确认的明细数量
     */
    @Select("SELECT COUNT(*) FROM inv_detail WHERE task_id = #{taskId} AND is_confirmed = 0 AND is_deleted = 0")
    int countUnconfirmedByTaskId(@Param("taskId") Long taskId);

    /**
     * 统计某盘点任务下各盘点结果的数量
     * @param taskId 任务ID
     * @param inventoryResult 盘点结果: 0-盘盈, 1-盘亏, 2-正常
     */
    @Select("SELECT COUNT(*) FROM inv_detail WHERE task_id = #{taskId} AND inventory_result = #{inventoryResult} AND is_deleted = 0")
    int countByTaskIdAndResult(@Param("taskId") Long taskId, @Param("inventoryResult") Integer inventoryResult);
}
