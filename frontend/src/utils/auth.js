import { computed, ref } from 'vue'

const TOKEN_KEY = 'blog_admin_token'
const USER_KEY = 'blog_user'

/** Bumps when session changes so UI can react. */
const sessionTick = ref(0)

function touchSession() {
  sessionTick.value += 1
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function isLoggedIn() {
  return Boolean(getToken())
}

export function setAuthSession({ token, userId, username, role, displayName, avatarUrl }) {
  setToken(token)
  localStorage.setItem(
    USER_KEY,
    JSON.stringify({
      userId: userId ?? null,
      username: username ?? '',
      role: role ?? '',
      displayName: displayName ?? null,
      avatarUrl: avatarUrl ?? null,
    }),
  )
  touchSession()
}

export function patchAuthUser(partial) {
  const current = getUser() || {}
  localStorage.setItem(
    USER_KEY,
    JSON.stringify({
      ...current,
      ...partial,
    }),
  )
  touchSession()
}

export function getUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function getRole() {
  return getUser()?.role ?? null
}

export function isAdmin() {
  return getRole() === 'ADMIN'
}

export function canAccessAdmin() {
  const role = getRole()
  return role === 'ADMIN' || role === 'AUTHOR'
}

export function clearAuth() {
  clearToken()
  localStorage.removeItem(USER_KEY)
  touchSession()
}

export function useAuthSession() {
  const user = computed(() => {
    void sessionTick.value
    return getUser()
  })
  const loggedIn = computed(() => {
    void sessionTick.value
    return isLoggedIn()
  })
  return { user, loggedIn, sessionTick }
}

/** Public-area redirect: path starting with / but not /admin, no protocol. */
export function isSafePublicRedirect(raw) {
  if (typeof raw !== 'string' || !raw.startsWith('/')) return false
  if (raw.startsWith('//') || raw.includes('://')) return false
  if (raw.startsWith('/admin')) return false
  return true
}

/** Admin-area redirect. */
export function isSafeAdminRedirect(raw) {
  if (typeof raw !== 'string') return false
  if (!raw.startsWith('/admin') || raw.startsWith('/admin/login')) return false
  if (raw.includes('://')) return false
  return true
}

export function displayLabel(user) {
  if (!user) return ''
  if (user.displayName && String(user.displayName).trim()) {
    return String(user.displayName).trim()
  }
  return user.username || ''
}
