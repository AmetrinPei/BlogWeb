import http from './http'

export function listComments(articleId) {
  return http.get(`/articles/${articleId}/comments`)
}

export function createComment(articleId, content, parentId) {
  const body = { content }
  if (parentId != null) {
    body.parentId = parentId
  }
  return http.post(`/articles/${articleId}/comments`, body)
}

export function deleteComment(id) {
  return http.delete(`/comments/${id}`)
}

export function pinComment(id, pinned) {
  return http.put(`/comments/${id}/pin`, { pinned })
}
