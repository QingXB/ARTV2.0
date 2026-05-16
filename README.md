# 智研 ScholarAI - AI辅助文献综述工具

## 1. 项目概述

**智研 ScholarAI** 是一款面向学生和科研人员的AI辅助文献综述工具。用户上传批量PDF学术论文后，系统自动解析论文内容，提取核心研究问题、研究方法和结论，并自动生成结构化的文献综述提纲。系统还尝试识别不同论文间的观点传承关系与结论矛盾，帮助用户快速把握研究脉络。

### 核心功能
- 批量PDF文献上传与管理
- AI自动解析：提取研究问题、方法、结论
- 多文档综述提纲智能生成
- 论文间观点传承与矛盾识别
- 综述历史记录与导出

---

## 2. 技术栈

项目采用**前后端分离 + 微服务架构**，由三个独立子项目组成：

| 子项目 | 技术栈 | 说明 |
|--------|--------|------|
| **ART_H** (Java后端) | Spring Boot 4.0.3 + JPA + PostgreSQL | RESTful API服务，主业务逻辑 |
| **ART_P** (Python微服务) | FastAPI + PyMuPDF (fitz) + DeepSeek API | AI解析核心，PDF处理和大模型调用 |
| **ART_Q** (Vue前端) | Vue 3 + Vite + Vue Router + Axios | 单页应用，用户操作界面 |

---

## 3. 目录结构及说明

```
ART/
├── ART_H/                    # Java后端 (Spring Boot)
│   └── art/
│       └── src/main/java/com/quasar/art/
│           ├── ArtApplication.java          # 主启动类
│           ├── config/                      # 配置类
│           │   ├── AsyncConfig.java         # 异步线程池配置
│           │   ├── RestTemplateConfig.java  # HTTP客户端配置
│           │   ├── WebMvcConfig.java        # Web MVC配置
│           │   └── GlobalCorsConfig.java    # 跨域配置
│           ├── controller/                   # 控制层
│           │   ├── UserController.java      # 用户接口
│           │   └── PaperController.java     # 文献接口
│           ├── service/                      # 业务接口
│           │   ├── PaperService.java
│           │   └── impl/
│           ├── repository/                   # 数据访问层
│           ├── entity/                        # 实体类
│           ├── dto/                          # 数据传输对象
│           ├── vo/                           # 视图对象
│           └── util/                         # 工具类
│
├── ART_P/                    # Python AI微服务
│   ├── config.py             # 全局配置
│   ├── api.py                # 核心业务逻辑
│   ├── main.py               # FastAPI启动入口
│   └── .env                  # 环境变量
│
├── ART_Q/                    # Vue前端
│   ├── src/
│   │   ├── main.js           # Vue应用入口
│   │   ├── App.vue           # 根组件
│   │   ├── router/           # 路由配置
│   │   ├── components/       # 组件
│   │   │   ├── index.vue     # 首页
│   │   │   ├── user/         # 用户模块
│   │   │   └── main/         # 主模块
│   │   └── utils/            # 工具函数
│   └── package.json
│
├── README.md
└── Study.md
```

---

## 4. 主要功能模块

### 4.1 用户认证模块
- **注册**：用户名/邮箱/密码注册
- **登录**：支持用户名/邮箱/手机号三种方式登录
- **Token机制**：`quasar-auth-token-{userId}` 格式

### 4.2 文献管理模块
- **上传PDF**：支持批量上传，自动保存
- **文献列表**：展示所有文献及解析状态
- **解析状态**：0=待解析, 1=解析中, 2=已解析, 3=解析失败

### 4.3 AI解析模块
- **PDF文本提取**：使用PyMuPDF提取前5页文本
- **核心要素提取**：调用DeepSeek API提取研究问题、方法、结论
- **异步执行**：后台异步处理，前端轮询状态

### 4.4 综述生成模块
- **多文档选择**：勾选至少2篇已解析文献
- **综述大纲生成**：生成Markdown格式综述
- **异步任务机制**：支持超时中断
- **历史记录**：保存每次生成记录

### 4.5 观点传承分析模块
- **关系类型**：INHERIT(传承)、CONTRADICT(矛盾)、SUPPORT(支持)

---

## 5. 核心组件介绍

### 5.1 后端核心组件

#### PaperController (`/api/papers`)
| 接口 | 方法 | 说明 |
|------|------|------|
| `/upload` | POST | 上传PDF文献 |
| `/list` | GET | 获取文献列表 |
| `/{id}/parse` | POST | 触发解析 |
| `/{id}` | DELETE | 删除文献 |
| `/{id}/analysis` | GET | 获取AI解析详情 |
| `/generate-outline` | POST | 同步生成综述 |
| `/generate-async` | POST | 异步综述任务 |
| `/task-status/{taskId}` | GET | 查询任务状态 |
| `/review-history` | GET | 获取历史记录 |

### 5.2 Python微服务核心组件

#### api.py 核心接口
```python
POST /api/ai/parse        # PDF解析
POST /api/ai/generate-outline  # 综述生成
```

### 5.3 前端核心组件

#### Workbench.vue (工作台主组件)
- 左侧：文献库列表 + 上传按钮
- 右侧三个Tab页：
  - **单篇精读**：显示AI解析结果
  - **综述提纲**：多文档勾选 + 综述生成
  - **观点传承图谱**：论文关系可视化

---

## 6. 数据库设计

使用 **PostgreSQL** 数据库，通过 JPA + Flyway 管理。

### 主要表结构

#### users (用户表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| username | VARCHAR(50) | 用户名 |
| email | VARCHAR(100) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| password | VARCHAR | 密码 |

