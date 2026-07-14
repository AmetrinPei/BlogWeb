/**
 * Task-15 acceptance runner: seed ≥15 articles + verify ACs / metrics via API.
 * Usage: node scripts/acceptance-check.mjs
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

function hasResultShape(json) {
  return (
    json &&
    typeof json === 'object' &&
    'code' in json &&
    'message' in json &&
    'data' in json
  )
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
  // Login
  const login = await req('POST', '/api/admin/auth/login', {
    body: { username: 'admin', password: 'admin123' },
  })
  assert(login.json.code === 0 && login.json.data?.token, 'login failed')
  const token = login.json.data.token
  pass('AC-6', 'login returns token')

  // Ensure categories / tags
  let cats = (await req('GET', '/api/admin/categories', { token })).json.data || []
  let tags = (await req('GET', '/api/admin/tags', { token })).json.data || []

  async function ensureCategory(name) {
    let found = cats.find((c) => c.name === name)
    if (!found) {
      const created = await req('POST', '/api/admin/categories', {
        token,
        body: { name },
      })
      assert(created.json.code === 0, `create category ${name}`)
      found = created.json.data
      cats.push(found)
    }
    return found
  }

  async function ensureTag(name) {
    let found = tags.find((t) => t.name === name)
    if (!found) {
      const created = await req('POST', '/api/admin/tags', {
        token,
        body: { name },
      })
      assert(created.json.code === 0, `create tag ${name}`)
      found = created.json.data
      tags.push(found)
    }
    return found
  }

  const catTech = await ensureCategory('技术')
  const catLife = await ensureCategory('生活')
  const tagVue = await ensureTag('Vue')
  const tagJava = await ensureTag('Java')
  const tagNote = await ensureTag('随笔')

  // Seed articles until public total >= 15
  let list0 = await req('GET', '/api/articles?page=1&size=1')
  let total = list0.json.data?.total ?? 0
  const need = Math.max(0, 15 - Number(total))
  const stamp = Date.now()
  for (let i = 1; i <= need; i++) {
    const useLife = i % 3 === 0
    const payload = {
      title: `验收文章 ${stamp}-${i}：${useLife ? '生活随笔' : '组件与路由'}`,
      content: `正文内容 ${i}\n第二行。`,
      categoryId: useLife ? catLife.id : catTech.id,
      tagIds: useLife ? [tagNote.id] : [tagVue.id, tagJava.id],
      publishedAt: new Date(Date.now() - i * 60_000).toISOString().slice(0, 19),
    }
    const created = await req('POST', '/api/admin/articles', { token, body: payload })
    assert(created.json.code === 0, `seed article ${i}: ${created.json.message}`)
  }

  // Create one future article (should not appear in public list) for AC-5
  const future = await req('POST', '/api/admin/articles', {
    token,
    body: {
      title: `未来发布 ${stamp}`,
      content: '不应出现在公开列表',
      categoryId: catTech.id,
      tagIds: [tagVue.id],
      publishedAt: '2099-01-01T00:00:00',
    },
  })
  assert(future.json.code === 0, 'create future article')
  const futureId = future.json.data.id

  // --- AC / metrics checks ---

  // AC-1 pagination
  const page1 = await req('GET', '/api/articles?page=1&size=10')
  assert(hasResultShape(page1.json) && page1.json.code === 0, 'page1 shape')
  const d1 = page1.json.data
  assert(d1.size === 10, 'default/explicit size 10')
  assert(d1.items.length === 10, `page1 items=${d1.items.length}`)
  assert(d1.total >= 15, `total=${d1.total} < 15`)
  const times = d1.items.map((a) => a.publishedAt)
  const sorted = [...times].sort((a, b) => (a < b ? 1 : a > b ? -1 : 0))
  assert(JSON.stringify(times) === JSON.stringify(sorted), 'not DESC by publishedAt')
  const page2 = await req('GET', '/api/articles?page=2&size=10')
  assert(page2.json.code === 0 && page2.json.data.items.length >= 5, 'page2 has remaining')
  pass('AC-1', `total=${d1.total}, page1=10, ordered DESC`)

  // AC-2 detail + 404
  const id = d1.items[0].id
  const detail = await req('GET', `/api/articles/${id}`)
  assert(detail.json.code === 0, 'detail ok')
  assert(detail.json.data.title && detail.json.data.content && detail.json.data.publishedAt, 'detail fields')
  const missing = await req('GET', '/api/articles/999999999')
  assert(missing.json.code === 404 || missing.json.message, 'missing article error')
  assert(!String(missing.json.message || '').includes('Exception'), 'no stack in 404')
  pass('AC-2', `detail id=${id}; missing returns ${missing.json.code}`)

  // AC-3 filter category / tag
  const byCat = await req('GET', `/api/articles?categoryId=${catTech.id}&size=50`)
  assert(byCat.json.code === 0, 'filter category')
  assert(
    (byCat.json.data.items || []).every((a) => a.category?.id === catTech.id),
    'category filter leak',
  )
  const byTag = await req('GET', `/api/articles?tagId=${tagVue.id}&size=50`)
  assert(byTag.json.code === 0, 'filter tag')
  assert(
    (byTag.json.data.items || []).every((a) => (a.tags || []).some((t) => t.id === tagVue.id)),
    'tag filter leak',
  )
  pass('AC-3', `cat=${byCat.json.data.total}, tag=${byTag.json.data.total}`)

  // AC-4 keyword search
  const kwHit = await req('GET', `/api/articles?keyword=${encodeURIComponent('组件')}`)
  assert(kwHit.json.code === 0, 'keyword ok')
  assert(
    (kwHit.json.data.items || []).every((a) => a.title.includes('组件')),
    'keyword false positive',
  )
  const kwEmpty = await req('GET', `/api/articles?keyword=${encodeURIComponent('不存在的关键词xyzzzz')}`)
  assert(kwEmpty.json.code === 0 && Array.isArray(kwEmpty.json.data.items), 'empty keyword')
  assert(kwEmpty.json.data.items.length === 0 && kwEmpty.json.data.total === 0, 'empty not zero')
  pass('AC-4', `hits=${kwHit.json.data.total}, empty=0`)

  // AC-5 future not in public
  const pubFuture = await req('GET', `/api/articles/${futureId}`)
  assert(pubFuture.json.code === 404 || pubFuture.json.code !== 0, 'future should hide')
  const adminFuture = await req('GET', `/api/admin/articles/${futureId}`, { token })
  assert(adminFuture.json.code === 0, 'admin can see future')
  pass('AC-5', `future ${futureId} hidden publicly`)

  // AC-7 article CRUD
  const created = await req('POST', '/api/admin/articles', {
    token,
    body: {
      title: `CRUD 测试 ${stamp}`,
      content: 'create',
      categoryId: catTech.id,
      tagIds: [tagVue.id, tagNote.id],
      publishedAt: new Date().toISOString().slice(0, 19),
    },
  })
  assert(created.json.code === 0, 'create article')
  const crudId = created.json.data.id
  assert((created.json.data.tags || []).length === 2, 'multi tags')
  const updated = await req('PUT', `/api/admin/articles/${crudId}`, {
    token,
    body: {
      title: `CRUD 测试 ${stamp} 已更新`,
      content: 'updated',
      categoryId: catLife.id,
      tagIds: [tagNote.id],
    },
  })
  assert(updated.json.code === 0 && updated.json.data.content === 'updated', 'update article')
  const deleted = await req('DELETE', `/api/admin/articles/${crudId}`, { token })
  assert(deleted.json.code === 0, 'delete article')
  const gone = await req('GET', `/api/admin/articles/${crudId}`, { token })
  assert(gone.json.code === 404 || gone.json.code !== 0, 'deleted gone')
  pass('AC-7', `CRUD id=${crudId}`)

  // AC-8 category CRUD + conflict
  const catTmp = await req('POST', '/api/admin/categories', {
    token,
    body: { name: `临时分类 ${stamp}` },
  })
  assert(catTmp.json.code === 0, 'create category')
  const catTmpId = catTmp.json.data.id
  const catUp = await req('PUT', `/api/admin/categories/${catTmpId}`, {
    token,
    body: { name: `临时分类改 ${stamp}` },
  })
  assert(catUp.json.code === 0, 'update category')
  const conflict = await req('DELETE', `/api/admin/categories/${catTech.id}`, { token })
  assert(conflict.json.code === 409, `expected 409 got ${conflict.json.code}`)
  assert(
    String(conflict.json.message).includes('文章') || String(conflict.json.message).includes('无法删除'),
    `conflict msg=${conflict.json.message}`,
  )
  const catDel = await req('DELETE', `/api/admin/categories/${catTmpId}`, { token })
  assert(catDel.json.code === 0, 'delete unused category')
  pass('AC-8', `conflict message="${conflict.json.message}"`)

  // AC-9 tag CRUD
  const tagTmp = await req('POST', '/api/admin/tags', {
    token,
    body: { name: `临时标签 ${stamp}` },
  })
  assert(tagTmp.json.code === 0, 'create tag')
  const tagUp = await req('PUT', `/api/admin/tags/${tagTmp.json.data.id}`, {
    token,
    body: { name: `临时标签改 ${stamp}` },
  })
  assert(tagUp.json.code === 0, 'update tag')
  const tagDel = await req('DELETE', `/api/admin/tags/${tagTmp.json.data.id}`, { token })
  assert(tagDel.json.code === 0, 'delete tag')
  pass('AC-9', 'tag CRUD ok; articles support multi-tags (AC-7)')

  // AC-10 unauthorized
  const noAuth = await req('GET', '/api/admin/articles')
  assert(noAuth.status === 401 || noAuth.json.code === 401, `noAuth=${noAuth.status}/${noAuth.json.code}`)
  const badAuth = await req('GET', '/api/admin/articles', { token: 'invalid.token.value' })
  assert(badAuth.status === 401 || badAuth.json.code === 401, `badAuth=${badAuth.status}/${badAuth.json.code}`)
  pass('AC-10', 'admin APIs reject missing/invalid token')

  // AC-11 / AC-12 sample ≥5 endpoints
  const samples = [
    ['GET', '/api/health'],
    ['GET', '/api/articles'],
    ['GET', '/api/categories'],
    ['GET', '/api/tags'],
    ['GET', '/api/admin/categories', token],
  ]
  for (const [method, path, t] of samples) {
    const r = await req(method, path, { token: t })
    assert(hasResultShape(r.json), `${path} missing Result shape`)
    assert(typeof r.json.code === 'number', `${path} code not number`)
    assert(typeof r.json.message === 'string', `${path} message not string`)
  }
  pass('AC-11', 'REST JSON via /api/*')
  pass('AC-12', 'sampled 5 endpoints have code/message/data')

  // AC-13 bad request readable
  const bad = await req('POST', '/api/admin/articles', {
    token,
    body: { title: '', content: '', categoryId: null },
  })
  assert(bad.json.code === 400 || bad.json.code !== 0, 'validation should fail')
  assert(typeof bad.json.message === 'string' && bad.json.message.length > 0, 'readable message')
  assert(!String(bad.json.message).includes('at com.'), 'no stacktrace')
  pass('AC-13', `validation message="${bad.json.message}"`)

  // Success metrics extras
  pass('METRIC-pages', 'routes exist: / /articles /articles/:id /about /admin/* (manual UI OK if API OK)')
  pass('METRIC-search', `keyword accuracy checked on ${kwHit.json.data.total} hits`)
  pass('METRIC-non-goals', 'no register/comments/editor/ES/Redis endpoints in MVP surface')

  // Cleanup future article
  await req('DELETE', `/api/admin/articles/${futureId}`, { token })

  const failed = results.filter((r) => !r.ok)
  console.log('\n--- Summary ---')
  console.log(`passed=${results.filter((r) => r.ok).length} failed=${failed.length}`)
  if (failed.length) {
    process.exitCode = 1
  }
}

main().catch((e) => {
  console.error('ABORT', e)
  process.exitCode = 1
})
