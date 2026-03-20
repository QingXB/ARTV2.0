<template>
    <div class="register-container">
      <div class="top-nav">
        <div class="nav-left">
          <span class="logo-icon">🍵</span>
          <a href="#">探索</a>
          <a href="#">帮助</a>
        </div>
        <div class="nav-right">
          <a href="#" class="active"><i class="icon-user"></i> 注册</a>
          <a href="#" @click.prevent="goToLogin"><i class="icon-login"></i> 登录</a>
        </div>
      </div>
  
      <div class="register-card">
        <div class="card-header">
          <h2>注册</h2>
        </div>
        
        <div class="card-body">
          <form @submit.prevent="handleRegister">
            
            <div class="form-group">
              <label>用户名 <span class="required">*</span></label>
              <input type="text" v-model="formData.username" required />
            </div>
  
            <div class="form-group">
              <label>邮箱地址 <span class="required">*</span></label>
              <input type="email" v-model="formData.email" required />
            </div>
  
            <div class="form-group">
              <label>密码 <span class="required">*</span></label>
              <input type="password" v-model="formData.password" required />
            </div>
  
            <div class="form-group">
              <label>确认密码 <span class="required">*</span></label>
              <input type="password" v-model="formData.confirmPassword" required />
            </div>
  
            <button type="submit" class="btn-primary" :disabled="isLoading">
              {{ isLoading ? '注册中...' : '注册帐号' }}
            </button>
          </form>
  
          
        </div>
  
        <div class="card-footer">
          已有账号？ <a href="#" @click.prevent="goToLogin">立即登录</a>
        </div>
      </div>
    </div>
  </template>
  
  <script setup>
  import { reactive, ref } from 'vue'
  import { useRouter } from 'vue-router'
  import axios from 'axios' // 确保你已经 npm install axios
  
  const router = useRouter()
  const isLoading = ref(false)
  
  // 对应后端的 RegisterDTO + 前端的确认密码
  const formData = reactive({
    username: '',
    email: '',
    password: '',
    confirmPassword: ''
  })
  
  const handleRegister = async () => {
    // 1. 前端基础拦截：校验两次密码是否一致
    if (formData.password !== formData.confirmPassword) {
      alert('两次输入的密码不一致，请重新输入！')
      return
    }
  
    try {
      isLoading.value = true
      
      // 2. 组装发给后端的 DTO 数据 (剔除不需要的 confirmPassword)
      const payload = {
        username: formData.username,
        email: formData.email,
        password: formData.password
      }
  
      // 3. 发送 Axios 请求给 Spring Boot
      const response = await axios.post('http://localhost:8080/api/users/register', payload)
      
      // 4. 处理后端的统一 Result 返回
      if (response.data.code === 200) {
        alert('注册成功！即将跳转到登录页。')
        router.push('/login') // 跳转到你的登录页面路由
      } else {
        alert(response.data.message || '注册失败')
      }
    } catch (error) {
      console.error('注册请求报错:', error)
      alert('网络异常，请检查后端服务是否启动。')
    } finally {
      isLoading.value = false
    }
  }
  
  const goToLogin = () => {
    router.push('/login') // 根据你的实际路由名称修改
  }
  </script>
  
  <style scoped>
  /* 还原截图的纯粹 CSS */
  .register-container {
    min-height: 100vh;
    background-color: #f9f9fa; /* 截图的浅灰色背景 */
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  }
  
  .top-nav {
    display: flex;
    justify-content: space-between;
    padding: 0 20px;
    height: 50px;
    align-items: center;
    background-color: #fff;
    border-bottom: 1px solid #e1e4e8;
    font-size: 14px;
  }
  .nav-left a, .nav-right a {
    text-decoration: none;
    color: #555;
    margin-left: 20px;
  }
  .nav-right a.active {
    color: #333;
    font-weight: 500;
  }
  
  .register-card {
    width: 100%;
    max-width: 420px;
    margin: 60px auto;
    background: #fff;
    border: 1px solid #d8dde3;
    border-radius: 4px;
    box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  }
  
  .card-header {
    background-color: #f7f8fa;
    padding: 15px 20px;
    border-bottom: 1px solid #d8dde3;
    text-align: center;
    border-radius: 4px 4px 0 0;
  }
  .card-header h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 500;
    color: #333;
  }
  
  .card-body {
    padding: 25px 30px;
  }
  
  .form-group {
    margin-bottom: 15px;
  }
  .form-group label {
    display: block;
    font-size: 13px;
    color: #333;
    margin-bottom: 5px;
    font-weight: 500;
  }
  .required {
    color: #d22828;
  }
  .form-group input {
    width: 100%;
    box-sizing: border-box;
    padding: 8px 12px;
    font-size: 14px;
    border: 1px solid #d8dde3;
    border-radius: 4px;
    outline: none;
    transition: border-color 0.2s;
  }
  .form-group input:focus {
    border-color: #4183c4;
  }
  
  .btn-primary {
    width: 100%;
    padding: 10px;
    background-color: #4183c4; /* 经典的蓝色按钮 */
    color: #fff;
    border: none;
    border-radius: 4px;
    font-size: 14px;
    cursor: pointer;
    margin-top: 10px;
  }
  .btn-primary:hover {
    background-color: #316eaa;
  }
  .btn-primary:disabled {
    background-color: #8db5db;
    cursor: not-allowed;
  }
  
  .divider {
    text-align: center;
    margin: 20px 0;
    position: relative;
  }
  .divider::before {
    content: "";
    position: absolute;
    top: 50%;
    left: 0;
    right: 0;
    border-top: 1px solid #e1e4e8;
    z-index: 1;
  }
  .divider span {
    background: #fff;
    padding: 0 10px;
    color: #888;
    font-size: 12px;
    position: relative;
    z-index: 2;
  }
  
  .btn-openid {
    width: 100%;
    padding: 10px;
    background-color: #f8f9fa;
    color: #333;
    border: 1px solid #d8dde3;
    border-radius: 4px;
    font-size: 14px;
    cursor: pointer;
  }
  .btn-openid:hover {
    background-color: #f1f2f4;
  }
  
  .card-footer {
    text-align: center;
    padding: 15px;
    border-top: 1px solid #d8dde3;
    font-size: 13px;
    color: #666;
  }
  .card-footer a {
    color: #4183c4;
    text-decoration: none;
  }
  .card-footer a:hover {
    text-decoration: underline;
  }
  </style>