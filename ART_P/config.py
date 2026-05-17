"""
配置文件 - ART_P Python AI 微服务
=====================================

所有配置项均通过环境变量注入，支持 .env 文件加载。
详细解释每一行代码的作用。

作者: ART Team
版本: v1.1
"""

# ============================================================
# 1. 基础导入
# ============================================================
import os
import httpx
from dotenv import load_dotenv
from openai import OpenAI

# ============================================================
# 2. 环境变量加载
# ============================================================
# load_dotenv() 会自动查找项目根目录的 .env 文件
# 并将文件中的键值对加载到 os.environ 中
# 例如 .env 文件中的 DEEPSEEK_API_KEY=xxx 会被加载为 os.environ["DEEPSEEK_API_KEY"]
load_dotenv()

# ============================================================
# 3. 网络代理配置（解决国内环境 Connection Error 问题）
# ============================================================
# 中国大陆环境运行 AI API 通常需要代理
# 以下环境变量用于禁用或配置代理

# HTTP 代理地址（用于 HTTP 请求）
# 如果你的网络需要代理才能访问外网，在此填入代理地址
# 例如: "http://127.0.0.1:7890" 或 "http://user:pass@proxy.com:8080"
# 如果留空或为 "none"，则不使用代理
HTTP_PROXY = os.getenv("HTTP_PROXY", "")  # 默认空字符串，不使用代理

# HTTPS 代理地址（用于 HTTPS 请求，通常与 HTTP_PROXY 相同）
HTTPS_PROXY = os.getenv("HTTPS_PROXY", "")  # 默认空字符串，不使用代理

# NO_PROXY 指定不走代理的域名或 IP（逗号分隔）
# 例如: "localhost,127.0.0.1,*.internal.com"
# 通配符用 * 表示，如 *.example.com 表示所有 example.com 域名
NO_PROXY = os.getenv("NO_PROXY", "*")  # 默认 * 表示所有域名都禁用代理

# 设置到系统环境变量（确保 OpenAI SDK 等库也生效）
if HTTP_PROXY and HTTP_PROXY.lower() not in ("", "none", "false"):
    os.environ["HTTP_PROXY"] = HTTP_PROXY
    os.environ["http_proxy"] = HTTP_PROXY
else:
    # 禁用代理 - 解决某些环境下代理干扰导致的 Connection Error
    os.environ["HTTP_PROXY"] = ""
    os.environ["http_proxy"] = ""
    os.environ["HTTPS_PROXY"] = ""
    os.environ["https_proxy"] = ""

if NO_PROXY:
    os.environ["NO_PROXY"] = NO_PROXY
    os.environ["no_proxy"] = NO_PROXY

# ============================================================
# 4. AI 大模型服务商配置
# ============================================================

# ------------------------------
# 4.1 DeepSeek API 配置
# ------------------------------

# DeepSeek API 密钥（必须）
# 获取地址: https://platform.deepseek.com/api_keys
# 格式: sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "")

if not DEEPSEEK_API_KEY:
    raise ValueError("❌ 严重错误：未在 .env 文件中找到 DEEPSEEK_API_KEY！请检查配置！")

# DeepSeek API 基础 URL
# DeepSeek 官方: https://api.deepseek.com
# 国内代理/中转站: https://yunai.chat (你当前使用的)
# 如果你使用官方 API，填入 https://api.deepseek.com
DEEPSEEK_BASE_URL = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1")

# 使用的模型名称
# 常用模型:
#   - deepseek-chat: DeepSeek 最新对话模型（推荐）
#   - deepseek-coder: 代码专用模型
#   - gpt-4: OpenAI GPT-4（需要另外配置 API Key）
DEEPSEEK_MODEL = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")

# ------------------------------
# 4.2 请求超时配置
# ------------------------------

# 整体请求超时时间（秒）
# 指从发送请求到收到完整响应的时间
# PDF 解析通常需要 30-60 秒，综述生成可能需要 60-180 秒
REQUEST_TIMEOUT = float(os.getenv("REQUEST_TIMEOUT", "120"))

