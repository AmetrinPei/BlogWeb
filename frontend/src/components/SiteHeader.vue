<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSiteSettings } from '@/composables/useSiteSettings'
import { useTheme } from '@/composables/useTheme'
import { logout as apiLogout } from '@/api/auth'
import {
  canAccessAdmin,
  clearAuth,
  displayLabel,
  getRefreshToken,
  useAuthSession,
} from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const { site } = useSiteSettings()
const { isDark, toggleTheme } = useTheme()
const { user, loggedIn } = useAuthSession()

const menuOpen = ref(false)

const isHome = computed(() => route.path === '/')
const isArticles = computed(() => route.path.startsWith('/articles'))
const isArchive = computed(() => route.path === '/archive')
const isAbout = computed(() => route.path === '/about')

const label = computed(() => displayLabel(user.value) || '账号')
const avatarSrc = computed(() => user.value?.avatarUrl || null)
const showAdminEntry = computed(() => canAccessAdmin())

const loginTo = computed(() => ({
  path: '/login',
  query: route.path.startsWith('/login') ? undefined : { redirect: route.fullPath },
}))

function toggleMenu() {
  menuOpen.value = !menuOpen.value
}

function closeMenu() {
  menuOpen.value = false
}

async function endServerSession() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return
  try {
    await apiLogout(refreshToken)
  } catch {
    // ignore — still clear local session
  }
}

async function onLogout() {
  await endServerSession()
  clearAuth()
  closeMenu()
}

async function onSwitchAccount() {
  await endServerSession()
  clearAuth()
  closeMenu()
  router.push({ name: 'login' })
}

function onDocClick(e) {
  if (!menuOpen.value) return
  const root = e.target?.closest?.('.account')
  if (!root) closeMenu()
}

onMounted(() => {
  document.addEventListener('click', onDocClick)
})
onUnmounted(() => {
  document.removeEventListener('click', onDocClick)
})
</script>

<template>
  <header class="site-header">
    <div class="site-header__inner content-wide">
      <RouterLink class="logo" to="/">{{ site.name }}</RouterLink>

      <nav class="pill-nav" aria-label="主导航">
        <RouterLink class="pill" :class="{ 'is-active': isHome }" to="/">首页</RouterLink>
        <RouterLink class="pill" :class="{ 'is-active': isArticles }" to="/articles">文章</RouterLink>
        <RouterLink class="pill" :class="{ 'is-active': isArchive }" to="/archive">归档</RouterLink>
        <RouterLink class="pill" :class="{ 'is-active': isAbout }" to="/about">关于我</RouterLink>
      </nav>

      <div class="header-actions">
        <button
          class="theme-toggle"
          type="button"
          :title="isDark ? '切换为亮色' : '切换为暗色'"
          :aria-label="isDark ? '切换为亮色' : '切换为暗色'"
          @click="toggleTheme"
        >
          {{ isDark ? '☀' : '☾' }}
        </button>

        <div v-if="!loggedIn" class="guest-auth">
          <RouterLink class="auth-link" :to="loginTo">登录</RouterLink>
          <RouterLink
            v-if="site.publicRegistrationEnabled"
            class="auth-link auth-link--strong"
            to="/register"
          >
            注册
          </RouterLink>
        </div>

        <div v-else class="account">
          <button
            class="account-trigger"
            type="button"
            :aria-expanded="menuOpen"
            aria-haspopup="true"
            @click.stop="toggleMenu"
          >
            <span v-if="avatarSrc" class="user-avatar">
              <img :src="avatarSrc" alt="" width="36" height="36" />
            </span>
            <span v-else class="user-avatar user-avatar--fallback" aria-hidden="true">
              {{ label.slice(0, 1) }}
            </span>
            <span class="account-name">{{ label }}</span>
          </button>
          <div v-if="menuOpen" class="account-menu" role="menu">
            <RouterLink role="menuitem" to="/profile" @click="closeMenu">我的资料</RouterLink>
            <RouterLink
              v-if="showAdminEntry"
              role="menuitem"
              to="/admin/articles"
              @click="closeMenu"
            >
              管理后台
            </RouterLink>
            <button role="menuitem" type="button" @click="onLogout">退出登录</button>
            <button role="menuitem" type="button" @click="onSwitchAccount">切换账号</button>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: var(--header-bg);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border-soft);
}

.site-header__inner {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 16px;
  min-height: var(--header-height);
}

.logo {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.45rem;
  letter-spacing: 0.04em;
  text-decoration: none;
  color: var(--text);
  white-space: nowrap;
}

.pill-nav {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
}

.pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 18px;
  border-radius: var(--radius-pill);
  text-decoration: none;
  color: var(--text-muted);
  background: transparent;
  transition: background 0.2s ease, color 0.2s ease;
}

.pill:hover {
  color: var(--text);
  background: var(--primary-soft);
}

.pill.is-active {
  color: var(--text);
  background: var(--primary);
  font-weight: 600;
}

.theme-toggle {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border-soft);
  background: var(--bg-elevated);
  color: var(--text);
  cursor: pointer;
  font-size: 1rem;
  line-height: 1;
}

.theme-toggle:hover {
  border-color: color-mix(in srgb, var(--primary) 50%, transparent);
  background: var(--primary-soft);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.guest-auth {
  display: flex;
  align-items: center;
  gap: 8px;
}

.auth-link {
  font-size: 0.9rem;
  color: var(--text-muted);
  text-decoration: none;
  padding: 6px 10px;
}

.auth-link:hover {
  color: var(--text);
}

.auth-link--strong {
  color: var(--text);
  background: var(--primary-soft);
  border-radius: var(--radius-pill);
  font-weight: 600;
}

.account {
  position: relative;
}

.account-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 999px;
  color: var(--text);
}

.account-trigger:hover {
  background: var(--primary-soft);
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid var(--border-soft);
  flex-shrink: 0;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-avatar--fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--primary);
  font-weight: 700;
  font-size: 0.9rem;
}

.account-name {
  max-width: 7em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.9rem;
  font-weight: 600;
}

.account-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  min-width: 160px;
  padding: 8px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-soft);
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  gap: 2px;
  z-index: 30;
}

.account-menu a,
.account-menu button {
  display: block;
  width: 100%;
  text-align: left;
  padding: 10px 12px;
  border: none;
  background: transparent;
  border-radius: 8px;
  color: var(--text);
  text-decoration: none;
  font-size: 0.92rem;
  cursor: pointer;
}

.account-menu a:hover,
.account-menu button:hover {
  background: var(--primary-soft);
}

@media (max-width: 720px) {
  .site-header__inner {
    grid-template-columns: 1fr auto;
    grid-template-areas:
      'logo actions'
      'nav nav';
    padding-block: 10px;
    gap: 10px;
  }

  .logo {
    grid-area: logo;
    font-size: 1.25rem;
  }

  .header-actions {
    grid-area: actions;
  }

  .pill-nav {
    grid-area: nav;
    justify-content: flex-start;
  }

  .pill {
    min-height: 32px;
    padding: 0 14px;
    font-size: 0.92rem;
  }

  .account-name {
    display: none;
  }
}
</style>
