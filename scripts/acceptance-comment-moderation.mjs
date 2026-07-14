/**
 * blog-comment-moderation acceptance: status, sensitive words, approve/reject, rate limit.
 * Usage: node scripts/acceptance-comment-moderation.mjs
 * Requires backend at API_BASE (default http://localhost:8080)
 */
const BASE = process.env.API_BASE || 'http://localhost:8080'

async function req(method, path, { token, body } = {}) {
  const headers = { Accept: 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json; charset=utf-8'
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
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
  const login = await req('POST', '/api/auth/login', {
    body: { username: 'admin', password: 'admin123' },
  })
  assert(login.json.code === 0 && login.json.data?.token, 'admin login failed')
  const token = login.json.data.token

  let cats = (await req('GET', '/api/admin/categories', { token })).json.data || []
  let cat = cats[0]
  if (!cat) {
    const created = await req('POST', '/api/admin/categories', {
      token,
      body: { name: '审核验收分类' },
    })
    assert(created.json.code === 0, 'create category')
    cat = created.json.data
  }

  const suffix = Date.now()
  const article = await req('POST', '/api/admin/articles', {
    token,
    body: {
      title: `审核文章_${suffix}`,
      content: 'moderation body',
      categoryId: cat.id,
      status: 'PUBLISHED',
      publishedAt: '2026-06-01T10:00:00',
    },
  })
  assert(article.json.code === 0, `create article: ${JSON.stringify(article.json)}`)
  const articleId = article.json.data.id

  // clean comment for auto approve
  const clean = await req('POST', `/api/articles/${articleId}/comments`, {
    token,
    body: { content: `干净评论_${suffix}` },
  })
  assert(clean.json.code === 0 && clean.json.data.status === 'APPROVED', 'clean should APPROVED')
  const list1 = await req('GET', `/api/articles/${articleId}/comments`)
  assert(
    (list1.json.data || []).some((c) => c.id === clean.json.data.id),
    'clean visible in public list',
  )
  pass('CM-1', 'clean comment auto-approved and listed')

  const word = `敏感_${suffix}`
  const sw = await req('POST', '/api/admin/sensitive-words', {
    token,
    body: { word },
  })
  assert(sw.json.code === 0, `create sensitive word: ${JSON.stringify(sw.json)}`)
  const wordId = sw.json.data.id

  const pending = await req('POST', `/api/articles/${articleId}/comments`, {
    token,
    body: { content: `含有${word}的内容` },
  })
  assert(pending.json.code === 0 && pending.json.data.status === 'PENDING', 'hit → PENDING')
  const pendingId = pending.json.data.id

  const list2 = await req('GET', `/api/articles/${articleId}/comments`)
  assert(
    !(list2.json.data || []).some((c) => c.id === pendingId),
    'pending not in public list',
  )
  pass('CM-2', 'sensitive hit pending and hidden')

  const queue = await req('GET', '/api/admin/comments?status=PENDING', { token })
  assert(
    queue.json.code === 0 &&
      (queue.json.data.items || []).some((c) => c.id === pendingId),
    'pending in admin queue',
  )

  const approved = await req('PUT', `/api/admin/comments/${pendingId}/status`, {
    token,
    body: { status: 'APPROVED' },
  })
  assert(approved.json.code === 0 && approved.json.data.status === 'APPROVED', 'approve ok')
  const list3 = await req('GET', `/api/articles/${articleId}/comments`)
  assert(
    (list3.json.data || []).some((c) => c.id === pendingId),
    'approved visible',
  )
  pass('CM-3', 'admin approve makes visible')

  const rejected = await req('PUT', `/api/admin/comments/${pendingId}/status`, {
    token,
    body: { status: 'REJECTED' },
  })
  assert(rejected.json.code === 0 && rejected.json.data.status === 'REJECTED', 'reject ok')
  const list4 = await req('GET', `/api/articles/${articleId}/comments`)
  assert(
    !(list4.json.data || []).some((c) => c.id === pendingId),
    'rejected hidden again',
  )
  pass('CM-4', 'admin reject hides again')

  const regUser = `mod_user_${suffix}`
  const reg = await req('POST', '/api/auth/register', {
    body: { username: regUser, password: 'pass1234' },
  })
  assert(reg.json.code === 0, 'register user')
  const userLogin = await req('POST', '/api/auth/login', {
    body: { username: regUser, password: 'pass1234' },
  })
  const userToken = userLogin.json.data.token
  const forbid = await req('GET', '/api/admin/comments', { token: userToken })
  assert(forbid.json.code === 403, 'non-admin 403')
  pass('CM-5', 'non-admin moderation 403')

  // rate limit with dedicated user on a fresh article to avoid admin quota pollution
  const rateArticle = await req('POST', '/api/admin/articles', {
    token,
    body: {
      title: `限流文章_${suffix}`,
      content: 'rate',
      categoryId: cat.id,
      status: 'PUBLISHED',
      publishedAt: '2026-06-01T10:00:00',
    },
  })
  const rateArticleId = rateArticle.json.data.id
  let sixthCode = 0
  for (let i = 0; i < 6; i++) {
    const r = await req('POST', `/api/articles/${rateArticleId}/comments`, {
      token: userToken,
      body: { content: `rate_${i}_${suffix}` },
    })
    if (i < 5) {
      assert(r.json.code === 0, `rate comment ${i} should succeed`)
    } else {
      sixthCode = r.json.code
      assert(r.json.code === 400 && String(r.json.message).includes('频繁'), '6th rate limited')
    }
  }
  assert(sixthCode === 400, 'sixth must be 400')
  pass('CM-6', 'comment rate limit')

  await req('DELETE', `/api/admin/sensitive-words/${wordId}`, { token })

  const failed = results.filter((r) => !r.ok)
  console.log(`\n${results.length - failed.length}/${results.length} passed`)
  if (failed.length) process.exit(1)
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
