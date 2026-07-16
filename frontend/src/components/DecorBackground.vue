<template>
  <div class="decor" aria-hidden="true">
    <div class="blob blob-a" />
    <div class="blob blob-b" />
    <div class="blob blob-c" />
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

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(48px);
  opacity: 0.55;
  animation: drift 18s ease-in-out infinite;
}

.blob-a {
  width: min(320px, 42vw);
  height: min(320px, 42vw);
  top: 6%;
  left: -4%;
  background: var(--atmosphere-1);
}

.blob-b {
  width: min(260px, 36vw);
  height: min(260px, 36vw);
  top: 16%;
  right: 2%;
  background: var(--atmosphere-2);
  animation-delay: -6s;
}

.blob-c {
  width: min(280px, 40vw);
  height: min(220px, 32vw);
  bottom: 8%;
  left: 32%;
  background: var(--atmosphere-3);
  animation-delay: -11s;
}

.star,
.wave {
  position: absolute;
  opacity: 0.45;
}

.star {
  width: 16px;
  height: 16px;
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
  width: 13px;
  height: 13px;
  color: var(--accent-lilac);
  opacity: 0.4;
}

.star-c {
  top: 58%;
  left: 14%;
  width: 12px;
  height: 12px;
  color: var(--accent-peach);
  opacity: 0.38;
}

.wave {
  width: min(280px, 40vw);
  height: 40px;
  right: 6%;
  bottom: 18%;
  color: var(--primary);
  opacity: 0.35;
}

@keyframes drift {
  0%,
  100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(16px, -12px);
  }
}

@media (max-width: 720px) {
  .star-c,
  .wave,
  .blob-c {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .blob {
    animation: none;
  }
}
</style>
