/**
 * blog-auth-refresh acceptance: login dual tokens, refresh rotate, logout, password revoke
 * Usage: node scripts/acceptance-auth-refresh.mjs
 * Requires backend at API_BASE (default http://localhost:8080)
 */
const BASE = process.env.API_BASE || 'http://localhost:8080'

async function req(method, path, { token, body } = {}) {
  const headers = { Accept: 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  let payload
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json; charset=utf-8'
    payload = JSON.stringify(body)
  }
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: payload,
  })
  const text = await res.text()
  let json
  try {
    json = JSON.parse(text)
  } catch {
    json = { raw: text }
  }
  return { status: res.status, json }
}

function assert(cond, msg) {
  if (!cond) throw new Error(msg)
}

function pass(id, detail) {
  console.log(`PASS  ${id}  ${detail}`)
}

async function main() {
  const username = `auth_ref_${Date.now()}`
  const reg = await req('POST', '/api/auth/register', {
    body: { username, password: 'pass1234' },
  })
  assert(reg.json.code === 0, `register failed: ${JSON.stringify(reg.json)}`)
  assert(reg.json.data?.token, 'missing access token')
  assert(reg.json.data?.refreshToken, 'missing refresh token')
  assert(!('passwordHash' in (reg.json.data || {})), 'passwordHash must not appear')
  pass('AR-1', 'register returns token + refreshToken')

  const refresh1 = reg.json.data.refreshToken

  const refreshed = await req('POST', '/api/auth/refresh', {
    body: { refreshToken: refresh1 },
  })
  assert(refreshed.json.code === 0, `refresh failed: ${JSON.stringify(refreshed.json)}`)
  assert(refreshed.json.data?.token, 'refresh missing token')
  assert(refreshed.json.data?.refreshToken, 'refresh missing refreshToken')
  assert(refreshed.json.data.refreshToken !== refresh1, 'refresh must rotate')
  pass('AR-2', 'refresh rotates refresh token')

  const oldAgain = await req('POST', '/api/auth/refresh', {
    body: { refreshToken: refresh1 },
  })
  assert(oldAgain.json.code === 401, `old refresh should fail: ${JSON.stringify(oldAgain.json)}`)
  pass('AR-3', 'old refresh after rotate → 401')

  const refresh2 = refreshed.json.data.refreshToken
  const logout = await req('POST', '/api/auth/logout', {
    body: { refreshToken: refresh2 },
  })
  assert(logout.json.code === 0, `logout failed: ${JSON.stringify(logout.json)}`)
  pass('AR-4', 'logout ok')

  const afterLogout = await req('POST', '/api/auth/refresh', {
    body: { refreshToken: refresh2 },
  })
  assert(afterLogout.json.code === 401, `refresh after logout: ${JSON.stringify(afterLogout.json)}`)
  pass('AR-5', 'refresh after logout → 401')

  const login = await req('POST', '/api/auth/login', {
    body: { username, password: 'pass1234' },
  })
  assert(login.json.code === 0 && login.json.data?.refreshToken, 're-login failed')
  const token = login.json.data.token
  const refresh3 = login.json.data.refreshToken

  const changed = await req('PUT', '/api/me/password', {
    token,
    body: {
      currentPassword: 'pass1234',
      newPassword: 'pass5678',
      confirmPassword: 'pass5678',
    },
  })
  assert(changed.json.code === 0, `change password: ${JSON.stringify(changed.json)}`)
  pass('AR-6', 'change password ok')

  const afterPwd = await req('POST', '/api/auth/refresh', {
    body: { refreshToken: refresh3 },
  })
  assert(afterPwd.json.code === 401, `refresh after password change: ${JSON.stringify(afterPwd.json)}`)
  pass('AR-7', 'refresh after password change → 401')

  const newLogin = await req('POST', '/api/auth/login', {
    body: { username, password: 'pass5678' },
  })
  assert(newLogin.json.code === 0 && newLogin.json.data?.refreshToken, 'login with new password failed')
  pass('AR-8', 'new password login returns new refresh')

  console.log('\nAll auth-refresh acceptance checks passed.')
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
