/**
 * blog-prod-hardening acceptance: site flag + registration (dev profile usually open)
 * Usage: node scripts/acceptance-prod-hardening.mjs
 * Requires backend at API_BASE (default http://localhost:8080)
 */
const BASE = process.env.API_BASE || 'http://localhost:8080'

async function req(method, path, { body } = {}) {
  const headers = { Accept: 'application/json' }
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
  const site = await req('GET', '/api/site')
  assert(site.json.code === 0, `GET /api/site failed: ${JSON.stringify(site.json)}`)
  assert(
    typeof site.json.data?.publicRegistrationEnabled === 'boolean',
    `publicRegistrationEnabled missing: ${JSON.stringify(site.json.data)}`,
  )
  pass('PH-1', `GET /api/site publicRegistrationEnabled=${site.json.data.publicRegistrationEnabled}`)

  const open = site.json.data.publicRegistrationEnabled === true
  const username = `ph_acc_${Date.now()}`
  const reg = await req('POST', '/api/auth/register', {
    body: { username, password: 'pass1234' },
  })

  if (open) {
    assert(reg.json.code === 0 && reg.json.data?.token, `register should work when open: ${JSON.stringify(reg.json)}`)
    pass('PH-2', 'registration open → register succeeds')
  } else {
    assert(reg.json.code === 403, `register should be 403 when closed: ${JSON.stringify(reg.json)}`)
    assert(reg.json.message === '公开注册已关闭', `message: ${reg.json.message}`)
    pass('PH-2', 'registration closed → register rejected with 403')
  }

  console.log('All acceptance checks passed.')
}

main().catch((err) => {
  console.error('FAIL', err.message || err)
  process.exit(1)
})