# 连接建立超时时间（秒）
# 指建立 TCP 连接的时间，如果网络慢可以适当调大
CONNECT_TIMEOUT = float(os.getenv("CONNECT_TIMEOUT", "30"))

# ------------------------------
# 4.3 重试配置
# ------------------------------

# 请求失败时的最大重试次数
# 网络不稳定时可以调大，但会增加响应时间
MAX_RETRIES = int(os.getenv("MAX_RETRIES", "3"))

# 重试间隔时间（秒）
# 每次重试之间等待的时间，建议递增（如 1, 2, 4, 8...）
RETRY_DELAY = float(os.getenv("RETRY_DELAY", "1.0"))

# ============================================================
# 5. OpenAI 兼容客户端初始化
# ============================================================

# 创建 httpx.Client 实例，配置超时和代理
# httpx 是 Python 现代 HTTP 客户端库，支持同步/异步请求
# timeout: 总超时时间（包含连接、数据传输等所有阶段）
# proxy: 代理地址，None 表示不使用代理
# follow_redirects: 是否跟随重定向（推荐 True）
# retries: 重试次数（httpx 内置重试机制）
http_client = httpx.Client(
    proxy=HTTPS_PROXY if HTTPS_PROXY and HTTPS_PROXY.lower() not in ("", "none", "false") else None,
    timeout=httpx.Timeout(REQUEST_TIMEOUT, connect=CONNECT_TIMEOUT),
    follow_redirects=True,
    limits=httpx.Limits(max_connections=10, max_keepalive_connections=5)
)

# 初始化 OpenAI 兼容客户端
# 由于 DeepSeek API 兼容 OpenAI SDK 格式，可以直接使用 OpenAI 库
# api_key: 你的 API 密钥
# base_url: API 地址（注意：OpenAI 格式是 {base_url}/v1 结尾）
# http_client: 使用我们配置的 httpx 客户端（实现超时和代理控制）
client = OpenAI(
    api_key=DEEPSEEK_API_KEY,
    base_url=DEEPSEEK_BASE_URL,
    http_client=http_client,
    max_retries=MAX_RETRIES,
    timeout=REQUEST_TIMEOUT
)

# Embedding 专用客户端（DeepSeek 不支持 embedding，需要单独配置）
EMBEDDING_API_KEY = os.getenv("EMBEDDING_API_KEY", "sk-xwffdzenucgssipmchllxhstwccflrhgiliplyvpmxjfdbox")
EMBEDDING_BASE_URL = os.getenv("EMBEDDING_BASE_URL", "https://api.openai.com/v1")
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "text-embedding-ada-002")

embedding_client = OpenAI(
    api_key=EMBEDDING_API_KEY,
    base_url=EMBEDDING_BASE_URL,
    http_client=http_client,
    max_retries=MAX_RETRIES,
    timeout=REQUEST_TIMEOUT
)

# ============================================================
# 6. 应用配置
# ============================================================

# 服务端口（FastAPI 监听端口）
# 注意：Java 后端通过这个端口调用 Python 服务
# 如果修改，需要同步修改 Java 端的 ai.python.api.url 配置
APP_PORT = int(os.getenv("APP_PORT", "8000"))

# 服务主机地址
# 0.0.0.0 表示监听所有网络接口（允许外部访问，生产环境必须）
# 127.0.0.1 表示仅监听本机（仅本地可访问）
APP_HOST = os.getenv("APP_HOST", "0.0.0.0")

# 是否开启调试模式
# True: 显示详细日志，包括收到的请求内容和发送给 AI 的提示词
# False: 生产环境模式，日志更简洁
DEBUG_MODE = os.getenv("DEBUG_MODE", "false").lower() in ("true", "1", "yes")

# ============================================================
# 7. PDF 解析配置
# ============================================================

# PDF 解析时提取的页数
# 学术论文通常在前几页包含摘要，足以提取核心信息
# 如果论文结构特殊（如前置内容较多），可以调大
PDF_MAX_PAGES = int(os.getenv("PDF_MAX_PAGES", "5"))

