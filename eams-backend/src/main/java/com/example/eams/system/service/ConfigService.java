package com.example.eams.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eams.common.exception.BusinessException;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.util.RedisUtil;
import com.example.eams.system.entity.SysConfig;
import com.example.eams.system.entity.SysConfigHistory;
import com.example.eams.system.mapper.SysConfigHistoryMapper;
import com.example.eams.system.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 系统参数配置服务
 * <p>
 * 参数修改后通过 Redis 实时生效，无需重启。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final SysConfigMapper configMapper;
    private final SysConfigHistoryMapper configHistoryMapper;

    private static final String CACHE_KEY = "eams:config:all";

    /**
     * 分页查询参数
     */
    public PageResult<SysConfig> list(String paramKey, String paramName, String paramGroup, int pageNum, int pageSize) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysConfig> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysConfig>()
                        .eq(SysConfig::getIsDeleted, 0);
        if (StrUtil.isNotBlank(paramKey)) wrapper.like(SysConfig::getParamKey, paramKey);
        if (StrUtil.isNotBlank(paramName)) wrapper.like(SysConfig::getParamName, paramName);
        if (StrUtil.isNotBlank(paramGroup)) wrapper.eq(SysConfig::getParamGroup, paramGroup);
        wrapper.orderByAsc(SysConfig::getSortOrder);

        IPage<SysConfig> page = configMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    /**
     * 新增参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void add(SysConfig config) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysConfig> check =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysConfig>()
                        .eq(SysConfig::getParamKey, config.getParamKey())
                        .eq(SysConfig::getIsDeleted, 0);
        if (configMapper.selectCount(check) > 0) {
            throw new BusinessException(400, "参数键名已存在");
        }
        configMapper.insert(config);
        refreshCache();
    }

    /**
     * 编辑参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void edit(SysConfig config) {
        SysConfig old = configMapper.selectById(config.getId());
        if (old == null) throw BusinessException.notFound("参数不存在");

        // 记录变更历史
        if (!Objects.equals(old.getParamValue(), config.getParamValue())) {
            SysConfigHistory history = new SysConfigHistory();
            history.setConfigId(config.getId());
            history.setParamKey(old.getParamKey());
            history.setOldValue(old.getParamValue());
            history.setNewValue(config.getParamValue());
            configHistoryMapper.insert(history);
        }

        configMapper.updateById(config);
        refreshCache();
    }

    /**
     * 删除参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysConfig config = configMapper.selectById(id);
        if (config == null) throw BusinessException.notFound("参数不存在");
        if (config.getIsSystem() == 1) {
            throw new BusinessException(400, "系统预置参数不可删除");
        }
        configMapper.deleteById(id);
        refreshCache();
    }

    /**
     * 重置为默认值
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetToDefault(Long id) {
        SysConfig config = configMapper.selectById(id);
        if (config == null) throw BusinessException.notFound("参数不存在");
        config.setParamValue(config.getDefaultValue());
        configMapper.updateById(config);
        refreshCache();
    }

    /**
     * 获取变更历史
     */
    public List<SysConfigHistory> getHistory(Long configId) {
        return configHistoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysConfigHistory>()
                        .eq(SysConfigHistory::getConfigId, configId)
                        .eq(SysConfigHistory::getIsDeleted, 0)
                        .orderByDesc(SysConfigHistory::getCreateTime));
    }

    /**
     * 读取参数（带Redis缓存）
     */
    public String getValue(String paramKey) {
        // 查Redis
        String val = RedisUtil.hget(CACHE_KEY, paramKey);
        if (val != null) return val;

        // 查MySQL
        SysConfig config = configMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysConfig>()
                        .eq(SysConfig::getParamKey, paramKey)
                        .eq(SysConfig::getStatus, 1) // 仅启用
                        .eq(SysConfig::getIsDeleted, 0));
        if (config != null) {
            RedisUtil.hset(CACHE_KEY, paramKey, config.getParamValue(), 86400);
            return config.getParamValue();
        }
        return null;
    }

    /**
     * 读取整型参数
     */
    public int getInt(String paramKey, int defaultValue) {
        String val = getValue(paramKey);
        if (val == null) return defaultValue;
        try { return Integer.parseInt(val); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    @PostConstruct
    public void refreshCache() {
        List<SysConfig> list = configMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysConfig>()
                        .eq(SysConfig::getIsDeleted, 0));
        Map<String, String> map = new LinkedHashMap<>();
        for (SysConfig c : list) {
            if (c.getStatus() == 1) {
                map.put(c.getParamKey(), c.getParamValue());
            }
        }
        if (!map.isEmpty()) {
            RedisUtil.hmset(CACHE_KEY, map, 86400);
        }
        log.info("ConfigService: 系统参数缓存已刷新，共{}项", map.size());
    }
}
