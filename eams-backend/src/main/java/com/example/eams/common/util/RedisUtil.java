package com.example.eams.common.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * <p>
 * 统一封装 String/Hash/分布式锁/计数器操作。
 * 需在 Spring 容器启动时注入 StringRedisTemplate。
 */
public class RedisUtil {

    private static StringRedisTemplate redisTemplate;

    /** Spring 容器注入 */
    public static void setRedisTemplate(StringRedisTemplate template) {
        RedisUtil.redisTemplate = template;
    }

    // ==================== String 操作 ====================

    /** 获取字符串值 */
    public static String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /** 写入字符串并设过期时间（秒） */
    public static void set(String key, String value, long expireSec) {
        redisTemplate.opsForValue().set(key, value, expireSec, TimeUnit.SECONDS);
    }

    /** 判断 Key 是否存在 */
    public static boolean exists(String key) {
        Boolean result = redisTemplate.hasKey(key);
        return result != null && result;
    }

    /** 获取Key剩余存活时间（秒） */
    public static long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire != null ? expire : 0;
    }

    /** 删除单个 Key */
    public static void del(String key) {
        redisTemplate.delete(key);
    }

    /** 自增1，首次调用设过期（秒），返回自增后的值 */
    public static long incr(String key, long expireSec) {
        Long count = redisTemplate.opsForValue().increment(key, 1);
        // 首次创建时设置过期
        if (count != null && count == 1) {
            redisTemplate.expire(key, expireSec, TimeUnit.SECONDS);
        }
        return count != null ? count : 0;
    }

    // ==================== Hash 操作 ====================

    /** 获取 Hash 全部字段 */
    public static Map<Object, Object> hgetAll(String key) {
        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
        return map.isEmpty() ? null : map;
    }

    /** Hash 单字段写入 */
    public static void hset(String key, String field, String value, long expireSec) {
        redisTemplate.opsForHash().put(key, field, value);
        redisTemplate.expire(key, expireSec, TimeUnit.SECONDS);
    }

    /** Hash 批量写入 */
    public static void hmset(String key, Map<String, String> map, long expireSec) {
        redisTemplate.opsForHash().putAll(key, map);
        redisTemplate.expire(key, expireSec, TimeUnit.SECONDS);
    }

    /** Hash 单字段读取 */
    public static String hget(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);
        return value != null ? value.toString() : null;
    }

    // ==================== 分布式锁 ====================

    /**
     * 获取分布式锁
     *
     * @param key     锁 Key
     * @param waitSec 等待获取时间（秒）
     * @param holdSec 最大持有时间（秒），超时自动释放防死锁
     * @return lockValue (UUID)，释放时用于校验归属；获取失败返回 null
     */
    public static String tryLock(String key, int waitSec, int holdSec) {
        String lockValue = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + waitSec * 1000L;
        while (System.currentTimeMillis() < deadline) {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(key, lockValue, holdSec, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(success)) {
                return lockValue;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    /**
     * 释放分布式锁（Lua 脚本原子操作，校验归属）
     *
     * @param key       锁 Key
     * @param lockValue 加锁时返回的 UUID 令牌
     */
    public static void unlock(String key, String lockValue) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                        "then return redis.call('del', KEYS[1]) " +
                        "else return 0 end";
        redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(key),
                lockValue);
    }

    // ==================== 批量操作 ====================

    /**
     * 模糊删除（SCAN 迭代，不阻塞 Redis）
     *
     * @param pattern 匹配模式，如 "eams:asset:list:*"
     */
    public static void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