# 是否启用 PDF 页眉页脚过滤（预留功能）
PDF_FILTER_HEADER_FOOTER = os.getenv("PDF_FILTER_HEADER_FOOTER", "true").lower() in ("true", "1", "yes")

# ============================================================
# 8. AI 生成参数配置
# ============================================================

# ------------------------------
# 8.1 PDF 解析参数（单篇文献）
# ------------------------------

# 温度参数 (Temperature) - 控制随机性
# 范围: 0.0 - 2.0
# 较低的值（如 0.3）产生更确定性、更聚焦的输出
# 较高的值（如 0.7-1.0）产生更有创意、多样的输出
# 对于结构化信息提取，0.3 是较好的选择
PARSE_TEMPERATURE = float(os.getenv("PARSE_TEMPERATURE", "0.3"))

# 最大生成 Tokens 数 - 控制单篇解析输出的长度
# 学术文献的核心三要素通常 500-1000 tokens 足够
PARSE_MAX_TOKENS = int(os.getenv("PARSE_MAX_TOKENS", "1000"))

# ------------------------------
# 8.2 综述生成参数（多篇文献）
# ------------------------------

# 综述生成的温度参数
# 由于需要创造性地综合多篇文献，适当提高随机性
REVIEW_TEMPERATURE = float(os.getenv("REVIEW_TEMPERATURE", "0.7"))

# 综述最大 Tokens 数
# 综述大纲通常较长，3000-5000 tokens 可以生成较完整的结构
REVIEW_MAX_TOKENS = int(os.getenv("REVIEW_MAX_TOKENS", "4000"))

# ============================================================
# 9. 日志配置
# ============================================================

# 日志级别
# 可选值: DEBUG, INFO, WARNING, ERROR, CRITICAL
# DEBUG 模式会显示最详细的信息，包括 API 请求和响应
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")

# 是否在控制台显示彩色日志
# 建议在开发环境开启，生产环境关闭
COLORED_LOG = os.getenv("COLORED_LOG", "true").lower() in ("true", "1", "yes")

# ============================================================
# 10. 示例 .env 文件内容
# ============================================================
EXAMPLE_ENV = '''
# ============================================================
# ART_P 环境变量配置示例
# ============================================================

# ---- AI 服务配置 ----
# DeepSeek API 密钥（必填，从 https://platform.deepseek.com/api_keys 获取）
DEEPSEEK_API_KEY=sk-your-api-key-here

# API 基础地址（使用代理或中转站时填写）
# 官方: https://api.deepseek.com
# 国内中转: https://yunai.chat/v1
DEEPSEEK_BASE_URL=https://yunai.chat/v1

# 使用的模型（默认 deepseek-chat）
DEEPSEEK_MODEL=deepseek-chat

# ---- 网络配置 ----
# HTTP 代理地址（国内环境访问外网需要，如不需要请留空或删除此行）
HTTP_PROXY=
HTTPS_PROXY=
# 禁用代理的域名
NO_PROXY=*

# ---- 超时配置 ----
# 整体请求超时（秒）
REQUEST_TIMEOUT=120
# 连接超时（秒）
CONNECT_TIMEOUT=30
# 最大重试次数
MAX_RETRIES=3
# 重试间隔（秒）
RETRY_DELAY=1.0

# ---- 应用配置 ----
# 服务端口
APP_PORT=8000
# 服务主机
APP_HOST=127.0.0.1
# 调试模式
DEBUG_MODE=false

# ---- PDF 配置 ----
# 解析的最多页数
PDF_MAX_PAGES=5

# ---- AI 参数 ----
# 解析温度（0-2，越低越确定）
PARSE_TEMPERATURE=0.3
# 解析最大 Tokens
PARSE_MAX_TOKENS=1000
# 综述温度
REVIEW_TEMPERATURE=0.7
# 综述最大 Tokens
REVIEW_MAX_TOKENS=4000

# ---- 日志 ----
LOG_LEVEL=INFO
COLORED_LOG=true
'''
