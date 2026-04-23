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
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

ENV_FILE="$PROJECT_DIR/.env"
ENV_EXAMPLE="$PROJECT_DIR/.env.example"

# ============================================
# 基础工具
# ============================================

print_header() {
    echo -e "${CYAN}========================================${NC}"
    echo -e "${CYAN}     ART 项目 - Docker 部署脚本${NC}"
    echo -e "${CYAN}========================================${NC}"
}

# 确保 .env 存在
ensure_env_file() {
    if [ ! -f "$ENV_FILE" ]; then
        if [ -f "$ENV_EXAMPLE" ]; then
            echo -e "${YELLOW}首次运行：从 .env.example 创建 .env${NC}"
            cp "$ENV_EXAMPLE" "$ENV_FILE"
            echo -e "${GREEN}✓${NC} 已创建 $ENV_FILE"
            echo -e "${YELLOW}请通过菜单 [配置] 修改数据库、API 密钥等实际值${NC}"
            echo ""
        else
            echo -e "${RED}错误: .env 和 .env.example 都不存在${NC}"
            exit 1
        fi
    fi
}

# 加载 .env 到当前 shell
load_env() {
    ensure_env_file
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
    # 把 SELECTED_SERVICES 字符串转成数组
    read -ra SELECTED_SERVICES_ARR <<< "${SELECTED_SERVICES:-art-p art-h art-q}"
}

# 原地修改 .env 中某一行（没有则追加）
save_env_var() {
    local key=$1 value=$2
    if grep -qE "^${key}=" "$ENV_FILE"; then
        sed "s|^${key}=.*|${key}=${value}|" "$ENV_FILE" > "$ENV_FILE.tmp" && mv "$ENV_FILE.tmp" "$ENV_FILE"
    else
        echo "${key}=${value}" >> "$ENV_FILE"
    fi
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

    DOCKER_COMPOSE="docker compose"
    if ! docker compose version &> /dev/null; then
        DOCKER_COMPOSE="docker-compose"
    fi
    echo -e "  ${GREEN}✓${NC} $DOCKER_COMPOSE"
}

# ============================================
# Git / 数据备份 / 镜像清理
# ============================================

git_pull() {
    if [ ! -d ".git" ]; then
        echo -e "  ${YELLOW}⚠${NC} 非 Git 仓库，跳过"
        return
    fi
    local current_branch
    current_branch=$(git branch --show-current)
    echo -e "  当前分支: ${BLUE}${current_branch}${NC}"
    if [ -n "$TARGET_BRANCH" ] && [ "$TARGET_BRANCH" != "$current_branch" ]; then
        echo -e "  切换到分支: ${BLUE}${TARGET_BRANCH}${NC}"
        git checkout "$TARGET_BRANCH"
    fi
    echo -e "  拉取最新代码..."
    git pull origin "${TARGET_BRANCH:-$current_branch}"
    echo -e "  ${GREEN}✓${NC} 代码已更新"
}

backup_data() {
    if [ "$BACKUP_DATA" = "true" ]; then
        echo -e "${YELLOW}[数据备份]${NC}"
        local backup_dir="$PROJECT_DIR/backups/$(date +%Y%m%d_%H%M%S)"
        mkdir -p "$backup_dir"
        if [ -d "ART_H/art/paper" ]; then
            cp -r "ART_H/art/paper" "$backup_dir/"
            echo -e "  ${GREEN}✓${NC} 备份 paper 目录"
        fi
        echo -e "  位置: ${BLUE}${backup_dir}${NC}"
    fi
}

clean_images() {
    if [ "$CLEAN_IMAGES" = "true" ]; then
        echo -e "${YELLOW}[清理旧镜像]${NC}"
        docker image prune -f
        echo -e "  ${GREEN}✓${NC} 镜像已清理"
    fi
}

# ============================================
# 部署模式
# ============================================

pre_deploy_hooks() {
    if [ "$AUTO_PULL" = "true" ]; then
        echo -e "${YELLOW}[Git 操作]${NC}"
        git_pull
    fi
    backup_data
    clean_images
}

