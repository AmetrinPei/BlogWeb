<script setup>
import { computed } from 'vue'

const props = defineProps({
  src: {
    type: String,
    default: '',
  },
  alt: {
    type: String,
    default: '',
  },
})

const imageSrc = computed(() => props.src || '/avatar.svg')
</script>

<template>
  <div class="about-avatar">
    <div class="about-avatar__glow" aria-hidden="true" />
    <div class="about-avatar__ring">
      <div class="about-avatar__inner">
        <img class="about-avatar__img" :src="imageSrc" :alt="alt" width="140" height="140" />
      </div>
    </div>
    <svg class="ornament ornament-a" viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M12 2l2.2 6.8H21l-5.4 4 2.1 6.8L12 15.8 6.3 19.6l2.1-6.8L3 8.8h6.8L12 2z"
        fill="currentColor"
      />
    </svg>
    <svg class="ornament ornament-b" viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M12 2l2.2 6.8H21l-5.4 4 2.1 6.8L12 15.8 6.3 19.6l2.1-6.8L3 8.8h6.8L12 2z"
        fill="currentColor"
      />
    </svg>
  </div>
</template>

<style scoped>
.about-avatar {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 0 auto 16px;
}

.about-avatar__glow {
  position: absolute;
  inset: -18px;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    color-mix(in srgb, var(--primary) 40%, transparent),
    transparent 70%
  );
  filter: blur(6px);
  pointer-events: none;
}

.about-avatar__ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  padding: 5px;
  background: conic-gradient(
    from 200deg,
    var(--primary),
    var(--accent-lilac),
    var(--highlight),
    var(--accent-peach),
    var(--primary)
  );
}

.about-avatar__inner {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  padding: 4px;
  background: var(--bg-elevated);
}

.about-avatar__img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.ornament {
  position: absolute;
  width: 16px;
  height: 16px;
  color: var(--highlight);
  pointer-events: none;
}

.ornament-a {
  top: 2px;
  right: 6px;
}

.ornament-b {
  bottom: 8px;
  left: -2px;
  width: 12px;
  height: 12px;
  color: var(--accent-peach);
}
</style>
