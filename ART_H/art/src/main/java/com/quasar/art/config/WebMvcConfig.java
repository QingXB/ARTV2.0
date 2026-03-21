package com.quasar.art.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // 把我们在 application.properties 里配的 "./paper" 读进来
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 将相对路径转换成系统真实的绝对物理路径
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        // 2. 转换成 Spring 认识的 file:/// 协议格式
        String documentRoot = uploadPath.toUri().toString(); 

        // 3. 核心映射规则：
        // 只要前端访问 http://localhost:8080/pdf/xxx.pdf
        // 系统就会自动去本地硬盘的 paper 文件夹下找 xxx.pdf 返回给浏览器
        registry.addResourceHandler("/pdf/**")
                .addResourceLocations(documentRoot);
    }
}