<script setup>
import { watch, onUnmounted } from 'vue'
import { provideSiteSettings } from '@/composables/useSiteSettings'
import SiteHeader from '@/components/SiteHeader.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import DecorBackground from '@/components/DecorBackground.vue'

const { site } = provideSiteSettings()

let alternateLink = null

watch(
  () => site.value.name,
  (name) => {
    if (typeof document === 'undefined') return
    if (!alternateLink) {
      alternateLink = document.createElement('link')
      alternateLink.setAttribute('rel', 'alternate')
      alternateLink.setAttribute('type', 'application/rss+xml')
      alternateLink.setAttribute('href', '/feed.xml')
      document.head.appendChild(alternateLink)
    }
    alternateLink.setAttribute('title', name || 'RSS')
  },
  { immediate: true },
)

onUnmounted(() => {
  alternateLink?.remove()
  alternateLink = null
})
</script>

<template>
  <div class="public-layout">
    <DecorBackground />
    <SiteHeader />
    <main class="public-main">
      <RouterView />
    </main>
    <SiteFooter />
  </div>
</template>

<style scoped>
.public-layout {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}

.public-main {
  position: relative;
  z-index: 1;
  flex: 1;
  padding-block: var(--space-section);
}
</style>
