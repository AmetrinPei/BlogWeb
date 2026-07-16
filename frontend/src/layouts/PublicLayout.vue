<script setup>
import { computed, watch, onUnmounted } from 'vue'
import { provideSiteSettings } from '@/composables/useSiteSettings'
import { useTheme } from '@/composables/useTheme'
import SiteHeader from '@/components/SiteHeader.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import DecorBackground from '@/components/DecorBackground.vue'

const { site } = provideSiteSettings()
const { syncFromSiteDefault } = useTheme()

const GRADIENT_CLASS = {
  'mint-wash': 'grad-mint-wash',
  'lilac-mist': 'grad-lilac-mist',
  'peach-glow': 'grad-peach-glow',
}

watch(
  () => site.value.defaultTheme,
  (defaultTheme) => {
    syncFromSiteDefault(defaultTheme)
  },
  { immediate: true },
)

const shellMode = computed(() => site.value.backgroundMode || 'theme')
const shellClass = computed(() => {
  const mode = shellMode.value
  if (mode === 'gradient') {
    return GRADIENT_CLASS[site.value.backgroundGradient] || 'grad-mint-wash'
  }
  return ''
})
const shellStyle = computed(() => {
  const mode = shellMode.value
  if (mode === 'color' && site.value.backgroundColor) {
    return { '--custom-color': site.value.backgroundColor }
  }
  if (mode === 'image' && site.value.backgroundImageUrl) {
    const url = site.value.backgroundImageUrl.replace(/"/g, '')
    return { '--custom-image': `url("${url}")` }
  }
  return {}
})
const decorDimmed = computed(() => shellMode.value === 'image')

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
    <div
      class="shell-bg"
      :class="[shellClass, { 'is-image-dim': decorDimmed }]"
      :data-mode="shellMode"
      :style="shellStyle"
      aria-hidden="true"
    />
    <DecorBackground :class="{ 'decor--dim': decorDimmed }" />
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
  background: transparent;
}

.shell-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: var(--bg);
}

.shell-bg[data-mode='theme'] {
  background:
    radial-gradient(900px 480px at 12% -8%, var(--atmosphere-1), transparent 60%),
    radial-gradient(700px 420px at 92% 8%, var(--atmosphere-2), transparent 55%),
    radial-gradient(640px 380px at 70% 88%, var(--atmosphere-3), transparent 50%),
    linear-gradient(
      165deg,
      var(--bg) 0%,
      color-mix(in srgb, var(--bg) 88%, var(--primary)) 100%
    );
}

.shell-bg[data-mode='color'] {
  background: var(--custom-color, var(--bg));
}

.shell-bg[data-mode='gradient'].grad-mint-wash {
  background: linear-gradient(135deg, #e7f8ef 0%, #fff4e8 55%, #f3ecff 100%);
}
.shell-bg[data-mode='gradient'].grad-lilac-mist {
  background: linear-gradient(145deg, #efe8ff 0%, #e8f7f0 50%, #f7fbff 100%);
}
.shell-bg[data-mode='gradient'].grad-peach-glow {
  background: linear-gradient(160deg, #ffe8de 0%, #fff6d6 45%, #eef9f2 100%);
}

:global([data-theme='dark']) .shell-bg[data-mode='gradient'].grad-mint-wash {
  background: linear-gradient(135deg, #1e2a24 0%, #2a2520 55%, #222430 100%);
}
:global([data-theme='dark']) .shell-bg[data-mode='gradient'].grad-lilac-mist {
  background: linear-gradient(145deg, #262034 0%, #1e2a26 50%, #1c242c 100%);
}
:global([data-theme='dark']) .shell-bg[data-mode='gradient'].grad-peach-glow {
  background: linear-gradient(160deg, #2c221e 0%, #2a271c 45%, #1e2a24 100%);
}

.shell-bg[data-mode='image'] {
  background-image:
    linear-gradient(var(--shell-overlay), var(--shell-overlay)),
    var(--custom-image);
  background-size: cover;
  background-position: center;
}

.public-main {
  position: relative;
  z-index: 1;
  flex: 1;
  padding-block: var(--space-section);
}

:deep(.decor--dim) {
  opacity: 0.35;
}
</style>
