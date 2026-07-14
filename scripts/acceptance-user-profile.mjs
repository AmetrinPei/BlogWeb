/**
 * blog-user-profile acceptance: /api/me, public author fields on article/comment.
 * Usage: node scripts/acceptance-user-profile.mjs
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
  const noAuth = await req('GET', '/api/me')
  assert(noAuth.json.code === 401, `unauth get me: ${JSON.stringify(noAuth.json)}`)
  pass('UP-1', 'GET /api/me without token → 401')

  const login = await req('POST', '/api/auth/login', {
    body: { username: 'admin', password: 'admin123' },
  })
  assert(login.json.code === 0 && login.json.data?.token, 'admin login failed')
  const token = login.json.data.token

  const updated = await req('PUT', '/api/me', {
    token,
    body: {
      displayName: '验收展示名',
      bio: '验收简介',
      avatarUrl: 'https://cdn.example/avatar.png',
    },
  })
  assert(updated.json.code === 0, `put me failed: ${JSON.stringify(updated.json)}`)
  assert(updated.json.data.displayName === '验收展示名', 'displayName mismatch')
  assert(!('passwordHash' in (updated.json.data || {})), 'passwordHash must not appear')
  pass('UP-2', 'PUT /api/me ok, no passwordHash')

  const me = await req('GET', '/api/me', { token })
  assert(me.json.code === 0, 'get me failed')
  assert(me.json.data.bio === '验收简介', 'bio mismatch')
  pass('UP-3', 'GET /api/me returns updated profile')

  const bad = await req('PUT', '/api/me', {
    token,
    body: { displayName: 'x'.repeat(33), bio: '', avatarUrl: '' },
  })
  assert(bad.json.code === 400, `overlong should 400: ${JSON.stringify(bad.json)}`)
  pass('UP-4', 'overlong displayName rejected')

  let cats = (await req('GET', '/api/admin/categories', { token })).json.data || []
  let cat = cats[0]
  if (!cat) {
    const created = await req('POST', '/api/admin/categories', {
      token,
      body: { name: '资料验收分类' },
    })
    assert(created.json.code === 0, 'create category')
    cat = created.json.data
  }

  const article = await req('POST', '/api/admin/articles', {
    token,
    body: {
      title: `资料验收文章 ${Date.now()}`,
      content: 'body',
      categoryId: cat.id,
      status: 'PUBLISHED',
      publishedAt: '2026-06-01T10:00:00',
    },
  })
  assert(article.json.code === 0, `create article: ${JSON.stringify(article.json)}`)
  const articleId = article.json.data.id

  const detail = await req('GET', `/api/articles/${articleId}`)
  assert(detail.json.code === 0, 'article detail')
  assert(detail.json.data.authorName === '验收展示名', `authorName=${detail.json.data.authorName}`)
  assert(detail.json.data.authorAvatarUrl === 'https://cdn.example/avatar.png', 'authorAvatarUrl')
  pass('UP-5', 'article authorName/avatar from profile')

  const comment = await req('POST', `/api/articles/${articleId}/comments`, {
    token,
    body: { content: '资料验收评论' },
  })
  assert(comment.json.code === 0, `comment: ${JSON.stringify(comment.json)}`)
  assert(comment.json.data.displayName === '验收展示名', 'comment displayName')
  assert(comment.json.data.avatarUrl === 'https://cdn.example/avatar.png', 'comment avatarUrl')
  pass('UP-6', 'comment carries displayName/avatarUrl')

  const failed = results.filter((r) => !r.ok)
  console.log(`\n${results.length - failed.length}/${results.length} passed`)
  if (failed.length) process.exit(1)
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
