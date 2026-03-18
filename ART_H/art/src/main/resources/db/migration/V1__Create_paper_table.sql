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
