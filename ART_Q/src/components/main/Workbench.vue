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
            
            <div class="actions" v-if="!isSelectingPapers && !generatedOutline">
              <p>基于文献库中已解析的文献，进行多文档观点碰撞，生成结构化综述。</p>
              <button class="btn-generate" @click="startSelection">
                ✨ 自动生成文献综述大纲
              </button>
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
              <button class="btn-export">导出为 Markdown</button>
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
        const papersList = res.data || res;
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
            // if (activePaper.value && activePaper.value.id === paper.id) {
            //   activePaper.value = { ...paper };
            // }
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

// 3. 核心：异步提交并轮询进度
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
    
    // 根据你的 axios 拦截器，这里拿到的是后端的 Result.success(taskId)
    // 假设直接解构出 taskId
    const taskId = submitRes.data || submitRes
    
    if (!taskId) {
      throw new Error("未能从后端获取到任务ID")
    }
    
    console.log(`✅ 任务提交成功！拿到取餐牌：TaskID = ${taskId}，开始轮询...`)

    // ==========================================
    // 第二步：开启定时器，每 5 秒问一次后台“做好了没”
    // ==========================================
    let pollCount = 0
    const maxPolls = 60 // 最多问 60 次 (5秒 * 60 = 5分钟)，防止死循环
    
    const timer = setInterval(async () => {
      pollCount++
      
      try {
        // 去问 Java 这个任务的状态
        const statusRes = await request.get(`/api/papers/task-status/${taskId}`, { headers })
        const task = statusRes.data || statusRes // 拿到 ReviewTask 对象
        
        if (task.status === 2) { 
          // 🟢 状态 2：生成成功！
          clearInterval(timer) // 砸掉定时器
          generatedOutline.value = task.content // 把 Markdown 渲染到页面上
          isGenerating.value = false
          isSelectingPapers.value = false // 隐藏多选面板
          console.log("🎉 轮询结束，大模型生成成功！")
          
        } else if (task.status === 3) { 
          // 🔴 状态 3：生成失败
          clearInterval(timer)
          alert('AI 生成失败: ' + (task.errorMessage || '大模型开小差了'))
          isGenerating.value = false
          
        } else if (pollCount >= maxPolls) { 
          // 🟡 超过 5 分钟还没好，放过前端吧
          clearInterval(timer)
          alert('AI 思考时间太长，请稍后刷新页面查看历史记录。')
          isGenerating.value = false
        }
        // 如果 status 是 0 (等待) 或 1 (处理中)，啥也不干，让它接着转圈圈
        
      } catch (pollError) {
        console.error('轮询请求异常:', pollError)
        // 网络偶尔抖动可以不砸定时器，让它继续尝试
      }
      
    }, 5000) // 5000 毫秒 = 5 秒去问一次

  } catch (error) {
    alert('提交综述任务失败，请检查网络或后端报错！')
    console.error('综述提交异常:', error)
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
/* --- 文献勾选面板样式 --- */
.selection-panel h3 {
  margin-top: 0;
  color: #2c3e50;
  font-size: 18px;
}
.selection-panel .hint {
  font-size: 13px;
  color: #7f8c8d;
  margin-bottom: 15px;
}
.paper-checklist {
  text-align: left;
  background: #f8fafc;
  border: 1px solid #e1e4e8;
  border-radius: 6px;
  padding: 15px;
  max-height: 250px;
  overflow-y: auto;
  margin-bottom: 20px;
}
.check-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #ecf0f1;
}
.check-item:last-child {
  border-bottom: none;
}
.check-item.disabled {
  opacity: 0.5;
}
.check-item input[type="checkbox"] {
  margin-right: 12px;
  width: 16px;
  height: 16px;
  cursor: pointer;
}
.check-item label {
  font-size: 14px;
  color: #34495e;
  cursor: pointer;
  flex: 1;
}
.status-hint {
  color: #e74c3c;
  font-size: 12px;
  margin-left: 10px;
}
.bottom-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.btn-cancel {
  padding: 10px 20px;
  background: #ecf0f1;
  color: #7f8c8d;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-weight: bold;
}
.btn-cancel:hover {
  background: #bdc3c7;
}
.result-header {
  text-align: right;
  margin-bottom: -10px;
}
/* 基础按钮样式 (你原有的可以保留，这里补充特殊状态) */
.disabled-btn {
  background-color: #cbd5e1 !important; /* 灰色 */
  color: #64748b !important;
  cursor: not-allowed !important;
  border: none;
}

.success-btn {
  background-color: #ecfdf5 !important; /* 浅绿色 */
  color: #10b981 !important;
  border: 1px solid #10b981 !important;
  cursor: default !important;
}

.retry-btn {
  background-color: #fef2f2 !important; /* 浅红色 */
  color: #ef4444 !important;
  border: 1px solid #ef4444 !important;
  cursor: pointer;
}
.retry-btn:hover {
  background-color: #fee2e2 !important;
}

/* 状态标签样式 */
.tag-gray { color: #64748b; font-size: 13px; }
.tag-blue { color: #3b82f6; font-size: 13px; font-weight: bold; }
.tag-green { color: #10b981; font-size: 13px; font-weight: bold; }
.tag-red { color: #ef4444; font-size: 13px; font-weight: bold; }
/* --- 卡片底部布局 --- */
.paper-footer {
  display: flex;
  justify-content: space-between; /* 左右两端对齐 */
  align-items: center;            /* 垂直居中 */
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px dashed #e2e8f0; /* 加一条虚线分割更好看 */
  gap: 10px;                      /* 防止左右撞车 */
}

/* --- 状态标签区 --- */
.status-cell {
  flex-shrink: 0;                 /* 🌟 核心：绝对不允许被挤压缩小 */
  white-space: nowrap;            /* 🌟 核心：绝对不允许文字换行 */
}

.tag {
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
}
.tag-gray { color: #64748b; }
.tag-blue { color: #3b82f6; }
.tag-green { color: #10b981; }
.tag-red { color: #ef4444; }

/* --- 按钮组布局 --- */
.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;                       /* 按钮之间的间距 */
  flex-wrap: wrap;                /* 如果屏幕实在太小，按钮允许换行显示 */
  justify-content: flex-end;
}

/* --- 统一按钮颜值 --- */
.btn {
  padding: 6px 12px;
  font-size: 13px;
  border-radius: 6px;             /* 统一圆角 */
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;            /* 按钮文字也不允许换行 */
}

.btn-primary { background: #3b82f6; color: white; }
.btn-primary:hover { background: #2563eb; }

.btn-action { background: #f8fafc; border-color: #cbd5e1; color: #334155; }
.btn-action:hover { background: #f1f5f9; border-color: #94a3b8; }

.btn-danger { background: #fef2f2; border-color: #fca5a5; color: #ef4444; }
.btn-danger:hover { background: #fee2e2; }

.btn-disabled { background: #f1f5f9; color: #94a3b8; cursor: not-allowed; }
.btn-success { background: #ecfdf5; border-color: #6ee7b7; color: #10b981; cursor: default; }

.btn-retry { background: #fff0f2; border-color: #fda4af; color: #e11d48; font-weight: bold; }
.btn-retry:hover { background: #ffe4e6; }
  </style>