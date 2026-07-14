import http from './http'

export function listComments(articleId) {
  return http.get(`/articles/${articleId}/comments`)
}

export function createComment(articleId, content) {
  return http.post(`/articles/${articleId}/comments`, { content })
}

export function deleteComment(id) {
  return http.delete(`/comments/${id}`)
}
