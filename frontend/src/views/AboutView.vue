<script setup>
import { siteConfig } from '@/config/site'
</script>

<template>
  <section class="page content-about">
    <article class="panel">
      <img class="avatar" :src="siteConfig.avatar" :alt="siteConfig.author" width="96" height="96" />
      <h1>{{ siteConfig.about.title }}</h1>
      <p class="name">{{ siteConfig.author }}</p>
      <p class="intro">{{ siteConfig.about.intro }}</p>
      <ul class="highlights">
        <li v-for="(item, index) in siteConfig.about.highlights" :key="index">
          {{ item }}
        </li>
      </ul>
      <div v-if="siteConfig.socials.length" class="socials">
        <a
          v-for="item in siteConfig.socials"
          :key="item.href"
          class="social"
          :href="item.href"
          target="_blank"
          rel="noopener noreferrer"
        >
          {{ item.label }}
        </a>
      </div>
    </article>
  </section>
</template>

<style scoped>
.panel {
  padding: 40px 32px;
  border-radius: var(--radius-lg);
  background: #fff;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-soft);
  text-align: center;
}

.avatar {
  width: 96px;
  height: 96px;
  margin: 0 auto 16px;
  border-radius: 50%;
  border: 3px solid var(--highlight);
  box-shadow: 0 0 0 4px rgba(255, 217, 61, 0.2);
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
</style>
