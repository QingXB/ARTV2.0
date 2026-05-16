"""
API 路由 - ART_P Python AI 微服务
=====================================

提供3个核心接口：
1. /parse - 单篇 PDF 文献解析，提取研究问题、方法、结论
2. /generate-outline - 多篇文献综述大纲生成
3. /analyze-relations - 文献关系分析
4. /embedding - 文本向量化（新增）

作者: ART Team
版本: v1.2
"""

# ============================================================
# 1. 基础导入
# ============================================================
from fastapi import APIRouter, HTTPException  # FastAPI 路由和异常处理
from pydantic import BaseModel                # Pydantic 数据模型，用于请求体验证
import fitz                                  # PyMuPDF，PDF 文本提取库
import json                                  # JSON 序列化
import httpx                                 # HTTP 客户端库
import config                                # 配置文件，注入所有配置参数

# 创建 API 路由实例
# prefix 已在 main.py 的 include_router 中统一添加
# tags=["AI"] 在 Swagger 文档中分组显示
router = APIRouter(tags=["AI"])

# ============================================================
# 2. 请求体模型定义
# ============================================================

class ParseRequest(BaseModel):
    """
    PDF 解析请求体

    属性:
        file_path: PDF 文件的绝对路径
    """
    file_path: str

class PaperData(BaseModel):
    """
    文献核心数据模型

    属性:
        title: 文献标题
        research_question: 研究问题
        methodology: 研究方法
        conclusion: 结论与贡献
    """
    title: str
    research_question: str
    methodology: str
    conclusion: str

class OutlineRequest(BaseModel):
    """
    综述生成请求体

    属性:
        papers: 多篇文献的列表，每篇包含核心三要素
    """
    papers: list[PaperData]

class RelationRequest(BaseModel):
    """
    文献关系分析请求体

    属性:
        papers: 多篇文献的列表，每篇包含核心三要素
    """
    papers: list[PaperData]

class EmbeddingRequest(BaseModel):
    """
    文本向量化请求体

    属性:
        text: 需要向量化的文本
        model: 可选，使用的模型名称
    """
    text: str
    model: str = "text-embedding-3-small"

# ============================================================
# 3. API 接口实现
# ============================================================

@router.post("/parse")
def parse_pdf(req: ParseRequest):
    """
    接口 1: 单篇文献 PDF 解析

    功能:
        1. 接收 PDF 文件路径
        2. 使用 PyMuPDF 提取前 N 页文本
        3. 调用大模型提取研究问题、方法、结论
        4. 返回结构化 JSON

    参数:
        req: ParseRequest，包含 file_path

    返回:
        {
            "code": 200,
            "data": {
                "research_question": "...",
                "methodology": "...",
                "conclusion": "..."
            },
            "message": "解析成功"
        }

    错误:
        500: 解析过程出错
        503: 无法连接 AI 服务
        504: AI 服务响应超时
    """
    try:
        print(f"📥 收到解析任务，文件: {req.file_path}")

        # Step 1: 使用 PyMuPDF 提取 PDF 文本
        # fitz.open() 打开 PDF 文件，支持上下文管理器自动关闭
        # doc[page_num].get_text() 提取指定页的纯文本
        # PDF_MAX_PAGES 控制提取的页数（在 config.py 中配置）
        text = ""
        with fitz.open(req.file_path) as doc:
            for page_num in range(min(config.PDF_MAX_PAGES, doc.page_count)):
                text += doc[page_num].get_text()

        # 如果提取的文本为空，抛出明确错误
        if not text.strip():
            raise ValueError("PDF 文件内容为空或无法提取文本")

        # 调试模式下打印提取的文本长度
        if config.DEBUG_MODE:
            print(f"📄 提取文本长度: {len(text)} 字符")

        # Step 2: 构造 System Prompt
        # 这个提示词指导大模型以特定格式输出
        system_prompt = """
        你是一个专业的学术文献解析专家。请精准提取以下三个核心要素：
        1. 必须且只能返回合法的 JSON 对象。绝对不要包含 ```json 标签！
        2. 必须包含 key: "research_question", "methodology", "conclusion"
        3. 每个字段的值应该是完整、简洁的句子，能够准确概括文献内容
        """

        # Step 3: 调用大模型 API
        # 使用 config 中注入的参数配置请求
        response = config.client.chat.completions.create(
            model=config.DEEPSEEK_MODEL,                    # 模型名称
            messages=[
                {"role": "system", "content": system_prompt},  # 系统提示词
                {"role": "user", "content": f"文本如下：\n{text}"}  # 用户内容（PDF 文本）
            ],
            temperature=config.PARSE_TEMPERATURE,  # 温度参数
            max_tokens=config.PARSE_MAX_TOKENS     # 最大生成 tokens
        )

        # Step 4: 解析大模型返回内容
        # response.choices[0].message.content 是生成的文本内容
        ai_content = response.choices[0].message.content.strip()

        # 清理可能的 markdown 代码块标签
        ai_content = ai_content.replace('```json', '').replace('```', '').strip()

        # Step 5: 解析 JSON 字符串为 Python 对象
        result_data = json.loads(ai_content)

        print(f"✅ 解析成功！")
        if config.DEBUG_MODE:
            print(f"   研究问题: {result_data.get('research_question', '')[:50]}...")
            print(f"   研究方法: {result_data.get('methodology', '')[:50]}...")
            print(f"   结论: {result_data.get('conclusion', '')[:50]}...")

        return {"code": 200, "data": result_data, "message": "解析成功"}

    except httpx.TimeoutException as e:
        # 超时错误（连接超时或响应超时）
        print(f"❌ 解析超时: {e}")
        raise HTTPException(status_code=504, detail="API 请求超时，请重试或检查网络")

    except httpx.ConnectError as e:
        # 连接错误（无法连接到 API 服务器）
        print(f"❌ 连接失败，请检查网络或API配置: {e}")
        raise HTTPException(status_code=503, detail="无法连接到 AI 服务，请检查网络或代理配置")

    except json.JSONDecodeError as e:
        # JSON 解析错误（大模型返回的不是合法 JSON）
        print(f"❌ AI 返回格式错误，非合法 JSON: {e}")
        raise HTTPException(status_code=500, detail="AI 返回格式错误，请重试")

    except ValueError as e:
        # 业务逻辑错误（PDF 空等问题）
        print(f"❌ PDF 处理错误: {e}")
        raise HTTPException(status_code=400, detail=str(e))

    except Exception as e:
        # 其他未知错误
        print(f"❌ 解析失败: {e}")
        raise HTTPException(status_code=500, detail=f"解析失败: {str(e)}")


