<template>
  <div class="decor" aria-hidden="true">
    <svg class="star star-a" viewBox="0 0 24 24">
      <path
        d="M12 2l2.2 6.8H21l-5.4 4 2.1 6.8L12 15.8 6.3 19.6l2.1-6.8L3 8.8h6.8L12 2z"
        fill="currentColor"
      />
    </svg>
    <svg class="star star-b" viewBox="0 0 24 24">
      <path
        d="M12 2l2.2 6.8H21l-5.4 4 2.1 6.8L12 15.8 6.3 19.6l2.1-6.8L3 8.8h6.8L12 2z"
        fill="currentColor"
      />
    </svg>
    <svg class="star star-c" viewBox="0 0 24 24">
      <path
        d="M12 2l2.2 6.8H21l-5.4 4 2.1 6.8L12 15.8 6.3 19.6l2.1-6.8L3 8.8h6.8L12 2z"
        fill="currentColor"
      />
    </svg>
    <svg class="wave" viewBox="0 0 200 40" preserveAspectRatio="none">
      <path
        d="M0 24c30-16 50-16 80 0s50 16 80 0 40-12 40-12"
        fill="none"
        stroke="currentColor"
        stroke-width="3"
        stroke-linecap="round"
      />
    </svg>
  </div>
</template>

<style scoped>
.decor {
  pointer-events: none;
  position: absolute;
  inset: 0;
  overflow: hidden;
  z-index: 0;
}

.star,
.wave {
  position: absolute;
  opacity: 0.18;
}

.star {
  width: 18px;
  height: 18px;
  color: var(--highlight);
}

.star-a {
  top: 12%;
  left: 8%;
  transform: rotate(12deg);
}

.star-b {
  top: 28%;
  right: 10%;
  width: 14px;
  height: 14px;
  color: var(--accent-lilac);
  opacity: 0.16;
}

.star-c {
  top: 58%;
  left: 14%;
  width: 12px;
  height: 12px;
  color: var(--accent-peach);
  opacity: 0.14;
}

.wave {
  width: min(280px, 40vw);
  height: 40px;
  right: 6%;
  bottom: 18%;
  color: var(--primary);
  opacity: 0.2;
}

@media (max-width: 720px) {
  .star-c,
  .wave {
    display: none;
  }
}
</style>
