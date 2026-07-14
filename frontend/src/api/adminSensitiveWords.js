import http from './http'

export function fetchSensitiveWords() {
  return http.get('/admin/sensitive-words')
}

export function createSensitiveWord(payload) {
  return http.post('/admin/sensitive-words', payload)
}

export function deleteSensitiveWord(id) {
  return http.delete(`/admin/sensitive-words/${id}`)
}
