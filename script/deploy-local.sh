#!/bin/bash

# ============================================
# ART 项目 - 一键本地部署脚本 (Git Bash / Windows)
# ============================================
# 用法:
#   ./script/deploy-local.sh            # 交互式菜单
#   ./script/deploy-local.sh --start    # 启动全部
#   ./script/deploy-local.sh --stop     # 停止全部
#   ./script/deploy-local.sh --status   # 查看状态
#   ./script/deploy-local.sh --clean-venv   # 清理并重建 Python 虚拟环境
# ============================================

set -e

# ---- 路径 ----
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

ENV_FILE="$PROJECT_DIR/.env"
ENV_EXAMPLE="$PROJECT_DIR/.env.example"
PID_FILE="$SCRIPT_DIR/.deploy-local.pids"
LOG_DIR="$SCRIPT_DIR/.logs"

ART_P_DIR="$PROJECT_DIR/ART_P"
ART_H_DIR="$PROJECT_DIR/ART_H/art"
ART_Q_DIR="$PROJECT_DIR/ART_Q"

PORT_P=8000
PORT_H=8080
PORT_Q=5173

# ---- 颜色 ----
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; MAGENTA='\033[0;35m'; NC='\033[0m'

# ---- 输出工具 ----
print_header() {
    echo ""
    echo -e "${CYAN}================================================${NC}"
    echo -e "${CYAN}     智研 ScholarAI - 一键本地部署${NC}"
    echo -e "${CYAN}================================================${NC}"
    echo ""
}

ok()      { echo -e "  ${GREEN}✓${NC} $1"; }
warn()    { echo -e "  ${YELLOW}!${NC} $1"; }
err()     { echo -e "  ${RED}✗${NC} $1"; }
info()    { echo -e "  ${BLUE}..${NC} $1"; }
step()    { echo -e "  ${CYAN}>${NC} $1"; }
section() { echo ""; echo -e "${MAGENTA}--- $1 ---${NC}"; }

# ============================================
#  .env 配置管理
# ============================================
ensure_env_file() {
    if [ ! -f "$ENV_FILE" ]; then
        if [ -f "$ENV_EXAMPLE" ]; then
            warn "首次运行: 从 .env.example 创建 .env"
            cp "$ENV_EXAMPLE" "$ENV_FILE"
            ok "已创建 $ENV_FILE"
        else
            err ".env 和 .env.example 都不存在"
            exit 1
        fi
    fi
}

load_env() {
    ensure_env_file
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
    read -ra SELECTED_SERVICES_ARR <<< "${SELECTED_SERVICES:-art-p art-h art-q}"
}

save_env_var() {
    local key=$1 value=$2
    if grep -qE "^${key}=" "$ENV_FILE"; then
        sed "s|^${key}=.*|${key}=${value}|" "$ENV_FILE" > "$ENV_FILE.tmp" && mv "$ENV_FILE.tmp" "$ENV_FILE"
    else
        echo "${key}=${value}" >> "$ENV_FILE"
    fi
}

# ============================================
#  端口工具 (Windows)
# ============================================
get_port_pid() {
    local port=$1
    # Windows netstat: TCP  0.0.0.0:8080  0.0.0.0:0  LISTENING  PID
    netstat -ano 2>/dev/null | awk -v p=":${port}" '$0 ~ "LISTENING" && $2 ~ p {print $NF; exit}' || true
}

kill_port() {
    local port=$1
    local pid
    pid=$(get_port_pid "$port")
    if [ -n "$pid" ] && [ "$pid" != "0" ]; then
        warn "端口 $port 被占用 (PID: $pid), 正在关闭..."
        taskkill //PID "$pid" //F //T > /dev/null 2>&1 || true
        sleep 3
    fi
}

