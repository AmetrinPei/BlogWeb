import http from './http'

export function fetchAdminArticles(params = {}) {
  return http.get('/admin/articles', { params })
}

export function fetchAdminArticle(id) {
  return http.get(`/admin/articles/${id}`)
}

export function createAdminArticle(payload) {
  return http.post('/admin/articles', payload)
}

export function updateAdminArticle(id, payload) {
  return http.put(`/admin/articles/${id}`, payload)
}

export function deleteAdminArticle(id) {
  return http.delete(`/admin/articles/${id}`)
}
