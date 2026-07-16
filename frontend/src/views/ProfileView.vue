<script setup>
import { onMounted, ref } from 'vue'
import { changePassword, getMe, updateMe } from '@/api/profile'
import { uploadMedia } from '@/api/adminMedia'
import { patchAuthUser } from '@/utils/auth'

const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)
const error = ref('')
const success = ref('')

const username = ref('')
const role = ref('')
const displayName = ref('')
const bio = ref('')
const avatarUrl = ref('')

const pwdSaving = ref(false)
const pwdError = ref('')
const pwdSuccess = ref('')
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await getMe()
    username.value = data.username || ''
    role.value = data.role || ''
    displayName.value = data.displayName || ''
    bio.value = data.bio || ''
    avatarUrl.value = data.avatarUrl || ''
  } catch (e) {
    error.value = e.message || '加载资料失败'
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const data = await updateMe({
      displayName: displayName.value,
      bio: bio.value,
      avatarUrl: avatarUrl.value,
    })
    displayName.value = data.displayName || ''
    bio.value = data.bio || ''
    avatarUrl.value = data.avatarUrl || ''
    patchAuthUser({
      displayName: data.displayName,
      avatarUrl: data.avatarUrl,
      username: data.username,
      role: data.role,
    })
    success.value = '已保存'
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function onPickAvatar(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  uploading.value = true
  error.value = ''
  try {
    const data = await uploadMedia(file)
    avatarUrl.value = data.url
    success.value = '头像已上传，请点击保存'
  } catch (err) {
    error.value = err.message || '上传失败'
  } finally {
    uploading.value = false
  }
}

async function onChangePassword() {
  pwdSaving.value = true
  pwdError.value = ''
  pwdSuccess.value = ''
  if (!currentPassword.value || !newPassword.value || !confirmPassword.value) {
    pwdError.value = '请填写当前密码与新密码'
    pwdSaving.value = false
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    pwdError.value = '两次输入的新密码不一致'
    pwdSaving.value = false
    return
  }
  try {
    await changePassword({
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
      confirmPassword: confirmPassword.value,
    })
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    pwdSuccess.value = '密码已修改。其它设备上的登录将失效，需重新登录。'
  } catch (e) {
    pwdError.value = e.message || '修改密码失败'
  } finally {
    pwdSaving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="page content-wide">
    <form class="card" @submit.prevent="onSave">
      <h1>我的资料</h1>
      <p class="hint">展示名与头像会显示在评论与文章作者处</p>

      <p v-if="loading" class="state">加载中…</p>

      <template v-else>
        <div class="preview">
          <img
            v-if="avatarUrl"
            class="preview-avatar"
            :src="avatarUrl"
            alt="头像预览"
            width="72"
            height="72"
          />
          <span v-else class="preview-avatar preview-avatar--fallback">
            {{ (displayName || username || '?').slice(0, 1) }}
          </span>
          <div class="preview-meta">
            <strong>{{ displayName || username }}</strong>
            <span>登录名 {{ username }} · {{ role }}</span>
          </div>
        </div>

        <label class="field">
          <span>展示名</span>
          <input v-model="displayName" type="text" maxlength="32" placeholder="对外显示的名称" />
        </label>

        <label class="field">
          <span>简介</span>
          <textarea v-model="bio" rows="3" maxlength="200" placeholder="一句话介绍自己" />
        </label>

        <label class="field">
          <span>头像 URL</span>
          <input
            v-model="avatarUrl"
            type="text"
            maxlength="512"
            placeholder="https://… 或 /uploads/…"
          />
        </label>

        <label class="upload">
          <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" hidden @change="onPickAvatar" />
          <span>{{ uploading ? '上传中…' : '上传本站图片' }}</span>
        </label>

        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="success" class="success">{{ success }}</p>

        <button class="submit" type="submit" :disabled="saving || uploading">
          {{ saving ? '保存中…' : '保存资料' }}
        </button>
      </template>
    </form>

    <form v-if="!loading" class="card card--pwd" @submit.prevent="onChangePassword">
      <h2>修改密码</h2>
      <p class="hint">修改后可继续使用当前登录状态；下次请用新密码登录</p>

      <label class="field">
        <span>当前密码</span>
        <input
          v-model="currentPassword"
          type="password"
          autocomplete="current-password"
          placeholder="输入当前密码"
        />
      </label>

      <label class="field">
        <span>新密码</span>
        <input
          v-model="newPassword"
          type="password"
          autocomplete="new-password"
          minlength="6"
          maxlength="64"
          placeholder="6～64 位"
        />
      </label>

      <label class="field">
        <span>确认新密码</span>
        <input
          v-model="confirmPassword"
          type="password"
          autocomplete="new-password"
          minlength="6"
          maxlength="64"
          placeholder="再次输入新密码"
        />
      </label>

      <p v-if="pwdError" class="error">{{ pwdError }}</p>
      <p v-if="pwdSuccess" class="success">{{ pwdSuccess }}</p>

      <button class="submit" type="submit" :disabled="pwdSaving">
        {{ pwdSaving ? '提交中…' : '修改密码' }}
      </button>
    </form>
  </section>
</template>

<style scoped>
.card {
  max-width: 520px;
  margin: 0 auto;
  padding: 32px 28px;
  border-radius: var(--radius-lg);
  background: var(--bg-elevated);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-soft);
}

.card--pwd {
  margin-top: 20px;
}

h1,
h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.5rem;
}

h2 {
  font-size: 1.25rem;
}

.hint {
  margin: 8px 0 22px;
  color: var(--text-muted);
  font-size: 0.92rem;
}

.state {
  color: var(--text-muted);
}

.preview {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}

.preview-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--highlight);
}

.preview-avatar--fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--primary);
  font-size: 1.4rem;
  font-weight: 700;
}

.preview-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: var(--text-muted);
  font-size: 0.88rem;
}

.preview-meta strong {
  color: var(--text);
  font-size: 1.05rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
  font-size: 0.88rem;
  color: var(--text-muted);
}

.field input,
.field textarea {
  padding: 10px 12px;
  border: 1px solid var(--border-soft);
  border-radius: 12px;
  outline: none;
  color: var(--text);
  font: inherit;
}

.field input:focus,
.field textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

.upload {
  display: inline-flex;
  margin-bottom: 16px;
  padding: 8px 14px;
  border-radius: var(--radius-pill);
  background: var(--primary-soft);
  color: var(--text);
  font-size: 0.9rem;
  cursor: pointer;
  font-weight: 600;
}

.error {
  margin: 0 0 12px;
  color: #d63031;
  font-size: 0.9rem;
}

.success {
  margin: 0 0 12px;
  color: #2d6a4f;
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
</style>
