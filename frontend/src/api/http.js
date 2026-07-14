import axios from 'axios'
import { clearAuth, getToken } from '@/utils/auth'

/**
 * 统一 Axios 实例：开发期走 Vite 代理 /api → 8080。
 * 响应约定与后端 Result(code/message/data) 对齐（AC-11、AC-12）。
 */
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data
    // 非统一 Result 结构时原样返回（兼容极少数非 JSON 响应）
    if (payload == null || typeof payload !== 'object' || !('code' in payload)) {
      return payload
    }
    if (payload.code === 0) {
      return payload.data
    }
    const error = new Error(payload.message || '请求失败')
    error.code = payload.code
    error.response = response
    return Promise.reject(error)
  },
  (error) => {
    const status = error.response?.status
    const payload = error.response?.data
    const message =
      (payload && typeof payload === 'object' && payload.message) ||
      error.message ||
      '网络异常，请稍后重试'

    if (status === 401) {
      clearAuth()
      const path = window.location.pathname
      if (path.startsWith('/admin') && path !== '/admin/login') {
        const redirect = encodeURIComponent(path + window.location.search)
        window.location.assign(`/admin/login?redirect=${redirect}`)
      }
    }

    const normalized = new Error(message)
    normalized.code = (payload && payload.code) || status || 500
    normalized.response = error.response
    return Promise.reject(normalized)
  },
)

export default http