# ============================================
# 向导步骤（用于 2/3 增量/全量部署）
# ============================================

wizard_step() {
    local step=$1 total=$2 title=$3
    echo ""
    echo -e "${MAGENTA}━━━ 步骤 ${step}/${total}: ${title} ━━━${NC}"
}

# 步骤：分支
wizard_branch() {
    local current
    current=$(git branch --show-current 2>/dev/null || echo "N/A")
    echo -e "  本地当前分支: ${BLUE}${current}${NC}"
    echo -e "  目标分支 (.env): ${BLUE}${TARGET_BRANCH}${NC}"
    read -p "切换/修改分支? (y/N): " confirm
    if [[ "$confirm" =~ ^[Yy]$ ]]; then
        configure_branch
    else
        echo -e "  ${GREEN}✓${NC} 保持当前"
    fi
}

# 步骤：配置确认
wizard_config() {
    echo -e "  端口:     前端=${ART_Q_PORT}  后端=${ART_H_PORT}  AI=${ART_P_PORT}"
    echo -e "  数据库:   ${DB_URL}"
    echo -e "  API密钥:  ${DEEPSEEK_API_KEY:0:10}***"
    echo -e "  开关:     auto_pull=${AUTO_PULL}  clean=${CLEAN_IMAGES}  backup=${BACKUP_DATA}"
    read -p "需要修改配置? (y/N): " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        echo -e "  ${GREEN}✓${NC} 使用现有配置"
        return 0
    fi
    while true; do
        echo ""
        echo -e "  ${GREEN}1${NC}) 端口"
        echo -e "  ${GREEN}2${NC}) 数据库"
        echo -e "  ${GREEN}3${NC}) API 密钥"
        echo -e "  ${GREEN}4${NC}) 部署行为开关"
        echo -e "  ${GREEN}0${NC}) 完成，继续向导"
        read -p "修改哪项: " c
        case $c in
            1) configure_ports ;;
            2) configure_database ;;
            3) configure_api_keys ;;
            4) configure_flags ;;
            0|"") break ;;
            *) echo -e "${RED}无效${NC}" ;;
        esac
    done
    # 重新加载
    load_env
}

# 步骤：选择服务（复用已有 pick_services_interactive）
wizard_services() {
    pick_services_interactive
}

# 单个服务的 y/N 询问，追加到 _SVC_ARR
ask_service() {
    local svc=$1 label=$2
    local default_yes=false
    [[ " ${SELECTED_SERVICES_ARR[*]} " == *" ${svc} "* ]] && default_yes=true
    local hint
    if [ "$default_yes" = "true" ]; then
        hint="[Y/n]"
    else
        hint="[y/N]"
    fi
    local input
    read -p "  ${svc} (${label})   ${hint}: " input
    if [ "$default_yes" = "true" ]; then
        [[ ! "$input" =~ ^[Nn]$ ]] && _SVC_ARR+=("$svc")
    else
        [[ "$input" =~ ^[Yy]$ ]] && _SVC_ARR+=("$svc")
    fi
}

