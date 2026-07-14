<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ArticleCard from '@/components/ArticleCard.vue'
import { fetchArticles } from '@/api/articles'
import { fetchCategories } from '@/api/categories'
import { fetchTags } from '@/api/tags'

const route = useRoute()
const router = useRouter()

const categories = ref([])
const tags = ref([])
const articles = ref([])
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const error = ref('')

const keywordInput = ref('')

const page = computed(() => {
  const n = Number(route.query.page)
  return Number.isFinite(n) && n >= 1 ? Math.floor(n) : 1
})

const categoryId = computed(() => {
  const v = route.query.categoryId
  return v ? String(v) : ''
})

const tagId = computed(() => {
  const v = route.query.tagId
  return v ? String(v) : ''
})

const keyword = computed(() => {
  const v = route.query.keyword
  return typeof v === 'string' ? v : ''
})

const yearMonth = computed(() => {
  const v = route.query.yearMonth
  return typeof v === 'string' ? v : ''
})

function buildQuery(overrides = {}) {
  const next = {
    page: overrides.page ?? page.value,
    categoryId: overrides.categoryId ?? categoryId.value,
    tagId: overrides.tagId ?? tagId.value,
    keyword: overrides.keyword ?? keyword.value,
    yearMonth: overrides.yearMonth ?? yearMonth.value,
  }
  const query = {}
  if (next.page && Number(next.page) > 1) query.page = String(next.page)
  if (next.categoryId) query.categoryId = String(next.categoryId)
  if (next.tagId) query.tagId = String(next.tagId)
  if (next.keyword?.trim()) query.keyword = next.keyword.trim()
  if (next.yearMonth) query.yearMonth = next.yearMonth
  return query
}

function replaceQuery(overrides) {
  router.replace({ name: 'articles', query: buildQuery(overrides) })
}

function onCategoryChange(event) {
  replaceQuery({ categoryId: event.target.value, page: 1 })
}

function onTagChange(event) {
  replaceQuery({ tagId: event.target.value, page: 1 })
}

function onSearchSubmit() {
  replaceQuery({ keyword: keywordInput.value, page: 1 })
}

function clearFilters() {
  keywordInput.value = ''
  router.replace({ name: 'articles', query: {} })
}

function goPage(target) {
  if (target < 1 || (totalPages.value > 0 && target > totalPages.value)) return
  replaceQuery({ page: target })
}

async function loadFilters() {
  const [cats, tgs] = await Promise.all([fetchCategories(), fetchTags()])
  categories.value = cats ?? []
  tags.value = tgs ?? []
}

async function loadArticles() {
  loading.value = true
  error.value = ''
  try {
    const params = {
      page: page.value,
      size: 10,
    }
    if (categoryId.value) params.categoryId = categoryId.value
    if (tagId.value) params.tagId = tagId.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (yearMonth.value) params.yearMonth = yearMonth.value

    const data = await fetchArticles(params)
    articles.value = data?.items ?? []
    total.value = data?.total ?? 0
    totalPages.value = data?.totalPages ?? 0
  } catch (e) {
    articles.value = []
    total.value = 0
    totalPages.value = 0
    error.value = e.message || '加载文章失败'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  keywordInput.value = keyword.value
  try {
    await loadFilters()
  } catch (e) {
    error.value = e.message || '加载筛选选项失败'
  }
  await loadArticles()
})

watch(
  () => [
    route.query.page,
    route.query.categoryId,
    route.query.tagId,
    route.query.keyword,
    route.query.yearMonth,
  ],
  () => {
    keywordInput.value = keyword.value
    loadArticles()
  },
)
</script>

<template>
  <section class="page content-wide">
    <header class="head">
      <h1>文章</h1>
      <p class="muted">
        <template v-if="yearMonth">归档：{{ yearMonth }}</template>
        <template v-else>按时间倒序浏览，支持分类、标签与标题关键词筛选。</template>
      </p>
    </header>

    <form class="filters" @submit.prevent="onSearchSubmit">
      <label class="field">
        <span>分类</span>
        <select :value="categoryId" @change="onCategoryChange">
          <option value="">全部</option>
          <option v-for="c in categories" :key="c.id" :value="String(c.id)">
            {{ c.name }}
          </option>
        </select>
      </label>

      <label class="field">
        <span>标签</span>
        <select :value="tagId" @change="onTagChange">
          <option value="">全部</option>
          <option v-for="t in tags" :key="t.id" :value="String(t.id)">
            {{ t.name }}
          </option>
        </select>
      </label>

      <label class="field keyword">
        <span>关键词</span>
        <input
          v-model="keywordInput"
          type="search"
          placeholder="搜索标题…"
          maxlength="100"
        />
      </label>

      <div class="actions">
        <button class="btn-primary" type="submit">搜索</button>
        <button class="btn-ghost" type="button" @click="clearFilters">清空</button>
      </div>
    </form>

    <p v-if="loading" class="state">加载中…</p>
    <p v-else-if="error" class="state error">{{ error }}</p>
    <p v-else-if="!articles.length" class="state">没有找到相关文章。</p>
    <div v-else class="list">
      <ArticleCard v-for="item in articles" :key="item.id" :article="item" />
    </div>

    <nav v-if="totalPages > 1" class="pager" aria-label="分页">
      <button type="button" :disabled="page <= 1 || loading" @click="goPage(page - 1)">
        上一页
      </button>
      <span class="pager__info">第 {{ page }} / {{ totalPages }} 页 · 共 {{ total }} 篇</span>
      <button
        type="button"
        :disabled="page >= totalPages || loading"
        @click="goPage(page + 1)"
      >
        下一页
      </button>
    </nav>
    <p v-else-if="total > 0 && !loading && !error" class="pager-single">
      共 {{ total }} 篇
    </p>
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

.filters {
  display: grid;
  grid-template-columns: repeat(2, minmax(140px, 200px)) minmax(180px, 1fr) auto;
  gap: 14px;
  align-items: end;
  margin-bottom: 28px;
  padding: 18px;
  border-radius: var(--radius-lg);
  background: rgba(111, 207, 151, 0.08);
  border: 1px solid var(--border-soft);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 0.88rem;
  color: var(--text-muted);
}

.field select,
.field input {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid var(--border-soft);
  border-radius: 12px;
  background: #fff;
  color: var(--text);
  outline: none;
}

.field select:focus,
.field input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.btn-primary,
.btn-ghost {
  min-height: 40px;
  padding: 0 18px;
  border: none;
  border-radius: var(--radius-pill);
  cursor: pointer;
  font-weight: 600;
}

.btn-primary {
  background: var(--primary);
  color: var(--text);
}

.btn-ghost {
  background: #fff;
  color: var(--text-muted);
  border: 1px solid var(--border-soft);
}

.list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.state {
  margin: 0;
  color: var(--text-muted);
}

.state.error {
  color: #d63031;
}

.pager {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 28px;
}

.pager button {
  min-height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--primary-soft);
  color: var(--text);
  cursor: pointer;
  font-weight: 600;
}

.pager button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.pager__info,
.pager-single {
  color: var(--text-muted);
  font-size: 0.92rem;
}

.pager-single {
  margin: 24px 0 0;
  text-align: center;
}

@media (max-width: 900px) {
  .filters {
    grid-template-columns: 1fr 1fr;
  }

  .keyword,
  .actions {
    grid-column: 1 / -1;
  }
}

@media (max-width: 560px) {
  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
