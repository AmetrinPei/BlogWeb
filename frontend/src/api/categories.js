import http from './http'

export function fetchCategories() {
  return http.get('/categories')
}
