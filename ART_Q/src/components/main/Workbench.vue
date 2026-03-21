<template>
    <div class="workbench">
      <header class="header">
        <div class="logo">📚 智研 ScholarAI 工作台</div>
        <div class="user-info">
          <span>欢迎, {{ currentUsername }}</span>
          <button class="logout-btn" @click="handleLogout">退出</button>
        </div>
      </header>
  
      <div class="main-container">
        <aside class="left-panel">
          <div class="upload-section">
            <input type="file" ref="fileInput" multiple accept="application/pdf" @change="handleFileUpload" style="display: none;" />
            <button class="btn-upload" @click="triggerUpload" :disabled="isUploading">
              <i class="icon-plus"></i> {{ isUploading ? '正在上传中...' : '上传 PDF 文献' }}
            </button>
          </div>
  
          <div class="paper-list">
            <h3>我的文献库 ({{ papers.length }})</h3>
            <ul>
              <li 
                v-for="paper in papers" 
                :key="paper.id" 
                :class="{ active: selectedPaper?.id === paper.id }"
                @click="selectPaper(paper)"
              >
                <div class="paper-title" :title="paper.title">{{ paper.title || '未知文献' }}</div>
                <div class="paper-status"> 
  <button class="delete-btn" @click.stop="deletePaper(paper)">删除</button>
  
  <button class="parse-btn" @click.stop="triggerParse(paper)">解析PDF</button>
  <button class="view-pdf-btn" @click.stop="viewPdf(paper)">查看原文</button>
</div>
              </li>
            </ul>
          </div>
        </aside>
  
        <main class="right-panel">
          <div class="tabs">
            <button :class="{ active: activeTab === 'detail' }" @click="activeTab = 'detail'">单篇精读</button>
            <button :class="{ active: activeTab === 'outline' }" @click="activeTab = 'outline'">💡 综述提纲生成</button>
            <button :class="{ active: activeTab === 'graph' }" @click="activeTab = 'graph'">🕸️ 观点传承图谱</button>
          </div>
  
          <div v-if="activeTab === 'detail'" class="tab-content detail-view">
            <div v-if="selectedPaper">
              <h2 class="doc-title">{{ selectedPaper.title }}</h2>
              <div class="ai-card">
                <h4>🎯 核心研究问题 (RQ)</h4>
                <p>{{ selectedPaper.aiSummary?.research_question || '暂无解析数据' }}</p>
              </div>
              <div class="ai-card">
                <h4>🛠️ 研究方法 (Methodology)</h4>
                <p>{{ selectedPaper.aiSummary?.methodology || '暂无解析数据' }}</p>
              </div>
              <div class="ai-card">
                <h4>📊 结论与贡献 (Conclusion)</h4>
                <p>{{ selectedPaper.aiSummary?.conclusion || '暂无解析数据' }}</p>
              </div>
            </div>
            <div v-else class="empty-state">请在左侧选择一篇文献查看 AI 解析结果</div>
          </div>
  
          <div v-if="activeTab === 'outline'" class="tab-content outline-view">
            <div class="actions">
              <p>基于当前文献库中的 {{ papers.length }} 篇文献，一键生成综述结构。</p>
              <button class="btn-generate" @click="generateOutline" :disabled="isGenerating">
                {{ isGenerating ? 'AI 正在疯狂思考中...' : '✨ 自动生成文献综述大纲' }}
              </button>
            </div>
            <div class="outline-result" v-if="generatedOutline">
              <pre>{{ generatedOutline }}</pre>
              <button class="btn-export">导出为 Markdown</button>
            </div>
          </div>
  
          <div v-if="activeTab === 'graph'" class="tab-content graph-view">
            <div class="empty-state">
              🕸️ 知识图谱模块正在开发中... <br>
              <span class="sub-text">(这里未来将渲染各论文间的传承与分歧网络)</span>
            </div>
          </div>
  
        </main>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue'
  import { useRouter } from 'vue-router' // 如果你还没有引入 router
  import request from '@/utils/request.js'

