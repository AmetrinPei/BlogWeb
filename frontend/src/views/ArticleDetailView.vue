<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ArticleDetailSkeleton from '@/components/ArticleDetailSkeleton.vue'
import ArticleToc from '@/components/ArticleToc.vue'
import { createComment, deleteComment, listComments } from '@/api/comments'
import { fetchLikeStatus, toggleLike } from '@/api/likes'
import { fetchArticle } from '@/api/articles'
import { useCodeHighlight } from '@/composables/useCodeHighlight'
import { formatDate, categoryEmoji } from '@/utils/format'
import { getUser, isAdmin, isLoggedIn } from '@/utils/auth'
import { renderArticleMarkdown } from '@/utils/markdown'

const route = useRoute()

const article = ref(null)
const loading = ref(true)
const error = ref('')

const likeCount = ref(0)
const liked = ref(false)
const likeLoading = ref(false)

const comments = ref([])
const commentsLoading = ref(false)
const commentText = ref('')
const commentSubmitting = ref(false)
const commentError = ref('')

const contentEl = ref(null)

const publishedAt = computed(() => formatDate(article.value?.publishedAt))
const emoji = computed(() => categoryEmoji(article.value?.category?.name))

const rendered = computed(() => renderArticleMarkdown(article.value?.content || ''))
const renderedHtml = computed(() => rendered.value.html)
const tocItems = computed(() => rendered.value.toc)

useCodeHighlight(renderedHtml, () => contentEl.value)

const currentUser = computed(() => getUser())

function canDeleteComment(comment) {
  if (!isLoggedIn()) return false
  if (isAdmin()) return true
  return comment.userId === currentUser.value?.userId
}

async function loadLikeStatus(articleId) {
  try {
    const data = await fetchLikeStatus('ARTICLE', articleId)
    liked.value = data?.liked ?? false
    likeCount.value = data?.count ?? 0
  } catch {
    liked.value = false
    likeCount.value = 0
  }
}

async function loadComments(articleId) {
  commentsLoading.value = true
  try {
    comments.value = (await listComments(articleId)) ?? []
  } catch {
    comments.value = []
  } finally {
    commentsLoading.value = false
  }
}

async function loadDetail(id) {
  loading.value = true
  error.value = ''
  article.value = null
  try {
    article.value = await fetchArticle(id)
    await Promise.all([loadLikeStatus(id), loadComments(id)])
  } catch (e) {
    error.value = e.message || '文章不存在或无法访问'
  } finally {
    loading.value = false
  }
}

async function onToggleLike() {
  if (!article.value || likeLoading.value) return
  if (!isLoggedIn()) {
    commentError.value = '请先登录后再点赞'
    return
  }
  likeLoading.value = true
  try {
    const data = await toggleLike('ARTICLE', article.value.id)
    liked.value = data?.liked ?? false
    likeCount.value = data?.count ?? 0
  } catch (e) {
    commentError.value = e.message || '点赞失败'
  } finally {
    likeLoading.value = false
  }
}

async function onSubmitComment() {
  commentError.value = ''
  const content = commentText.value.trim()
  if (!content) return
  if (!isLoggedIn()) {
    commentError.value = '请先登录后再评论'
    return
  }
  commentSubmitting.value = true
  try {
    await createComment(article.value.id, content)
    commentText.value = ''
    await loadComments(article.value.id)
  } catch (e) {
    commentError.value = e.message || '发表评论失败'
  } finally {
    commentSubmitting.value = false
  }
}

async function onDeleteComment(comment) {
  try {
    await deleteComment(comment.id)
    await loadComments(article.value.id)
  } catch (e) {
    commentError.value = e.message || '删除评论失败'
  }
}

watch(
  () => route.params.id,
  (id) => {
    if (id) loadDetail(id)
  },
  { immediate: true },
)
</script>

<template>
  <section class="page content-article">
    <ArticleDetailSkeleton v-if="loading" />

    <div v-else-if="error" class="error-panel">
      <h1>找不到这篇文章</h1>
      <p>{{ error }}</p>
      <RouterLink class="back" to="/articles">返回文章列表</RouterLink>
    </div>

    <article v-else-if="article" class="panel">
      <img
        v-if="article.coverUrl"
        class="cover"
        :src="article.coverUrl"
        :alt="article.title"
      />

      <div class="meta">
        <span class="emoji" aria-hidden="true">{{ emoji }}</span>
        <time v-if="publishedAt" :datetime="article.publishedAt">{{ publishedAt }}</time>
        <span v-if="article.viewCount != null" class="views">{{ article.viewCount }} 阅读</span>
        <RouterLink
          v-if="article.category"
          class="category"
          :to="{ name: 'articles', query: { categoryId: String(article.category.id) } }"
        >
          {{ article.category.name }}
        </RouterLink>
      </div>

      <h1>{{ article.title }}</h1>

      <ul v-if="article.tags?.length" class="tags">
        <li v-for="tag in article.tags" :key="tag.id">
          <RouterLink :to="{ name: 'articles', query: { tagId: String(tag.id) } }">
            #{{ tag.name }}
          </RouterLink>
        </li>
      </ul>

      <ArticleToc :items="tocItems" />

      <div ref="contentEl" class="content markdown-body" v-html="renderedHtml" />

      <div class="actions">
        <button
          class="like-btn"
          type="button"
          :class="{ 'is-liked': liked }"
          :disabled="likeLoading"
          @click="onToggleLike"
        >
          {{ liked ? '已赞' : '点赞' }} · {{ likeCount }}
        </button>
      </div>

      <section class="comments">
        <h2>评论</h2>

        <form v-if="isLoggedIn()" class="comment-form" @submit.prevent="onSubmitComment">
          <textarea
            v-model="commentText"
            rows="3"
            placeholder="写下你的想法…"
            maxlength="1000"
          />
          <button type="submit" :disabled="commentSubmitting || !commentText.trim()">
            {{ commentSubmitting ? '发送中…' : '发表评论' }}
          </button>
        </form>
        <p v-else class="comment-hint">
          <RouterLink to="/login">登录</RouterLink> 后可发表评论
        </p>

        <p v-if="commentError" class="comment-error">{{ commentError }}</p>

        <p v-if="commentsLoading" class="state">加载评论…</p>
        <p v-else-if="!comments.length" class="state">暂无评论，来抢沙发吧。</p>
        <ul v-else class="comment-list">
          <li v-for="c in comments" :key="c.id" class="comment-item">
            <div class="comment-head">
              <strong>{{ c.username }}</strong>
              <time>{{ formatDate(c.createdAt) }}</time>
              <button
                v-if="canDeleteComment(c)"
                class="delete-btn"
                type="button"
                @click="onDeleteComment(c)"
              >
                删除
              </button>
            </div>
            <p class="comment-body">{{ c.content }}</p>
          </li>
        </ul>
      </section>

      <RouterLink class="back" to="/articles">← 返回列表</RouterLink>
    </article>
  </section>
