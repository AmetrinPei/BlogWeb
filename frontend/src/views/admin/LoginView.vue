<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '@/api/auth'
import { isLoggedIn, setAuthSession } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const username = ref('admin')
const password = ref('')
const loading = ref(false)
const error = ref('')

function redirectTarget() {
  const raw = route.query.redirect
  if (typeof raw === 'string' && raw.startsWith('/admin') && !raw.startsWith('/admin/login')) {
    return raw
  }
  return '/admin/articles'
}

onMounted(() => {
  if (isLoggedIn()) {
    router.replace(redirectTarget())
  }
})

async function onSubmit() {
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = '请输入账号和密码'
    return
  }
  loading.value = true
  try {
    const data = await login(username.value.trim(), password.value)
    setAuthSession({
      token: data.token,
      userId: data.userId,
      username: data.username,
      role: data.role,
    })
    await router.replace(redirectTarget())
  } catch (e) {
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <form class="card" @submit.prevent="onSubmit">
      <h1>管理登录</h1>
      <p class="hint">使用账号进入后台</p>

      <label class="field">
        <span>账号</span>
        <input v-model="username" type="text" autocomplete="username" required />
      </label>

      <label class="field">
        <span>密码</span>
        <input
          v-model="password"
          type="password"
          autocomplete="current-password"
          required
        />
      </label>

      <p v-if="error" class="error">{{ error }}</p>

      <button class="submit" type="submit" :disabled="loading">
        {{ loading ? '登录中…' : '登录' }}
      </button>

      <p class="links">
        <RouterLink to="/register">还没有账号？去注册</RouterLink>
      </p>

      <RouterLink class="back" to="/">← 返回访客站</RouterLink>
    </form>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  background: #f5f7f8;
  font-family: system-ui, -apple-system, 'Segoe UI', sans-serif;
  color: #2d3436;
}

.card {
  width: 100%;
  max-width: 400px;
  padding: 28px 24px;
  background: #fff;
  border: 1px solid #e6ebef;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(45, 52, 54, 0.04);
}

h1 {
  margin: 0;
  font-size: 1.4rem;
}

.hint {
  margin: 8px 0 22px;
  color: #636e72;
  font-size: 0.92rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
  font-size: 0.88rem;
  color: #636e72;
}

.field input {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid #dfe6e9;
  border-radius: 8px;
  outline: none;
  color: #2d3436;
}

.field input:focus {
  border-color: #6fcf97;
  box-shadow: 0 0 0 3px rgba(111, 207, 151, 0.2);
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
  border-radius: 8px;
  background: #6fcf97;
  color: #2d3436;
  font-weight: 700;
  cursor: pointer;
}

.submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.links {
  margin: 14px 0 0;
  text-align: center;
  font-size: 0.9rem;
}

.links a {
  color: #636e72;
  text-decoration: none;
}

.links a:hover {
  color: #2d3436;
}

.back {
  display: inline-flex;
  margin-top: 16px;
  color: #636e72;
  text-decoration: none;
  font-size: 0.9rem;
}

.back:hover {
  color: #2d3436;
}
</style>
