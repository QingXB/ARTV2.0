<template>
    <div class="workbench">
      <header class="header">
        <div class="header-left">
          <button class="hamburger-btn" @click="sidebarOpen = !sidebarOpen" aria-label="菜单">☰</button>
          <div class="logo">📚 智研 ScholarAI 工作台</div>
        </div>
        <div class="user-info">
          <span>欢迎, {{ currentUsername }}</span>
          <button class="logout-btn" @click="handleLogout">退出</button>
        </div>
      </header>
  
      <div class="main-container">
        <div class="sidebar-overlay" v-if="sidebarOpen" @click="sidebarOpen = false"></div>
        <aside class="left-panel" :class="{ open: sidebarOpen }">
          <div class="upload-section">
            <input type="file" ref="fileInput" multiple accept="application/pdf" @change="handleFileUpload" style="display: none;" />
            <button class="btn-upload" @click="triggerUpload" :disabled="isUploading">
              <i class="icon-plus"></i> {{ isUploading ? '上传中...' : '上传 PDF' }}
            </button>
            <button class="btn-batch-parse" @click="parseAllPapers" title="一键解析所有待解析文献">
              🔥 批量解析
            </button>
          </div>

          <div class="search-section">
            <input
              type="text"
              v-model="searchKeyword"
              placeholder="搜索文献标题..."
              @keyup.enter="handleSearch"
              class="search-input"
            />
            <select v-model="filterStatus" @change="handleSearch" class="filter-select">
              <option :value="null">全部状态</option>
              <option :value="0">待解析</option>
              <option :value="1">解析中</option>
              <option :value="2">已解析</option>
              <option :value="3">解析失败</option>
            </select>
          </div>

          <div class="paper-list">
            <h3>文献库 ({{ totalPapers }})</h3>

            <!-- 加载状态 -->
            <div v-if="isLoading" class="loading-state">
              <div class="spinner"></div>
              <p>加载中...</p>
            </div>

            <!-- 空状态引导 -->
            <div v-else-if="papers.length === 0" class="empty-state">
              <div class="empty-icon">📚</div>
              <p v-if="searchKeyword || filterStatus !== null">没有找到匹配的文献</p>
              <p v-else>暂无文献</p>
              <p class="empty-hint">点击上方「上传 PDF」添加第一篇文献</p>
            </div>

            <!-- 文献列表 -->
            <ul v-else>
              <li 
                v-for="paper in papers" 
                :key="paper.id" 
                :class="{ active: selectedPaper?.id === paper.id }"
                @click="selectPaper(paper)"
              >
                <div class="paper-title" :title="paper.title">{{ paper.title || '未知文献' }}</div>
                <div class="paper-footer">
  <div class="status-cell">
    <span v-if="paper.parseStatus === 0" class="tag tag-gray">待解析</span>
    <span v-if="paper.parseStatus === 1" class="tag tag-blue">⏳ 努力解析中...</span>
    <span v-if="paper.parseStatus === 2" class="tag tag-green">✅ 已解析</span>
    <span v-if="paper.parseStatus === 3" class="tag tag-red" title="大模型网络波动，请重试">❌ 解析失败</span>
  </div>

  <div class="action-buttons">
    <button class="btn btn-primary" @click.stop="viewPdf(paper)">查看原文</button>

    <button v-if="paper.parseStatus === 0" class="btn btn-action" @click.stop="triggerParse(paper)">解析 PDF</button>
    <button v-if="paper.parseStatus === 1" class="btn btn-disabled" disabled>解析中...</button>
    <button v-if="paper.parseStatus === 2" class="btn btn-success" disabled>解析完成</button>
    <button v-if="paper.parseStatus === 3" class="btn btn-retry" @click.stop="triggerParse(paper)">🔄 重试解析</button>

    <button class="btn btn-danger" @click.stop="deletePaper(paper)">删除</button>
  </div>
</div>
              </li>
            </ul>

            <!-- 分页控制 -->
            <div class="pagination" v-if="totalPages > 1">
              <button @click="goToPage(currentPage - 1)" :disabled="currentPage === 0">上一页</button>
              <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
              <button @click="goToPage(currentPage + 1)" :disabled="currentPage >= totalPages - 1">下一页</button>
            </div>
          </div>
        </aside>
  
        <main class="right-panel">
          <div class="tabs">
            <button :class="{ active: activeTab === 'detail' }" @click="activeTab = 'detail'">单篇精读</button>
            <button :class="{ active: activeTab === 'outline' }" @click="activeTab = 'outline'">💡 综述提纲生成</button>
            <!-- <button :class="{ active: activeTab === 'graph' }" @click="activeTab = 'graph'; activeGraphTab = 'relation'">🕸️ 观点传承图谱</button> -->
            <button :class="{ active: activeTab === 'similarity' }" @click="activeTab = 'similarity'">📊 语义相似度图谱</button>
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
            
            <div class="actions" v-if="!isSelectingPapers && !generatedOutline">
  <p>基于文献库中已解析的文献，进行多文档观点碰撞，生成结构化综述。</p>
  <button class="btn-generate" @click="startSelection">
    ✨ 自动生成新综述大纲
  </button>

  <div class="history-panel" v-if="reviewHistoryList.length > 0">
    <h3 class="history-title">📚 我的历史综述</h3>
    <div class="history-cards">
      <div 
        class="history-card" 
        v-for="(task, index) in reviewHistoryList" 
        :key="task.id"
        @click="viewHistory(task)"
      >
        <div class="card-icon">📄</div>
        <div class="card-info">
          <h4>多文档智能综述 #{{ reviewHistoryList.length - index }}</h4>
          <p>生成时间: {{ new Date(task.createdAt).toLocaleString() }}</p>
        </div>
        <button class="btn-view">查看</button>
      </div>
    </div>
  </div>