# 逐个询问服务，最后汇总确认
pick_services_interactive() {
    if [ "$CLI_MODE" = "true" ]; then
        return 0
    fi
    echo -e "  (回车=使用默认, y=选中, n=跳过)"
    _SVC_ARR=()
    ask_service "art-p" "Python AI"
    ask_service "art-h" "Java 后端"
    ask_service "art-q" "Vue 前端"

    if [ ${#_SVC_ARR[@]} -eq 0 ]; then
        echo -e "${RED}未选择任何服务，已取消${NC}"
        return 1
    fi
    SELECTED_SERVICES_ARR=("${_SVC_ARR[@]}")
    echo -e "  ${GREEN}✓${NC} 选中: ${SELECTED_SERVICES_ARR[*]}"
    return 0
}

# 通用部署向导：分支 → 配置 → 服务 → 汇总确认
# 参数: $1=部署模式名（显示用）
run_deploy_wizard() {
    local mode_name=$1
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║    ${mode_name} 向导 (共 4 步)${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════╝${NC}"

    wizard_step 1 4 "分支确认"
    wizard_branch

    wizard_step 2 4 "配置确认"
    wizard_config

    wizard_step 3 4 "服务选择"
    wizard_services || return 1

    wizard_step 4 4 "最终确认"
    echo -e "  模式:     ${mode_name}"
    echo -e "  分支:     ${TARGET_BRANCH} (本地 $(git branch --show-current 2>/dev/null))"
    echo -e "  服务:     ${SELECTED_SERVICES_ARR[*]}"
    read -p "开始执行? (Y/n): " confirm
    if [[ "$confirm" =~ ^[Nn]$ ]]; then
        echo -e "${YELLOW}已取消${NC}"
        return 1
    fi
    return 0
}

# ============================================
# 部署模式
# ============================================

deploy_quick_restart() {
    echo ""
    echo -e "${YELLOW}[快速重启] ${SELECTED_SERVICES_ARR[*]}${NC}"
    $DOCKER_COMPOSE restart "${SELECTED_SERVICES_ARR[@]}"
    show_status
}

deploy_incremental() {
    if [ "$CLI_MODE" != "true" ]; then
        run_deploy_wizard "增量部署" || return 0
    fi
    echo ""
    echo -e "${YELLOW}[增量部署] ${SELECTED_SERVICES_ARR[*]}${NC}"
    pre_deploy_hooks
    for svc in "${SELECTED_SERVICES_ARR[@]}"; do
        echo -e "  构建 ${BLUE}${svc}${NC}..."
        $DOCKER_COMPOSE build "$svc"
    done
    $DOCKER_COMPOSE up -d "${SELECTED_SERVICES_ARR[@]}"
    show_status
}

deploy_full_rebuild() {
    if [ "$CLI_MODE" != "true" ]; then
        run_deploy_wizard "全量重建" || return 0
    fi
    echo ""
    echo -e "${YELLOW}[全量重建] ${SELECTED_SERVICES_ARR[*]}${NC}"
    pre_deploy_hooks
    for svc in "${SELECTED_SERVICES_ARR[@]}"; do
        echo -e "  无缓存构建 ${BLUE}${svc}${NC}..."
        $DOCKER_COMPOSE build --no-cache "$svc"
    done
    $DOCKER_COMPOSE up -d --force-recreate "${SELECTED_SERVICES_ARR[@]}"
    show_status
}

deploy_pull_only() {
    echo ""
    echo -e "${CYAN}--- 仅拉代码 ---${NC}"
    wizard_branch
    echo ""
    git_pull
    echo -e "${GREEN}✓${NC} 代码已同步，未触发部署"
    echo -e "  提示: 使用菜单 2/3 进行增量或全量部署"
}

# ============================================
# 状态 / 日志 / 停止 / 清理
# ============================================

show_status() {
    echo ""
    echo -e "${GREEN}==== 当前服务状态 ====${NC}"
    $DOCKER_COMPOSE ps
    echo ""
    echo -e "${YELLOW}访问地址:${NC}"
    if [ "$ENABLE_HTTPS" = "true" ]; then
        echo -e "  前端(HTTPS): https://localhost"
        echo -e "  后端(HTTPS): https://localhost/api"
    else
        echo -e "  前端:    http://localhost:${ART_Q_PORT:-5173}"
        echo -e "  后端:    http://localhost:${ART_H_PORT:-8080}"
        echo -e "  AI服务:  http://localhost:${ART_P_PORT:-8000}/docs"
    fi
    echo ""
}

show_logs() {
    echo -e "${CYAN}选择要查看日志的服务:${NC}"
    echo -e "  ${GREEN}1${NC}) 全部"
    echo -e "  ${GREEN}2${NC}) art-p (Python AI)"
    echo -e "  ${GREEN}3${NC}) art-h (Java 后端)"
    echo -e "  ${GREEN}4${NC}) art-q (Vue 前端)"
    read -p "选择 [1]: " choice
    choice=${choice:-1}
    echo -e "${YELLOW}Ctrl+C 退出${NC}"
    case $choice in
        1) $DOCKER_COMPOSE logs -f ;;
        2) $DOCKER_COMPOSE logs -f art-p ;;
        3) $DOCKER_COMPOSE logs -f art-h ;;
        4) $DOCKER_COMPOSE logs -f art-q ;;
        *) echo -e "${RED}无效${NC}" ;;
    esac
}