@router.post("/generate-outline")
def generate_outline(req: OutlineRequest):
    """
    接口 2: 多文献综述大纲生成

    功能:
        1. 接收多篇文献的核心信息列表
        2. 拼接文献摘要信息
        3. 调用大模型生成结构化综述大纲
        4. 返回 Markdown 格式的大纲

    参数:
        req: OutlineRequest，包含 papers 列表

    返回:
        {
            "code": 200,
            "data": "# Markdown 格式的综述大纲...",
            "message": "生成成功"
        }

    错误:
        500: 生成过程出错
        503: 无法连接 AI 服务
        504: AI 服务响应超时
    """
    try:
        print(f"📥 收到综述任务，共包含 {len(req.papers)} 篇文献。")

        # Step 1: 拼接多篇文献的摘要信息
        # 构造用户提示词，包含所有文献的核心信息
        user_content = "以下是参与综述的文献核心信息：\n\n"

        for i, paper in enumerate(req.papers):
            user_content += f"【文献 {i+1}】: {paper.title}\n"
            user_content += f"- 研究问题: {paper.research_question}\n"
            user_content += f"- 研究方法: {paper.methodology}\n"
            user_content += f"- 结论与贡献: {paper.conclusion}\n\n"

        user_content += "请根据以上信息，生成文献综述大纲。"

        # Step 2: 构造 System Prompt
        # 指导大模型生成高质量的综述大纲
        system_prompt = """
        你是一位顶级的学术研究员，擅长撰写结构清晰、逻辑严密的文献综述。

        【要求】：
        1. 必须使用 Markdown 格式输出，使用 ## 和 ### 构建层级结构
        2. 综述大纲必须包含以下模块：
           - 引言/研究背景（说明该领域的重要性和研究意义）
           - 核心问题对比（对比不同文献的研究问题）
           - 研究方法演进（分析方法的异同和发展脉络）
           - 主要发现与贡献（总结关键结论）
           - 分歧与争议点（识别文献间的矛盾或分歧）
           - 未来研究方向
        3. 在综述中要明确指出不同文献之间的关系：
           - 观点传承关系（后续研究如何在前人基础上发展）
           - 共识点（多个研究一致支持的观点）
           - 分歧点（结论相互矛盾的研究）
        4. 语言要学术化、简洁，避免空洞的套话
        """

        print("🚀 正在向 AI API 发送请求，请耐心等待大模型推理 (可能需要20-60秒)...")

        # Step 3: 调用大模型 API
        response = config.client.chat.completions.create(
            model=config.DEEPSEEK_MODEL,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_content}
            ],
            temperature=config.REVIEW_TEMPERATURE,  # 综述生成使用较高温度
            max_tokens=config.REVIEW_MAX_TOKENS     # 综述较长，需要更大 token 数
        )

        # Step 4: 获取生成的综述大纲
        outline_markdown = response.choices[0].message.content.strip()
        print(f"🎉 综述生成完毕！内容长度: {len(outline_markdown)} 字符")

        return {"code": 200, "data": outline_markdown, "message": "生成成功"}

    except httpx.TimeoutException as e:
        print(f"❌ 综述生成超时: {e}")
        raise HTTPException(status_code=504, detail="综述生成超时，请重试或减少文献数量")

    except httpx.ConnectError as e:
        print(f"❌ 连接失败，请检查网络或API配置: {e}")
        raise HTTPException(status_code=503, detail="无法连接到 AI 服务，请检查网络或代理配置")

    except Exception as e:
        print(f"❌ 综述生成失败: {e}")
        raise HTTPException(status_code=500, detail=f"综述生成失败: {str(e)}")


