import axios from 'axios'
import router from '../router'

const request = axios.create({
  baseURL: import.meta.env.VITE_APP_API_URL ?? 'http://localhost:8080',
  timeout: 180000
})

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    } else if (res.code === 401) {
      alert(res.message || '登录已过期，请重新登录')
      localStorage.clear()
      sessionStorage.clear()
      router.push('/login')
      return Promise.reject(new Error(res.message || '未登录'))
    } else if (res.code === 429) {
      alert(res.message || '请求过于频繁，请稍后再试')
      return Promise.reject(new Error(res.message))
    } else {
      alert(res.message || '操作失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }
  },
  (error) => {
    console.error('网络请求错误:', error)
    if (error.response) {
      switch (error.response.status) {
        case 401:
          alert('登录已过期，请重新登录')
          localStorage.clear()
          sessionStorage.clear()
          router.push('/login')
          break
        case 429:
          alert('请求过于频繁，请稍后再试')
          break
        case 500:
          alert('服务器内部错误，请联系管理员')
          break
        default:
          alert('网络异常，请检查网络后重试')
      }
    } else {
      alert('网络连接失败，请检查网络')
    }
    return Promise.reject(error)
  }
)

export default request