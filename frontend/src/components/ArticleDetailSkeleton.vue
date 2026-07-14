<script setup>
</script>

<template>
  <div class="detail-skeleton panel" aria-busy="true" aria-label="文章加载中">
    <div class="sk-cover sk-pulse" />
    <div class="sk-meta">
      <div class="sk-emoji sk-pulse" />
      <div class="sk-line sk-pulse short" />
      <div class="sk-line sk-pulse shorter" />
    </div>
    <div class="sk-line sk-pulse title" />
    <div class="sk-line sk-pulse title-sub" />
    <div class="sk-body">
      <div class="sk-line sk-pulse" />
      <div class="sk-line sk-pulse" />
      <div class="sk-line sk-pulse medium" />
      <div class="sk-line sk-pulse" />
      <div class="sk-line sk-pulse long" />
      <div class="sk-line sk-pulse medium" />
    </div>
  </div>
</template>

<style scoped>
.panel {
  padding: 36px 32px;
  border-radius: var(--radius-lg);
  background: #fff;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-soft);
}

.sk-cover {
  width: 100%;
  height: 180px;
  border-radius: var(--radius-lg);
  margin-bottom: 20px;
}

.sk-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.sk-emoji {
  width: 36px;
  height: 36px;
  border-radius: 12px;
}

.sk-body {
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid var(--border-soft);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sk-line {
  height: 12px;
  border-radius: 6px;
  width: 100%;
}

.sk-line.short {
  width: 88px;
  height: 10px;
}

.sk-line.shorter {
  width: 64px;
  height: 10px;
}

.sk-line.title {
  width: 78%;
  height: 28px;
  margin-bottom: 8px;
}

.sk-line.title-sub {
  width: 42%;
  height: 14px;
  margin-bottom: 8px;
}

.sk-line.medium {
  width: 70%;
}

.sk-line.long {
  width: 92%;
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

@media (max-width: 640px) {
  .panel {
    padding: 24px 18px;
  }

  .sk-cover {
    height: 120px;
  }
}
</style>
