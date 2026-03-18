import { createRouter, createWebHistory } from 'vue-router'
// 注意：你的截图里 index.vue 是小写的 i，这里必须完全匹配
import Index from '../components/index.vue' 
import Login from '../components/user/Login.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Index',
      component: Index
    },
    {
      path: '/login',
      name: 'Login',
      component: Login
    }
  ]
})

export default router