# ============================================
#  环境检测
# ============================================
check_environment() {
    section "环境检测"
    local all_ok=true

    if command -v java &> /dev/null; then
        ok "Java: $(java -version 2>&1 | head -1)"
    else
        err "未找到 Java (需要 JDK 21+)"; all_ok=false
    fi

    PYTHON_CMD=""
    for cmd in python python3 py; do
        if command -v "$cmd" &> /dev/null; then
            local py_ver; py_ver=$($cmd --version 2>&1)
            if [[ "$py_ver" == *"Python"* ]]; then
                PYTHON_CMD="$cmd"; ok "Python: $py_ver (命令: $cmd)"; break
            fi
        fi
    done
    [ -z "$PYTHON_CMD" ] && { err "未找到 Python (需要 3.8+)"; all_ok=false; }

    if command -v node &> /dev/null; then
        ok "Node.js: $(node --version 2>&1)"
    else
        err "未找到 Node.js (需要 18+)"; all_ok=false
    fi

    if command -v npm &> /dev/null; then
        ok "npm: $(npm --version 2>&1)"
    else
        err "未找到 npm"; all_ok=false
    fi

    if [ "$all_ok" = false ]; then
        err "环境检测未通过, 请先安装缺失的依赖"; exit 1
    fi
}

# ============================================
#  依赖安装
# ============================================
install_dependencies() {
    section "依赖安装"

    step "Python 依赖 (ART_P)..."
    local venv_dir="$ART_P_DIR/venv"
    if [ ! -d "$venv_dir" ]; then
        info "创建 Python 虚拟环境..."
        $PYTHON_CMD -m venv "$venv_dir"
    fi
    "$venv_dir/Scripts/python.exe" -m pip install --upgrade pip -q
    "$venv_dir/Scripts/python.exe" -m pip install -r "$ART_P_DIR/requirements.txt" -q 2>&1 | tail -1 || true
    ok "Python 依赖就绪"

    step "Node.js 依赖 (ART_Q)..."
    if [ ! -d "$ART_Q_DIR/node_modules" ]; then
        info "正在执行 npm install (首次较慢)..."
        (cd "$ART_Q_DIR" && npm install --silent 2>&1 | tail -3) || true
    fi
    ok "Node.js 依赖就绪"

    step "Java 构建工具 (ART_H)..."
    if [ ! -f "$ART_H_DIR/mvnw.cmd" ]; then
        err "未找到 mvnw.cmd: $ART_H_DIR"; exit 1
    fi
    ok "Maven wrapper 就绪"
}

# ============================================
#  强制重建 Python 虚拟环境（无交互）
# ============================================
clean_venv() {
    section "重建 Python 虚拟环境"
    local venv_dir="$ART_P_DIR/venv"
    
    # 直接检测 Python 命令，不用变量
    local python_cmd=""
    for cmd in python python3 py; do
        if command -v "$cmd" &> /dev/null; then
            python_cmd="$cmd"
            break
        fi
    done
    
    if [ -z "$python_cmd" ]; then
        err "未找到 Python 命令！"
        return 1
    fi
    
    info "使用 Python 命令: $python_cmd"
    
    # 删除旧环境
    if [ -d "$venv_dir" ]; then
        info "删除旧的虚拟环境..."
        rm -rf "$venv_dir"
        ok "已删除旧 venv"
    fi
    
    sleep 1
    
    # 重建
    info "创建新的虚拟环境..."
    $python_cmd -m venv "$venv_dir"
    
    if [ ! -d "$venv_dir" ]; then
        err "虚拟环境创建失败！"
        return 1
    fi
    ok "虚拟环境已创建"
    
    # 升级 pip 和安装依赖
    info "升级 pip..."
    "$venv_dir/Scripts/python.exe" -m pip install --upgrade pip -q
    
    if [ -f "$ART_P_DIR/requirements.txt" ]; then
        info "安装依赖包..."
        "$venv_dir/Scripts/python.exe" -m pip install -r "$ART_P_DIR/requirements.txt" -q
        ok "依赖安装完成"
    fi
    
    ok "venv 重建完成！"
}

