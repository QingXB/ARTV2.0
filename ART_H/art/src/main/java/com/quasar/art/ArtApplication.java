package com.quasar.art;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ArtApplication {

    public static void main(String[] args) {
        // 这就是整个后端项目的入口，启动 Spring Boot 引擎
        SpringApplication.run(ArtApplication.class, args);
        System.out.println("====== 智研 ScholarAI 后端服务启动成功！ ======");
    }

}