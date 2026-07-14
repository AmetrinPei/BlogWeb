<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ArticleDetailSkeleton from '@/components/ArticleDetailSkeleton.vue'
import ArticleToc from '@/components/ArticleToc.vue'
import { createComment, deleteComment, listComments, pinComment } from '@/api/comments'
import { fetchLikeStatus, toggleLike } from '@/api/likes'
import { fetchArticle } from '@/api/articles'
import { useCodeHighlight } from '@/composables/useCodeHighlight'
import { formatDateTime, categoryEmoji } from '@/utils/format'
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
const commentNotice = ref('')
const replyToId = ref(null)
const replyText = ref('')
const replySubmitting = ref(false)

const contentEl = ref(null)

const publishedAt = computed(() => formatDateTime(article.value?.publishedAt))
const emoji = computed(() => categoryEmoji(article.value?.category?.name))

const rendered = computed(() => renderArticleMarkdown(article.value?.content || ''))
const renderedHtml = computed(() => rendered.value.html)
const tocItems = computed(() => rendered.value.toc)

useCodeHighlight(renderedHtml, () => contentEl.value)

const currentUser = computed(() => getUser())

function commentAuthorLabel(c) {
  if (c?.displayName && String(c.displayName).trim()) {
    return String(c.displayName).trim()
  }
  return c?.username || '用户'
}

const loginRedirectTo = computed(() => ({
  path: '/login',
  query: { redirect: route.fullPath },
}))

function canDeleteComment(comment) {
  if (!isLoggedIn()) return false
  if (isAdmin()) return true
  return comment.userId === currentUser.value?.userId
}

function canPinComment(comment) {
  if (!isLoggedIn() || !comment || comment.parentId != null) return false
  if (isAdmin()) return true
  return article.value?.authorId != null
    && article.value.authorId === currentUser.value?.userId
}

const pinLoadingId = ref(null)

async function onTogglePin(comment) {
  if (!canPinComment(comment) || pinLoadingId.value) return
  commentError.value = ''
  pinLoadingId.value = comment.id
  try {
    await pinComment(comment.id, !comment.pinned)
    await loadComments(article.value.id)
  } catch (e) {
    commentError.value = e.message || '置顶操作失败'
  } finally {
    pinLoadingId.value = null
  }
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
  commentNotice.value = ''
  const content = commentText.value.trim()
  if (!content) return
  if (!isLoggedIn()) {
    commentError.value = '请先登录后再评论'
    return
  }
  commentSubmitting.value = true
  try {
    const created = await createComment(article.value.id, content)
    commentText.value = ''
    if (created?.status === 'PENDING') {
      commentNotice.value = '评论已提交，待审核后显示'
    } else {
      await loadComments(article.value.id)
    }
  } catch (e) {
    commentError.value = e.message || '发表评论失败'
  } finally {
    commentSubmitting.value = false
  }
}

function startReply(comment) {
  commentError.value = ''
  commentNotice.value = ''
  if (!isLoggedIn()) {
    commentError.value = '请先登录后再回复'
    return
  }
  replyToId.value = comment.id
  replyText.value = ''
}

function cancelReply() {
  replyToId.value = null
  replyText.value = ''
}

async function onSubmitReply(parent) {
  commentError.value = ''
  commentNotice.value = ''
  const content = replyText.value.trim()
  if (!content) return
  if (!isLoggedIn()) {
    commentError.value = '请先登录后再回复'
    return
  }
  replySubmitting.value = true
  try {
    const created = await createComment(article.value.id, content, parent.id)
    cancelReply()
    if (created?.status === 'PENDING') {
      commentNotice.value = '回复已提交，待审核后显示'
    } else {
      await loadComments(article.value.id)
    }
  } catch (e) {
    commentError.value = e.message || '发表回复失败'
  } finally {
    replySubmitting.value = false
  }
}

