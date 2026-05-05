# ART_H - Java Spring Boot 后端
# 使用你本地已经下载好的基础镜像，绕过 429 限流
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# 【关键新增】：将 Alpine 的软件源替换为阿里云源，解决国内下载 maven 慢/卡死的问题
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories

# 复制 Maven 相关文件
COPY ART_H/art/pom.xml .
COPY ART_H/art .
COPY ART_H/settings.xml /root/.m2/settings.xml

# 现在使用阿里云源安装 maven 会非常快，不会再卡住 7 分钟了
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
ENV AI_PYTHON_API_URL=http://art-p:8000/api/ai/parse

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]