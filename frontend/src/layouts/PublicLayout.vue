<script setup>
import { provideSiteSettings } from '@/composables/useSiteSettings'
import SiteHeader from '@/components/SiteHeader.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import DecorBackground from '@/components/DecorBackground.vue'

provideSiteSettings()
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
