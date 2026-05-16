-- =========================================================================
-- 5. 论文向量表 (Paper_Embeddings) - 用于语义相似度计算
-- =========================================================================
-- 存储每篇论文的高维向量表示，用于计算文献间的语义相似度
CREATE TABLE IF NOT EXISTS paper_embeddings (
    id BIGSERIAL PRIMARY KEY,
    paper_id BIGINT NOT NULL UNIQUE,              -- 关联的论文ID
    embedding_vector TEXT NOT NULL,               -- 高维向量（JSON数组格式存储）
    vector_dimension INT NOT NULL,                -- 向量维度（如384、1024）
    model_name VARCHAR(100),                      -- 使用的Embedding模型名称
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_embedding_paper FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE
);

-- 创建索引优化查询
CREATE INDEX IF NOT EXISTS idx_paper_embeddings_paper_id ON paper_embeddings(paper_id);
CREATE INDEX IF NOT EXISTS idx_paper_embeddings_user_id ON paper_embeddings(paper_id);

-- 添加注释
COMMENT ON TABLE paper_embeddings IS '论文向量表 - 存储文献的高维语义向量';
COMMENT ON COLUMN paper_embeddings.embedding_vector IS '高维向量，JSON数组格式';
COMMENT ON COLUMN paper_embeddings.vector_dimension IS '向量维度';
COMMENT ON COLUMN paper_embeddings.model_name IS '生成向量的模型名称';
