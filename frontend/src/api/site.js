import http from './http'

export function fetchSite() {
  return http.get('/site')
}

export function updateSite(payload) {
  return http.put('/admin/site', payload)
}
