<script setup>
defineProps({
  count: {
    type: Number,
    default: 4,
  },
})
</script>

<template>
  <div class="list-skeleton" aria-busy="true" aria-label="文章列表加载中">
    <div v-for="n in count" :key="n" class="sk-card">
      <div class="sk-thumb sk-pulse" />
      <div class="sk-body">
        <div class="sk-line sk-pulse short" />
        <div class="sk-line sk-pulse title" />
        <div class="sk-line sk-pulse" />
        <div class="sk-line sk-pulse medium" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.list-skeleton {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sk-card {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding: 24px;
  border-radius: var(--radius-lg);
  background: var(--bg-elevated);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-soft);
}

.sk-thumb {
  flex-shrink: 0;
  width: 120px;
  height: 80px;
  border-radius: 12px;
}

.sk-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sk-line {
  height: 12px;
  border-radius: 6px;
  width: 100%;
}

.sk-line.short {
  width: 36%;
  height: 10px;
}

.sk-line.title {
  width: 72%;
  height: 16px;
}

.sk-line.medium {
  width: 58%;
}

.sk-pulse {
  background: linear-gradient(
    90deg,
    rgba(45, 52, 54, 0.06) 0%,
    rgba(111, 207, 151, 0.12) 50%,
    rgba(45, 52, 54, 0.06) 100%
  );
  background-size: 200% 100%;
  animation: shimmer 1.4s ease-in-out infinite;
}

@keyframes shimmer {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: -100% 0;
  }
}

@media (max-width: 560px) {
  .sk-thumb {
    width: 64px;
    height: 64px;
  }
}
</style>
