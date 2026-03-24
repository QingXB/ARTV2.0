package com.quasar.art.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration // 🌟 告诉 Spring 这是一个配置类，启动时必须来看一眼
public class RestTemplateConfig {

    @Bean // 🌟 告诉 Spring：把这个方法的返回值（RestTemplate）放进你的口袋里，谁用 @Autowired 要，你就给谁！
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}