const router = useRouter()

// 1. 定义一个响应式变量，默认给个保底的称呼
const currentUsername = ref('学者') 

onMounted(() => {
  // 2. 页面一加载，就去 localStorage 里找刚才存的名字
  const savedName = localStorage.getItem('current_user')
  
  if (savedName) {
    // 如果找到了，就把 "Quasar" 赋值给页面变量，页面会瞬间自动更新！
    currentUsername.value = savedName 
  }
})


const handleLogout = () => {
  // 1. 彻底销毁门票 (极其关键：同时兼顾 localStorage 和 sessionStorage 两种情况)
  localStorage.removeItem('token')
  sessionStorage.removeItem('token')
  
  // 2. 销毁用户信息痕迹
  localStorage.removeItem('current_user') 

  // 3. 爽快的提示
  alert('已成功退出登录！')
  
  // 4. 此时兜里比脸还干净，保安绝对会痛快地放你回登录页
  router.push('/login') 
}



// 📌 真实接口：查看 PDF 原文
const viewPdf = (paper) => {
  // 1. 防错校验：如果数据库里没有文件路径，提示一下
  if (!paper.filePath) {
    alert('该文献暂时无法找到实体 PDF 文件！')
    return
  }

  // 2. 拼接出后端的访问地址 (我们在 WebMvcConfig 里配好的映射规则)
  const pdfUrl = `${import.meta.env.VITE_APP_PDF_URL}${paper.filePath}`
  
  // 3. 极其优雅：直接在浏览器新开一个页签展示 PDF，不影响当前的工作台页面！
  window.open(pdfUrl, '_blank')
}
// 📌 真实接口：手动触发后端去呼叫 Python 解析
// 📌 真实接口：触发解析并开启自动侦听 (完整版)
const triggerParse = async (paper) => {
  try {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    
    // 1. 立即反馈：让列表显示“解析中”
    paper.status = 1 
    
    // 2. 发送请求给 Java
    await request.post(`/api/papers/${paper.id}/parse`, {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    })

    console.log("🚀 解析任务已提交，启动自动侦听器...")

    // 3. 🌟 开启定时侦听，直到后端“完美入库”
    let retryCount = 0;
    const maxRetries = 15; // 最多等 30 秒
    
    const checkStatus = setInterval(async () => {
      retryCount++;
      try {
        const res = await request.get(`/api/papers/${paper.id}/analysis`, {
          headers: { 'Authorization': `Bearer ${token}` }
        })

        // 如果查到了数据，说明 Java 那边已经执行完 Repository.save 了
        if (res && (res.researchQuestion || res.research_question)) {
          console.log("✅ 侦测到后端入库成功！正在自动渲染...");
          clearInterval(checkStatus);
          
          // 刷新列表（让 status 变成 2，变绿）
          await fetchPapers();
          
          // 自动触发渲染右侧详情
          selectPaper(paper); 
        }
      } catch (err) {
        console.warn("侦听中遇到的微小异常:", err);
      }

      if (retryCount >= maxRetries) {
        clearInterval(checkStatus);
        console.log("⏳ 侦听超时，AI 可能还在思考，请稍后手动点击。");
      }
    }, 2000); // 每 2 秒拉取一次

  } catch (error) {
    console.error('触发解析失败:', error);
    paper.status = 0; // 恢复状态
    alert('解析触发失败，请检查网络或后端服务');
  }
}
// 📌 真实接口：删除文献
const deletePaper = async (paper) => {
  // 危险操作，加一个二次确认防手抖
  if (!confirm(`确定要彻底删除文献《${paper.title}》吗？物理文件也将被销毁！`)) {
    return
  }

  try {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    
    // 调用后端的删除接口
    await request.delete(`/api/papers/${paper.id}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    
    alert('✅ 删除成功！')
    
    // 🌟 核心操作：删除成功后，重新拉取一次列表，让页面上的这条数据消失
    fetchPapers() 
    
  } catch (error) {
    console.error('删除失败:', error)
  }
}
// ==========================================
// 2. 选中文献并拉取 AI 解析数据的方法（独立）
// ==========================================
// 📌 选中左侧文献时触发的方法 (真正发请求的防弹完全体)
const selectPaper = async (paper) => {
  // 1. 先把基础信息赋给当前选中项，并切回详情 Tab
  // 🌟 核心修复点 1：把 paper 彻底解构重新赋值，强制触发 Vue 3 渲染！
  selectedPaper.value = { ...paper };
  activeTab.value = 'detail';
  
  // 2. 🌟 核心修复点 2：在获取新数据前，手动把 aiSummary 挂载一个"加载中"或"空"的初始状态
  // 这样右侧页面会瞬间从旧数据切过来，显示"暂无解析数据"，体验极佳！
  selectedPaper.value.aiSummary = null;

  // 3. 🌟 核心修复点 3：防错保底。万一传入的 paper 本身是 null (比如 fetchPapers 里没取到第一篇)
  if (!paper || !paper.id) {
    console.warn("⚠️ 选中的 paper 为空或没有 ID，终止发请求。");
    return;
  }

  // 4. 去后端拉取这篇文献的 AI 解析结果
  try {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    
    // 调用 GET 接口拉取数据
    // 🌟 核心修复点 4：加上防报错的可选链或逻辑判断！
    const res = await request.get(`/api/papers/${paper.id}/analysis`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    
    console.log("👀 [极其重要] 查到的 AI 解析原始数据:", res);

    // 🌟 核心修复点 5：因为拦截器剥过壳了，直接判断 res 本身存不存在！
    // 之前报错就是因为没做 null 检查直接去 res.data 取值了！
    if (res) {
      // 字段名映射：把 Java 的驼峰命名，赋给前端的下划线模板
      selectedPaper.value.aiSummary = {
        research_question: res.researchQuestion, 
        methodology: res.methodology,
        conclusion: res.conclusion
      }
    } else {
      // res 是 null，说明还没解析完成（异步还在跑）
      // 这里可以打个日志，或者右侧继续显示"暂无解析数据"
      console.log("⚠️ 还没解析完，所以查出来是 null。");
      selectedPaper.value.aiSummary = null;
    }
    
  } catch (error) {
    console.error('获取 AI 解析数据失败:', error);
    selectedPaper.value.aiSummary = null;
  }
}


  
  // 状态定义
  const fileInput = ref(null)
  const isUploading = ref(false)
  const isGenerating = ref(false)
  const papers = ref([]) // 左侧文献列表
  const selectedPaper = ref(null) // 当前选中的文献
  const activeTab = ref('detail') // 默认激活的 Tab
  const generatedOutline = ref('') // 生成的综述文本
  
  // 触发原生上传点击
  const triggerUpload = () => {
    fileInput.value.click()
  }
  
  // 📌 接口预留 1：获取用户的文献库列表
// 📌 真实接口 1：获取用户的文献库列表
const fetchPapers = async () => {
  try {
    // 1. 拿到本地门票
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    
    // 2. 发起真实网络请求
    const res = await request.get('/api/papers/list', {
      headers: { 
        'Authorization': `Bearer ${token}` 
      }
    });

    // 3. 拦截器已经把 Result 外壳剥掉了，res 直接就是那个 List<Paper> 数组
    papers.value = res; 
    
    // 默认选中列表里的第一篇论文（如果列表有数据的话）
    if (papers.value && papers.value.length > 0) {
      selectPaper(papers.value[0]);
    }

  } catch (error) {
    console.error("获取文献列表失败，错误详情:", error);
  }
}
  
// 📌 纯上传逻辑：只负责存库和刷新列表
const handleFileUpload = async (event) => {
  const files = event.target.files
  if (!files.length) return

  const formData = new FormData()
  for (let i = 0; i < files.length; i++) {
    formData.append('file', files[i]) 
  }

  try {
    isUploading.value = true
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');

    // 1. 发送上传请求
    await request.post('/api/papers/upload', formData, {
      headers: { 
        'Content-Type': 'multipart/form-data', 
        'Authorization': `Bearer ${token}` 
      }
    })
    
    // 2. 成功提示
    alert('✅ 文献上传成功！点击列表中的“解析”按钮即可开始分析。')
    
    // 3. 🌟 关键：只刷新列表，不要去触发 selectPaper
    await fetchPapers()

  } catch (error) {
    console.error('上传失败:', error)
  } finally {
    isUploading.value = false
    fileInput.value.value = '' // 清空，方便下次上传
  }
}
  


  
  // 📌 接口预留 3：一键生成综述大纲
  const generateOutline = async () => {
    if (papers.value.length === 0) {
      alert('文献库为空，请先上传文献！')
      return
    }
    
    try {
      isGenerating.value = true
      // 真实接口：
      // const paperIds = papers.value.map(p => p.id)
      // const res = await axios.post('http://localhost:8080/api/ai/generate-outline', { ids: paperIds })
      
      // 模拟大模型流式思考等待
      await new Promise(resolve => setTimeout(resolve, 2500))
      
      generatedOutline.value = `
  # 现代自然语言处理架构演进综述
  
  ## 1. 引言
  近年来，深度学习在 NLP 领域取得了突破性进展。本文将对这一演进过程进行综述。
  
  ## 2. 现有方法分类探讨
  ### 2.1 基础架构的颠覆：从 RNN 到 Transformer
  传统的 RNN 存在并行化困难的问题 [1]。文献 [1] 提出的 Transformer 架构通过引入 Self-Attention 机制，彻底改变了序列建模的方式。
  ### 2.2 预训练范式的崛起：双向上下文表示
  在 Transformer 基础上，文献 [2] 提出了 BERT，通过 MLM 任务进一步挖掘了双向特征...
  
  ## 3. 争议与挑战
  尽管预训练模型性能卓越，但其巨大的参数量带来的算力开销仍是亟待解决的核心痛点。
  
  ## 4. 未来研究方向
  模型轻量化与多模态融合将成为下一阶段的核心趋势。
      `.trim()
    } catch (error) {
      alert('生成失败！')
    } finally {
      isGenerating.value = false
    }
  }
  
  // 初始化时拉取列表
  onMounted(() => {
    fetchPapers()
  })
  </script>
  
  <style scoped>
  /* 学术风格 UI CSS */
  .workbench {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background-color: #f5f7f9;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  }
  
  .header {
    height: 60px;
    background: #2c3e50;
    color: white;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 24px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  }
  
  .logout-btn {
    margin-left: 15px;
    padding: 5px 10px;
    background: #e74c3c;
    border: none;
    color: white;
    border-radius: 4px;
    cursor: pointer;
  }
  
  .main-container {
    display: flex;
    flex: 1;
    overflow: hidden;
  }
  
  /* 左侧文献库 */
  .left-panel {
    width: 320px;
    background: white;
    border-right: 1px solid #e1e4e8;
    display: flex;
    flex-direction: column;
  }
  
  .upload-section {
    padding: 20px;
    border-bottom: 1px solid #e1e4e8;
  }
  
  .btn-upload {
    width: 100%;
    padding: 12px;
    background-color: #4183c4;
    color: white;
    border: none;
    border-radius: 6px;
    font-weight: bold;
    cursor: pointer;
    transition: background 0.2s;
  }
  .btn-upload:hover { background-color: #316eaa; }
  .btn-upload:disabled { background-color: #9cbadd; cursor: not-allowed; }
  
  .paper-list {
    flex: 1;
    overflow-y: auto;
  }
  .paper-list h3 {
    font-size: 14px;
    color: #666;
    padding: 15px 20px 5px;
    margin: 0;
  }
  .paper-list ul {
    list-style: none;
    padding: 0;
    margin: 0;
  }
  .paper-list li {
    padding: 15px 20px;
    border-bottom: 1px solid #f0f2f5;
    cursor: pointer;
    transition: background 0.2s;
  }
  .paper-list li:hover { background: #f8fafc; }
  .paper-list li.active {
    background: #eef2f6;
    border-left: 4px solid #4183c4;
  }
  
  .paper-title {
    font-weight: 500;
    font-size: 14px;
    color: #333;
    margin-bottom: 8px;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  
  .paper-status {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 12px;
  }
 
  .badge.success { background: #e6f4ea; color: #1e8e3e; }
  .badge.processing { background: #fff0f0; color: #d93025; }
  
  /* 右侧解析区 */
  .right-panel {
    flex: 1;
    display: flex;
    flex-direction: column;
    background: #fafbfc;
  }
  
  .tabs {
    display: flex;
    background: white;
    border-bottom: 1px solid #e1e4e8;
    padding: 0 20px;
  }
  .tabs button {
    padding: 15px 20px;
    background: none;
    border: none;
    font-size: 15px;
    color: #555;
    cursor: pointer;
    border-bottom: 3px solid transparent;
  }
  .tabs button.active {
    color: #4183c4;
    font-weight: bold;
    border-bottom-color: #4183c4;
  }
  
  .tab-content {
    flex: 1;
    padding: 30px;
    overflow-y: auto;
  }
  
  .doc-title {
    font-size: 22px;
    color: #24292e;
    margin-bottom: 25px;
  }
  
  .ai-card {
    background: white;
    border: 1px solid #e1e4e8;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 1px 3px rgba(0,0,0,0.02);
  }
  .ai-card h4 {
    margin: 0 0 10px 0;
    color: #4183c4;
    font-size: 16px;
  }
  .ai-card p {
    margin: 0;
    color: #333;
    line-height: 1.6;
  }
  
  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: #888;
    font-size: 16px;
    text-align: center;
  }
  .sub-text { font-size: 13px; color: #bbb; margin-top: 10px; }
  
  /* 综述区 */
  .outline-view .actions {
    background: white;
    padding: 20px;
    border-radius: 8px;
    border: 1px dashed #c0c5ce;
    text-align: center;
    margin-bottom: 20px;
  }
  .btn-generate {
    padding: 10px 24px;
    background: linear-gradient(135deg, #6e8efb, #a777e3);
    color: white;
    border: none;
    border-radius: 20px;
    font-size: 15px;
    cursor: pointer;
    box-shadow: 0 4px 10px rgba(110, 142, 251, 0.3);
  }
  .btn-generate:disabled { opacity: 0.7; cursor: not-allowed; }
  
  .outline-result pre {
    background: white;
    padding: 25px;
    border-radius: 8px;
    border: 1px solid #e1e4e8;
    white-space: pre-wrap;
    font-family: inherit;
    line-height: 1.8;
    color: #333;
  }
  .btn-export {
    margin-top: 15px;
    padding: 8px 16px;
    background: #28a745;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
  }
  /* 查看原文按钮样式 */
.view-pdf-btn {
  padding: 4px 10px;
  font-size: 12px;
  color: #fff;
  background-color: #409eff; /* 经典的 Element 蓝 */
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.view-pdf-btn:hover {
  background-color: #66b1ff;
  transform: translateY(-1px); /* 鼠标悬浮时微微上浮，手感极佳 */
}
.parse-btn {
  margin-left: 10px;
  padding: 4px 10px;
  font-size: 12px;
  color: #fff;
  background-color: #67c23a; /* 经典的 Element 绿色，代表处理/解析 */
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}
.parse-btn:hover {
  background-color: #85ce61;
  transform: translateY(-1px);
}
/* 删除按钮样式 */
.delete-btn {
  margin-left: 10px;
  padding: 4px 10px;
  font-size: 12px;
  color: #fff;
  background-color: #f56c6c; /* Element 危险红 */
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}
.delete-btn:hover {
  background-color: #f89898;
  transform: translateY(-1px);
}
  </style>