@router.post("/analyze-relations")
def analyze_relations(req: RelationRequest):
    """
    接口 3: 多文献关系分析

    功能:
        1. 接收多篇文献的核心信息列表
        2. 调用大模型分析文献之间的关系
        3. 返回关系类型和描述

    参数:
        req: RelationRequest，包含 papers 列表

    返回:
        {
            "code": 200,
            "data": [
                {
                    "sourcePaperId": 1,
                    "targetPaperId": 2,
                    "relationType": "INHERIT",
                    "description": "文献2在文献1的基础上..."
                },
                ...
            ],
            "message": "分析成功"
        }
    """
    try:
        print(f"🔍 收到关系分析任务，共 {len(req.papers)} 篇文献")

        # 构造用户提示词
        user_content = "请分析以下文献之间的关系：\n\n"

        for i, paper in enumerate(req.papers):
            user_content += f"【文献 {i+1}】: {paper.title}\n"
            user_content += f"- 研究问题: {paper.research_question}\n"
            user_content += f"- 研究方法: {paper.methodology}\n"
            user_content += f"- 结论: {paper.conclusion}\n\n"

        user_content += """
请分析这些文献之间的关系，输出 JSON 数组格式：
[
    {
        "sourcePaperId": <源文献的数组索引，从0开始，如0表示第一篇文献>,
        "targetPaperId": <目标文献的数组索引，从0开始>,
        "relationType": "INHERIT|CONTRADICT|SUPPORT",
        "description": "关系描述（50字以内）"
    }
]

重要提醒：
- sourcePaperId 和 targetPaperId 必须是数字（0, 1, 2...），表示文献在上述列表中的索引位置
- 绝对不要返回中文如"文献1"，必须返回数字索引
- 例如：第一篇文献是索引0，第二篇是索引1

关系类型说明：
- INHERIT: 传承关系（后续研究在前人基础上发展）
- CONTRADICT: 矛盾关系（研究结论相互矛盾）
- SUPPORT: 支持关系（多个研究结论相互支持）
"""

        # 调用大模型
        response = config.client.chat.completions.create(
            model=config.DEEPSEEK_MODEL,
            messages=[
                {"role": "system", "content": "你是一个专业的学术文献分析专家，擅长识别文献间的引用、传承、矛盾和支持关系。"},
                {"role": "user", "content": user_content}
            ],
            temperature=0.5,
            max_tokens=2000
        )

        ai_content = response.choices[0].message.content.strip()
        ai_content = ai_content.replace('```json', '').replace('```', '').strip()

        result_data = json.loads(ai_content)

        print(f"✅ 关系分析完成！发现 {len(result_data)} 对关系")

        return {"code": 200, "data": result_data, "message": "分析成功"}

    except json.JSONDecodeError as e:
        print(f"❌ AI 返回格式错误: {e}")
        raise HTTPException(status_code=500, detail="AI 返回格式错误，请重试")

    except Exception as e:
        print(f"❌ 关系分析失败: {e}")
        raise HTTPException(status_code=500, detail=f"关系分析失败: {str(e)}")


@router.post("/embedding")
def generate_embedding(req: EmbeddingRequest):
    """
    接口 4: 文本向量化

    功能:
        1. 接收文本内容
        2. 调用 Embedding 模型生成高维向量
        3. 返回向量数组

    参数:
        req: EmbeddingRequest，包含 text 和可选的 model

    返回:
        {
            "code": 200,
            "data": [0.123, -0.456, 0.789, ...],
            "message": "向量化成功",
            "dimension": 384,
            "model": "text-embedding-3-small"
        }

    错误:
        500: 向量化过程出错
        503: 无法连接 AI 服务
        504: AI 服务响应超时
    """
    try:
        print(f"📥 收到向量化任务，文本长度: {len(req.text)} 字符")

        if not req.text.strip():
            raise ValueError("文本内容不能为空")

        # 调用 Embedding API
        # 注意：使用 OpenAI-compatible 的 embedding 接口
        response = config.client.embeddings.create(
            input=req.text,
            model=req.model
        )

        # 提取向量数据
        embedding = response.data[0].embedding
        dimension = len(embedding)

        print(f"✅ 向量化成功！向量维度: {dimension}")

        return {
            "code": 200,
            "data": embedding,
            "message": "向量化成功",
            "dimension": dimension,
            "model": req.model
        }

    except httpx.TimeoutException as e:
        print(f"❌ 向量化超时: {e}")
        raise HTTPException(status_code=504, detail="向量化请求超时，请重试")

    except httpx.ConnectError as e:
        print(f"❌ 连接失败: {e}")
        raise HTTPException(status_code=503, detail="无法连接到 AI 服务")

    except ValueError as e:
        print(f"❌ 参数错误: {e}")
        raise HTTPException(status_code=400, detail=str(e))

    except Exception as e:
        print(f"❌ 向量化失败: {e}")
        raise HTTPException(status_code=500, detail=f"向量化失败: {str(e)}")