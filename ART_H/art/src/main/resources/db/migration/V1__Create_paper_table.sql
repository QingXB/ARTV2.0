-- ==========================================
-- 1. 创建用户表 (Users)
-- 支持：用户名、邮箱、手机号 三合一登录
-- ==========================================
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,                  -- 自增主键
    username VARCHAR(50) UNIQUE NOT NULL,   -- 用户名 (唯一，必填)
    email VARCHAR(100) UNIQUE,              -- 邮箱 (唯一，可为空，因为可能用手机号注册)
    phone VARCHAR(20) UNIQUE,               -- 手机号 (唯一，可为空)
    password VARCHAR(255) NOT NULL,         -- 密码 (注意：这里将来存的是 BCrypt 加密后的哈希值，长度要够)
    avatar VARCHAR(255),                    -- 用户头像 URL
    status SMALLINT DEFAULT 1,              -- 账号状态 (1: 正常, 0: 封禁)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 注册时间
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP  -- 最后更新时间
);

-- 为三个登录字段添加索引，极大提升登录验证时的查询速度
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);

-- =========================================================================
-- 1. 论文基础表 (Papers) - 存储物理文件与元数据
-- =========================================================================
CREATE TABLE IF NOT EXISTS papers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,              -- 归属用户
    title VARCHAR(255) NOT NULL,          -- 论文标题
    author VARCHAR(255),                  -- 作者
    publish_year INT,                     -- 发表年份
    file_path VARCHAR(500) NOT NULL,      -- PDF 在服务器上的物理路径
    parse_status INT DEFAULT 0,           -- 状态: 0未解析, 1解析中, 2已解析, 3解析失败
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 假设你之前已经建了 users 表，这里做个外键关联
    CONSTRAINT fk_paper_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================================================================
-- 2. AI 深度解析表 (Paper_AI_Analyses) - 对应 F3 核心要素提取
-- =========================================================================
-- 为什么独立建表？因为这部分文本极长，分离出来可以提高主表的查询性能
CREATE TABLE IF NOT EXISTS paper_ai_analyses (
    id BIGSERIAL PRIMARY KEY,
    paper_id BIGINT NOT NULL UNIQUE,      -- 一篇论文对应一份深度解析
    research_question TEXT,               -- RQ: 解决的核心痛点
    methodology TEXT,                     -- 方法: 核心算法/理论模型
    conclusion TEXT,                      -- 结论: 实验结果与贡献
    raw_ai_response JSONB,                -- 备用字段：存放 AI 返回的原始 JSON，防止日后需要其他字段
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_analysis_paper FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE
);

-- =========================================================================
-- 3. 论文关系图谱表 (Paper_Relationships) - 你的项目“杀手锏” (F4)
-- =========================================================================
-- 这是一张精妙的“自关联映射表”，用来画知识图谱
CREATE TABLE IF NOT EXISTS paper_relationships (
    id BIGSERIAL PRIMARY KEY,
    source_paper_id BIGINT NOT NULL,      -- 源论文 (比如：后发表的文献 B)
    target_paper_id BIGINT NOT NULL,      -- 目标论文 (比如：先发表的文献 A)
    relation_type VARCHAR(50) NOT NULL,   -- 关系类型: 'INHERIT'(传承/优化), 'CONTRADICT'(矛盾/分歧), 'SUPPORT'(支撑)
    description TEXT,                     -- AI 给出的判定理由 (如:"文献B指出了文献A在小样本下的过拟合问题")
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rel_source FOREIGN KEY (source_paper_id) REFERENCES papers(id) ON DELETE CASCADE,
    CONSTRAINT fk_rel_target FOREIGN KEY (target_paper_id) REFERENCES papers(id) ON DELETE CASCADE,
    -- 防止插入重复的边
    CONSTRAINT unq_paper_relation UNIQUE (source_paper_id, target_paper_id, relation_type) 
);

-- =========================================================================
-- 4. 综述生成记录表 (Literature_Reviews) - 对应 F5 大纲生成
-- =========================================================================
CREATE TABLE IF NOT EXISTS literature_reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,          -- 综述主题 (如: "大语言模型综述")
    outline_content TEXT,                 -- AI 生成的 Markdown 格式大纲
    selected_paper_ids JSONB,             -- 参与生成这篇综述的论文 ID 列表 (存成 JSON 数组很方便)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
