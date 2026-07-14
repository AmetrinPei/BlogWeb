import http from './http'

/** 公开文章列表，支持 page/size/categoryId/tagId/keyword/yearMonth */
export function fetchArticles(params = {}) {
  return http.get('/articles', { params })
}

/** 公开文章详情 */
export function fetchArticle(id) {
  return http.get(`/articles/${id}`)
}

/** 精选推荐文章 */
export function fetchFeatured(size) {
  const params = size != null ? { size } : {}
  return http.get('/articles/featured', { params })
}

/** 归档年月统计 */
export function fetchArchive() {
  return http.get('/articles/archive')
}
