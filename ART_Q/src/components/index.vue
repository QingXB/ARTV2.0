<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
const goLogin = () => router.push('/login')
</script>

<template>
    <div class="landing-container">
     
      <nav class="navbar">
        <div class="nav-left">
          <div class="nav-logo">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="16" x2="12" y2="12"></line>
              <line x1="12" y1="8" x2="12.01" y2="8"></line>
            </svg>
            <span class="brand-name">智研 ScholarAI</span>
          </div>
          <a href="#" class="nav-link">功能演示</a>
          <a href="#" class="nav-link">使用文档</a>
        </div>
        <div class="nav-right">
          <a href="#" class="nav-link">注册账号</a>
          <a href="#" class="nav-link login-btn" @click.prevent="goLogin">登录</a>
        </div>
      </nav>
  
      <header class="hero-section">
        <div class="main-logo">
          <svg viewBox="0 0 100 100" class="hero-icon">
            <path d="M20,80 V25 C20,15 45,15 50,25 C55,15 80,15 80,25 V80 C80,70 55,70 50,80 C45,70 20,70 20,80 Z" fill="none" stroke="#3b82f6" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>
            <line x1="50" y1="25" x2="50" y2="80" stroke="#3b82f6" stroke-width="4" stroke-linecap="round" />
            <path d="M40,10 L50,0 L60,10 L50,20 Z" fill="#60a5fa" />
          </svg>
        </div>
        <h1 class="project-title">AI 辅助文献综述工具</h1>
        <h2 class="project-subtitle">科研人员的超级大脑：一键解析海量文献，智能构建综述提纲。</h2>
      </header>
  
      <section class="features-grid">
        
        <div class="feature-card">
          <div class="feature-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="17 8 12 3 7 8" />
              <line x1="12" y1="3" x2="12" y2="15" />
            </svg>
          </div>
          <h3 class="feature-title">批量 PDF 智能解析</h3>
          <p class="feature-desc">
            无需手动阅读。一键上传批量 PDF 论文，系统自动穿透双栏排版与复杂格式，瞬间完成文档结构化。
          </p>
        </div>
  
        <div class="feature-card">
          <div class="feature-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
              <line x1="11" y1="8" x2="11" y2="14" />
              <line x1="8" y1="11" x2="14" y2="11" />
            </svg>
          </div>
          <h3 class="feature-title">核心要素精准提取</h3>
          <p class="feature-desc">
            深度阅读理解引擎，精准提取每篇论文的<strong>研究问题、核心方法与最终结论</strong>，告别信息过载。
          </p>
        </div>
  
        <div class="feature-card">
          <div class="feature-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="8" y1="6" x2="21" y2="6" />
              <line x1="8" y1="12" x2="21" y2="12" />
              <line x1="8" y1="18" x2="21" y2="18" />
              <line x1="3" y1="6" x2="3.01" y2="6" />
              <line x1="3" y1="12" x2="3.01" y2="12" />
              <line x1="3" y1="18" x2="3.01" y2="18" />
            </svg>
          </div>
          <h3 class="feature-title">自动生成综述提纲</h3>
          <p class="feature-desc">
            基于多篇文献的核心要素，自动归纳研究脉络，一键导出逻辑严密的结构化文献综述大纲。
          </p>
        </div>
  
        <div class="feature-card">
          <div class="feature-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="18" cy="5" r="3" />
              <circle cx="6" cy="12" r="3" />
              <circle cx="18" cy="19" r="3" />
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
              <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
            </svg>
          </div>
          <h3 class="feature-title">观点传承与矛盾识别</h3>
          <p class="feature-desc">
            不仅是总结，更是推理。系统自动尝试识别不同论文间的<strong>观点传承</strong>与<strong>实验结论矛盾</strong>，助你发现科研突破口。
          </p>
        </div>
  
      </section>
    </div>
  </template>
  
  <style scoped>
  /* 调整为科技蓝/学术风配色 */
  :root {
    --primary-blue: #3b82f6;
    --primary-blue-hover: #2563eb;
    --text-main: #1f2937;
    --text-muted: #4b5563;
    --bg-color: #ffffff;
  }
  
  .landing-container {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
    color: var(--text-main);
    background-color: var(--bg-color);
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  
  .navbar {
    width: 100%;
    padding: 15px 40px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #f3f4f6;
    box-sizing: border-box;
  }

  .corner-login {
    position: fixed;
    top: 14px;
    right: 18px;
    z-index: 30;
    text-decoration: none;
    background-color: var(--primary-blue);
    color: white;
    padding: 8px 14px;
    border-radius: 10px;
    font-weight: 600;
    font-size: 14px;
    box-shadow: 0 10px 24px rgba(37, 99, 235, 0.25);
  }

  .corner-login:hover {
    background-color: var(--primary-blue-hover);
    color: white;
  }
  
  .nav-left, .nav-right {
    display: flex;
    align-items: center;
    gap: 25px;
  }
  
  .nav-logo {
    display: flex;
    align-items: center;
    gap: 8px;
    color: var(--primary-blue);
    font-weight: bold;
    font-size: 18px;
    margin-right: 20px;
  }
  
  .nav-link {
    text-decoration: none;
    color: var(--text-muted);
    font-size: 15px;
    transition: color 0.3s;
  }
  
  .nav-link:hover {
    color: var(--primary-blue);
  }
  
  .login-btn {
    background-color: var(--primary-blue);
    color: #888686;
    padding: 8px 16px;
    border-radius: 6px;
    font-weight: 500;
  }
  
  .login-btn:hover {
    background-color: var(--primary-blue-hover);
    color: rgb(190, 189, 189);
  }
  
  .hero-section {
    text-align: center;
    margin-top: 80px;
    margin-bottom: 70px;
    padding: 0 20px;
  }
  
  .hero-icon {
    width: 140px;
    height: 140px;
    margin-bottom: 20px;
  }
  
  .project-title {
    font-size: 56px;
    font-weight: 700;
    margin: 0;
    color: rgb(17, 24, 39);
    letter-spacing: -1px;
  }
  
  .project-subtitle {
    font-size: 22px;
    font-weight: 400;
    color: #6b7280;
    margin-top: 20px;
    max-width: 800px;
    line-height: 1.5;
  }
  
  .features-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 60px 80px;
    max-width: 960px;
    padding: 0 20px;
    margin-bottom: 100px;
  }
  
  .feature-card {
    text-align: center;
    padding: 20px;
    border-radius: 12px;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
  }
  
  .feature-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 10px 25px rgba(0,0,0,0.05);
  }
  
  .feature-icon {
    width: 56px;
    height: 56px;
    margin: 0 auto 20px auto;
    color: var(--primary-blue);
  }
  
  .feature-title {
    font-size: 22px;
    font-weight: 600;
    margin-bottom: 15px;
    color: #1f2937;
  }
  
  .feature-desc {
    font-size: 16px;
    line-height: 1.6;
    color: #4b5563;
    margin: 0;
  }
  
  .feature-desc strong {
    color: var(--primary-blue);
  }
  
  @media (max-width: 768px) {
    .features-grid {
      grid-template-columns: 1fr;
      gap: 40px;
    }
    .project-title {
      font-size: 40px;
    }
    .project-subtitle {
      font-size: 18px;
    }
  }
  </style>