# ============================================
#  服务选择
# ============================================
ask_service() {
    local svc=$1 label=$2
    local default_yes=false
    [[ " ${SELECTED_SERVICES_ARR[*]} " == *" ${svc} "* ]] && default_yes=true
    local hint; [ "$default_yes" = "true" ] && hint="[Y/n]" || hint="[y/N]"
    local input; read -p "  ${svc} (${label})  ${hint}: " input
    if [ "$default_yes" = "true" ]; then
        [[ ! "$input" =~ ^[Nn]$ ]] && _SVC_ARR+=("$svc")
    else
        [[ "$input" =~ ^[Yy]$ ]] && _SVC_ARR+=("$svc")
    fi
}

pick_services() {
    echo -e "  (回车=默认, y=选中, n=跳过)"
    _SVC_ARR=()
    ask_service "art-p" "Python AI"
    ask_service "art-h" "Java 后端"
    ask_service "art-q" "Vue 前端"
    if [ ${#_SVC_ARR[@]} -eq 0 ]; then
        err "未选择任何服务, 已取消"; return 1
    fi
    SELECTED_SERVICES_ARR=("${_SVC_ARR[@]}")
    ok "已选中: ${SELECTED_SERVICES_ARR[*]}"
}

# ============================================
#  启动服务
# ============================================
do_start() {
    check_environment

    if [ "${SKIP_INSTALL:-}" != "true" ]; then
        install_dependencies
    else
        section "跳过依赖安装"
    fi

    section "端口清理"
    local svc_p=false svc_h=false svc_q=false
    for s in "${SELECTED_SERVICES_ARR[@]}"; do
        case "$s" in
            art-p) svc_p=true; kill_port $PORT_P ;;
            art-h) svc_h=true; kill_port $PORT_H ;;
            art-q) svc_q=true; kill_port $PORT_Q ;;
        esac
    done
    ok "端口已就绪"

    mkdir -p "$LOG_DIR"
    > "$PID_FILE"

    # 导出环境变量
    export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/art}"
    export DB_USERNAME="${DB_USERNAME:-postgres}"
    export DB_PASSWORD="${DB_PASSWORD:-postgres}"
    export DEEPSEEK_API_KEY="${DEEPSEEK_API_KEY:-}"

    # ---- ART_P: Python AI 微服务 ----
    if [ "$svc_p" = true ]; then
        section "启动 ART_P (Python AI, 端口 $PORT_P)"
        local python_exe="$ART_P_DIR/venv/Scripts/python.exe"
        export PYTHONUTF8=1
        (cd "$ART_P_DIR" && "$python_exe" main.py > "$LOG_DIR/art_p.log" 2>&1) &
        echo "ART_P=$!" >> "$PID_FILE"
        ok "ART_P 已启动 (PID: $!)"

        info "等待 ART_P 就绪..."
        for i in $(seq 1 20); do
            sleep 1
            if curl -s -o /dev/null -w "%{http_code}" "http://localhost:$PORT_P/docs" 2>/dev/null | grep -q "200"; then
                ok "ART_P 健康检查通过"; break
            fi
            [ "$i" -eq 20 ] && warn "ART_P 健康检查超时, 服务可能仍在启动中"
            echo -n "." >&2
        done
        echo ""
    fi

    # ---- ART_H: Java Spring Boot 后端 ----
    if [ "$svc_h" = true ]; then
        section "启动 ART_H (Java 后端, 端口 $PORT_H)"
        (cd "$ART_H_DIR" && ./mvnw.cmd spring-boot:run > "$LOG_DIR/art_h.log" 2>&1) &
        echo "ART_H=$!" >> "$PID_FILE"
        ok "ART_H 已启动 (PID: $!)"

        info "等待 ART_H 就绪 (Spring Boot 启动较慢, 请耐心等待)..."
        local ready=false
        for i in $(seq 1 90); do
            sleep 2
            local code; code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$PORT_H" 2>/dev/null || echo "000")
            if [ "$code" != "000" ]; then ready=true; break; fi
            echo -n "." >&2
        done
        echo ""
        [ "$ready" = true ] && ok "ART_H 健康检查通过" || warn "ART_H 健康检查超时 (查看日志: $LOG_DIR/art_h.log)"
    fi

    # ---- ART_Q: Vue 前端 ----
    if [ "$svc_q" = true ]; then
        section "启动 ART_Q (Vue 前端, 端口 $PORT_Q)"
        (cd "$ART_Q_DIR" && npx vite --host > "$LOG_DIR/art_q.log" 2>&1) &
        echo "ART_Q=$!" >> "$PID_FILE"
        ok "ART_Q 已启动 (PID: $!)"

        info "等待 ART_Q 就绪..."
        for i in $(seq 1 15); do
            sleep 1
            local code; code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$PORT_Q" 2>/dev/null || echo "000")
            if [ "$code" != "000" ]; then ok "ART_Q 健康检查通过"; break; fi
            [ "$i" -eq 15 ] && warn "ART_Q 健康检查超时"
            echo -n "." >&2
        done
        echo ""
    fi

    show_status
}

