<script setup>
import { computed } from 'vue'
import AboutAvatar from '@/components/AboutAvatar.vue'
import { useSiteSettings } from '@/composables/useSiteSettings'

const { site } = useSiteSettings()

const avatarSrc = computed(() => site.value.aboutAvatarUrl || site.value.avatar)
const aboutTitle = computed(() => site.value.aboutTitle || '关于我')
const displayName = computed(() => site.value.author || '')
const aboutIntro = computed(() => (site.value.aboutText || '').trim())
const highlights = computed(() => site.value.aboutHighlights || [])
const friendLinks = computed(() => site.value.friendLinks || [])
</script>

<template>
  <section class="page content-about">
    <article class="panel">
      <AboutAvatar :src="avatarSrc" :alt="displayName" />
      <h1>{{ aboutTitle }}</h1>
      <p v-if="displayName" class="name">{{ displayName }}</p>
      <p v-if="aboutIntro" class="intro">{{ aboutIntro }}</p>
      <ul v-if="highlights.length" class="highlights">
        <li v-for="(item, index) in highlights" :key="index">
          {{ item }}
        </li>
      </ul>
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

      <section v-if="friendLinks.length" class="friends" aria-label="友情链接">
        <h2>友情链接</h2>
        <ul>
          <li v-for="item in friendLinks" :key="item.url + item.name">
            <a :href="item.url" target="_blank" rel="noopener noreferrer">
              {{ item.name }}
            </a>
            <p v-if="item.description" class="friend-desc">{{ item.description }}</p>
          </li>
        </ul>
      </section>
    </article>
  </section>
</template>

<style scoped>
.panel {
  padding: 40px 32px;
  border-radius: var(--radius-lg);
  background: var(--bg-elevated);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-soft);
  text-align: center;
}

h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.7rem;
}

.name {
  margin: 8px 0 0;
  color: var(--accent-peach);
  font-weight: 600;
}

.intro {
  margin: 18px auto 0;
  max-width: 36em;
  color: var(--text-muted);
  line-height: 1.75;
  text-align: left;
}

.highlights {
  margin: 24px auto 0;
  padding: 0;
  max-width: 28em;
  list-style: none;
  text-align: left;
}

.highlights li {
  position: relative;
  padding: 10px 12px 10px 28px;
  border-radius: 12px;
}

.highlights li::before {
  content: '✦';
  position: absolute;
  left: 8px;
  color: var(--highlight);
}

.highlights li:nth-child(odd) {
  background: rgba(111, 207, 151, 0.08);
}

.socials {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 24px;
}

.social {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: var(--radius-pill);
  background: var(--primary-soft);
  text-decoration: none;
  color: var(--text);
  font-size: 0.9rem;
}

.friends {
  margin: 32px auto 0;
  max-width: 32em;
  text-align: left;
  border-top: 1px solid var(--border-soft);
  padding-top: 24px;
}

.friends h2 {
  margin: 0 0 14px;
  font-family: var(--font-display);
  font-size: 1.15rem;
  text-align: center;
}

.friends ul {
  margin: 0;
  padding: 0;
  list-style: none;
}

.friends li {
  padding: 10px 0;
  border-bottom: 1px dashed var(--border-soft);
}

.friends li:last-child {
  border-bottom: none;
}

.friends a {
  color: var(--text);
  font-weight: 600;
  text-decoration: none;
}

.friends a:hover {
  color: var(--accent-peach);
}

.friend-desc {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 0.9rem;
  line-height: 1.5;
}
</style>