stop_services() {
    pick_services_interactive || return 0
    echo -e "${YELLOW}[停止服务] ${SELECTED_SERVICES_ARR[*]}${NC}"
    if [ "${#SELECTED_SERVICES_ARR[@]}" -eq 3 ]; then
        $DOCKER_COMPOSE down
    else
        $DOCKER_COMPOSE stop "${SELECTED_SERVICES_ARR[@]}"
    fi
    echo -e "${GREEN}✓${NC} 完成"
}

clean_all() {
    echo -e "${YELLOW}[清理环境]${NC}"
    read -p "删除所有容器及卷? (y/N): " confirm
    if [[ "$confirm" =~ ^[Yy]$ ]]; then
        $DOCKER_COMPOSE down -v
        echo -e "${GREEN}✓${NC} 容器已删除"
    fi
    read -p "删除所有未使用镜像? (y/N): " confirm
    if [[ "$confirm" =~ ^[Yy]$ ]]; then
        docker image prune -a -f
        echo -e "${GREEN}✓${NC} 镜像已清理"
    fi
}

# ============================================
# 配置子菜单
# ============================================

show_config() {
    echo -e "${CYAN}---------- 当前配置 ----------${NC}"
    echo -e "  ${MAGENTA}分支${NC}:        ${TARGET_BRANCH}  (本地: $(git branch --show-current 2>/dev/null || echo 'N/A'))"
    echo -e "  ${MAGENTA}端口${NC}:        前端=${ART_Q_PORT}  后端=${ART_H_PORT}  AI=${ART_P_PORT}"
    echo -e "  ${MAGENTA}数据库${NC}:      ${DB_URL}"
    echo -e "  ${MAGENTA}API 密钥${NC}:    ${DEEPSEEK_API_KEY:0:10}***"
    echo -e "  ${MAGENTA}选中服务${NC}:    ${SELECTED_SERVICES}  ${YELLOW}(部署时仍可临时选择)${NC}"
    echo -e "  ${MAGENTA}行为开关${NC}:    auto_pull=${AUTO_PULL}  clean=${CLEAN_IMAGES}  backup=${BACKUP_DATA}  https=${ENABLE_HTTPS}"
    echo ""
}

configure_branch() {
    if [ ! -d ".git" ]; then
        echo -e "${RED}非 Git 仓库${NC}"
        return
    fi
    echo -e "${CYAN}---------- 分支选择 ----------${NC}"
    echo -e "当前: ${BLUE}$(git branch --show-current)${NC}"
    echo ""
    # 收集分支（去掉 HEAD 指针行、远程重复）
    local branches=()
    while IFS= read -r line; do
        line=$(echo "$line" | sed 's/^[* ]*//;s|^remotes/origin/||' | awk '{print $1}')
        [ -z "$line" ] && continue
        [[ "$line" == "HEAD" ]] && continue
        branches+=("$line")
    done < <(git branch -a | grep -v 'HEAD ->' | awk '{print $NF}' | sort -u)

    local i=1
    for b in "${branches[@]}"; do
        echo -e "  ${GREEN}${i}${NC}) ${b}"
        ((i++))
    done
    echo -e "  ${GREEN}m${NC}) 手动输入"
    echo ""
    read -p "选择 [回车=取消]: " choice
    [ -z "$choice" ] && return

    local target=""
    if [ "$choice" = "m" ]; then
        read -p "分支名: " target
    elif [[ "$choice" =~ ^[0-9]+$ ]] && [ "$choice" -ge 1 ] && [ "$choice" -le "${#branches[@]}" ]; then
        target="${branches[$((choice-1))]}"
    else
        echo -e "${RED}无效选项${NC}"
        return
    fi

    [ -z "$target" ] && return
    save_env_var "TARGET_BRANCH" "$target"
    TARGET_BRANCH="$target"
    echo -e "${GREEN}✓${NC} 目标分支设为: ${BLUE}${target}${NC}"

    read -p "立即切换到该分支? (y/N): " confirm
    if [[ "$confirm" =~ ^[Yy]$ ]]; then
        git checkout "$target" 2>/dev/null || git checkout -b "$target" "origin/$target"
        echo -e "${GREEN}✓${NC} 已切换"
    fi
}

