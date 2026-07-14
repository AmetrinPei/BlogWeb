import http from './http'

export function fetchAdminCategories() {
  return http.get('/admin/categories')
}

export function createAdminCategory(payload) {
  return http.post('/admin/categories', payload)
}

export function updateAdminCategory(id, payload) {
  return http.put(`/admin/categories/${id}`, payload)
}

export function deleteAdminCategory(id) {
  return http.delete(`/admin/categories/${id}`)
}