</div>

            <div class="selection-panel actions" v-if="isSelectingPapers && !generatedOutline">
  <h3>📑 请勾选需要参与综述的文献（至少2篇）</h3>
  <p class="hint">注：只有“已解析”状态的文献才能参与多文档综述。</p>
  
  <div class="paper-checklist">
    <div v-for="paper in papers" :key="paper.id" class="check-item" :class="{ disabled: paper.parseStatus !== 2 }">
      <input 
        type="checkbox" 
        :id="'chk-' + paper.id" 
        :value="paper.id" 
        v-model="selectedPaperIds"
        :disabled="paper.parseStatus !== 2"
      >
      <label :for="'chk-' + paper.id">
        {{ paper.title }} 
        <span v-if="paper.parseStatus !== 2" class="status-hint">(需先完成单篇解析)</span>
      </label>
    </div>
  </div>
  
  <div class="bottom-actions">
    <button class="btn-cancel" @click="isSelectingPapers = false" :disabled="isGenerating">取消返回</button>
    
    <button class="btn-generate" @click="doGenerateOutline" :disabled="selectedPaperIds.length < 2 || isGenerating">
      {{ isGenerating ? '🧠 AI 疯狂融合思考中...' : `🚀 开始生成 (已选 ${selectedPaperIds.length} 篇)` }}
    </button>
  </div>

  <div v-if="isGenerating" style="margin-top: 15px; color: #ff9800; font-size: 14px; text-align: center;">
    ⏳ 正在后台深度对比多篇文献。由于内容极长，通常需要 1~2 分钟，请耐心等待，大纲马上呈现...
  </div>
