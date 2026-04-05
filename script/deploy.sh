#!/bin/bash

# ============================================
# ART 项目 Docker 部署脚本
# ============================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 默认配置
CONFIG_FILE=".deploy.conf"
ENABLE_HTTPS=false
AUTO_PULL=false
TARGET_BRANCH="main"
CLEAN_IMAGES=false
BACKUP_DATA=false
SELECTED_SERVICES=("art-p" "art-h" "art-q")

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

# 加载配置文件
load_config() {
    if [ -f "$PROJECT_DIR/$CONFIG_FILE" ]; then
        echo -e "${BLUE}加载配置文件: $CONFIG_FILE${NC}"
        source "$PROJECT_DIR/$CONFIG_FILE"
    fi
}

# 保存配置
save_config() {
    cat > "$PROJECT_DIR/$CONFIG_FILE" << EOF
# ART 部署配置文件
ENABLE_HTTPS=$ENABLE_HTTPS
AUTO_PULL=$AUTO_PULL
TARGET_BRANCH="$TARGET_BRANCH"
CLEAN_IMAGES=$CLEAN_IMAGES
BACKUP_DATA=$BACKUP_DATA
EOF
    echo -e "${GREEN}配置已保存到: $CONFIG_FILE${NC}"
}

# 打印标题
print_header() {
    echo -e "${CYAN}========================================${NC}"
    echo -e "${CYAN}     ART 项目 - Docker 部署脚本${NC}"
    echo -e "${CYAN}========================================${NC}"
}

# 检查依赖
check_dependencies() {
    echo -e "${YELLOW}[检查依赖]${NC}"

    if ! command -v docker &> /dev/null; then
        echo -e "${RED}错误: Docker 未安装或未运行${NC}"
        exit 1
    fi
    echo -e "  ${GREEN}✓${NC} Docker"

    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        echo -e "${RED}错误: Docker Compose 未安装${NC}"
        exit 1
    fi
    echo -e "  ${GREEN}✓${NC} Docker Compose"

    # Docker Compose 命令（兼容新旧版本）
    DOCKER_COMPOSE="docker compose"
    if ! docker compose version &> /dev/null; then
        DOCKER_COMPOSE="docker-compose"
    fi
    echo -e "  ${GREEN}✓${NC} 使用命令: $DOCKER_COMPOSE"
}

# Git 操作
git_pull() {
    if [ "$AUTO_PULL" = true ]; then
        echo -e "${YELLOW}[Git 操作]${NC}"
        if [ -d ".git" ]; then
            echo -e "  当前分支: ${BLUE}$(git branch --show-current)${NC}"
            if [ "$TARGET_BRANCH" != "$(git branch --show-current)" ]; then
                echo -e "  切换到分支: ${BLUE}$TARGET_BRANCH${NC}"
                git checkout "$TARGET_BRANCH"
            fi
            echo -e "  拉取最新代码..."
            git pull origin "$TARGET_BRANCH"
            echo -e "  ${GREEN}✓${NC} 代码已更新"
        else
            echo -e "  ${YELLOW}⚠${NC} 非 Git 仓库，跳过拉取"
        fi
    fi
}

# 备份数据
backup_data() {
    if [ "$BACKUP_DATA" = true ]; then
        echo -e "${YELLOW}[数据备份]${NC}"
        BACKUP_DIR="$PROJECT_DIR/backups/$(date +%Y%m%d_%H%M%S)"
        mkdir -p "$BACKUP_DIR"

        # 备份 paper 目录
        if [ -d "ART_H/art/paper" ]; then
            cp -r "ART_H/art/paper" "$BACKUP_DIR/"
            echo -e "  ${GREEN}✓${NC} 备份 paper 目录"
        fi

        echo -e "  备份位置: ${BLUE}$BACKUP_DIR${NC}"
    fi
}

# 清理旧镜像
clean_images() {
    if [ "$CLEAN_IMAGES" = true ]; then
        echo -e "${YELLOW}[清理旧镜像]${NC}"
        read -p "  确认清理未使用的镜像? (y/N): " confirm
        if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
            docker image prune -f
            echo -e "  ${GREEN}✓${NC} 镜像已清理"
        fi
    fi
}

# 构建服务
build_services() {
    echo -e "${YELLOW}[构建镜像]${NC}"
    for service in "${SELECTED_SERVICES[@]}"; do
        echo -e "  构建 ${BLUE}$service${NC}..."
        $DOCKER_COMPOSE build "$service"
    done
}

# 启动服务
start_services() {
    echo -e "${YELLOW}[启动服务]${NC}"
    $DOCKER_COMPOSE up -d "${SELECTED_SERVICES[@]}"
}

# 显示服务状态
show_status() {
    echo ""
    print_header
    echo ""
    echo -e "${GREEN}部署完成！${NC}"
    echo ""
    $DOCKER_COMPOSE ps
    echo ""
    echo -e "${YELLOW}访问地址:${NC}"

    if [ "$ENABLE_HTTPS" = true ]; then
        echo -e "  - 前端(HTTPS): https://localhost"
        echo -e "  - 后端(HTTPS):  https://localhost/api"
    else
        echo -e "  - 前端: http://localhost:5173"
        echo -e "  - 后端: http://localhost:8080"
        echo -e "  - AI服务: http://localhost:8000/docs"
    fi
    echo ""
    echo -e "${YELLOW}常用命令:${NC}"
    echo "  - 查看日志: $DOCKER_COMPOSE logs -f"
    echo "  - 停止服务: $DOCKER_COMPOSE down"
    echo "  - 重启服务: $DOCKER_COMPOSE restart"
    echo ""
}

