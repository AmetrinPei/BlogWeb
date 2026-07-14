import http from './http'

/** 登录，成功返回 { token, tokenType, expireHours, userId, username, role } */
export function login(username, password) {
  return http.post('/auth/login', { username, password })
}

/** 注册，成功返回与登录相同的会话信息 */
export function register(username, password) {
  return http.post('/auth/register', { username, password })
}
