import axios from 'axios'
import { ElMessage, ElLoading } from 'element-plus'

// 创建 Axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

// ---- 全局 Loading 计数器 ----
let loadingInstance = null
let loadingCount = 0

function showLoading() {
  loadingCount++
  if (loadingCount === 1) {
    loadingInstance = ElLoading.service({
      lock: true,
      text: '加载中...',
      background: 'rgba(255,255,255,0.7)',
    })
  }
}

function hideLoading() {
  loadingCount--
  if (loadingCount <= 0) {
    loadingCount = 0
    if (loadingInstance) {
      loadingInstance.close()
      loadingInstance = null
    }
  }
}

// ---- 请求拦截器 ----
request.interceptors.request.use(
  (config) => {
    // 自动携带 Token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // GET 请求不显示 loading（页面初始化场景可以自定义）
    if (config.method !== 'get' && config.showLoading !== false) {
      showLoading()
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ---- 响应拦截器 ----
request.interceptors.response.use(
  (response) => {
    hideLoading()
    const res = response.data

    // Blob 响应（文件下载）直接返回
    if (res instanceof Blob || res instanceof ArrayBuffer) {
      return response
    }

    // 后端业务状态码校验
    if (res && res.code === 200) {
      return res
    }

    // 401 → 跳转登录页
    if (res.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.href = '/login'
      ElMessage.error(res.message || '登录已过期，请重新登录')
      return Promise.reject(new Error(res.message))
    }

    // 403 → 无权限
    if (res.code === 403) {
      ElMessage.warning(res.message || '您没有权限执行此操作')
      return Promise.reject(new Error(res.message))
    }

    // 其他错误码
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  (error) => {
    hideLoading()
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络')
    } else if (!error.response) {
      ElMessage.error('网络异常，无法连接服务器')
    } else {
      const status = error.response.status
      if (status === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      } else if (status === 403) {
        ElMessage.warning('您没有权限执行此操作')
      } else if (status === 429) {
        ElMessage.warning('请求过于频繁，请稍后再试')
      } else if (status >= 500) {
        ElMessage.error('服务器繁忙，请稍后重试')
      } else {
        ElMessage.error(error.response.data?.message || '请求失败')
      }
    }
    return Promise.reject(error)
  }
)

export default request
