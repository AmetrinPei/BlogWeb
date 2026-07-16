<script setup>
import { computed } from 'vue'

const props = defineProps({
  src: {
    type: String,
    default: '',
  },
})

const hasCustom = computed(() => Boolean(props.src?.trim()))
</script>

<template>
  <div class="hero-art">
    <div class="sticker sticker-wave" aria-hidden="true">
      <svg viewBox="0 0 200 40" width="70" height="28">
        <path
          d="M0 24c30-16 50-16 80 0s50 16 80 0 40-12 40-12"
          fill="none"
          stroke="currentColor"
          stroke-width="8"
          stroke-linecap="round"
        />
      </svg>
    </div>
    <div class="sticker sticker-star" aria-hidden="true">
      <svg viewBox="0 0 24 24">
        <path
          fill="currentColor"
          d="M12 2l2.2 6.8H21l-5.4 4 2.1 6.8L12 15.8 6.3 19.6l2.1-6.8L3 8.8h6.8L12 2z"
        />
      </svg>
    </div>
    <div class="sticker sticker-note" aria-hidden="true">今日份灵感</div>
    <div class="sticker sticker-dot" aria-hidden="true" />

    <div class="hero-stage">
      <img
        v-if="hasCustom"
        class="hero-main hero-main--img"
        :src="src"
        alt=""
        width="420"
        height="320"
      />
      <div v-else class="hero-main hero-main--default" aria-hidden="true">
        <div class="face">
          <span class="smile" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hero-art {
  position: relative;
  min-height: 280px;
}

.hero-stage {
  position: relative;
  border-radius: var(--radius-lg);
  background: var(--bg-elevated);
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  aspect-ratio: 420 / 320;
}

.hero-main--img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-main--default {
  position: absolute;
  inset: 12% 14% 18% 14%;
  border-radius: 28px;
  background:
    radial-gradient(circle at 78% 22%, var(--highlight), transparent 40%),
    radial-gradient(circle at 42% 68%, var(--atmosphere-1), transparent 55%),
    linear-gradient(160deg, color-mix(in srgb, var(--bg) 70%, white), var(--primary-soft));
}

.face {
  position: absolute;
  left: 50%;
  top: 42%;
  width: 72px;
  height: 72px;
  margin: -36px 0 0 -36px;
  border-radius: 50%;
  background: #fff3e0;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

[data-theme='dark'] .face {
  background: #3a342c;
}

.face::before,
.face::after {
  content: '';
  position: absolute;
  top: 28px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--text);
}

.face::before {
  left: 22px;
}

.face::after {
  right: 22px;
}

.smile {
  position: absolute;
  left: 50%;
  bottom: 20px;
  width: 22px;
  height: 10px;
  margin-left: -11px;
  border: 3px solid var(--accent-peach);
  border-top: none;
  border-radius: 0 0 12px 12px;
}

.sticker {
  position: absolute;
  z-index: 2;
  animation: float 4.5s ease-in-out infinite;
  filter: drop-shadow(0 6px 10px rgba(0, 0, 0, 0.08));
  pointer-events: none;
}

.sticker-star {
  top: 4%;
  right: 6%;
  width: 44px;
  height: 44px;
  color: var(--highlight);
  animation-delay: -1s;
}

.sticker-note {
  bottom: 8%;
  left: 2%;
  padding: 10px 12px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--accent-lilac) 22%, var(--bg-elevated));
  border: 1px solid var(--border-soft);
  font-family: var(--font-display);
  font-size: 0.78rem;
  font-weight: 600;
  transform: rotate(-8deg);
  animation-delay: -2.2s;
  color: var(--text);
}

.sticker-wave {
  top: 16%;
  left: 4%;
  color: var(--primary);
  opacity: 0.85;
  animation-delay: -0.6s;
}

.sticker-dot {
  bottom: 14%;
  right: 8%;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--accent-peach);
  opacity: 0.75;
  animation-delay: -3s;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

@media (max-width: 860px) {
  .sticker-wave,
  .sticker-dot {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .sticker {
    animation: none;
  }
}
</style>
