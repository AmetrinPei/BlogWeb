/**
 * blog-password-change acceptance: PUT /api/me/password
 * Usage: node scripts/acceptance-password-change.mjs
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
  const noAuth = await req('PUT', '/api/me/password', {
    body: {
      currentPassword: 'pass1234',
      newPassword: 'pass5678',
      confirmPassword: 'pass5678',
    },
  })
  assert(noAuth.json.code === 401, `unauth change password: ${JSON.stringify(noAuth.json)}`)
  pass('PC-1', 'PUT /api/me/password without token → 401')

  const username = `pwd_acc_${Date.now()}`
  const reg = await req('POST', '/api/auth/register', {
    body: { username, password: 'pass1234' },
  })
  assert(reg.json.code === 0 && reg.json.data?.token, `register failed: ${JSON.stringify(reg.json)}`)
  const token = reg.json.data.token

  const mismatch = await req('PUT', '/api/me/password', {
    token,
    body: {
      currentPassword: 'pass1234',
      newPassword: 'pass5678',
      confirmPassword: 'pass9999',
    },
  })
  assert(mismatch.json.code === 400, `confirm mismatch: ${JSON.stringify(mismatch.json)}`)
  pass('PC-2', 'confirm mismatch → 400')

  const wrong = await req('PUT', '/api/me/password', {
    token,
    body: {
      currentPassword: 'wrong-old',
      newPassword: 'pass5678',
      confirmPassword: 'pass5678',
    },
  })
  assert(wrong.json.code === 400, `wrong current: ${JSON.stringify(wrong.json)}`)
  assert(wrong.json.message === '当前密码不正确', 'wrong current message')
  pass('PC-3', 'wrong current password → 400')

  const same = await req('PUT', '/api/me/password', {
    token,
    body: {
      currentPassword: 'pass1234',
      newPassword: 'pass1234',
      confirmPassword: 'pass1234',
    },
  })
  assert(same.json.code === 400, `same password: ${JSON.stringify(same.json)}`)
  pass('PC-4', 'same as current → 400')

  const changed = await req('PUT', '/api/me/password', {
    token,
    body: {
      currentPassword: 'pass1234',
      newPassword: 'pass5678',
      confirmPassword: 'pass5678',
    },
  })
  assert(changed.json.code === 0, `change failed: ${JSON.stringify(changed.json)}`)
  assert(changed.json.data?.username === username, 'username mismatch')
  assert(!('passwordHash' in (changed.json.data || {})), 'passwordHash must not appear')
  pass('PC-5', 'change password ok, no passwordHash')

  const oldLogin = await req('POST', '/api/auth/login', {
    body: { username, password: 'pass1234' },
  })
  assert(oldLogin.json.code === 401, `old password should fail: ${JSON.stringify(oldLogin.json)}`)
  pass('PC-6', 'old password login fails')

  const newLogin = await req('POST', '/api/auth/login', {
    body: { username, password: 'pass5678' },
  })
  assert(newLogin.json.code === 0 && newLogin.json.data?.token, `new password login failed`)
  pass('PC-7', 'new password login ok')

  console.log('\nAll password-change acceptance checks passed.')
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
