package com.quasar.art.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class GlobalCorsConfig {

    // 🌟 这是一个 Filter 级别的跨域配置，优先级在全站最高！
    // 任何 OPTIONS 请求刚进门就会被它秒处理，直接放行！
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 1. 允许任何来源的前端访问 (比如 localhost:5173)
        config.addAllowedOriginPattern("*"); 
        
        // 2. 允许前端携带 Token、Cookie 等凭证信息
        config.setAllowCredentials(true); 
        
        // 3. 允许请求携带任何请求头
        config.addAllowedHeader("*"); 
        
        // 4. 允许所有的请求方法 (GET, POST, OPTIONS 等)
        config.addAllowedMethod("*"); 
        
        // 5. 将这个跨域策略应用到所有的接口路径上 (/**)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}