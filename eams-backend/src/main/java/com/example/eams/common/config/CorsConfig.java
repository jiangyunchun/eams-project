package com.example.eams.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置
 * <p>
 * 允许前端独立部署时跨域请求后端API。
 * 生产环境应配置具体前端域名白名单。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许的前端域名（生产改为具体域名）
        config.addAllowedOrigin("*");
        // 允许携带凭证（Cookie/Authorization Header）— 注意: 与 allowCredentials=true 时不能使用 "*"
        // 开发环境使用 addAllowedOrigin("*")；生产环境替换为具体域名
        config.setAllowCredentials(false);
        // 允许的请求方法
        config.addAllowedMethod("*");
        // 允许的请求头
        config.addAllowedHeader("*");
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
