import axios from 'axios'
import {
  clearAuth,
  getRefreshToken,
  getToken,
  setRefreshToken,
  setToken,
} from '@/utils/auth'

/**
 * 统一 Axios 实例：开发期走 Vite 代理 /api → 8080。
 * 响应约定与后端 Result(code/message/data) 对齐。
 * 注意：未认证多为 HTTP 200 + body.code=401，需在业务码路径做静默刷新。
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

let refreshPromise = null

function isAuthSessionUrl(url = '') {
  return (
    url.includes('/auth/login') ||
    url.includes('/auth/register') ||
    url.includes('/auth/refresh') ||
    url.includes('/auth/logout') ||
    url.includes('/admin/auth/login')
  )
}

function redirectAfterAuthClear() {
  const path = window.location.pathname
  if (path.startsWith('/admin') && path !== '/admin/login') {
    const redirect = encodeURIComponent(path + window.location.search)
    window.location.assign(`/admin/login?redirect=${redirect}`)
  }
}

async function silentRefresh() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('no refresh token')
  }
  const res = await axios.post(
    `${http.defaults.baseURL}/auth/refresh`,
    { refreshToken },
    { timeout: 15000, headers: { 'Content-Type': 'application/json' } },
  )
  const payload = res.data
  if (!payload || payload.code !== 0 || !payload.data?.token) {
    throw new Error(payload?.message || 'refresh failed')
  }
  setToken(payload.data.token)
  if (payload.data.refreshToken) {
    setRefreshToken(payload.data.refreshToken)
  }
  return payload.data.token
}

function refreshSingleFlight() {
  if (!refreshPromise) {
    refreshPromise = silentRefresh().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

async function tryRefreshAndRetry(config) {
  const newToken = await refreshSingleFlight()
  config.headers = config.headers || {}
  config.headers.Authorization = `Bearer ${newToken}`
  return http(config)
}

http.interceptors.response.use(
  async (response) => {
    const payload = response.data
    if (payload == null || typeof payload !== 'object' || !('code' in payload)) {
      return payload
    }
    if (payload.code === 0) {
      return payload.data
    }

    const original = response.config
    const url = original?.url || ''

    if (payload.code === 401 && original && !original._retry && !isAuthSessionUrl(url)) {
      if (getRefreshToken()) {
        try {
          original._retry = true
          return await tryRefreshAndRetry(original)
        } catch {
          clearAuth()
          redirectAfterAuthClear()
        }
      } else {
        clearAuth()
        redirectAfterAuthClear()
      }
    }

    const error = new Error(payload.message || '请求失败')
    error.code = payload.code
    error.response = response
    return Promise.reject(error)
  },
  async (error) => {
    const status = error.response?.status
    const payload = error.response?.data
    const message =
      (payload && typeof payload === 'object' && payload.message) ||
      error.message ||
      '网络异常，请稍后重试'

    const original = error.config
    const url = original?.url || ''

    if (status === 401 && original && !original._retry && !isAuthSessionUrl(url)) {
      if (getRefreshToken()) {
        try {
          original._retry = true
          return await tryRefreshAndRetry(original)
        } catch {
          clearAuth()
          redirectAfterAuthClear()
        }
      } else {
        clearAuth()
        redirectAfterAuthClear()
      }
    }

    const normalized = new Error(message)
    normalized.code = (payload && payload.code) || status || 500
    normalized.response = error.response
    return Promise.reject(normalized)
  },
)

export default http
