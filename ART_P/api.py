from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
import fitz
import json
from config import client  # 引入刚才配置好的 client

router = APIRouter()

# --- 请求体定义 ---
class ParseRequest(BaseModel):
    file_path: str

class PaperData(BaseModel):
    title: str
    research_question: str
    methodology: str
    conclusion: str

class OutlineRequest(BaseModel):
    papers: list[PaperData]

# 📌 接口 1：单篇文献解析提取 JSON
@router.post("/parse")
def parse_pdf(req: ParseRequest):
    try:
        print(f"📥 收到解析任务，文件: {req.file_path}")
        text = ""
        with fitz.open(req.file_path) as doc:
            for page_num in range(min(5, doc.page_count)):
                text += doc[page_num].get_text()

        system_prompt = """
        你是一个专业的学术文献解析专家。请精准提取以下三个核心要素：
        1. 必须且只能返回合法的 JSON 对象。绝对不要包含 ```json 标签！
        2. 必须包含 key: "research_question", "methodology", "conclusion"
        """
        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": f"文本如下：\n{text}"}
            ],
            temperature=0.3,
        )
        ai_content = response.choices[0].message.content.strip()
        ai_content = ai_content.replace('```json', '').replace('```', '').strip()
        return {"code": 200, "data": json.loads(ai_content), "message": "解析成功"}
    except Exception as e:
        print(f"❌ 解析失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))

# 📌 接口 2：多文献综述提纲生成 (🌟 新增大招)
@router.post("/generate-outline")
def generate_outline(req: OutlineRequest):
    try:
        print(f"📥 收到综述任务，共包含 {len(req.papers)} 篇文献。")
        
        # 1. 拼接多篇文献的摘要信息作为输入
        user_content = "以下是参与综述的文献核心信息：\n\n"
        for i, paper in enumerate(req.papers):
            user_content += f"【文献 {i+1}】: {paper.title}\n"
            user_content += f"- 研究问题: {paper.research_question}\n"
            user_content += f"- 研究方法: {paper.methodology}\n"
            user_content += f"- 结论与贡献: {paper.conclusion}\n\n"
        user_content += "请根据以上信息，生成文献综述大纲。"

        # 2. 构造强大的 System Prompt
        system_prompt = """
        你是一位顶级的学术研究员。请根据我提供的多篇文献的核心摘要，生成一份逻辑严密、结构清晰的文献综述大纲。
        【要求】：
        1. 必须使用 Markdown 格式输出。
        2. 包含：引言、核心问题对比、研究方法演进、结论与未来展望等模块。
        3. 在综述中要明确指出不同文献之间的观点传承关系、共识点以及分歧点。
        """

        print("🚀 正在向凌云 API 发送请求，请耐心等待大模型推理 (可能需要20-60秒)...")
        
        # 3. 呼叫大模型（加上显式的超时设置）
        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_content}
            ],
            temperature=0.7,
            timeout=180.0 # 🌟 强制设置 Python 端最多等 80 秒，超过就报错，绝不死等！
        )
        
        outline_markdown = response.choices[0].message.content.strip()
        print("🎉 综述生成完毕！内容长度:", len(outline_markdown))
        return {"code": 200, "data": outline_markdown, "message": "生成成功"}
        
    except Exception as e:
        print(f"❌ 综述生成失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))