#### papers (文献表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| user_id | BIGINT | 用户ID |
| title | VARCHAR | 文献标题 |
| file_path | VARCHAR(500) | 物理文件路径 |
| parse_status | INT | 解析状态 |

#### paper_ai_analyses (AI解析结果表)
| 字段 | 类型 | 说明 |
|------|------|------|
| paper_id | BIGINT | 文献ID |
| research_question | TEXT | 研究问题 |
| methodology | TEXT | 研究方法 |
| conclusion | TEXT | 结论 |

#### review_tasks (综述任务表)
| 字段 | 类型 | 说明 |
|------|------|------|
| status | INT | 0=等待, 1=生成中, 2=成功, 3=失败 |
| content | TEXT | 生成的Markdown综述 |

#### paper_relationships (论文关系表)
| 字段 | 类型 | 说明 |
|------|------|------|
| relation_type | VARCHAR(50) | INHERIT/CONTRADICT/SUPPORT |

#### paper_embeddings (论文向量表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| paper_id | BIGINT | 关联的论文ID (唯一) |
| embedding_vector | TEXT | 高维向量（JSON数组格式） |
| vector_dimension | INT | 向量维度 |
| model_name | VARCHAR(100) | 生成向量的模型名称 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |


---

## 7. 特色功能

### 7.1 微服务架构
- **Java后端**：业务逻辑、数据库、文件管理
- **Python微服务**：AI能力，PDF解析 + 大模型调用
- **RestTemplate通信**：HTTP调用，松耦合

### 7.2 异步任务处理
- `@Async` 注解实现后台执行
- 线程池：核心5线程，最大10线程
- 任务状态持久化

### 7.3 智能轮询机制
- 每3秒轮询解析状态
- 综述生成每5秒轮询
- 最多等待60次(5分钟超时)

### 7.4 PDF智能解析
- PyMuPDF提取PDF文本(前5页)
- DeepSeek API提取三要素

### 7.5 多文档综述生成
- 多文献交叉对比
- 自动识别观点传承与矛盾
- 生成Markdown大纲

---

## 8. 部署说明

### 环境要求

| 组件 | 要求 |
|------|------|
| Java | JDK 21+ |
| Python | 3.8+ |
| Node.js | 18+ |
| PostgreSQL | 13+（可选） |
| Docker | 20+ (可选) |
| Docker Compose | 2.0+ (可选) |

---
### 方式一：脚本部署

#### 1. 运行脚本

在git仓库根目录执行：

```bash
./script/deploy-local.sh
```

 #### 2. 重构python虚拟环境

 输入选项`c`，等待脚本执行完毕。

#### 3. 配置api秘钥

输入选项`7`，输入API密钥，等待脚本执行完毕。

#### 4. 启动服务

输入选项`1`，等待脚本执行完毕，即可启动服务。
 

### 方式二：Docker 部署（推荐）

#### 1. 配置文件

创建 `.env` 文件（从项目根目录）：
```bash
# DeepSeek API 密钥（必须）
DEEPSEEK_API_KEY=your-api-key-here

# 数据库配置（可选，有外部数据库时填写）
DB_URL=jdbc:postgresql://localhost:5432/art
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

#### 2. 启动服务

```bash
# 使用自动部署脚本
chmod +x script/deploy.sh
./script/deploy.sh

# 或手动执行
docker compose up -d --build
```

#### 3. 服务地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:5173 |
| 后端 | http://localhost:8080 |
| AI服务 | http://localhost:8000/docs |
| PostgreSQL | localhost:5432 (需外部) |

#### 4. 常用命令

```bash
# 查看状态
docker compose ps

# 查看日志
docker compose logs -f

# 停止服务
docker compose down

# 重启服务
docker compose restart

# 重新构建
docker compose up -d --build
```

> **注意**：Docker 部署默认连接外部 PostgreSQL。如需使用容器内的数据库，取消注释 `docker-compose.yml` 中的 `postgres` 服务配置。

---

### 方式三：本地部署

#### 1. 环境准备

```bash
# 安装依赖
# JDK 21
# Python 3.8+
# Node.js 18+
# PostgreSQL 13+
```

#### 2. 数据库配置

创建 PostgreSQL 数据库：
```sql
CREATE DATABASE art;
```

修改 `ART_H/art/src/main/resources/application.properties`：
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/art
spring.datasource.username=postgres
spring.datasource.password=postgres
```

#### 3. Python AI 服务

```bash
cd ART_P
pip install -r requirements.txt
python main.py
# 服务运行在 http://localhost:8000
```

#### 4. Java 后端

```bash
cd ART_H/art
mvn spring-boot:run
# 服务运行在 http://localhost:8080
```

#### 5. Vue 前端

```bash
cd ART_Q
npm install
npm run dev
# 服务运行在 http://localhost:5173
```

#### 6. 启动顺序

1. **PostgreSQL** - 数据库服务 (端口5432)
2. **Python微服务** - AI解析 (端口8000)
3. **Java后端** - 业务逻辑 (端口8080)
4. **Vue前端** - 用户界面 (端口5173)

---

---

### Docker Compose 服务架构

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   ART_Q     │────▶│   ART_H     │────▶│   ART_P     │
│  (Vue前端)  │     │ (Java后端)  │     │ (Python AI) │
│   :5173     │     │   :8080     │     │   :8000     │
└─────────────┘     └─────────────┘     └─────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │ PostgreSQL  │
                    │  (外部)     │
                    │   :5432     │
                    └─────────────┘
```