# ============================================
#  停止服务
# ============================================
do_stop() {
    section "停止所有服务"

    if [ -f "$PID_FILE" ]; then
        while IFS='=' read -r name pid; do
            [ -z "$pid" ] && continue
            if kill -0 "$pid" 2>/dev/null; then
                taskkill //PID "$pid" //F //T > /dev/null 2>&1 || kill "$pid" 2>/dev/null || true
                ok "$name (PID: $pid) 已停止"
            else
                info "$name (PID: $pid) 已不在运行"
            fi
        done < "$PID_FILE"
        rm -f "$PID_FILE"
    fi

    info "通过端口兜底清理..."
    kill_port $PORT_P
    kill_port $PORT_H
    kill_port $PORT_Q
    ok "所有服务已停止"
}

# ============================================
#  查看状态
# ============================================
show_status() {
    echo ""
    echo -e "${GREEN}==== 服务状态 ====${NC}"
    local services=("ART_P:$PORT_P:Python AI" "ART_H:$PORT_H:Java 后端" "ART_Q:$PORT_Q:Vue 前端")
    for svc in "${services[@]}"; do
        IFS=':' read -r name port label <<< "$svc"
        local pid; pid=$(get_port_pid "$port")
        if [ -n "$pid" ] && [ "$pid" != "0" ]; then
            echo -e "  ${GREEN}●${NC} $name ($label)  :$port  [PID: $pid]"
        else
            echo -e "  ${RED}○${NC} $name ($label)  :$port  [未运行]"
        fi
    done
    echo ""
    echo -e "${YELLOW}访问地址:${NC}"
    echo "  前端:    http://localhost:${ART_Q_PORT:-$PORT_Q}"
    echo "  后端:    http://localhost:${ART_H_PORT:-$PORT_H}"
    echo "  AI服务:  http://localhost:${ART_P_PORT:-$PORT_P}/docs"
    echo ""
    echo -e "${YELLOW}停止服务:${NC} ./script/deploy-local.sh --stop"
    echo -e "${YELLOW}查看日志:${NC} tail -f $LOG_DIR/*.log"
    echo ""
}

