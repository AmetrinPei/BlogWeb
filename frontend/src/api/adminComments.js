import http from './http'

export function fetchAdminComments(params = {}) {
  return http.get('/admin/comments', { params })
}

export function updateCommentStatus(id, status) {
  return http.put(`/admin/comments/${id}/status`, { status })
}
