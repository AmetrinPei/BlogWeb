<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { register } from '@/api/auth'
import { useSiteSettings } from '@/composables/useSiteSettings'
import { isSafePublicRedirect, setAuthSession } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const { site } = useSiteSettings()

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref('')

const registrationOpen = computed(() => site.value.publicRegistrationEnabled !== false)

function redirectTarget() {
  const raw = route.query.redirect
  if (isSafePublicRedirect(raw)) return raw
  return '/'
}

async function onSubmit() {
  error.value = ''
  if (!registrationOpen.value) {
    error.value = '当前不开放注册'
    return
  }
  const name = username.value.trim()
  if (!name || !password.value) {
    error.value = '请输入用户名和密码'
    return
  }
  if (password.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  try {
    const data = await register(name, password.value)
    setAuthSession({
      token: data.token,
      refreshToken: data.refreshToken,
      userId: data.userId,
      username: data.username,
      role: data.role,
      displayName: data.displayName,
      avatarUrl: data.avatarUrl,
    })
    await router.replace(redirectTarget())
  } catch (e) {
    error.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="page content-wide">
    <form class="card" @submit.prevent="onSubmit">
      <h1>注册账号</h1>
      <p v-if="!registrationOpen" class="hint closed">当前不开放注册，请联系站点管理员。</p>
      <p v-else class="hint">注册后即可评论与写作；默认回到访客站</p>

      <label class="field">
        <span>用户名</span>
        <input
          v-model="username"
          type="text"
          autocomplete="username"
          required
          :disabled="!registrationOpen"
        />
      </label>

      <label class="field">
        <span>密码</span>
        <input
          v-model="password"
          type="password"
          autocomplete="new-password"
          required
          :disabled="!registrationOpen"
        />
      </label>

      <label class="field">
        <span>确认密码</span>
        <input
          v-model="confirmPassword"
          type="password"
          autocomplete="new-password"
          required
          :disabled="!registrationOpen"
        />
      </label>

      <p v-if="error" class="error">{{ error }}</p>

      <button class="submit" type="submit" :disabled="loading || !registrationOpen">
        {{ loading ? '注册中…' : '注册' }}
      </button>

      <p class="links">
        <RouterLink
          :to="
            route.query.redirect
              ? { path: '/login', query: { redirect: route.query.redirect } }
              : '/login'
          "
        >
          已有账号？去登录
        </RouterLink>
      </p>
    </form>
  </section>
</template>

<style scoped>
.card {
  max-width: 420px;
  margin: 0 auto;
  padding: 32px 28px;
  border-radius: var(--radius-lg);
  background: var(--bg-elevated);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-soft);
}

h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.5rem;
}

.hint {
  margin: 8px 0 22px;
  color: var(--text-muted);
  font-size: 0.92rem;
}

.hint.closed {
  color: #d63031;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
  font-size: 0.88rem;
  color: var(--text-muted);
}

.field input {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid var(--border-soft);
  border-radius: 12px;
  outline: none;
  color: var(--text);
}

.field input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

.error {
  margin: 0 0 12px;
  color: #d63031;
  font-size: 0.9rem;
}

.submit {
  width: 100%;
  min-height: 42px;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--primary);
  color: var(--text);
  font-weight: 700;
  cursor: pointer;
}

.submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.links {
  margin: 16px 0 0;
  text-align: center;
  font-size: 0.9rem;
}

.links a {
  color: var(--text-muted);
  text-decoration: none;
}

.links a:hover {
  color: var(--text);
}
</style>
