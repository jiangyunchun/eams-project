package com.example.eams.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.eams.inventory.entity.InvTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 盘点任务 Mapper
 * <p>
 * 对应 inv_task 表（PRD 6.4.1 / 技术方案 4.3.9）
 */
public interface InvTaskMapper extends BaseMapper<InvTask> {

    /**
     * 查询指定前缀的最大任务编号
     * 用于生成新编号: PD-YYYYMMDD-XXX
     */
    @Select("SELECT MAX(task_no) FROM inv_task WHERE task_no LIKE CONCAT(#{prefix}, '%') AND is_deleted = 0")
    String selectMaxTaskNoByPrefix(@Param("prefix") String prefix);

    /**
     * 检查指定范围内是否存在进行中的盘点任务（status=0）
     * @param scopeType 范围类型
     * @param scopeValue 范围值
     * @return 进行中的任务计数
     */
    @Select("SELECT COUNT(*) FROM inv_task WHERE scope_type = #{scopeType} AND scope_value = #{scopeValue} AND status = 0 AND is_deleted = 0")
    int countInProgressByScope(@Param("scopeType") String scopeType, @Param("scopeValue") String scopeValue);
}
