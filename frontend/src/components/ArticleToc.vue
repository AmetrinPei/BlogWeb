<script setup>
defineProps({
  items: {
    type: Array,
    default: () => [],
  },
})

function onClick(event, id) {
  event.preventDefault()
  const el = document.getElementById(id)
  if (!el) return
  el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  const url = `${window.location.pathname}${window.location.search}#${id}`
  history.replaceState(null, '', url)
}
</script>

<template>
  <nav v-if="items.length" class="article-toc" aria-label="文章目录">
    <p class="toc-title">目录</p>
    <ol class="toc-list">
      <li
        v-for="item in items"
        :key="item.id"
        :class="['toc-item', `level-${item.level}`]"
      >
        <a :href="`#${item.id}`" @click="onClick($event, item.id)">{{ item.text }}</a>
      </li>
    </ol>
  </nav>
</template>

<style scoped>
.article-toc {
  margin: 20px 0 0;
  padding: 16px 18px;
  border-radius: 14px;
  background: rgba(111, 207, 151, 0.08);
  border: 1px solid var(--border-soft);
}

.toc-title {
  margin: 0 0 10px;
  font-family: var(--font-display);
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--text);
}

.toc-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.toc-item a {
  color: var(--text-muted);
  text-decoration: none;
  font-size: 0.92rem;
  line-height: 1.45;
}

.toc-item a:hover {
  color: var(--text);
  text-decoration: underline;
}

.toc-item.level-3 {
  padding-left: 14px;
}

.toc-item.level-3 a {
  font-size: 0.88rem;
}
</style>
