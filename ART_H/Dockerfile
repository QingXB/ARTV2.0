# ART_H - Java Spring Boot 后端
# 直接用官方 Maven 镜像（自带 JDK + Maven），省掉 apk install 等待
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# 复制 Maven 配置（阿里云镜像加速）
COPY ART_H/settings.xml /root/.m2/settings.xml

# 复制源码
COPY ART_H/art/pom.xml .
COPY ART_H/art .

# 直接构建，无需额外安装任何东西
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 创建paper目录
RUN mkdir -p /app/paper

# 从构建阶段复制 jar 文件
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 环境变量配置（外部PostgreSQL）
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/art
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=postgres
ENV AI_PYTHON_API_URL=http://art-p:8000/api/ai/parse

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]