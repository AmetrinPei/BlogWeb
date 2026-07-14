/**
 * blog-comment-ux: floorNo, pin replace, list order.
 * Usage: node scripts/acceptance-comment-ux.mjs
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
  const res = await fetch(`${BASE}${path}`, { method, headers, body: payload })
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

async function main() {
  const login = await req('POST', '/api/auth/login', {
    body: { username: 'admin', password: 'admin123' },
  })
  assert(login.json.code === 0, 'login failed')
  const token = login.json.data.token

  let cats = (await req('GET', '/api/admin/categories', { token })).json.data || []
  let cat = cats[0]
  if (!cat) {
    const created = await req('POST', '/api/admin/categories', {
      token,
      body: { name: '评论UX验收' },
    })
    cat = created.json.data
  }

  const article = await req('POST', '/api/admin/articles', {
    token,
    body: {
      title: `UX验收 ${Date.now()}`,
      content: 'body',
      categoryId: cat.id,
      status: 'PUBLISHED',
      publishedAt: '2026-06-01T10:00:00',
    },
  })
  assert(article.json.code === 0, 'create article')
  const articleId = article.json.data.id

  const ids = []
  for (const content of ['一楼', '二楼', '三楼']) {
    const c = await req('POST', `/api/articles/${articleId}/comments`, {
      token,
      body: { content },
    })
    assert(c.json.code === 0, `comment ${content}`)
    ids.push(c.json.data.id)
  }

  let list = await req('GET', `/api/articles/${articleId}/comments`)
  assert(list.json.data[0].floorNo === 1 && list.json.data[1].floorNo === 2, 'floors')
  console.log('PASS  CU-1  floor numbers')

  const pin = await req('PUT', `/api/comments/${ids[1]}/pin`, {
    token,
    body: { pinned: true },
  })
  assert(pin.json.code === 0 && pin.json.data.floorNo === 2, 'pin')
  list = await req('GET', `/api/articles/${articleId}/comments`)
  assert(list.json.data[0].id === ids[1] && list.json.data[0].pinned, 'pinned first')
  assert(list.json.data[0].floorNo === 2, 'floor stable after pin')
  console.log('PASS  CU-2  pin to front, floor stable')

  await req('PUT', `/api/comments/${ids[2]}/pin`, { token, body: { pinned: true } })
  list = await req('GET', `/api/articles/${articleId}/comments`)
  assert(list.json.data[0].id === ids[2], 'replace pin')
  assert(list.json.data.find((x) => x.id === ids[1])?.pinned === false, 'old unpinned')
  console.log('PASS  CU-3  pin replace')

  const reply = await req('POST', `/api/articles/${articleId}/comments`, {
    token,
    body: { content: '回复', parentId: ids[0] },
  })
  const bad = await req('PUT', `/api/comments/${reply.json.data.id}/pin`, {
    token,
    body: { pinned: true },
  })
  assert(bad.json.code === 400, 'reply pin rejected')
  console.log('PASS  CU-4  reply cannot pin')

  console.log('\n4/4 passed')
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
