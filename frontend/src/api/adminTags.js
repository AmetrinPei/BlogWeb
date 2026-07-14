import http from './http'

export function fetchAdminTags() {
  return http.get('/admin/tags')
}

export function createAdminTag(payload) {
  return http.post('/admin/tags', payload)
}

export function updateAdminTag(id, payload) {
  return http.put(`/admin/tags/${id}`, payload)
}

export function deleteAdminTag(id) {
  return http.delete(`/admin/tags/${id}`)
}
