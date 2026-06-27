package com.example.eams.common.config;

import com.example.eams.common.util.AIClientUtil;
import com.example.eams.common.util.JwtUtil;
import com.example.eams.common.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 静态工具类初始化配置
 * <p>
 * 将 Spring 容器管理的 Bean / 配置注入到非 Spring 管理的静态工具类中。
 */
@Slf4j
@Configuration
public class StaticBeanInitConfig {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${deepseek.api.key}")
    private String deepseekApiKey;

    @PostConstruct
    public void init() {
        // 注入 Redis 模板
        RedisUtil.setRedisTemplate(stringRedisTemplate);
        log.info("RedisUtil 初始化完成");

        // 注入 JWT 密钥
        JwtUtil.setSecret(jwtSecret);
        log.info("JwtUtil 初始化完成");

        // 注入 DeepSeek API Key
        AIClientUtil.setApiKey(deepseekApiKey);
        log.info("AIClientUtil 初始化完成");
    }
}
