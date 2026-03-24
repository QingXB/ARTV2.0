import axios from 'axios'

// 1. 第一步：先创建实例（必须放在最前面！）
const request = axios.create({
  baseURL: 'http://localhost:8080', 
  timeout: 180000 //前端最长等待时间
})

// 2. 第二步：给已经创建好的 request 挂载拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data 
    } else {
      alert(res.message || '业务处理失败') 
      return Promise.reject(new Error(res.message || 'Error'))
    }
  },
  (error) => {
    console.error('网络请求崩溃啦:', error)
    alert('网络异常，请稍后再试！')
    return Promise.reject(error)
  }
)

// 3. 第三步：导出
export default request