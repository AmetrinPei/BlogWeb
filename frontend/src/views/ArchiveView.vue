<script setup>
import { onMounted, ref } from 'vue'
import { fetchArchive } from '@/api/articles'

const loading = ref(true)
const error = ref('')
const months = ref([])

onMounted(async () => {
  try {
    months.value = (await fetchArchive()) ?? []
  } catch (e) {
    error.value = e.message || '加载归档失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="page content-wide">
    <header class="head">
      <h1>归档</h1>
      <p class="muted">按年月浏览已发布文章</p>
    </header>

    <p v-if="loading" class="state">加载中…</p>
    <p v-else-if="error" class="state error">{{ error }}</p>
    <p v-else-if="!months.length" class="state">暂无归档记录。</p>
    <ul v-else class="archive-list">
      <li v-for="item in months" :key="item.yearMonth">
        <RouterLink
          class="archive-item"
          :to="{ name: 'articles', query: { yearMonth: item.yearMonth } }"
        >
          <span class="month">{{ item.yearMonth }}</span>
          <span class="count">{{ item.count }} 篇</span>
        </RouterLink>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.head {
  margin-bottom: 24px;
}

h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.7rem;
}

.muted {
  margin: 8px 0 0;
  color: var(--text-muted);
}

.archive-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.archive-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
  border-radius: var(--radius-lg);
  background: #fff;
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-card);
  text-decoration: none;
  color: inherit;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.archive-item:hover {
  border-color: rgba(111, 207, 151, 0.45);
  transform: translateY(-1px);
}

.month {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.1rem;
}

.count {
  color: var(--text-muted);
  font-size: 0.92rem;
}

.state {
  margin: 0;
  color: var(--text-muted);
}

.state.error {
  color: #d63031;
}
</style>
