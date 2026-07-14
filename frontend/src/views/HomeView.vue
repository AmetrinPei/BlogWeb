<script setup>
import { onMounted, ref } from 'vue'
import ArticleCard from '@/components/ArticleCard.vue'
import { fetchArticles, fetchFeatured } from '@/api/articles'
import { useSiteSettings } from '@/composables/useSiteSettings'

const { site } = useSiteSettings()

const loading = ref(true)
const featuredLoading = ref(true)
const error = ref('')
const featuredError = ref('')
const articles = ref([])
const featured = ref([])

onMounted(async () => {
  try {
    const data = await fetchArticles({ page: 1, size: 5 })
    articles.value = data?.items ?? []
  } catch (e) {
    error.value = e.message || '加载文章失败'
  } finally {
    loading.value = false
  }

  try {
    featured.value = (await fetchFeatured(4)) ?? []
  } catch (e) {
    featuredError.value = e.message || '加载精选失败'
  } finally {
    featuredLoading.value = false
  }
})
</script>

<template>
  <div class="home">
    <section class="hero content-wide">
      <div class="hero__copy">
        <p class="eyebrow">Welcome</p>
        <h1>Hi, 我是{{ site.author }} 👋</h1>
        <p class="lead">{{ site.heroSubtitle }}</p>
        <div class="cta">
          <RouterLink class="btn-primary" to="/articles">看看文章</RouterLink>
          <RouterLink class="btn-ghost" to="/about">关于我</RouterLink>
        </div>
      </div>
      <div class="hero__art">
        <img src="/hero-illustration.svg" alt="" width="420" height="320" />
      </div>
    </section>

    <section v-if="featured.length || featuredLoading" class="featured content-wide section-gap">
      <div class="featured__head">
        <h2>精选</h2>
      </div>
      <p v-if="featuredLoading" class="state">加载中…</p>
      <p v-else-if="featuredError" class="state error">{{ featuredError }}</p>
      <div v-else class="list">
        <ArticleCard v-for="item in featured" :key="item.id" :article="item" />
      </div>
    </section>

    <section class="latest content-wide section-gap">
      <div class="latest__head">
        <h2>最新文章</h2>
        <RouterLink class="more" to="/articles">全部文章 →</RouterLink>
      </div>

      <p v-if="loading" class="state">加载中…</p>
      <p v-else-if="error" class="state error">{{ error }}</p>
      <p v-else-if="!articles.length" class="state">暂时还没有文章，稍后再来看看吧。</p>
      <div v-else class="list">
        <ArticleCard v-for="item in articles" :key="item.id" :article="item" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 40px;
  align-items: center;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--accent-lilac);
  font-family: var(--font-display);
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  font-size: 0.85rem;
}

h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(1.8rem, 3vw, 2.6rem);
  line-height: 1.25;
  letter-spacing: 0.02em;
}

.lead {
  margin: 16px 0 0;
  color: var(--text-muted);
  font-size: 1.05rem;
  max-width: 34em;
}

.cta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.btn-primary,
.btn-ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 20px;
  border-radius: var(--radius-pill);
  text-decoration: none;
  font-weight: 600;
}

.btn-primary {
  background: var(--primary);
  color: var(--text);
}

.btn-ghost {
  background: rgba(179, 136, 255, 0.14);
  color: var(--text);
}

.hero__art img {
  width: 100%;
  height: auto;
  border-radius: var(--radius-lg);
}

.featured__head,
.latest__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.featured__head h2,
.latest__head h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.4rem;
}

.more {
  color: var(--text-muted);
  text-decoration: none;
  font-size: 0.95rem;
}

.more:hover {
  color: var(--text);
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

@media (max-width: 860px) {
  .hero {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .hero__art {
    order: -1;
  }
}
</style>
