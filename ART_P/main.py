from fastapi import FastAPI, HTTPException  # pyright: ignore[reportMissingImports]
from pydantic import BaseModel  # pyright: ignore[reportMissingImports]
import fitz  # PyMuPDF  # pyright: ignore[reportMissingImports]
import uvicorn  # pyright: ignore[reportMissingImports]
import json
import os
import time  # 🌟 引入 time 模块用来模拟延迟
from dotenv import load_dotenv  # 🌟 引入 dotenv  # pyright: ignore[reportMissingImports]
from openai import OpenAI  # pyright: ignore[reportMissingImports]

# 🌟 1. 加载同目录下的 .env 文件
load_dotenv()

# 🌟 2. 从环境变量中安全读取 API Key
DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY")

# 加上一个防呆设计，如果没读到就报错提醒
if not DEEPSEEK_API_KEY:
    raise ValueError("❌ 极其严重的错误：未在 .env 文件中找到 DEEPSEEK_API_KEY！请检查配置！")

# 初始化 DeepSeek 客户端 (目前处于封印状态，等充值后解封)
client = OpenAI(api_key=DEEPSEEK_API_KEY, base_url="https://api.deepseek.com")

app = FastAPI()

class ParseRequest(BaseModel):
    file_path: str

@app.post("/api/ai/parse")
def parse_pdf(req: ParseRequest):
    try:
        print(f"📥 收到 Java 任务，文件路径: {req.file_path}")
        
        # 1. 提取 PDF 文本 (为了节约 API 额度和等待时间，我们先提取前 5 页)
        text = ""
        with fitz.open(req.file_path) as doc:
            for page_num in range(min(5, doc.page_count)):
                text += doc[page_num].get_text()

        print(f"✅ 成功提取文本，共 {len(text)} 个字符，准备进入 AI 解析流程...")

        # ====== 🚧 挡板模式（Mock 阶段测试专用） ======
        # 注意：当你充值完毕后，把这段替换回真实的 client.chat.completions.create 即可！
        
        print("⏳ 模拟 AI 思考中，请等待 3 秒...")
        time.sleep(3) # 模拟大模型生成耗时
        
        # 直接伪造一个完美的 JSON 结果，假装这是大模型吐出来的
        real_ai_result = {
            "research_question": "本文旨在解决长文本大模型在推理过程中的内存消耗过大（KV Cache）的问题。",
            "methodology": "提出了一种基于稀疏注意力的动态窗口机制，通过丢弃不重要的 Token 来减少计算量。",
            "conclusion": "在多个长文本基准测试中，该方法在保持准确率不降的情况下，将推理速度提升了 3 倍。"
        }
        
        print("🧠 模拟解析完毕！返回的数据如下：\n", real_ai_result)
        # ====== 🚧 挡板模式 结束 ======

        # 把真实的 AI 结果返回给 Java
        return {"code": 200, "data": real_ai_result, "message": "解析成功"}

    except Exception as e:
        print(f"❌ 解析失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8000)