async function onDeleteComment(comment) {
  try {
    await deleteComment(comment.id)
    if (replyToId.value === comment.id) {
      cancelReply()
    }
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
          <RouterLink :to="loginRedirectTo">登录</RouterLink> 后可发表评论
        </p>

        <p v-if="commentError" class="comment-error">{{ commentError }}</p>
        <p v-if="commentNotice" class="comment-notice">{{ commentNotice }}</p>

        <p v-if="commentsLoading" class="state">加载评论…</p>
        <p v-else-if="!comments.length" class="state">暂无评论，来抢沙发吧。</p>
        <ul v-else class="comment-list">
          <li v-for="c in comments" :key="c.id" class="comment-item" :class="{ 'is-pinned': c.pinned }">
            <div class="comment-head">
              <span v-if="c.floorNo != null" class="floor-badge">#{{ c.floorNo }}</span>
              <span v-if="c.pinned" class="pin-badge">置顶</span>
              <img
                v-if="c.avatarUrl"
                class="comment-avatar"
                :src="c.avatarUrl"
                alt=""
                width="28"
                height="28"
              />
              <strong>{{ commentAuthorLabel(c) }}</strong>
              <time>{{ formatDateTime(c.createdAt) }}</time>
              <button
                class="reply-btn"
                type="button"
                @click="startReply(c)"
              >
                回复
              </button>
              <button
                v-if="canPinComment(c)"
                class="pin-btn"
                type="button"
                :disabled="pinLoadingId === c.id"
                @click="onTogglePin(c)"
              >
                {{ c.pinned ? '取消置顶' : '置顶' }}
              </button>
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

            <form
              v-if="replyToId === c.id"
              class="reply-form"
              @submit.prevent="onSubmitReply(c)"
            >
              <textarea
                v-model="replyText"
                rows="2"
                :placeholder="`回复 ${commentAuthorLabel(c)}…`"
                maxlength="1000"
              />
              <div class="reply-actions">
                <button type="submit" :disabled="replySubmitting || !replyText.trim()">
                  {{ replySubmitting ? '发送中…' : '发送回复' }}
                </button>
                <button type="button" class="cancel-btn" @click="cancelReply">取消</button>
              </div>
            </form>

            <ul v-if="c.replies?.length" class="reply-list">
              <li v-for="r in c.replies" :key="r.id" class="comment-item is-reply">
                <div class="comment-head">
                  <img
                    v-if="r.avatarUrl"
                    class="comment-avatar"
                    :src="r.avatarUrl"
                    alt=""
                    width="28"
                    height="28"
                  />
                  <strong>{{ commentAuthorLabel(r) }}</strong>
                  <time>{{ formatDateTime(r.createdAt) }}</time>
                  <button
                    v-if="canDeleteComment(r)"
                    class="delete-btn"
                    type="button"
                    @click="onDeleteComment(r)"
                  >
                    删除
                  </button>
                </div>
                <p class="comment-body">{{ r.content }}</p>
              </li>
            </ul>
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

.comment-notice {
  margin: 0 0 12px;
  color: #0c7a5c;
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

.comment-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--border-soft);
}

.floor-badge {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--text-muted);
}

.pin-badge {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--text);
  background: var(--highlight);
  padding: 2px 8px;
  border-radius: 999px;
}

.comment-item.is-pinned {
  border-left: 3px solid var(--highlight);
  padding-left: 10px;
}

.comment-head strong {
  color: var(--text);
}

.reply-btn {
  margin-left: auto;
  border: none;
  background: transparent;
  color: var(--accent-lilac);
  font-size: 0.85rem;
  cursor: pointer;
}

.pin-btn {
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-size: 0.85rem;
  cursor: pointer;
}

.pin-btn:hover:not(:disabled) {
  color: var(--text);
}

.pin-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.delete-btn {
  border: none;
  background: transparent;
  color: #d63031;
  font-size: 0.85rem;
  cursor: pointer;
}

.comment-item.is-reply .delete-btn {
  margin-left: auto;
}

.reply-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.reply-form textarea {
  padding: 10px 12px;
  border: 1px solid var(--border-soft);
  border-radius: 10px;
  resize: vertical;
  font-family: inherit;
  font-size: 0.9rem;
}

.reply-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.reply-form button[type='submit'] {
  min-height: 32px;
  padding: 0 14px;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--primary);
  font-weight: 600;
  cursor: pointer;
}

.reply-form .cancel-btn {
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-size: 0.88rem;
  cursor: pointer;
}

.reply-list {
  margin: 12px 0 0;
  padding: 0 0 0 16px;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-left: 2px solid var(--border-soft);
}

.comment-item.is-reply {
  padding: 10px 12px;
  background: rgba(45, 52, 54, 0.03);
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