</div>

            <div class="outline-result" v-if="generatedOutline">
              <div class="result-header">
                <button class="btn-cancel" @click="resetOutline">🔄 重新选择文献</button>
              </div>
              <pre>{{ generatedOutline }}</pre>
              <button class="btn-export" @click="exportToMarkdown">📥 导出为 Markdown</button>
            </div>

          </div>

          <div v-if="activeTab === 'graph'" class="tab-content graph-view">
            <div v-if="!analyzedRelations.length && !isAnalyzing">
              <p>分析多篇文献之间的关系，识别观点传承、矛盾和支持。</p>
              <button class="btn-generate" @click="startRelationAnalysis">
                🔍 开始关系分析
              </button>
            </div>

            <div v-if="isAnalyzing" class="loading-state">
              <p>🧠 AI 正在分析文献关系，请稍候...</p>
            </div>

            <div v-if="analyzedRelations.length > 0" class="relations-result">
              <div class="result-header">
                <button class="btn-cancel" @click="analyzedRelations = []">🔄 重新分析</button>
                <h3>发现 {{ analyzedRelations.length }} 对关系</h3>
              </div>

              <div class="relation-list">
                <div v-for="(rel, index) in analyzedRelations" :key="index" class="relation-card">
                  <div class="relation-type" :class="rel.relationType.toLowerCase()">
                    <span v-if="rel.relationType === 'INHERIT'">📚 传承</span>
                    <span v-if="rel.relationType === 'CONTRADICT'">⚔️ 矛盾</span>
                    <span v-if="rel.relationType === 'SUPPORT'">🤝 支持</span>
                  </div>
                  <div class="relation-desc">{{ rel.description }}</div>
                  <div class="relation-papers">
                    文献 {{ rel.sourcePaperId }} → 文献 {{ rel.targetPaperId }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="activeTab === 'similarity'" class="tab-content similarity-view">
            <SimilarityGraph @send-to-review="handleSendToReview" />
          </div>

        </main>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue'
  import { useRouter } from 'vue-router' // 如果你还没有引入 router
  import request from '@/utils/request.js'
  import SimilarityGraph from './SimilarityGraph.vue'

const router = useRouter()
// 存放历史记录的数组
const reviewHistoryList = ref([]);
// 存放关系分析结果的数组
const analyzedRelations = ref([]);
const isAnalyzing = ref(false);
// 1. 定义一个响应式变量，默认给个保差的称呼
const currentUsername = ref('学者') 
// 🌟 去后端拉取历史记录的方法
const fetchReviewHistory = async () => {
  try {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const res = await request.get('/api/papers/review-history', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    // 取出成功状态 (status === 2) 的历史记录
    const allHistory = res.data || res;
    reviewHistoryList.value = allHistory.filter(task => task.status === 2);
  } catch (error) {
    console.error("获取综述历史失败:", error);
  }
};

// 🌟 点击历史记录，直接把内容显示到屏幕上！
const viewHistory = (task) => {
  generatedOutline.value = task.content;
};

// 🌟 开始关系分析
const startRelationAnalysis = async () => {
  // 获取已解析的文献
  const parsedPapers = papers.value.filter(p => p.parseStatus === 2);
  if (parsedPapers.length < 2) {
    alert('至少需要2篇已解析的文献才能分析关系！');
    return;
  }

  const paperIds = parsedPapers.map(p => p.id);

  try {
    isAnalyzing.value = true;
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const res = await request.post('/api/papers/analyze-relations', { paperIds }, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    analyzedRelations.value = res.data || [];
  } catch (error) {
    console.error("关系分析失败:", error);
    alert('关系分析失败，请重试');
  } finally {
    isAnalyzing.value = false;
  }
};

// 🌟 页面加载时自动去拉一次历史记录
onMounted(() => {
  fetchReviewHistory();
  // 页面一加载，就去 localStorage 里找刚才存的名字
  const savedName = localStorage.getItem('current_user')
  if (savedName) {
    currentUsername.value = savedName
  }
});


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
// 假设你之前有一个拉取文献列表的方法叫 fetchPapers
const triggerParse = async (paper) => {
  try {
    // 1. 乐观更新：界面立刻变成“解析中”
    paper.parseStatus = 1;
    
    // 2. 发送解析请求给 Java
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    await request.post(`/api/papers/${paper.id}/parse`, {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    // 3. 🌟 核心：开启“探子”轮询模式，每 3 秒查一次最新状态
    const timer = setInterval(async () => {
      try {
        // 重新拉取一次文献列表
        const res = await request.get('/api/papers/list', {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        
        // 找到当前正在解析的这篇文献的新数据
        const rawData = res.data || res;
        const papersList = Array.isArray(rawData) ? rawData : (rawData.content || []);
        const updatedPaper = papersList.find(p => p.id === paper.id);

        if (updatedPaper) {
          // 实时将后台的新状态同步到前端视图上！
          paper.parseStatus = updatedPaper.parseStatus;

          // 🌟 侦测到状态变更，结束轮询！
          if (paper.parseStatus === 2) {
            clearInterval(timer); // 砸掉定时器
            console.log(`文献 [${paper.title}] 解析成功！`);

            // ==========================================
            // 🌟 核心绝杀修改区：无刷新渲染右侧面板
            // ==========================================
            // 1. 将后台最新拉取到的包含解析内容的数据，无缝覆盖给当前的 paper 对象
            Object.assign(paper, updatedPaper);

            // 2. ⚠️ 注意：如果你的右侧面板是绑定在一个类似 activePaper 或 selectedPaper 的变量上，
            // 并且当前右侧面板正在显示这篇刚解析完的文献，你需要让它也更新一下。
            // 比如 (请把 activePaper 换成你实际使用的变量名)：
            if (selectedPaper.value && selectedPaper.value.id === paper.id) {
              selectPaper(paper); 
            }
            // ==========================================

          } else if (paper.parseStatus === 3) {
            clearInterval(timer); // 砸掉定时器
            console.error(`文献 [${paper.title}] 解析失败！`);
            alert(`文献《${paper.title}》解析遇到网络波动，请点击“重试”按钮`);
          }
        }
      } catch (pollErr) {
        console.error("轮询状态异常", pollErr);
      }
    }, 1000); // 每 3 秒问一次

  } catch (error) {
    console.error("触发解析失败", error);
    paper.parseStatus = 3; // 如果刚点下去网络就断了，直接变失败
    alert('请求发送失败，请检查网络');
  }
};
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
  sidebarOpen.value = false; // 手机端选中后自动收起侧边栏
  
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
  const sidebarOpen = ref(false)
  const papers = ref([]) // 左侧文献列表
  const selectedPaper = ref(null) // 当前选中的文献
  const activeTab = ref('detail')
  const activeGraphTab = ref('relation') // 图谱子标签页
  const generatedOutline = ref('')
  const searchKeyword = ref('')
  const filterStatus = ref(null)
  const currentPage = ref(0)
  const pageSize = ref(10)
  const totalPapers = ref(0)
  const totalPages = ref(0)
  const isLoading = ref(false)
  
  // 触发原生上传点击
  const triggerUpload = () => {
    fileInput.value.click()
  }
  
  // 📌 获取用户的文献库列表（支持分页、搜索、筛选）
const fetchPapers = async (page = 0, size = 10) => {
  try {
    isLoading.value = true
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');

    const params = new URLSearchParams({ page, size })
    if (searchKeyword.value) params.append('keyword', searchKeyword.value)
    if (filterStatus.value !== null) params.append('status', filterStatus.value)

    const res = await request.get(`/api/papers/list?${params.toString()}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    papers.value = res.content || res
    totalPapers.value = res.totalElements || res.length || 0
    totalPages.value = res.totalPages || 1
    currentPage.value = res.page || page

    if (papers.value && papers.value.length > 0) {
      selectPaper(papers.value[0])
    }

  } catch (error) {
    console.error("获取文献列表失败:", error)
  } finally {
    isLoading.value = false
  }
}

// 搜索和筛选
const handleSearch = () => {
  currentPage.value = 0
  fetchPapers(0, pageSize.value)
}

// 批量解析
const parseAllPapers = async () => {
  const unparsedPapers = papers.value.filter(p => p.parseStatus === 0)
  if (unparsedPapers.length === 0) {
    alert('没有待解析的文献')
    return
  }
  if (!confirm(`确定要批量解析 ${unparsedPapers.length} 篇文献吗？`)) return

  try {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    await request.post('/api/papers/parse-all', {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    alert(`已提交 ${unparsedPapers.length} 篇文献的解析任务，请耐心等待...`)
    await fetchPapers()
  } catch (error) {
    console.error('批量解析失败:', error)
    alert('批量解析失败，请重试')
  }
}

// 分页
const goToPage = (page) => {
  if (page < 0 || page >= totalPages.value) return
  fetchPapers(page, pageSize.value)
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
    
    // 3. 🌟 关键：只刷新列表
    await fetchPapers(0, pageSize.value)

  } catch (error) {
    console.error('上传失败:', error)
  } finally {
    isUploading.value = false
    fileInput.value.value = '' // 清空，方便下次上传
  }
}
  


  
// 🌟 新增：用于控制页面状态的变量
const isSelectingPapers = ref(false) // 是否正在显示打勾面板
const selectedPaperIds = ref([])     // 存放用户打勾选中的论文 ID 数组

// 1. 点击“✨ 自动生成文献综述大纲”触发，进入选择模式
const startSelection = () => {
  selectedPaperIds.value = [] // 每次打开先清空之前的选择
  isSelectingPapers.value = true
}

// 2. 如果对结果不满意，点击重新选择文献
const resetOutline = () => {
  generatedOutline.value = ''
  isSelectingPapers.value = true
}

const doGenerateOutline = async () => {
  if (selectedPaperIds.value.length < 2) {
    alert('为了进行观点交叉对比，请至少选择 2 篇已解析的文献！')
    return
  }
  
  try {
    isGenerating.value = true
    generatedOutline.value = '' // 清空之前的结果
    
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    const headers = { 'Authorization': `Bearer ${token}` }
    
    // ==========================================
    // 第一步：提交任务，秒拿“取餐牌” (Task ID)
    // ==========================================
    const submitRes = await request.post('/api/papers/generate-async', { 
      paperIds: selectedPaperIds.value 
    }, { headers })
    
    const taskId = submitRes.data || submitRes
    
    if (!taskId) {
      throw new Error("未能从后端获取到任务ID")
    }
    
    console.log(`✅ 任务提交成功！拿到取餐牌：TaskID = ${taskId}，开始轮询...`)

    // ==========================================
    // 🌟🌟🌟 核心修复：把丢失的计数器变量补回来！
    // ==========================================
    let pollCount = 0
    const maxPolls = 60 // 最多问 60 次 (5秒 * 60 = 5分钟)
    
    // ==========================================
    // 第二步：开启定时器，每 5 秒查一次进度
    // ==========================================
    const timer = setInterval(async () => {
      pollCount++ // 每次轮询加 1
      
      try {
        const statusRes = await request.get(`/api/papers/task-status/${taskId}`, { headers })
        
        // 剥离外壳，拿到真正的 ReviewTask 数据
        const task = statusRes.code === 200 ? statusRes.data : (statusRes.data || statusRes)
        
        console.log(`🕵️ 第 ${pollCount} 次轮询，最新状态：`, task)

        // 🟢 状态 2：生成成功！
        if (task.status === 2) { 
          clearInterval(timer) // 砸掉定时器
          
          // 渲染 3100 字的大模型结果！
          generatedOutline.value = task.content 
          
          isGenerating.value = false
          isSelectingPapers.value = false 
          console.log("🎉 轮询结束，大模型生成成功！渲染到页面！")
          
        } 
        // 🔴 状态 3：生成失败
        else if (task.status === 3) { 
          clearInterval(timer)
          alert('AI 生成失败: ' + (task.errorMessage || '大模型开小差了'))
          isGenerating.value = false
          
        } 
        // 🟡 超时保底：超过 5 分钟还没好，放过前端吧
        else if (pollCount >= maxPolls) { 
          clearInterval(timer)
          alert('AI 思考时间太长，请稍后刷新页面查看历史记录。')
          isGenerating.value = false
        }
        
      } catch (pollError) {
        console.error('轮询请求异常:', pollError)
        // 网络偶尔抖动可以不砸定时器，让它继续尝试
      }
      
    }, 5000) // 5000 毫秒 = 5 秒

  } catch (error) {
    alert('提交综述任务失败，请检查网络或后端报错！')
    console.error('综述提交异常:', error)
    isGenerating.value = false
  }
}
// ==========================================
// 🌟 核心功能：纯前端导出 Markdown 文件
// ==========================================
const exportToMarkdown = () => {
  // 1. 防呆校验：如果没有内容，直接 return
  if (!generatedOutline.value) {
    alert('暂无大纲内容可导出！');
    return;
  }

  // 2. 将大纲字符串转换为 Blob 对象，指定 MIME 类型为 markdown
  const blob = new Blob([generatedOutline.value], { type: 'text/markdown;charset=utf-8' });

  // 3. 在浏览器内存里为这个 Blob 对象生成一个临时的下载链接
  const url = URL.createObjectURL(blob);

  // 4. 动态创建一个隐藏的 <a> 标签
  const link = document.createElement('a');
  link.href = url;
  
  // 5. 生成高逼格的文件名（包含当天的日期）
  const dateStr = new Date().toISOString().slice(0, 10); // 格式：2026-03-25
  link.download = `智研AI_文献综述大纲_${dateStr}.md`; 

  // 6. 把这个隐藏的 <a> 标签塞进页面，模拟用户点击，然后再光速销毁它
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  // 7. 释放内存，好习惯
  URL.revokeObjectURL(url);
  
  console.log('🎉 导出成功！');
};

// ==========================================
// 🌟 语义相似度图谱：发送选中节点到综述模块
// ==========================================
const handleSendToReview = (paperIds) => {
  if (paperIds.length < 2) {
    alert('请至少选择2篇文献！');
    return;
  }
  
  selectedPaperIds.value = paperIds;
  generatedOutline.value = '';
  activeTab.value = 'outline';
  isSelectingPapers.value = true;
  
  // alert(`已选择 ${paperIds.length} 篇文献，请点击"开始生成"按钮创建综述！`);
};
  
  // 初始化时拉取列表
  onMounted(() => {
    fetchPapers()
  })
  </script>
  
  <style scoped>
  /* ============================================
   * 设计令牌 (Design Tokens)
   * ============================================ */
  .workbench {
    /* 主色系 - Indigo/Violet */
    --primary: #6366f1;
    --primary-hover: #4f46e5;
    --primary-light: #eef2ff;
    --accent: #8b5cf6;
    --gradient-primary: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
    --gradient-hover: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
    --gradient-subtle: linear-gradient(135deg, #eef2ff 0%, #f5f3ff 100%);

    /* 语义色 */
    --success: #10b981;
    --success-bg: #ecfdf5;
    --success-border: #a7f3d0;
    --warning: #f59e0b;
    --warning-bg: #fffbeb;
    --danger: #ef4444;
    --danger-bg: #fef2f2;
    --danger-border: #fecaca;
    --info: #3b82f6;
    --info-bg: #eff6ff;

    /* 中性色 */
    --bg: #f8fafc;
    --surface: #ffffff;
    --surface-muted: #f8fafc;
    --border: #e2e8f0;
    --border-subtle: #f1f5f9;

    /* 文字 */
    --text: #0f172a;
    --text-secondary: #475569;
    --text-muted: #94a3b8;

    /* 阴影 */
    --shadow-sm: 0 1px 2px 0 rgba(15, 23, 42, 0.04);
    --shadow-md: 0 4px 6px -1px rgba(15, 23, 42, 0.06), 0 2px 4px -2px rgba(15, 23, 42, 0.03);
    --shadow-lg: 0 10px 15px -3px rgba(15, 23, 42, 0.08), 0 4px 6px -4px rgba(15, 23, 42, 0.04);
    --shadow-glow: 0 8px 24px -6px rgba(99, 102, 241, 0.35);

    /* 圆角 */
    --radius-sm: 6px;
    --radius: 10px;
    --radius-lg: 14px;
    --radius-xl: 20px;
    --radius-full: 999px;

    /* 动画 */
    --transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    --transition-slow: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    display: flex;
    flex-direction: column;
    height: 100vh;
    background: var(--bg);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Inter", "PingFang SC", "Microsoft YaHei", sans-serif;
    color: var(--text);
    -webkit-font-smoothing: antialiased;
  }

  /* 装饰性背景光晕 */
  .workbench::before {
    content: "";
    position: fixed;
    top: -50%;
    right: -20%;
    width: 60%;
    height: 80%;
    background: radial-gradient(circle, rgba(139, 92, 246, 0.08) 0%, transparent 60%);
    pointer-events: none;
    z-index: 0;
  }

  /* ============================================
   * Header - 顶部导航
   * ============================================ */
  .header {
    position: relative;
    z-index: 10;
    height: 64px;
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: saturate(180%) blur(14px);
    -webkit-backdrop-filter: saturate(180%) blur(14px);
    border-bottom: 1px solid var(--border-subtle);
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 28px;
    color: var(--text);
  }

  .logo {
    font-size: 18px;
    font-weight: 700;
    background: var(--gradient-primary);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    letter-spacing: 0.3px;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 14px;
    font-size: 14px;
    color: var(--text-secondary);
  }

  .logout-btn {
    margin-left: 4px;
    padding: 6px 14px;
    background: var(--surface);
    border: 1px solid var(--border);
    color: var(--text-secondary);
    border-radius: var(--radius-full);
    cursor: pointer;
    font-size: 13px;
    font-weight: 500;
    transition: var(--transition);
  }
  .logout-btn:hover {
    background: var(--danger-bg);
    border-color: var(--danger-border);
    color: var(--danger);
  }

  /* ============================================
   * 主体布局
   * ============================================ */
  .main-container {
    position: relative;
    z-index: 1;
    display: flex;
    flex: 1;
    overflow: hidden;
  }

  /* ============================================
   * 左侧栏 - 文献库
   * ============================================ */
  .left-panel {
    width: 340px;
    background: var(--surface);
    border-right: 1px solid var(--border-subtle);
    display: flex;
    flex-direction: column;
    box-shadow: var(--shadow-sm);
  }

  .upload-section {
    padding: 20px;
    border-bottom: 1px solid var(--border-subtle);
  }

  .btn-upload {
    width: 100%;
    margin-bottom: 8px;
    background: var(--gradient-primary);
    color: white;
    border: none;
    border-radius: var(--radius);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: var(--transition);
    box-shadow: var(--shadow-glow);
    letter-spacing: 0.3px;
    padding: 10px 16px;
  }
  .btn-batch-parse {
    width: 100%;
    padding: 10px 16px;
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    color: white;
    border: none;
    border-radius: var(--radius);
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: var(--transition);
  }
  .btn-batch-parse:hover {
    background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
    transform: translateY(-1px);
  }

  .search-section {
    padding: 12px 20px;
    border-bottom: 1px solid var(--border-subtle);
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .search-input {
    width: 100%;
    padding: 8px 12px;
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 13px;
    outline: none;
    transition: var(--transition);
  }
  .search-input:focus {
    border-color: var(--primary);
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
  }
  .filter-select {
    width: 100%;
    padding: 8px 12px;
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 13px;
    background: var(--surface);
    cursor: pointer;
    outline: none;
  }
  .btn-upload:hover {
    background: var(--gradient-hover);
    transform: translateY(-1px);
    box-shadow: 0 10px 28px -6px rgba(99, 102, 241, 0.45);
  }
  .btn-upload:active { transform: translateY(0); }
  .btn-upload:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
    box-shadow: var(--shadow-sm);
  }

  .paper-list {
    flex: 1;
    overflow-y: auto;
  }
  .paper-list::-webkit-scrollbar { width: 6px; }
  .paper-list::-webkit-scrollbar-thumb {
    background: var(--border);
    border-radius: 3px;
  }
  .paper-list::-webkit-scrollbar-thumb:hover { background: var(--text-muted); }

  .paper-list h3 {
    font-size: 11px;
    font-weight: 600;
    color: var(--text-muted);
    padding: 18px 20px 10px;
    margin: 0;
    text-transform: uppercase;
    letter-spacing: 0.8px;
  }
  .paper-list ul {
    list-style: none;
    padding: 0 8px;
    margin: 0;
  }
  .paper-list li {
    padding: 14px 16px;
    margin-bottom: 4px;
    border-radius: var(--radius);
    cursor: pointer;
    transition: var(--transition);
    border: 1px solid transparent;
  }
  .paper-list li:hover {
    background: var(--surface-muted);
    border-color: var(--border-subtle);
  }
  .paper-list li.active {
    background: var(--primary-light);
    border-color: rgba(99, 102, 241, 0.25);
    box-shadow: inset 3px 0 0 var(--primary);
  }

  .paper-title {
    font-weight: 600;
    font-size: 14px;
    color: var(--text);
    margin-bottom: 10px;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  /* ============================================
   * 文献卡片底部 - 状态 + 操作按钮
   * ============================================ */
  .paper-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 10px;
    padding-top: 10px;
    border-top: 1px dashed var(--border);
    gap: 8px;
    flex-wrap: wrap;
  }

  .status-cell {
    flex-shrink: 0;
    white-space: nowrap;
  }

  /* 状态 Tag */
  .tag {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 11px;
    font-weight: 600;
    padding: 3px 10px;
    border-radius: var(--radius-full);
    border: 1px solid transparent;
  }
  .tag-gray {
    background: #f1f5f9;
    color: #64748b;
    border-color: #e2e8f0;
  }
  .tag-blue {
    background: var(--info-bg);
    color: var(--info);
    border-color: #bfdbfe;
    animation: pulse-blue 1.8s ease-in-out infinite;
  }
  .tag-green {
    background: var(--success-bg);
    color: var(--success);
    border-color: var(--success-border);
  }
  .tag-red {
    background: var(--danger-bg);
    color: var(--danger);
    border-color: var(--danger-border);
  }
  @keyframes pulse-blue {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.6; }
  }

  /* 操作按钮组 */
  .action-buttons {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  /* 统一按钮系统 */
  .btn {
    padding: 5px 11px;
    font-size: 12px;
    font-weight: 500;
    border-radius: var(--radius-sm);
    border: 1px solid transparent;
    cursor: pointer;
    transition: var(--transition);
    white-space: nowrap;
    letter-spacing: 0.1px;
  }
  .btn:hover { transform: translateY(-1px); }
  .btn:active { transform: translateY(0); }

  .btn-primary {
    background: var(--primary);
    color: white;
    box-shadow: 0 2px 6px -1px rgba(99, 102, 241, 0.35);
  }
  .btn-primary:hover {
    background: var(--primary-hover);
    box-shadow: 0 4px 10px -2px rgba(99, 102, 241, 0.45);
  }

  .btn-action {
    background: var(--surface);
    border-color: var(--border);
    color: var(--text-secondary);
  }
  .btn-action:hover {
    background: var(--primary-light);
    border-color: rgba(99, 102, 241, 0.3);
    color: var(--primary);
  }

  .btn-danger {
    background: var(--surface);
    border-color: var(--danger-border);
    color: var(--danger);
  }
  .btn-danger:hover {
    background: var(--danger-bg);
    border-color: var(--danger);
  }

  .btn-disabled {
    background: var(--surface-muted);
    color: var(--text-muted);
    cursor: not-allowed;
    border-color: var(--border-subtle);
  }
  .btn-disabled:hover { transform: none; }

  .btn-success {
    background: var(--success-bg);
    border-color: var(--success-border);
    color: var(--success);
    cursor: default;
  }
  .btn-success:hover { transform: none; }

  .btn-retry {
    background: var(--danger-bg);
    border-color: var(--danger-border);
    color: var(--danger);
    font-weight: 600;
  }
  .btn-retry:hover { background: #fee2e2; }

  /* ============================================
   * 右侧面板
   * ============================================ */
  .right-panel {
    flex: 1;
    display: flex;
    flex-direction: column;
    background: var(--bg);
    overflow: hidden;
  }

  .tabs {
    display: flex;
    background: var(--surface);
    border-bottom: 1px solid var(--border-subtle);
    padding: 0 24px;
    gap: 4px;
  }
  .tabs button {
    position: relative;
    padding: 16px 20px;
    background: none;
    border: none;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-secondary);
    cursor: pointer;
    transition: var(--transition);
  }
  .tabs button::after {
    content: "";
    position: absolute;
    left: 20px;
    right: 20px;
    bottom: -1px;
    height: 2px;
    background: var(--gradient-primary);
    border-radius: 2px 2px 0 0;
    transform: scaleX(0);
    transform-origin: center;
    transition: transform 0.25s ease;
  }
  .tabs button:hover { color: var(--primary); }
  .tabs button.active {
    color: var(--primary);
    font-weight: 600;
  }
  .tabs button.active::after { transform: scaleX(1); }

  .tab-content {
    flex: 1;
    padding: 32px;
    overflow-y: auto;
  }
  .tab-content::-webkit-scrollbar { width: 8px; }
  .tab-content::-webkit-scrollbar-thumb {
    background: var(--border);
    border-radius: 4px;
  }

  /* ============================================
   * 详情视图 - AI 解析卡片
   * ============================================ */
  .doc-title {
    font-size: 22px;
    font-weight: 700;
    color: var(--text);
    margin: 0 0 24px 0;
    line-height: 1.4;
    letter-spacing: -0.2px;
  }

  .ai-card {
    position: relative;
    background: var(--surface);
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-lg);
    padding: 22px 24px;
    margin-bottom: 16px;
    box-shadow: var(--shadow-sm);
    transition: var(--transition);
    overflow: hidden;
  }
  .ai-card::before {
    content: "";
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    background: var(--gradient-primary);
    opacity: 0.7;
  }
  .ai-card:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-1px);
    border-color: rgba(99, 102, 241, 0.2);
  }
  .ai-card h4 {
    margin: 0 0 10px 0;
    color: var(--primary);
    font-size: 14px;
    font-weight: 600;
    letter-spacing: 0.2px;
  }
  .ai-card p {
    margin: 0;
    color: var(--text);
    font-size: 14.5px;
    line-height: 1.7;
  }

  /* ============================================
   * 空状态 & 加载状态
   * ============================================ */
  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: var(--text-muted);
    font-size: 15px;
    text-align: center;
    gap: 12px;
    padding: 60px 20px;
  }
  .empty-icon {
    font-size: 48px;
    margin-bottom: 8px;
  }
  .empty-hint {
    font-size: 13px;
    color: var(--text-muted);
    margin-top: 8px;
  }

  .loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 20px;
    color: var(--text-secondary);
  }
  .spinner {
    width: 32px;
    height: 32px;
    border: 3px solid var(--border);
    border-top-color: var(--primary);
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin-bottom: 12px;
  }
  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  .pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 12px;
    padding: 16px 20px;
    border-top: 1px solid var(--border-subtle);
  }
  .pagination button {
    padding: 6px 14px;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 13px;
    cursor: pointer;
    transition: var(--transition);
  }
  .pagination button:hover:not(:disabled) {
    background: var(--primary-light);
    border-color: var(--primary);
    color: var(--primary);
  }
  .pagination button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  .pagination span {
    font-size: 13px;
    color: var(--text-secondary);
  }
  .empty-state::before {
    content: "";
    width: 72px;
    height: 72px;
    background: var(--gradient-subtle);
    border-radius: 50%;
    margin-bottom: 8px;
    box-shadow: inset 0 0 0 1px rgba(99, 102, 241, 0.15);
  }
  .sub-text {
    font-size: 13px;
    color: var(--text-muted);
    margin-top: 8px;
  }

  .loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 20px;
    text-align: center;
    color: var(--text-secondary);
    font-size: 15px;
    background: var(--surface);
    border-radius: var(--radius-lg);
    border: 1px solid var(--border-subtle);
  }

  /* ============================================
   * 综述大纲视图
   * ============================================ */
  .outline-view .actions {
    background: var(--surface);
    padding: 40px 32px;
    border-radius: var(--radius-lg);
    border: 1px solid var(--border-subtle);
    text-align: center;
    margin-bottom: 20px;
    box-shadow: var(--shadow-sm);
  }
  .outline-view .actions > p {
    color: var(--text-secondary);
    font-size: 15px;
    line-height: 1.6;
    margin: 0 0 20px 0;
  }

  .btn-generate {
    padding: 12px 28px;
    background: var(--gradient-primary);
    color: white;
    border: none;
    border-radius: var(--radius-full);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: var(--transition);
    box-shadow: var(--shadow-glow);
    letter-spacing: 0.3px;
  }
  .btn-generate:hover {
    background: var(--gradient-hover);
    transform: translateY(-1px);
    box-shadow: 0 10px 28px -6px rgba(99, 102, 241, 0.5);
  }
  .btn-generate:disabled {
    opacity: 0.65;
    cursor: not-allowed;
    transform: none;
  }

  .outline-result pre {
    background: var(--surface);
    padding: 28px 32px;
    border-radius: var(--radius-lg);
    border: 1px solid var(--border-subtle);
    box-shadow: var(--shadow-sm);
    white-space: pre-wrap;
    font-family: inherit;
    line-height: 1.8;
    color: var(--text);
    font-size: 14.5px;
    margin: 0;
  }

  .btn-export {
    margin-top: 16px;
    padding: 10px 20px;
    background: var(--success);
    color: white;
    border: none;
    border-radius: var(--radius);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: var(--transition);
    box-shadow: 0 4px 10px -2px rgba(16, 185, 129, 0.35);
  }
  .btn-export:hover {
    background: #059669;
    transform: translateY(-1px);
    box-shadow: 0 6px 14px -2px rgba(16, 185, 129, 0.45);
  }

  /* ============================================
   * 文献勾选面板
   * ============================================ */
  .selection-panel h3 {
    margin: 0 0 6px 0;
    color: var(--text);
    font-size: 18px;
    font-weight: 700;
  }
  .selection-panel .hint {
    font-size: 13px;
    color: var(--text-muted);
    margin-bottom: 18px;
  }

  .paper-checklist {
    text-align: left;
    background: var(--surface-muted);
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius);
    padding: 8px;
    max-height: 280px;
    overflow-y: auto;
    margin-bottom: 20px;
  }
  .check-item {
    display: flex;
    align-items: center;
    padding: 10px 12px;
    border-radius: var(--radius-sm);
    transition: var(--transition);
  }
  .check-item:hover:not(.disabled) { background: var(--surface); }
  .check-item.disabled { opacity: 0.5; }

  .check-item input[type="checkbox"] {
    margin-right: 12px;
    width: 16px;
    height: 16px;
    cursor: pointer;
    accent-color: var(--primary);
  }
  .check-item label {
    font-size: 14px;
    color: var(--text);
    cursor: pointer;
    flex: 1;
    line-height: 1.4;
  }
  .status-hint {
    color: var(--danger);
    font-size: 12px;
    margin-left: 10px;
  }

  .bottom-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
  }

  .btn-cancel {
    padding: 10px 20px;
    background: var(--surface);
    color: var(--text-secondary);
    border: 1px solid var(--border);
    border-radius: var(--radius-full);
    cursor: pointer;
    font-size: 13px;
    font-weight: 500;
    transition: var(--transition);
  }
  .btn-cancel:hover {
    background: var(--surface-muted);
    border-color: var(--text-muted);
    color: var(--text);
  }

  .result-header {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 12px;
  }

  /* ============================================
   * 历史综述卡片
   * ============================================ */
  .history-panel {
    margin-top: 32px;
    text-align: left;
  }
  .history-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-muted);
    margin-bottom: 14px;
    padding-bottom: 10px;
    border-bottom: 1px solid var(--border-subtle);
    text-transform: uppercase;
    letter-spacing: 0.8px;
  }
  .history-cards {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .history-card {
    display: flex;
    align-items: center;
    padding: 14px 16px;
    background: var(--surface);
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius);
    cursor: pointer;
    transition: var(--transition);
    box-shadow: var(--shadow-sm);
  }
  .history-card:hover {
    border-color: rgba(99, 102, 241, 0.3);
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
    background: linear-gradient(135deg, var(--surface) 0%, var(--primary-light) 300%);
  }
  .card-icon {
    font-size: 22px;
    margin-right: 14px;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--gradient-subtle);
    border-radius: var(--radius);
    flex-shrink: 0;
  }
  .card-info {
    flex: 1;
    min-width: 0;
  }
  .card-info h4 {
    margin: 0 0 4px 0;
    color: var(--text);
    font-size: 14px;
    font-weight: 600;
  }
  .card-info p {
    margin: 0;
    color: var(--text-muted);
    font-size: 12.5px;
  }
  .btn-view {
    padding: 6px 14px;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius-full);
    color: var(--primary);
    font-weight: 500;
    font-size: 13px;
    cursor: pointer;
    transition: var(--transition);
    margin-left: 12px;
    flex-shrink: 0;
  }
  .btn-view:hover {
    background: var(--primary);
    color: white;
    border-color: var(--primary);
  }

  /* ============================================
   * 关系图谱视图
   * ============================================ */
  .graph-view > div:first-child > p {
    color: var(--text-secondary);
    font-size: 15px;
    line-height: 1.6;
    margin-bottom: 20px;
  }

  .relations-result .result-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }
  .relations-result .result-header h3 {
    margin: 0;
    font-size: 16px;
    color: var(--text);
    font-weight: 600;
  }

  .relation-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 14px;
  }
  .relation-card {
    background: var(--surface);
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-lg);
    padding: 18px 20px;
    transition: var(--transition);
    box-shadow: var(--shadow-sm);
  }
  .relation-card:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }
  .relation-type {
    display: inline-flex;
    align-items: center;
    font-size: 12px;
    font-weight: 600;
    padding: 4px 12px;
    border-radius: var(--radius-full);
    margin-bottom: 10px;
  }
  .relation-type.inherit { background: var(--primary-light); color: var(--primary); }
  .relation-type.contradict { background: var(--danger-bg); color: var(--danger); }
  .relation-type.support { background: var(--success-bg); color: var(--success); }

  .relation-desc {
    color: var(--text);
    font-size: 14px;
    line-height: 1.65;
    margin-bottom: 12px;
  }
  .relation-papers {
    color: var(--text-muted);
    font-size: 12px;
    padding-top: 10px;
    border-top: 1px dashed var(--border);
  }

  /* ============================================
   * 汉堡按钮 & 抽屉遮罩（默认隐藏）
   * ============================================ */
  .hamburger-btn {
    display: none;
    background: none;
    border: none;
    font-size: 22px;
    cursor: pointer;
    padding: 4px 8px;
    border-radius: 6px;
    color: var(--text-secondary);
  }
  .hamburger-btn:hover {
    background: var(--surface-muted);
  }
  .header-left {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .sidebar-overlay {
    display: none;
  }

  /* ============================================
   * 平板 (<=1024px)
   * ============================================ */
  @media (max-width: 1024px) {
    .left-panel { width: 280px; }
    .tab-content { padding: 20px; }
    .header { padding: 0 18px; }
    .tabs { padding: 0 16px; }
  }

  /* ============================================
   * 手机 (<=768px) — 抽屉模式
   * ============================================ */
  @media (max-width: 768px) {
    .hamburger-btn {
      display: block;
    }

    .header {
      height: 54px;
      padding: 0 12px;
    }
    .logo {
      font-size: 15px;
    }
    .user-info span {
      display: none;
    }

    .main-container {
      position: relative;
    }

    /* 遮罩层 */
    .sidebar-overlay {
      display: block;
      position: fixed;
      inset: 0;
      background: rgba(0,0,0,0.4);
      z-index: 99;
    }

    /* 侧边栏抽屉 */
    .left-panel {
      position: fixed;
      left: 0;
      top: 54px;
      bottom: 0;
      width: 300px;
      z-index: 100;
      transform: translateX(-100%);
      transition: transform 0.25s ease;
      box-shadow: var(--shadow-lg);
    }
    .left-panel.open {
      transform: translateX(0);
    }

    /* 标签栏可横向滚动 */
    .tabs {
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;
      padding: 0 10px;
      gap: 0;
    }
    .tabs button {
      padding: 12px 14px;
      font-size: 13px;
      white-space: nowrap;
      flex-shrink: 0;
    }

    .tab-content {
      padding: 16px;
    }

    /* AI 解析卡片 */
    .ai-card {
      padding: 16px;
    }
    .ai-card h4 {
      font-size: 13px;
    }
    .ai-card p {
      font-size: 13px;
    }
    .doc-title {
      font-size: 18px;
    }

    /* 综述大纲 */
    .outline-view .actions {
      padding: 24px 16px;
    }
    .outline-result pre {
      padding: 16px;
      font-size: 13px;
      overflow-x: auto;
    }

    /* 勾选面板 */
    .selection-panel h3 {
      font-size: 16px;
    }
    .paper-checklist {
      max-height: 200px;
    }

    /* 关系图谱 */
    .relation-list {
      grid-template-columns: 1fr;
    }
    .relations-result .result-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 10px;
    }

    /* 按钮 */
    .btn-generate {
      padding: 10px 20px;
      font-size: 13px;
    }
    .btn-export {
      padding: 8px 16px;
      font-size: 13px;
    }
    .btn-cancel {
      padding: 8px 14px;
      font-size: 12px;
    }
  }

  /* ============================================
   * 小屏手机 (<=480px)
   * ============================================ */
  @media (max-width: 480px) {
    .left-panel {
      width: 260px;
    }
    .header {
      height: 48px;
      padding: 0 8px;
    }
    .logo {
      font-size: 14px;
    }
    .tab-content {
      padding: 12px;
    }
    .ai-card {
      padding: 12px;
    }
    .doc-title {
      font-size: 16px;
    }
    .outline-view .actions {
      padding: 18px 12px;
    }
    .outline-result pre {
      padding: 12px;
      font-size: 12px;
    }
    .bottom-actions {
      flex-direction: column;
      gap: 8px;
    }
  }
  </style>