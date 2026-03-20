<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request.js'

const router = useRouter()
const emit = defineEmits(['back', 'submit'])

// 🌟 新增：请求状态控制，防止用户疯狂连击按钮
const isLoading = ref(false)

function onBack() {
  emit('back')
  if (window.history.length > 1) router.back()
  else router.push('/')
}

async function onSubmit(e) {
  e.preventDefault()
  
  if (isLoading.value) return // 如果正在登录中，直接拦截请求

  const form = e.currentTarget
  const data = new FormData(form)
  
  // 1. 组装数据：把表单里的 username 映射为后端需要的 account
  const payload = {
    account: String(data.get('username') ?? ''),
    password: String(data.get('password') ?? '')
  }
  const remember = data.get('remember') === 'on'

  try {
  isLoading.value = true
  
  // 1. 因为拦截器的功劳，这里的 result 直接就是 { token: "Quasar", username: null }
  const result = await request.post('/api/users/login', payload)
  
  // 2. 提取出真正的字符串
  const actualToken = result.token;
  // 如果 username 是 null，我们就临时拿 token 的值（Quasar）来当名字展示
  const actualName = result.username || result.token; 

  // 3. 存 Token（注意：只能存字符串，不能存完整的 result 对象）
  if (remember) {
    localStorage.setItem('token', actualToken) 
  } else {
    sessionStorage.setItem('token', actualToken) 
  }

  // 4. 存名字，专门给工作台右上角展示用的！
  localStorage.setItem('current_user', actualName);
  
  emit('submit', payload) 
  
  // 5. 完美起飞，跳转到工作台！
  router.push('/workbench') 
  
} catch (error) {
  // 💡 小技巧：把真实的 error 打印出来，以后再报错一眼就能看穿
  console.error("登录流程被拦截终止，真实原因：", error)
} finally {
  isLoading.value = false 
}
}
</script>

<template>
  <div class="page">
    <div class="bg"></div>

    <header class="topbar">
      <button class="back" type="button" @click="onBack">
        返回
      </button>
      <div class="brand">
        <span class="dot"></span>
        <span>智研 ScholarAI</span>
      </div>
    </header>

    <main class="wrap">
      <section class="card">
        <h1 class="title">登录</h1>
        <p class="sub">进入工作台，继续你的文献综述。</p>

        <form class="form" @submit="onSubmit">
          <label class="field">
            <span class="label">账号</span>
            <input name="username" autocomplete="username" placeholder="邮箱 / 手机号 / 用户名" required />
          </label>

          <label class="field">
            <span class="label">密码</span>
            <input
              name="password"
              type="password"
              autocomplete="current-password"
              placeholder="请输入密码"
              required
            />
          </label>

          <div class="row">
            <label class="remember">
              <input name="remember" type="checkbox" />
              <span class="remember-text">记住我</span>
            </label>
            <a class="link" href="#" @click.prevent>忘记密码？</a>
          </div>

          <button class="primary" type="submit">登录</button>

          <div class="divider">
            <span>或</span>
          </div>

          <button class="ghost" type="button" @click.prevent>
            使用验证码登录
          </button>

          <p class="foot">
            还没有账号？
            <a class="link" href="#" @click.prevent>去注册</a>
          </p>
        </form>
      </section>
    </main>
  </div>
</template>

<style scoped>
.page {
  position: relative;
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
  overflow: hidden;
  color: #0b1220;
}

.bg {
  position: absolute;
  inset: 0;
  background:
  
    radial-gradient(1200px 600px at 15% 10%, rgba(59, 130, 246, 0.25), transparent 55%),
    radial-gradient(900px 500px at 85% 35%, rgba(99, 102, 241, 0.22), transparent 55%),
    radial-gradient(800px 700px at 50% 95%, rgba(56, 189, 248, 0.18), transparent 55%),
    linear-gradient(180deg, #ffffff 0%, #f6f7fb 45%, #f3f5fb 100%);
  filter: saturate(1.05);
}

.topbar {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
}

.back {
  appearance: none;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 10px 12px;
  font-weight: 600;
  cursor: pointer;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 800;
  color: #1e40af;
  letter-spacing: 0.02em;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #3b82f6;
  box-shadow: 0 0 0 6px rgba(59, 130, 246, 0.12);
}

.wrap {
  position: relative;
  z-index: 1;
  min-height: calc(100vh - 68px);
  min-height: calc(100dvh - 68px);
  display: grid;
  place-items: center;
  padding: 18px;
}

.card {
  width: min(440px, 100%);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 26px 70px rgba(15, 23, 42, 0.12);
  padding: 26px 24px;
}

.title {
  margin: 0;
  font-size: 28px;
  letter-spacing: -0.02em;
}

.sub {
  margin: 8px 0 18px;
  color: rgba(15, 23, 42, 0.6);
  font-size: 14px;
  line-height: 1.6;
}

.form {
  display: grid;
  gap: 14px;
}

.field {
  display: grid;
  gap: 8px;
}

.label {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.65);
  font-weight: 700;
  letter-spacing: 0.06em;
}

input {
  width: 100%;
  padding: 12px 12px;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.85);
  outline: none;
  font-size: 14px;
}

input:focus {
  border-color: rgba(59, 130, 246, 0.55);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.14);
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.remember {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  font-size: 13px;
  color: rgba(15, 23, 42, 0.65);
}

.link {
  color: #2563eb;
  text-decoration: none;
  font-weight: 700;
  font-size: 13px;
}

.primary {
  border: none;
  border-radius: 14px;
  padding: 12px 14px;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 55%, #60a5fa 100%);
  color: white;
  font-weight: 800;
  letter-spacing: 0.02em;
  cursor: pointer;
  box-shadow: 0 18px 40px rgba(37, 99, 235, 0.25);
}

.divider {
  display: grid;
  place-items: center;
  color: rgba(15, 23, 42, 0.35);
  font-size: 12px;
  position: relative;
  margin: 4px 0;
}

.divider::before,
.divider::after {
  content: "";
  position: absolute;
  top: 50%;
  width: 42%;
  height: 1px;
  background: rgba(15, 23, 42, 0.12);
}

.divider::before { left: 0; }
.divider::after { right: 0; }

.ghost {
  border-radius: 14px;
  padding: 12px 14px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  cursor: pointer;
  font-weight: 800;
  color: rgba(15, 23, 42, 0.8);
}

.foot {
  margin: 0;
  text-align: center;
  font-size: 13px;
  color: rgba(15, 23, 42, 0.6);
}
.remember-text {
  display: inline-block;
  line-height: 1;
  white-space: nowrap;
}
</style>