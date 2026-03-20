import { createRouter, createWebHistory } from 'vue-router'
// 注意：你的截图里 index.vue 是小写的 i，这里必须完全匹配
import Index from '../components/index.vue' 
import Login from '../components/user/Login.vue'
import Register from '../components/user/Register.vue'
import Workbench from '../components/main/Workbench.vue'


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
    },
    {
      path: '/register',
      name: 'Register',
      component: Register
    },{
      path: '/workbench',
      name: 'Workbench',
      component: Workbench
    }
  ]
})

// 🌟 核心：全局前置路由守卫
router.beforeEach((to, from, next) => {
  // 1. 去缓存里找找看有没有咱们刚才存的 token (兼容了记住我和不记住我两种情况)
  const token = localStorage.getItem('token') || sessionStorage.getItem('token');

  // 2. 检查你要去的页面是不是工作台 (如果你未来有多个私密页面，可以用 to.path.startsWith('/workbench') 等方式)
  if (to.path === '/workbench') {
    if (token) {
      // 兜里有门票 (token存在)，保安放行！
      next(); 
    } else {
      // 没门票还想硬闯？直接打回登录页！
      alert('⚠️ 访问被拒绝：请先登录后再进入工作台！');
      next('/login'); 
    }
  } 
  // 3. 补充优化：如果用户已经登录了（有 token），还非要硬闯去访问 /login 页面
  else if (to.path === '/login' && token) {
    // 已经登录了就别再登录了，直接请进工作台
    next('/workbench');
  } 
  // 4. 去的既不是工作台，又不是登录相关的冲突页面（比如去注册页 /register）
  else {
    // 随便进，放行！
    next();
  }
});

export default router