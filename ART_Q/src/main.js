import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router' // 🌟 新增：引入刚才写好的路由配置

const app = createApp(App)
app.use(router) // 🌟 新增：告诉 Vue 去使用这个路由
app.mount('#app')