# ============================================
#  查看日志
# ============================================
show_logs() {
    echo -e "${CYAN}选择要查看的日志:${NC}"
    echo -e "  ${GREEN}1${NC}) 全部"
    echo -e "  ${GREEN}2${NC}) ART_P (Python AI)"
    echo -e "  ${GREEN}3${NC}) ART_H (Java 后端)"
    echo -e "  ${GREEN}4${NC}) ART_Q (Vue 前端)"
    read -p "选择 [1]: " choice
    choice=${choice:-1}
    echo -e "${YELLOW}Ctrl+C 退出${NC}"
    case $choice in
        1) tail -f "$LOG_DIR"/*.log 2>/dev/null ;;
        2) tail -f "$LOG_DIR/art_p.log" 2>/dev/null ;;
        3) tail -f "$LOG_DIR/art_h.log" 2>/dev/null ;;
        4) tail -f "$LOG_DIR/art_q.log" 2>/dev/null ;;
        *) err "无效选项" ;;
    esac
}

# ============================================
#  配置管理
# ============================================
show_config() {
    echo -e "${CYAN}---------- 当前配置 ----------${NC}"
    echo -e "  ${MAGENTA}端口${NC}:     前端=${ART_Q_PORT:-$PORT_Q}  后端=${ART_H_PORT:-$PORT_H}  AI=${ART_P_PORT:-$PORT_P}"
    echo -e "  ${MAGENTA}数据库${NC}:   ${DB_URL}"
    echo -e "  ${MAGENTA}API密钥${NC}:  ${DEEPSEEK_API_KEY:0:10}***"
    echo -e "  ${MAGENTA}选中服务${NC}: ${SELECTED_SERVICES:-art-p art-h art-q}"
    echo -e "  ${MAGENTA}行为开关${NC}: auto_pull=${AUTO_PULL:-false}  backup=${BACKUP_DATA:-false}"
    echo ""
}

configure_ports() {
    echo -e "${CYAN}---------- 端口配置 ----------${NC}"
    read -p "ART_Q 前端端口 [${ART_Q_PORT:-$PORT_Q}]: " v
    [ -n "$v" ] && save_env_var "ART_Q_PORT" "$v" && ART_Q_PORT="$v"
    read -p "ART_H 后端端口 [${ART_H_PORT:-$PORT_H}]: " v
    [ -n "$v" ] && save_env_var "ART_H_PORT" "$v" && ART_H_PORT="$v"
    read -p "ART_P AI端口   [${ART_P_PORT:-$PORT_P}]: " v
    [ -n "$v" ] && save_env_var "ART_P_PORT" "$v" && ART_P_PORT="$v"
    ok "端口已保存"
}

configure_database() {
    echo -e "${CYAN}---------- 数据库配置 ----------${NC}"
    echo -e "${YELLOW}提示: 直接回车保留当前值${NC}"
    read -p "JDBC URL [${DB_URL}]: " v
    [ -n "$v" ] && save_env_var "DB_URL" "$v" && DB_URL="$v"
    read -p "用户名 [${DB_USERNAME}]: " v
    [ -n "$v" ] && save_env_var "DB_USERNAME" "$v" && DB_USERNAME="$v"
    read -s -p "密码 (当前: ${DB_PASSWORD:0:3}***): " v
    echo ""
    [ -n "$v" ] && save_env_var "DB_PASSWORD" "$v" && DB_PASSWORD="$v"
    ok "数据库配置已保存"
}

configure_api_keys() {
    echo -e "${CYAN}---------- API 密钥 ----------${NC}"
    read -p "DEEPSEEK_API_KEY [${DEEPSEEK_API_KEY:0:10}***]: " v
    [ -n "$v" ] && save_env_var "DEEPSEEK_API_KEY" "$v" && DEEPSEEK_API_KEY="$v"
    ok "API 密钥已保存"
}

configure_services() {
    echo -e "${CYAN}---------- 服务选择 ----------${NC}"
    echo -e "  ${GREEN}p${NC}) ART_P (Python AI)"
    echo -e "  ${GREEN}h${NC}) ART_H (Java 后端)"
    echo -e "  ${GREEN}q${NC}) ART_Q (Vue 前端)"
    echo -e "当前: ${SELECTED_SERVICES_ARR[*]}"
    read -p "输入 (如: p h q, 默认全部): " input
    local arr=()
    if [ -z "$input" ]; then arr=("art-p" "art-h" "art-q")
    else
        [[ "$input" =~ p ]] && arr+=("art-p")
        [[ "$input" =~ h ]] && arr+=("art-h")
        [[ "$input" =~ q ]] && arr+=("art-q")
    fi
    [ ${#arr[@]} -eq 0 ] && arr=("art-p" "art-h" "art-q")
    SELECTED_SERVICES="${arr[*]}"
    SELECTED_SERVICES_ARR=("${arr[@]}")
    save_env_var "SELECTED_SERVICES" "\"${SELECTED_SERVICES}\""
    ok "已保存: ${SELECTED_SERVICES_ARR[*]}"
}

# ============================================
#  Git 操作
# ============================================
git_pull() {
    if [ ! -d ".git" ]; then warn "非 Git 仓库, 跳过"; return; fi
    local current_branch; current_branch=$(git branch --show-current)
    echo -e "  当前分支: ${BLUE}${current_branch}${NC}"
    if [ -n "$TARGET_BRANCH" ] && [ "$TARGET_BRANCH" != "$current_branch" ]; then
        echo -e "  切换到: ${BLUE}${TARGET_BRANCH}${NC}"
        git checkout "$TARGET_BRANCH"
    fi
    echo -e "  拉取最新代码..."
    git pull origin "${TARGET_BRANCH:-$current_branch}"
    ok "代码已更新"
}

# ============================================
#  主菜单
# ============================================
show_menu() {
    echo ""
    echo -e "${CYAN}========== 主菜单 ==========${NC}"
    echo -e "${MAGENTA}[部署]${NC}"
    echo -e "  ${GREEN}1${NC}) 启动全部服务"
    echo -e "  ${GREEN}2${NC}) 选择性启动"
    echo -e "  ${GREEN}3${NC}) 重启全部服务"
    echo -e "  ${GREEN}4${NC}) 仅拉取代码"
    echo -e "${MAGENTA}[配置]${NC}"
    echo -e "  ${GREEN}5${NC}) 端口"
    echo -e "  ${GREEN}6${NC}) 数据库"
    echo -e "  ${GREEN}7${NC}) API 密钥"
    echo -e "  ${GREEN}8${NC}) 选择服务"
    echo -e "${MAGENTA}[管理]${NC}"
    echo -e "  ${GREEN}s${NC}) 查看状态"
    echo -e "  ${GREEN}l${NC}) 查看日志"
    echo -e "  ${GREEN}c${NC}) 清理并重建 Python 虚拟环境"
    echo -e "  ${GREEN}x${NC}) 停止服务"
    echo -e "  ${GREEN}0${NC}) 退出"
    echo ""
}

dispatch() {
    case "$1" in
        1)
            SELECTED_SERVICES_ARR=("art-p" "art-h" "art-q")
            do_start
            ;;
        2)
            pick_services || return 0
            do_start
            ;;
        3)
            do_stop
            sleep 1
            SELECTED_SERVICES_ARR=("art-p" "art-h" "art-q")
            do_start
            ;;
        4)
            section "拉取代码"
            git_pull
            ;;
        5) configure_ports ;;
        6) configure_database ;;
        7) configure_api_keys ;;
        8) configure_services ;;
        s|S) show_status ;;
        l|L) show_logs ;;
        c|C) clean_venv ;;
        x|X) do_stop ;;
        0|q|Q) echo -e "${GREEN}再见!${NC}"; exit 0 ;;
        "") ;;
        *) err "无效选项: $1"; return 1 ;;
    esac
}

# ============================================
#  主入口
# ============================================
main() {
    print_header
    load_env

    # CLI 模式
    if [ $# -gt 0 ]; then
        case "$1" in
            --start)
                SELECTED_SERVICES_ARR=("art-p" "art-h" "art-q")
                [ "${2:-}" = "--skip-install" ] && SKIP_INSTALL=true
                do_start
                ;;
            --stop)    do_stop ;;
            --status)  show_status ;;
            --clean-venv) clean_venv ;;
            --restart)
                do_stop; sleep 1
                SELECTED_SERVICES_ARR=("art-p" "art-h" "art-q")
                do_start
                ;;
            *)
                echo "用法: $0 [--start|--stop|--status|--restart|--clean-venv] [--skip-install]"
                exit 1
                ;;
        esac
        exit 0
    fi

    # 交互式菜单
    while true; do
        show_config
        show_menu
        read -p "请输入选项: " choice
        dispatch "$choice" || true
    done
}

main "$@"