configure_ports() {
    echo -e "${CYAN}---------- 端口配置 ----------${NC}"
    read -p "ART_Q 前端端口 [${ART_Q_PORT}]: " v
    [ -n "$v" ] && save_env_var "ART_Q_PORT" "$v" && ART_Q_PORT="$v"
    read -p "ART_H 后端端口 [${ART_H_PORT}]: " v
    [ -n "$v" ] && save_env_var "ART_H_PORT" "$v" && ART_H_PORT="$v"
    read -p "ART_P AI 端口 [${ART_P_PORT}]: " v
    [ -n "$v" ] && save_env_var "ART_P_PORT" "$v" && ART_P_PORT="$v"
    echo -e "${GREEN}✓${NC} 端口已保存"
}

configure_database() {
    echo -e "${CYAN}---------- 数据库配置（云端）----------${NC}"
    echo -e "${YELLOW}提示: 直接回车保留当前值${NC}"
    read -p "JDBC URL [${DB_URL}]: " v
    [ -n "$v" ] && save_env_var "DB_URL" "$v" && DB_URL="$v"
    read -p "用户名 [${DB_USERNAME}]: " v
    [ -n "$v" ] && save_env_var "DB_USERNAME" "$v" && DB_USERNAME="$v"
    read -s -p "密码 (当前: ${DB_PASSWORD:0:3}***): " v
    echo ""
    [ -n "$v" ] && save_env_var "DB_PASSWORD" "$v" && DB_PASSWORD="$v"
    echo -e "${GREEN}✓${NC} 数据库配置已保存"
}

configure_api_keys() {
    echo -e "${CYAN}---------- API 密钥 ----------${NC}"
    read -p "DEEPSEEK_API_KEY [${DEEPSEEK_API_KEY:0:10}***]: " v
    [ -n "$v" ] && save_env_var "DEEPSEEK_API_KEY" "$v" && DEEPSEEK_API_KEY="$v"
    echo -e "${GREEN}✓${NC} API 密钥已保存"
}

