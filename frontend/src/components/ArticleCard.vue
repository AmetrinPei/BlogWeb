<script setup>
import { computed } from 'vue'
import { categoryEmoji, formatDateTime } from '@/utils/format'

const props = defineProps({
  article: {
    type: Object,
    required: true,
  },
})

const emoji = computed(() => categoryEmoji(props.article.category?.name))
const publishedAt = computed(() => formatDateTime(props.article.publishedAt))
</script>

<template>
  <RouterLink class="article-card" :to="`/articles/${article.id}`">
    <img
      v-if="article.coverUrl"
      class="cover"
      :src="article.coverUrl"
      :alt="article.title"
      loading="lazy"
    />
    <span v-else class="emoji" aria-hidden="true">{{ emoji }}</span>
    <div class="body">
      <div class="meta">
        <time v-if="publishedAt" :datetime="article.publishedAt">{{ publishedAt }}</time>
        <span v-if="article.category" class="category">{{ article.category.name }}</span>
        <span v-if="article.pinned" class="badge">置顶</span>
        <span v-if="article.viewCount != null" class="views">{{ article.viewCount }} 阅读</span>
      </div>
      <h3 class="title">{{ article.title }}</h3>
      <p v-if="article.summary" class="summary">{{ article.summary }}</p>
      <ul v-if="article.tags?.length" class="tags">
        <li v-for="tag in article.tags" :key="tag.id">#{{ tag.name }}</li>
      </ul>
    </div>
  </RouterLink>
</template>

<style scoped>
.article-card {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding: 24px;
  border-radius: var(--radius-lg);
  background: #fff;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-soft);
  text-decoration: none;
  color: inherit;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.article-card:hover {
  transform: translateY(-2px);
  border-color: rgba(111, 207, 151, 0.45);
  box-shadow: 0 12px 32px rgba(45, 52, 54, 0.08);
}

.cover {
  flex-shrink: 0;
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 12px;
}

.emoji {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: var(--primary-soft);
  font-size: 1.35rem;
  line-height: 1;
}

.body {
  min-width: 0;
  flex: 1;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 8px;
  color: var(--text-muted);
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
}

.badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: var(--radius-pill);
  background: rgba(179, 136, 255, 0.16);
  color: var(--accent-lilac);
  font-weight: 600;
  font-size: 0.82rem;
}

.views {
  font-size: 0.85rem;
}

.title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.2rem;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: 0.01em;
}

.summary {
  margin: 8px 0 0;
  color: var(--text-muted);
  font-size: 0.92rem;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
  color: var(--accent-lilac);
  font-size: 0.88rem;
}
</style>
