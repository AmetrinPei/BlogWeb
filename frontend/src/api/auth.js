import http from './http'

/** 登录，成功返回 { token, refreshToken, tokenType, accessExpireMinutes, userId, username, role, ... } */
export function login(username, password) {
  return http.post('/auth/login', { username, password })
}

/** 注册，成功返回与登录相同的会话信息 */
export function register(username, password) {
  return http.post('/auth/register', { username, password })
}

/** 用 Refresh 换新 Access + Refresh */
export function refreshSession(refreshToken) {
  return http.post('/auth/refresh', { refreshToken })
}

/** 服务端吊销 Refresh（幂等） */
export function logout(refreshToken) {
  return http.post('/auth/logout', { refreshToken })
}
