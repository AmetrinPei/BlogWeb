import http from './http'

export function toggleLike(targetType, targetId) {
  return http.post('/likes/toggle', { targetType, targetId })
}

export function fetchLikeStatus(targetType, targetId) {
  return http.get('/likes/status', { params: { targetType, targetId } })
}
