import os
from dotenv import load_dotenv  # pyright: ignore[reportMissingImports]
from openai import OpenAI  # pyright: ignore[reportMissingImports]

# 🌟 强行关闭代理干扰，防止 Connection Error
os.environ["HTTP_PROXY"] = ""
os.environ["HTTPS_PROXY"] = ""
os.environ["http_proxy"] = ""
os.environ["https_proxy"] = ""

# 加载 .env
load_dotenv()
DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY")

if not DEEPSEEK_API_KEY:
    raise ValueError("❌ 极其严重的错误：未在 .env 文件中找到 DEEPSEEK_API_KEY！请检查配置！")

# 初始化全全局单例 Client
client = OpenAI(
    api_key=DEEPSEEK_API_KEY, 
    base_url="https://yunai.chat/v1"
)