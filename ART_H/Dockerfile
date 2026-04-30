# ART_H - Java Spring Boot 后端
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# 复制 Maven 相关文件
COPY ART_H/art/pom.xml .
COPY ART_H/art .
COPY ART_H/settings.xml /root/.m2/settings.xml

# 使用 Maven 构建（settings.xml 配置了阿里云镜像加速依赖下载）
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

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
ENV AI_PYTHON_API_URL=http://art-p:8000/api/ai

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
