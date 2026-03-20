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
              <i class="icon-plus"></i> {{ isUploading ? '正在上传解析中...' : '上传 PDF 文献' }}
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
                  <span class="badge" :class="paper.status === 1 ? 'success' : 'processing'">
                    {{ paper.status === 1 ? '已解析' : '解析中' }}
                  </span>
                  <span class="author">{{ paper.author || '未知作者' }}</span>
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

// 3. 顺手写一个退出登录的方法
const handleLogout = () => {
  localStorage.removeItem('current_user') // 清除登录痕迹
  alert('已退出登录')
  router.push('/login') // 跳回登录页
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
  const fetchPapers = async () => {
    try {
      // 真实接口：const res = await axios.get('http://localhost:8080/api/papers/list')
      // 这里我为你写了一份高质量的假数据，方便你现在就能看到 UI 效果
      papers.value = [
        {
          id: 1, title: 'Attention Is All You Need', author: 'Ashish Vaswani', status: 1,
          aiSummary: {
            research_question: '如何解决传统 RNN 在处理长序列序列时无法并行计算且存在长距离依赖丢失的问题？',
            methodology: '提出了一种完全基于注意力机制（Self-Attention）的新型网络架构 Transformer，彻底摒弃了循环和卷积结构。',
            conclusion: '在机器翻译任务上取得了 SOTA 结果，同时大幅减少了训练时间，证明了纯注意力机制的强大特征提取能力。'
          }
        },
        {
          id: 2, title: 'BERT: Pre-training of Deep Bidirectional Transformers', author: 'Jacob Devlin', status: 1,
          aiSummary: {
            research_question: '如何更好地利用无标注文本进行预训练，以获取深层的双向上下文语言表示？',
            methodology: '基于 Transformer 编码器，提出了 Masked LM (MLM) 和 Next Sentence Prediction (NSP) 两种无监督预训练任务。',
            conclusion: '在 11 项 NLP 核心任务上全面刷新纪录，开启了 NLP 的“预训练+微调”新范式。'
          }
        }
      ]
    } catch (error) {
      console.error("获取文献列表失败", error)
    }
  }
  
// 📌 接口预留 2：上传 PDF 并触发 AI 解析
const handleFileUpload = async (event) => {
  const files = event.target.files
  if (!files.length) return

  const formData = new FormData()
  for (let i = 0; i < files.length; i++) {
    // 这里的 'file' 必须和 SpringBoot 里 @RequestParam("file") 的名字一模一样！
    formData.append('file', files[i]) 
  }

  try {
    isUploading.value = true
    
    // 1. 拿门票
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');

    // 2. 发送请求：直接用 request，写相对路径！
    // 拦截器会自动帮我们扒掉 Axios 的外壳和 Result 的外壳
    const savedPaper = await request.post('/api/papers/upload', formData, {
      headers: { 
        'Content-Type': 'multipart/form-data', 
        'Authorization': `Bearer ${token}` 
      }
    })
    
    // 3. 只要代码能走到这里，说明拦截器判定为 200 成功！
    alert('🎉 文件上传成功，已保存到本地硬盘与数据库！')
    console.log('后端返回的论文记录:', savedPaper) // 你可以在控制台看看这条存入数据库的数据
    
    // fetchPapers() // TODO: 等查询接口写好了，这里解除注释就能刷新列表

  } catch (error) {
    // 4. 如果失败，拦截器会把它踹到这里，拦截器自己已经 alert 过错误信息了，这里只打印日志
    console.error('上传失败，流程终止:', error)
  } finally {
    isUploading.value = false
    fileInput.value.value = '' // 清空 input 框，方便下次上传
  }
}
  
  // 左侧点击文献
  const selectPaper = (paper) => {
    selectedPaper.value = paper
    activeTab.value = 'detail'
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
  .badge {
    padding: 2px 6px;
    border-radius: 10px;
    font-size: 11px;
  }
  .badge.success { background: #e6f4ea; color: #1e8e3e; }
  .badge.processing { background: #fff0f0; color: #d93025; }
  .author { color: #888; }
  
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
  </style>