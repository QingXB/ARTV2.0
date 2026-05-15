package com.quasar.art.config;

// 🔒 已迁移至 SecurityConfig 中通过 CorsConfigurationSource 统一配置 CORS
// 此类保留但不再注册为 Bean，避免与 SecurityFilterChain 冲突

// @Configuration  -- 已注释，防止重复注册 CorsFilter
public class GlobalCorsConfig {
    // CORS 配置已统一在 SecurityConfig.corsConfigurationSource() 中
}