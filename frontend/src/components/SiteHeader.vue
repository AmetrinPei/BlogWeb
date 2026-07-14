<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useSiteSettings } from '@/composables/useSiteSettings'

const route = useRoute()
const { site } = useSiteSettings()

const isHome = computed(() => route.path === '/')
const isArticles = computed(() => route.path.startsWith('/articles'))
const isArchive = computed(() => route.path === '/archive')
const isAbout = computed(() => route.path === '/about')
</script>

<template>
  <header class="site-header">
    <div class="site-header__inner content-wide">
      <RouterLink class="logo" to="/">{{ site.name }}</RouterLink>

      <nav class="pill-nav" aria-label="主导航">
        <RouterLink class="pill" :class="{ 'is-active': isHome }" to="/">首页</RouterLink>
        <RouterLink class="pill" :class="{ 'is-active': isArticles }" to="/articles">文章</RouterLink>
        <RouterLink class="pill" :class="{ 'is-active': isArchive }" to="/archive">归档</RouterLink>
        <RouterLink class="pill" :class="{ 'is-active': isAbout }" to="/about">关于我</RouterLink>
      </nav>

      <RouterLink class="avatar" to="/about" :title="`关于 ${site.author}`">
        <img :src="site.avatar" alt="头像" width="44" height="44" />
      </RouterLink>
    </div>
  </header>
</template>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border-soft);
}

.site-header__inner {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 16px;
  min-height: var(--header-height);
}

.logo {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.45rem;
  letter-spacing: 0.04em;
  text-decoration: none;
  color: var(--text);
  white-space: nowrap;
}

.pill-nav {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
}

.pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 18px;
  border-radius: var(--radius-pill);
  text-decoration: none;
  color: var(--text-muted);
  background: transparent;
  transition: background 0.2s ease, color 0.2s ease;
}

.pill:hover {
  color: var(--text);
  background: var(--primary-soft);
}

.pill.is-active {
  color: var(--text);
  background: var(--primary);
  font-weight: 600;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid var(--highlight);
  box-shadow: 0 0 0 3px rgba(255, 217, 61, 0.25);
  flex-shrink: 0;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

@media (max-width: 720px) {
  .site-header__inner {
    grid-template-columns: 1fr auto;
    grid-template-areas:
      'logo avatar'
      'nav nav';
    padding-block: 10px;
    gap: 10px;
  }

  .logo {
    grid-area: logo;
    font-size: 1.25rem;
  }

  .avatar {
    grid-area: avatar;
    width: 40px;
    height: 40px;
  }

  .pill-nav {
    grid-area: nav;
    justify-content: flex-start;
  }

  .pill {
    min-height: 32px;
    padding: 0 14px;
    font-size: 0.92rem;
  }
}
</style>
