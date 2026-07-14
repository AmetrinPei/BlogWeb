/**
 * blog-media-upload acceptance: upload image, public GET, optional coverUrl.
 * Usage: node scripts/acceptance-media-upload.mjs
 * Requires backend at API_BASE (default http://localhost:8080)
 */
const BASE = process.env.API_BASE || 'http://localhost:8080'

/** 1x1 PNG */
const PNG_BYTES = Uint8Array.from([
  0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52, 0x00,
  0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x02, 0x00, 0x00, 0x00, 0x90, 0x77, 0x53, 0xde, 0x00,
  0x00, 0x00, 0x0c, 0x49, 0x44, 0x41, 0x54, 0x08, 0xd7, 0x63, 0xf8, 0xcf, 0xc0, 0x00, 0x00, 0x00, 0x03,
  0x00, 0x01, 0x00, 0x05, 0xfe, 0x02, 0xfe, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4e, 0x44, 0xae, 0x42,
  0x60, 0x82,
])

async function req(method, path, { token, body, formData } = {}) {
  const headers = { Accept: 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  let payload
  if (formData) {
    payload = formData
  } else if (body !== undefined) {
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
  return { status: res.status, json, text, headers: res.headers }
}

function assert(cond, msg) {
  if (!cond) throw new Error(msg)
}

const results = []
function pass(id, detail) {
  results.push({ id, ok: true, detail })
  console.log(`PASS  ${id}  ${detail}`)
}
function fail(id, detail) {
  results.push({ id, ok: false, detail })
  console.error(`FAIL  ${id}  ${detail}`)
}

async function main() {
  const login = await req('POST', '/api/auth/login', {
    body: { username: 'admin', password: 'admin123' },
  })
  assert(login.json.code === 0 && login.json.data?.token, 'admin login failed')
  const token = login.json.data.token

  const noAuth = await req('POST', '/api/admin/media')
  assert(noAuth.json.code === 401, 'unauthenticated upload should 401')
  pass('MU-1', 'unauthenticated upload rejected')

  const form = new FormData()
  form.append('file', new Blob([PNG_BYTES], { type: 'image/png' }), 'cover.png')
  const uploaded = await req('POST', '/api/admin/media', { token, formData: form })
  assert(uploaded.json.code === 0, `upload failed: ${JSON.stringify(uploaded.json)}`)
  const url = uploaded.json.data.url
  assert(typeof url === 'string' && url.startsWith('/uploads/'), `bad url: ${url}`)
  assert(!url.includes('..'), 'url must not contain ..')
  pass('MU-2', `uploaded url=${url}`)

  const getImg = await fetch(`${BASE}${url}`)
  assert(getImg.status === 200, `GET ${url} status ${getImg.status}`)
  const ct = getImg.headers.get('content-type') || ''
  assert(ct.includes('image'), `content-type=${ct}`)
  const buf = new Uint8Array(await getImg.arrayBuffer())
  assert(buf.length > 0, 'empty image body')
  pass('MU-3', `public GET ok content-type=${ct}`)

  const bad = new FormData()
  bad.append('file', new Blob([new Uint8Array([0x00, 0x01])], { type: 'application/octet-stream' }), 'x.exe')
  const rejected = await req('POST', '/api/admin/media', { token, formData: bad })
  assert(rejected.json.code === 400, `illegal type should 400: ${JSON.stringify(rejected.json)}`)
  pass('MU-4', 'illegal type rejected')

  let cats = (await req('GET', '/api/admin/categories', { token })).json.data || []
  let cat = cats[0]
  if (!cat) {
    const created = await req('POST', '/api/admin/categories', {
      token,
      body: { name: '媒体验收分类' },
    })
    assert(created.json.code === 0, 'create category')
    cat = created.json.data
  }

  const suffix = Date.now()
  const article = await req('POST', '/api/admin/articles', {
    token,
    body: {
      title: `媒体封面_${suffix}`,
      content: `![img](${url})`,
      coverUrl: url,
      categoryId: cat.id,
      status: 'PUBLISHED',
      publishedAt: '2026-06-01T10:00:00',
    },
  })
  assert(article.json.code === 0, `create article: ${JSON.stringify(article.json)}`)
  const detail = await req('GET', `/api/articles/${article.json.data.id}`)
  assert(detail.json.code === 0 && detail.json.data.coverUrl === url, 'public coverUrl mismatch')
  pass('MU-5', 'coverUrl persisted and public')

  const failed = results.filter((r) => !r.ok)
  if (failed.length) {
    console.error(`\n${failed.length} failed`)
    process.exit(1)
  }
  console.log(`\nAll ${results.length} checks passed`)
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