configure_services() {
    echo -e "${CYAN}---------- 参与部署的服务 ----------${NC}"
    echo -e "  ${GREEN}p${NC}) ART_P (Python AI)"
    echo -e "  ${GREEN}h${NC}) ART_H (Java 后端)"
    echo -e "  ${GREEN}q${NC}) ART_Q (Vue 前端)"
    echo -e "当前: ${SELECTED_SERVICES_ARR[*]}"
    read -p "输入(如: p h q, 默认全部): " input
    local arr=()
    if [ -z "$input" ]; then
        arr=("art-p" "art-h" "art-q")
    else
        [[ "$input" =~ p ]] && arr+=("art-p")
        [[ "$input" =~ h ]] && arr+=("art-h")
        [[ "$input" =~ q ]] && arr+=("art-q")
    fi
    [ ${#arr[@]} -eq 0 ] && arr=("art-p" "art-h" "art-q")
    SELECTED_SERVICES="${arr[*]}"
    SELECTED_SERVICES_ARR=("${arr[@]}")
    save_env_var "SELECTED_SERVICES" "\"${SELECTED_SERVICES}\""
    echo -e "${GREEN}✓${NC} 已保存: ${SELECTED_SERVICES_ARR[*]}"
}

toggle_flag() {
    local key=$1
    local current=${!key}
    local new="true"
    [ "$current" = "true" ] && new="false"
    save_env_var "$key" "$new"
    eval "$key=\"$new\""
    echo -e "  ${key} = ${GREEN}${new}${NC}"
}

configure_flags() {
    while true; do
        echo ""
        echo -e "${CYAN}---------- 部署行为开关 ----------${NC}"
        echo -e "  ${GREEN}1${NC}) AUTO_PULL     = ${AUTO_PULL}     (部署前自动拉代码)"
        echo -e "  ${GREEN}2${NC}) CLEAN_IMAGES  = ${CLEAN_IMAGES}  (部署前清理悬空镜像)"
        echo -e "  ${GREEN}3${NC}) BACKUP_DATA   = ${BACKUP_DATA}   (部署前备份 paper/)"
        echo -e "  ${GREEN}4${NC}) ENABLE_HTTPS  = ${ENABLE_HTTPS}  (显示用，暂不实际启用)"
        echo -e "  ${GREEN}0${NC}) 返回"
        read -p "切换哪一项: " c
        case $c in
            1) toggle_flag "AUTO_PULL" ;;
            2) toggle_flag "CLEAN_IMAGES" ;;
            3) toggle_flag "BACKUP_DATA" ;;
            4) toggle_flag "ENABLE_HTTPS" ;;
            0|"") return ;;
            *) echo -e "${RED}无效${NC}" ;;
        esac
    done
}

# ============================================
# 主菜单
# ============================================

show_menu() {
    echo ""
    echo -e "${CYAN}========== 主菜单 ==========${NC}"
    echo -e "${MAGENTA}[部署]${NC}"
    echo -e "  ${GREEN}1${NC}) 快速重启（不重建，最快）"
    echo -e "  ${GREEN}2${NC}) 增量部署（build + up）"
    echo -e "  ${GREEN}3${NC}) 全量重建（--no-cache）"
    echo -e "  ${GREEN}4${NC}) 仅拉代码（不触发部署）"
    echo -e "${MAGENTA}[配置]${NC}"
    echo -e "  ${GREEN}5${NC}) 分支"
    echo -e "  ${GREEN}6${NC}) 端口"
    echo -e "  ${GREEN}7${NC}) 数据库"
    echo -e "  ${GREEN}8${NC}) API 密钥"
    echo -e "  ${GREEN}9${NC}) 参与部署的服务"
    echo -e "  ${GREEN}a${NC}) 部署行为开关"
    echo -e "${MAGENTA}[管理]${NC}"
    echo -e "  ${GREEN}s${NC}) 查看状态"
    echo -e "  ${GREEN}l${NC}) 查看日志"
    echo -e "  ${GREEN}x${NC}) 停止服务"
    echo -e "  ${GREEN}c${NC}) 清理环境"
    echo -e "  ${GREEN}0${NC}) 退出"
    echo ""
}

dispatch() {
    case "$1" in
        1|--quick)   deploy_quick_restart ;;
        2|--incr)    deploy_incremental ;;
        3|--full)    deploy_full_rebuild ;;
        4|--pull)    deploy_pull_only ;;
        5)           configure_branch ;;
        6)           configure_ports ;;
        7)           configure_database ;;
        8)           configure_api_keys ;;
        9)           configure_services ;;
        a|A)         configure_flags ;;
        s|S)         show_status ;;
        l|L)         show_logs ;;
        x|X)         stop_services ;;
        c|C)         clean_all ;;
        0|q|Q)       echo -e "${GREEN}再见!${NC}"; exit 0 ;;
        "")          ;;
        *)           echo -e "${RED}无效选项: $1${NC}"; return 1 ;;
    esac
}

main() {
    print_header
    load_env
    check_dependencies

    if [ $# -gt 0 ]; then
        CLI_MODE=true
        dispatch "$1"
        exit $?
    fi

    CLI_MODE=false
    while true; do
        show_config
        show_menu
        read -p "请输入选项: " choice
        dispatch "$choice" || true
    done
}

main "$@"
