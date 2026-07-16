/**
 * blog-rate-limit Slice-A acceptance: login failure limiter
 * Usage: node scripts/acceptance-login-rate-limit.mjs
 * Requires backend at API_BASE (default http://localhost:8080)
 *
 * Note: uses a unique username; expects default max-failures=5 unless BLOG_LOGIN_MAX_FAILURES is set.
 * Set EXPECT_MAX_FAILURES to match server config (default 5).
 */
const BASE = process.env.API_BASE || 'http://localhost:8080'
const MAX = Number(process.env.EXPECT_MAX_FAILURES || 5)

async function req(method, path, { body, headers } = {}) {
  const h = { Accept: 'application/json', ...(headers || {}) }
  let payload
  if (body !== undefined) {
    h['Content-Type'] = 'application/json; charset=utf-8'
    payload = JSON.stringify(body)
  }
  const res = await fetch(`${BASE}${path}`, { method, headers: h, body: payload })
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
  const username = `rl_acc_${Date.now()}`
  const ip = `203.0.113.${(Date.now() % 200) + 1}`

  const reg = await req('POST', '/api/auth/register', {
    body: { username, password: 'pass1234' },
  })
  assert(reg.json.code === 0, `register: ${JSON.stringify(reg.json)}`)
  pass('RL-1', `registered ${username}`)

  for (let i = 0; i < MAX; i++) {
    const fail = await req('POST', '/api/auth/login', {
      headers: { 'X-Forwarded-For': ip },
      body: { username, password: 'wrong-password' },
    })
    assert(fail.json.code === 401, `fail #${i + 1}: ${JSON.stringify(fail.json)}`)
  }
  pass('RL-2', `${MAX} failures → 401`)

  const blocked = await req('POST', '/api/auth/login', {
    headers: { 'X-Forwarded-For': ip },
    body: { username, password: 'wrong-password' },
  })
  assert(blocked.json.code === 429, `expected 429: ${JSON.stringify(blocked.json)}`)
  assert(blocked.json.message === '登录尝试过于频繁，请稍后再试', blocked.json.message)
  pass('RL-3', 'next attempt → 429')

  const stillBlocked = await req('POST', '/api/auth/login', {
    headers: { 'X-Forwarded-For': ip },
    body: { username, password: 'pass1234' },
  })
  assert(stillBlocked.json.code === 429, `correct password still blocked: ${JSON.stringify(stillBlocked.json)}`)
  pass('RL-4', 'correct password also blocked until window clears')

  console.log('All acceptance checks passed.')
  console.log('Tip: restart backend or wait window-seconds to clear; success after clear is covered by unit tests.')
}

main().catch((err) => {
  console.error('FAIL', err.message || err)
  process.exit(1)
})
