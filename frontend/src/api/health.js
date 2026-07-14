import http from './http'

/** 后端健康检查（骨架联调） */
export function fetchHealth() {
  return http.get('/health')
}
