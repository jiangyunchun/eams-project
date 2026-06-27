package com.example.eams.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eams.common.result.PageResult;
import com.example.eams.system.entity.SysOperationLog;
import com.example.eams.system.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 操作日志查询服务
 */
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final SysOperationLogMapper operationLogMapper;

    /**
     * 分页查询操作日志
     */
    public PageResult<SysOperationLog> list(String operator, String module,
                                            String actionType,
                                            LocalDateTime beginTime, LocalDateTime endTime,
                                            int pageNum, int pageSize) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(operator)) {
            wrapper.like(SysOperationLog::getOperator, operator);
        }
        if (StrUtil.isNotBlank(module)) {
            wrapper.eq(SysOperationLog::getModule, module);
        }
        if (StrUtil.isNotBlank(actionType)) {
            wrapper.eq(SysOperationLog::getActionType, actionType);
        }
        if (beginTime != null) {
            wrapper.ge(SysOperationLog::getCreateTime, beginTime);
        }
        if (endTime != null) {
            wrapper.le(SysOperationLog::getCreateTime, endTime);
        }

        wrapper.orderByDesc(SysOperationLog::getCreateTime);

        IPage<SysOperationLog> page = operationLogMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    /**
     * 查询日志详情
     */
    public SysOperationLog getDetail(Long id) {
        return operationLogMapper.selectById(id);
    }
}
