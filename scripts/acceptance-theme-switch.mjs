/**
 * blog-theme-switch acceptance: site theme / background fields
 * Usage: node scripts/acceptance-theme-switch.mjs
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
  const pub = await req('GET', '/api/site')
  assert(pub.json.code === 0, `GET /api/site failed: ${JSON.stringify(pub.json)}`)
  const d = pub.json.data
  assert(typeof d.defaultTheme === 'string', 'missing defaultTheme')
  assert(typeof d.backgroundMode === 'string', 'missing backgroundMode')
  assert('backgroundColor' in d, 'missing backgroundColor key')
  assert('backgroundGradient' in d, 'missing backgroundGradient key')
  assert('backgroundImageUrl' in d, 'missing backgroundImageUrl key')
  assert('aboutAvatarUrl' in d, 'missing aboutAvatarUrl key')
  assert('aboutAvatarUrl' in d && 'homeHeroUrl' in d, 'missing avatar/hero urls')
  pass('TS-1', `public site theme fields present (defaultTheme=${d.defaultTheme}, mode=${d.backgroundMode})`)

  const login = await req('POST', '/api/admin/auth/login', {
    body: { username: 'admin', password: 'admin123' },
  })
  assert(login.json.code === 0 && login.json.data?.token, 'admin login failed')
  const token = login.json.data.token
  pass('TS-2', 'admin login ok')

  const snapshot = {
    siteName: d.siteName,
    tagline: d.tagline || '',
    aboutText: d.aboutText || '',
    socialLinks: d.socialLinks || [],
    defaultTheme: d.defaultTheme || 'light',
    backgroundMode: d.backgroundMode || 'theme',
    backgroundColor: d.backgroundColor || '',
    backgroundGradient: d.backgroundGradient || '',
    backgroundImageUrl: d.backgroundImageUrl || '',
    aboutAvatarUrl: d.aboutAvatarUrl || '',
    homeHeroUrl: d.homeHeroUrl || '',
  }

  const putColor = await req('PUT', '/api/admin/site', {
    token,
    body: {
      ...snapshot,
      backgroundMode: 'color',
      backgroundColor: '#E8F6EE',
    },
  })
  assert(putColor.json.code === 0, `PUT color failed: ${JSON.stringify(putColor.json)}`)
  assert(putColor.json.data.backgroundMode === 'color', 'mode not color')
  assert(putColor.json.data.backgroundColor === '#E8F6EE', 'color not saved')
  pass('TS-3', 'ADMIN set backgroundMode=color')

  const pub2 = await req('GET', '/api/site')
  assert(pub2.json.data.backgroundMode === 'color', 'public not reflecting color mode')
  assert(pub2.json.data.backgroundColor === '#E8F6EE', 'public color mismatch')
  pass('TS-4', 'public GET reflects color background')

  const bad = await req('PUT', '/api/admin/site', {
    token,
    body: {
      ...snapshot,
      backgroundMode: 'color',
      backgroundColor: 'not-a-hex',
    },
  })
  assert(bad.json.code === 400, `illegal color should 400: ${JSON.stringify(bad.json)}`)
  pass('TS-5', 'illegal backgroundColor → 400')

  const restore = await req('PUT', '/api/admin/site', {
    token,
    body: snapshot,
  })
  assert(restore.json.code === 0, `restore failed: ${JSON.stringify(restore.json)}`)
  pass('TS-6', 'restored previous site settings')

  const noAuth = await req('PUT', '/api/admin/site', {
    body: { siteName: 'x', tagline: '', aboutText: '', socialLinks: [] },
  })
  assert(noAuth.json.code === 401, 'unauth PUT should 401')
  pass('TS-7', 'PUT without token → 401')

  console.log('\nAll theme-switch acceptance checks passed.')
}

main().catch((err) => {
  console.error('FAIL ', err.message || err)
  process.exit(1)
})