</template>

<style scoped>
.state {
  margin: 0;
  color: var(--text-muted);
}

.error-panel,
.panel {
  padding: 36px 32px;
  border-radius: var(--radius-lg);
  background: #fff;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-soft);
}

.cover {
  width: 100%;
  max-height: 320px;
  object-fit: cover;
  border-radius: var(--radius-lg);
  margin-bottom: 20px;
}

.error-panel h1,
.panel h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(1.6rem, 3vw, 2.1rem);
  line-height: 1.3;
  letter-spacing: 0.01em;
}

.error-panel p {
  margin: 12px 0 0;
  color: var(--text-muted);
}

.meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  color: var(--text-muted);
  font-size: 0.92rem;
}

.emoji {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: var(--primary-soft);
  font-size: 1.1rem;
}

.views {
  font-size: 0.88rem;
}

.category {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: var(--radius-pill);
  background: rgba(255, 138, 101, 0.14);
  color: var(--accent-peach);
  font-weight: 500;
  text-decoration: none;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin: 16px 0 0;
  padding: 0;
  list-style: none;
}

.tags a {
  color: var(--accent-lilac);
  text-decoration: none;
  font-size: 0.92rem;
}

.tags a:hover {
  text-decoration: underline;
}

.content {
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid var(--border-soft);
  line-height: 1.85;
  font-size: 1.05rem;
  color: var(--text);
}

.content :deep(h1),
.content :deep(h2),
.content :deep(h3) {
  font-family: var(--font-display);
  margin: 1.2em 0 0.6em;
  scroll-margin-top: calc(var(--header-height) + 12px);
}

.content :deep(p) {
  margin: 0.8em 0;
}

.content :deep(pre) {
  overflow-x: auto;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f6f8fa;
  border: 1px solid var(--border-soft);
}

.content :deep(pre code.hljs) {
  background: transparent;
  padding: 0;
}

.content :deep(code) {
  font-family: ui-monospace, monospace;
  font-size: 0.92em;
}

.content :deep(:not(pre) > code) {
  padding: 0.15em 0.4em;
  border-radius: 4px;
  background: rgba(45, 52, 54, 0.06);
}

.actions {
  margin-top: 24px;
}

.like-btn {
  min-height: 36px;
  padding: 0 16px;
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-pill);
  background: #fff;
  color: var(--text);
  font-weight: 600;
  cursor: pointer;
}

.like-btn.is-liked {
  background: var(--primary-soft);
  border-color: var(--primary);
}

.like-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.comments {
  margin-top: 36px;
  padding-top: 28px;
  border-top: 1px solid var(--border-soft);
}

.comments h2 {
  margin: 0 0 16px;
  font-family: var(--font-display);
  font-size: 1.2rem;
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.comment-form textarea {
  padding: 12px;
  border: 1px solid var(--border-soft);
  border-radius: 12px;
  resize: vertical;
  font-family: inherit;
  font-size: 0.95rem;
}

.comment-form button {
  align-self: flex-start;
  min-height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--primary);
  font-weight: 600;
  cursor: pointer;
}

.comment-hint {
  margin: 0 0 16px;
  color: var(--text-muted);
  font-size: 0.92rem;
}

.comment-hint a {
  color: var(--accent-lilac);
}

.comment-error {
  margin: 0 0 12px;
  color: #d63031;
  font-size: 0.9rem;
}

.comment-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  padding: 14px 16px;
  border-radius: 12px;
  background: rgba(111, 207, 151, 0.06);
  border: 1px solid var(--border-soft);
}

.comment-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 0.88rem;
  color: var(--text-muted);
}

.comment-head strong {
  color: var(--text);
}

.delete-btn {
  margin-left: auto;
  border: none;
  background: transparent;
  color: #d63031;
  font-size: 0.85rem;
  cursor: pointer;
}

.comment-body {
  margin: 0;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.back {
  display: inline-flex;
  margin-top: 28px;
  color: var(--text-muted);
  text-decoration: none;
  font-size: 0.95rem;
}

.back:hover {
  color: var(--text);
}

@media (max-width: 640px) {
  .error-panel,
  .panel {
    padding: 24px 18px;
  }
}
</style>