# 显示菜单
show_menu() {
    echo ""
    echo -e "${CYAN}请选择操作:${NC}"
    echo "  ${GREEN}1${NC}) 开始部署"
    echo "  ${GREEN}2${NC}) 配置参数"
    echo "  ${GREEN}3${NC}) 查看状态"
    echo "  ${GREEN}4${NC}) 查看日志"
    echo "  ${GREEN}5${NC}) 停止服务"
    echo "  ${GREEN}6${NC}) 清理环境"
    echo "  ${GREEN}0${NC}) 退出"
    echo ""
}

# 配置参数
configure() {
    echo ""
    echo -e "${CYAN}========== 配置参数 ==========${NC}"
    echo ""

    # HTTPS
    read -p "启用 HTTPS (Nginx反向代理)? (y/N): " input
    ENABLE_HTTPS=$([[ "$input" =~ ^[Yy]$ ]] && echo "true" || echo "false")

    # 自动拉取
    read -p "自动拉取 Git 最新代码? (y/N): " input
    AUTO_PULL=$([[ "$input" =~ ^[Yy]$ ]] && echo "true" || echo "false")

    if [ "$AUTO_PULL" = true ]; then
        read -p "  输入分支名 (默认: main): " input
        TARGET_BRANCH=${input:-main}
    fi

    # 清理镜像
    read -p "部署前清理旧镜像? (y/N): " input
    CLEAN_IMAGES=$([[ "$input" =~ ^[Yy]$ ]] && echo "true" || echo "false")

    # 备份数据
    read -p "部署前备份数据? (y/N): " input
    BACKUP_DATA=$([[ "$input" =~ ^[Yy]$ ]] && echo "true" || echo "false")

    # 选择服务
    echo ""
    echo -e "${CYAN}选择要启动的服务 (用空格分隔, 默认全部):${NC}"
    echo "  ${GREEN}p${NC}) ART_P (Python AI)"
    echo "  ${GREEN}h${NC}) ART_H (Java 后端)"
    echo "  ${GREEN}q${NC}) ART_Q (Vue 前端)"
    read -p "  输入选择 (如: p h q): " input

    SELECTED_SERVICES=()
    if [[ "$input" =~ "p" ]]; then SELECTED_SERVICES+=("art-p"); fi
    if [[ "$input" =~ "h" ]]; then SELECTED_SERVICES+=("art-h"); fi
    if [[ "$input" =~ "q" ]]; then SELECTED_SERVICES+=("art-q"); fi
    if [ ${#SELECTED_SERVICES[@]} -eq 0 ]; then
        SELECTED_SERVICES=("art-p" "art-h" "art-q")
    fi

    # 保存配置
    save_config

    echo ""
    echo -e "${CYAN}========== 当前配置 ==========${NC}"
    show_config
}

# 显示当前配置
show_config() {
    echo "  HTTPS 启用: $ENABLE_HTTPS"
    echo "  自动拉取: $AUTO_PULL"
    echo "  目标分支: $TARGET_BRANCH"
    echo "  清理镜像: $CLEAN_IMAGES"
    echo "  备份数据: $BACKUP_DATA"
    echo "  启动服务: ${SELECTED_SERVICES[*]}"
}

# 主流程
main() {
    print_header
    load_config
    check_dependencies

    # 无参数时显示菜单
    if [ $# -eq 0 ]; then
        show_config
        show_menu
        read -p "请输入选项: " choice
    else
        choice=$1
    fi

    case $choice in
        1|"")
            git_pull
            backup_data
            clean_images
            build_services
            start_services
            show_status
            ;;
        2)
            configure
            ;;
        3)
            echo -e "${YELLOW}[服务状态]${NC}"
            $DOCKER_COMPOSE ps
            ;;
        4)
            echo -e "${YELLOW}[查看日志 (Ctrl+C 退出)]${NC}"
            $DOCKER_COMPOSE logs -f
            ;;
        5)
            echo -e "${YELLOW}[停止服务]${NC}"
            $DOCKER_COMPOSE down
            echo -e "${GREEN}✓${NC} 服务已停止"
            ;;
        6)
            echo -e "${YELLOW}[清理环境]${NC}"
            read -p "  删除所有容器? (y/N): " confirm
            if [[ "$confirm" =~ ^[Yy]$ ]]; then
                $DOCKER_COMPOSE down -v
                echo -e "${GREEN}✓${NC} 容器已删除"
            fi
            read -p "  删除所有镜像? (y/N): " confirm
            if [[ "$confirm" =~ ^[Yy]$ ]]; then
                docker image prune -a -f
                echo -e "${GREEN}✓${NC} 镜像已删除"
            fi
            ;;
        0)
            echo -e "${GREEN}再见!${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}无效选项${NC}"
            exit 1
            ;;
    esac
}

# 命令行参数支持
case "${BASH_SOURCE[0]}" in
    deploy.sh)
        main "$@"
        ;;
esac
