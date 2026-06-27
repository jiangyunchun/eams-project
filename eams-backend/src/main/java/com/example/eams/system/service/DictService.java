package com.example.eams.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.eams.common.exception.BusinessException;
import com.example.eams.common.result.PageResult;
import com.example.eams.common.util.RedisUtil;
import com.example.eams.system.dto.DictItemDTO;
import com.example.eams.system.dto.DictTypeDTO;
import com.example.eams.system.entity.SysDictItem;
import com.example.eams.system.entity.SysDictType;
import com.example.eams.system.mapper.SysDictItemMapper;
import com.example.eams.system.mapper.SysDictTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据字典服务
 * <p>
 * 字典数据全量缓存至Redis，变更时清除缓存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictItemMapper dictItemMapper;

    private static final String CACHE_KEY_ALL = "eams:dict:all";
    private static final String CACHE_KEY_PREFIX = "eams:dict:";
    private static final int CACHE_TTL = 86400; // 24小时

    /** 系统预置字典编码 */
    private static final Set<String> SYSTEM_DICT_CODES = new HashSet<>(Arrays.asList(
            "asset_category", "asset_status", "requisition_status", "depreciation_method"));

    // ==================== 字典类型 CRUD ====================

    public PageResult<SysDictType> listTypes(String dictName, String dictCode, int pageNum, int pageSize) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getIsDeleted, 0);
        if (StrUtil.isNotBlank(dictName)) wrapper.like(SysDictType::getDictName, dictName);
        if (StrUtil.isNotBlank(dictCode)) wrapper.like(SysDictType::getDictCode, dictCode);
        wrapper.orderByAsc(SysDictType::getId);

        IPage<SysDictType> page = dictTypeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    public List<SysDictType> listAllTypes() {
        return dictTypeMapper.selectList(
                new LambdaQueryWrapper<SysDictType>()
                        .eq(SysDictType::getIsDeleted, 0)
                        .orderByAsc(SysDictType::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void addType(DictTypeDTO dto) {
        LambdaQueryWrapper<SysDictType> check = new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictCode, dto.getDictCode())
                .eq(SysDictType::getIsDeleted, 0);
        if (dictTypeMapper.selectCount(check) > 0) {
            throw new BusinessException(400, "字典编码已存在");
        }
        SysDictType type = new SysDictType();
        type.setDictName(dto.getDictName());
        type.setDictCode(dto.getDictCode());
        type.setStatus(dto.getStatus());
        type.setDescription(dto.getDescription());
        dictTypeMapper.insert(type);
        clearDictCache();
    }

    @Transactional(rollbackFor = Exception.class)
    public void editType(DictTypeDTO dto) {
        SysDictType type = dictTypeMapper.selectById(dto.getId());
        if (type == null) throw BusinessException.notFound("字典类型不存在");
        type.setDictName(dto.getDictName());
        type.setDictCode(dto.getDictCode());
        type.setStatus(dto.getStatus());
        type.setDescription(dto.getDescription());
        dictTypeMapper.updateById(type);
        clearDictCache();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteType(Long id) {
        SysDictType type = dictTypeMapper.selectById(id);
        if (type == null) throw BusinessException.notFound("字典类型不存在");
        if (type.getIsSystem() == 1 || SYSTEM_DICT_CODES.contains(type.getDictCode())) {
            throw new BusinessException(400, "系统预置字典不可删除");
        }
        // 先删除该类型下所有字典项
        dictItemMapper.delete(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictCode, type.getDictCode()));
        dictTypeMapper.deleteById(id);
        clearDictCache();
    }

    // ==================== 字典项 CRUD ====================

    public List<SysDictItem> listItems(String dictCode) {
        return dictItemMapper.selectList(
                new LambdaQueryWrapper<SysDictItem>()
                        .eq(SysDictItem::getDictCode, dictCode)
                        .eq(SysDictItem::getIsDeleted, 0)
                        .eq(SysDictItem::getStatus, 1)
                        .orderByAsc(SysDictItem::getSortOrder));
    }

    @Transactional(rollbackFor = Exception.class)
    public void addItem(DictItemDTO dto) {
        // 同类型下value唯一
        LambdaQueryWrapper<SysDictItem> check = new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictCode, dto.getDictCode())
                .eq(SysDictItem::getDictValue, dto.getDictValue())
                .eq(SysDictItem::getIsDeleted, 0);
        if (dictItemMapper.selectCount(check) > 0) {
            throw new BusinessException(400, "字典值已存在");
        }
        SysDictItem item = new SysDictItem();
        item.setDictCode(dto.getDictCode());
        item.setDictLabel(dto.getDictLabel());
        item.setDictValue(dto.getDictValue());
        item.setCssClass(dto.getCssClass());
        item.setSortOrder(dto.getSortOrder());
        item.setStatus(dto.getStatus());
        dictItemMapper.insert(item);
        clearDictCache(dto.getDictCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public void editItem(DictItemDTO dto) {
        SysDictItem item = dictItemMapper.selectById(dto.getId());
        if (item == null) throw BusinessException.notFound("字典项不存在");
        item.setDictLabel(dto.getDictLabel());
        item.setDictValue(dto.getDictValue());
        item.setCssClass(dto.getCssClass());
        item.setSortOrder(dto.getSortOrder());
        item.setStatus(dto.getStatus());
        dictItemMapper.updateById(item);
        clearDictCache(dto.getDictCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long id) {
        SysDictItem item = dictItemMapper.selectById(id);
        if (item == null) throw BusinessException.notFound("字典项不存在");
        dictItemMapper.deleteById(id);
        clearDictCache(item.getDictCode());
    }

    // ==================== Redis 缓存 ====================

    /**
     * 获取字典项列表（带缓存）
     */
    public List<SysDictItem> getDictItemsCached(String dictCode) {
        String cacheKey = CACHE_KEY_PREFIX + dictCode;
        // 查Redis
        @SuppressWarnings("unchecked")
        List<SysDictItem> cached = RedisUtil.hgetAll(cacheKey) != null ?
                toDictItems(dictCode, RedisUtil.hgetAll(cacheKey)) : null;
        if (cached != null) return cached;

        // 查MySQL
        List<SysDictItem> items = listItems(dictCode);
        if (items.isEmpty()) {
            RedisUtil.set(cacheKey, "{}", 60); // 空值缓存1分钟防穿透
            return Collections.emptyList();
        }
        // 写Redis
        Map<String, String> map = items.stream()
                .collect(Collectors.toMap(SysDictItem::getDictValue, i ->
                        i.getDictLabel() + "|" + (i.getCssClass() != null ? i.getCssClass() : "")));
        RedisUtil.hmset(cacheKey, map, CACHE_TTL);
        return items;
    }

    /**
     * 获取全量字典（用于前端下拉框）
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<Map<String, Object>>> getAllDictCached() {
        // 查Redis (存储为JSON字符串)
        String cachedJson = RedisUtil.get(CACHE_KEY_ALL);
        if (cachedJson != null && !cachedJson.isEmpty() && !"{}".equals(cachedJson)) {
            return cn.hutool.json.JSONUtil.toBean(cachedJson, Map.class);
        }

        // 查MySQL
        List<SysDictType> types = listAllTypes();
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (SysDictType type : types) {
            List<SysDictItem> items = listItems(type.getDictCode());
            List<Map<String, Object>> itemList = items.stream().map(item -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("label", item.getDictLabel());
                m.put("value", item.getDictValue());
                m.put("cssClass", item.getCssClass());
                return m;
            }).collect(Collectors.toList());
            result.put(type.getDictCode(), itemList);
        }
        // 写Redis (JSON字符串)
        RedisUtil.set(CACHE_KEY_ALL, cn.hutool.json.JSONUtil.toJsonStr(result), CACHE_TTL);
        return result;
    }

    @PostConstruct
    public void refreshCache() {
        log.info("DictService: 启动时刷新字典缓存...");
        clearDictCache();
    }

    private void clearDictCache() {
        RedisUtil.del(CACHE_KEY_ALL);
        // 不清除单个缓存，惰性加载
    }

    private void clearDictCache(String dictCode) {
        RedisUtil.del(CACHE_KEY_PREFIX + dictCode);
        RedisUtil.del(CACHE_KEY_ALL);
    }

    @SuppressWarnings("unchecked")
    private List<SysDictItem> toDictItems(String dictCode, Map<Object, Object> map) {
        if (map == null || map.isEmpty()) return null;
        List<SysDictItem> items = new ArrayList<>();
        for (Map.Entry<Object, Object> e : map.entrySet()) {
            if ("_data".equals(e.getKey())) continue;
            String val = e.getValue() != null ? e.getValue().toString() : "";
            String[] parts = val.split("\\|", 2);
            SysDictItem item = new SysDictItem();
            item.setDictCode(dictCode);
            item.setDictValue(e.getKey().toString());
            item.setDictLabel(parts[0]);
            if (parts.length > 1) item.setCssClass(parts[1]);
            items.add(item);
        }
        items.sort(Comparator.comparingInt(SysDictItem::getSortOrder));
        return items;
    }
}
