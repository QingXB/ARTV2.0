from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import fitz  # PyMuPDF 的引入名称
import uvicorn

app = FastAPI()

# 定义 Java 传过来的数据格式
class ParseRequest(BaseModel):
    file_path: str

@app.post("/api/ai/parse")
def parse_pdf(req: ParseRequest):
    try:
        print(f"收到 Java 老大哥的解析任务，文件路径: {req.file_path}")
        
        # 1. 使用 PyMuPDF 读取本地 PDF 文件
        text = ""
        with fitz.open(req.file_path) as doc:
            # 先只读前 3 页测试一下提取能力
            for page_num in range(min(3, doc.page_count)):
                text += doc[page_num].get_text()

        print(f"✅ 成功提取 PDF 文本，共 {len(text)} 个字符！")
        print("提取的前 200 个字符预览:\n", text[:200])

        # 2. 这里未来会接大模型，现在先返回假数据给 Java 测试链路
        mock_ai_result = {
            "research_question": "测试数据：这是一篇关于长文本大模型内存优化的论文。",
            "methodology": "测试数据：使用了极其优雅的动态稀疏注意力机制。",
            "conclusion": "测试数据：推理速度提升 300%，效果拔群！"
        }

        return {"code": 200, "data": mock_ai_result, "message": "解析成功"}

    except Exception as e:
        print(f"❌ 解析失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    # 启动 Python 服务，端口设为 8000
    uvicorn.run(app, host="127.0.0.1", port=8000)