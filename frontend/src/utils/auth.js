const TOKEN_KEY = 'blog_admin_token'
const USER_KEY = 'blog_user'

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

export function setAuthSession({ token, userId, username, role }) {
  setToken(token)
  localStorage.setItem(
    USER_KEY,
    JSON.stringify({
      userId: userId ?? null,
      username: username ?? '',
      role: role ?? '',
    }),
  )
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

export function clearAuth() {
  clearToken()
  localStorage.removeItem(USER_KEY)
}
