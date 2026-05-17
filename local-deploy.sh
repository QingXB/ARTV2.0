#!/bin/bash

echo "========================================"
echo "     ART 项目 - Docker 一键部署"
echo "========================================"

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "❌ 未安装 Docker，请先安装: https://www.docker.com/get-started"
    exit 1
fi

if ! docker info &> /dev/null; then
    echo "❌ Docker 未运行，请启动 Docker Desktop"
    exit 1
fi

echo "✓ Docker 已就绪"

# 检查 docker compose
if ! docker compose version &> /dev/null; then
    echo "❌ docker compose 不可用"
    exit 1
fi

echo "✓ docker compose 已就绪"

# 创建本地环境配置
if [ ! -f .env.local ]; then
    echo ""
    echo "首次运行，创建本地环境配置 .env.local ..."
    cat > .env.local <<'EOF'
# ART 本地开发环境配置
ART_P_PORT=8000
ART_H_PORT=8080
ART_Q_PORT=5173

# 数据库配置（需要提前准备 PostgreSQL，或取消 docker-compose.yml 中 postgres 的注释）
DB_URL=jdbc:postgresql://host.docker.internal:5432/art
DB_USERNAME=postgres
DB_PASSWORD=postgres

# DeepSeek API 密钥（必填，去 https://platform.deepseek.com 获取）
DEEPSEEK_API_KEY=
EOF
    echo "✓ 已创建 .env.local"
    echo ""
    echo "⚠️  请编辑 .env.local 填入以下必填项："
    echo "    1. DB_URL / DB_USERNAME / DB_PASSWORD — 数据库连接信息"
    echo "    2. DEEPSEEK_API_KEY — AI 服务密钥"
    echo ""
    read -p "编辑完成后按回车继续..."
fi

# 加载环境变量（安全方式，支持带空格的值）
while IFS='=' read -r key value; do
    # 跳过注释和空行
    [[ "$key" =~ ^#.*$ || -z "$key" ]] && continue
    # 去掉首尾空格和引号
    key=$(echo "$key" | xargs)
    value=$(echo "$value" | xargs | sed 's/^"//;s/"$//')
    export "$key=$value"
done < .env.local

# 校验必填项
missing=false
if [ -z "$DEEPSEEK_API_KEY" ]; then
    echo "❌ DEEPSEEK_API_KEY 未配置！请编辑 .env.local 填入 API 密钥"
    missing=true
fi
if [ -z "$DB_URL" ]; then
    echo "❌ DB_URL 未配置！请编辑 .env.local 填入数据库连接地址"
    missing=true
fi
if [ "$missing" = true ]; then
    echo ""
    echo "获取 API 密钥: https://platform.deepseek.com"
    exit 1
fi

echo ""
echo "========== 当前配置 =========="
echo "  前端端口: ${ART_Q_PORT:-5173}"
echo "  后端端口: ${ART_H_PORT:-8080}"
echo "  AI 端口:  ${ART_P_PORT:-8000}"
echo "  数据库:   ${DB_URL}"
echo "  API密钥:  ${DEEPSEEK_API_KEY:0:8}***"
echo "=============================="
echo ""

# 询问部署方式
echo "选择部署方式:"
echo "  1) 快速启动（使用已有镜像）"
echo "  2) 重新构建（首次部署或代码有更新）"
echo "  3) 停止所有容器"
echo "  4) 查看日志"
echo ""
read -p "请选择 [1-4]: " choice

case $choice in
    1)
        echo ""
        echo "启动容器..."
        docker compose --env-file .env.local up -d
        ;;
    2)
        echo ""
        echo "重新构建并启动..."
        docker compose --env-file .env.local up -d --build
        ;;
    3)
        echo ""
        echo "停止容器..."
        docker compose --env-file .env.local down
        echo "✓ 已停止"
        exit 0
        ;;
    4)
        echo ""
        echo "选择要查看的服务:"
        echo "  1) art-p (Python AI)"
        echo "  2) art-h (Java 后端)"
        echo "  3) art-q (Vue 前端)"
        read -p "请选择 [1-3]: " log_choice
        case $log_choice in
            1) docker logs -f art-p ;;
            2) docker logs -f art-h ;;
            3) docker logs -f art-q ;;
            *) echo "无效选择"; exit 1 ;;
        esac
        exit 0
        ;;
    *)
        echo "无效选择"
        exit 1
        ;;
esac

echo ""
echo "等待容器启动..."
sleep 3

# 显示状态
echo ""
echo "========== 容器状态 =========="
docker compose --env-file .env.local ps
echo ""

echo "========== 访问地址 =========="
echo "  前端:    http://localhost:${ART_Q_PORT:-5173}"
echo "  后端:    http://localhost:${ART_H_PORT:-8080}"
echo "  AI 服务: http://localhost:${ART_P_PORT:-8000}/docs"
echo "=============================="
echo ""
echo "✓ 部署完成！"
echo ""
echo "常用命令:"
echo "  查看日志: docker logs -f <容器名>"
echo "  停止服务: ./local-deploy.sh (选择选项 3)"
echo "  重启服务: docker compose --env-file .env.local restart"
