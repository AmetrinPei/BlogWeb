<script setup>
import { useSiteSettings } from '@/composables/useSiteSettings'

const { site } = useSiteSettings()
</script>

<template>
  <footer class="site-footer">
    <div class="site-footer__inner content-wide">
      <p class="tagline">{{ site.tagline }}</p>
      <div v-if="site.socials.length" class="socials">
        <a
          v-for="item in site.socials"
          :key="item.href"
          class="social"
          :href="item.href"
          target="_blank"
          rel="noopener noreferrer"
        >
          {{ item.label }}
        </a>
      </div>
      <p class="feed-row">
        <a class="feed-link" href="/feed.xml" target="_blank" rel="noopener noreferrer">
          RSS 订阅
        </a>
      </p>
      <p class="copy">© {{ new Date().getFullYear() }} {{ site.name }}</p>
    </div>
  </footer>
</template>

<style scoped>
.site-footer {
  margin-top: auto;
  padding: 48px 0 36px;
  border-top: 1px solid var(--border-soft);
  background: linear-gradient(180deg, rgba(111, 207, 151, 0.06), transparent 70%);
}

.site-footer__inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  text-align: center;
}

.tagline {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 1.05rem;
  letter-spacing: 0.03em;
  color: var(--text);
}

.socials {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}

.social {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 14px;
  border-radius: var(--radius-pill);
  background: var(--primary-soft);
  color: var(--text);
  text-decoration: none;
  font-size: 0.9rem;
}

.feed-row {
  margin: 0;
}

.feed-link {
  color: var(--text-muted);
  font-size: 0.9rem;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.feed-link:hover {
  color: var(--text);
}

.copy {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 0.85rem;
}
</style>
