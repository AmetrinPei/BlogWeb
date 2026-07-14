/**
 * blog-search-enhance acceptance: keyword matches title / summary / content.
 * Usage: node scripts/acceptance-search-enhance.mjs
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
      body: { name: '搜索验收分类' },
    })
    assert(created.json.code === 0, 'create category')
    cat = created.json.data
  }

  const suffix = Date.now()
  const alpha = `AlphaOnlyTitle_${suffix}`
  const beta = `BetaOnlySummary_${suffix}`
  const gamma = `GammaOnlyBody_${suffix}`
  const nomatch = `NoMatchZZZ_${suffix}`

  const a = await req('POST', '/api/admin/articles', {
    token,
    body: {
      title: `${alpha} article`,
      content: 'plain body without marker',
      categoryId: cat.id,
      status: 'PUBLISHED',
      publishedAt: '2026-05-01T10:00:00',
    },
  })
  assert(a.json.code === 0, `create A: ${JSON.stringify(a.json)}`)

  const b = await req('POST', '/api/admin/articles', {
    token,
    body: {
      title: `summary-only ${suffix}`,
      content: 'plain body',
      summary: `contains ${beta} here`,
      categoryId: cat.id,
      status: 'PUBLISHED',
      publishedAt: '2026-05-02T10:00:00',
    },
  })
  assert(b.json.code === 0, `create B: ${JSON.stringify(b.json)}`)

  const c = await req('POST', '/api/admin/articles', {
    token,
    body: {
      title: `body-only ${suffix}`,
      content: `markdown with ${gamma} token`,
      categoryId: cat.id,
      status: 'PUBLISHED',
      publishedAt: '2026-05-03T10:00:00',
    },
  })
  assert(c.json.code === 0, `create C: ${JSON.stringify(c.json)}`)

  const d = await req('POST', '/api/admin/articles', {
    token,
    body: {
      title: `draft ${suffix}`,
      content: `${gamma} in draft`,
      categoryId: cat.id,
      status: 'DRAFT',
    },
  })
  assert(d.json.code === 0, `create D: ${JSON.stringify(d.json)}`)

  try {
    const byTitle = await req('GET', `/api/articles?keyword=${encodeURIComponent(alpha)}`)
    assert(byTitle.json.code === 0, 'title search code')
    assert(byTitle.json.data.total === 1, `title total=${byTitle.json.data.total}`)
    assert(byTitle.json.data.items[0].title.includes(alpha), 'title hit')
    pass('SE-AC1-title', 'keyword hits title-only article')

    const bySummary = await req('GET', `/api/articles?keyword=${encodeURIComponent(beta)}`)
    assert(bySummary.json.code === 0 && bySummary.json.data.total === 1, 'summary hit')
    assert(bySummary.json.data.items[0].title.includes('summary-only'), 'summary article')
    pass('SE-AC1-summary', 'keyword hits summary-only article')

    const byBody = await req('GET', `/api/articles?keyword=${encodeURIComponent(gamma)}`)
    assert(byBody.json.code === 0 && byBody.json.data.total === 1, `body total=${byBody.json.data?.total}`)
    assert(byBody.json.data.items[0].title.includes('body-only'), 'body article only')
    pass('SE-AC2', 'draft with same body keyword hidden')

    const empty = await req('GET', `/api/articles?keyword=${encodeURIComponent(nomatch)}`)
    assert(empty.json.code === 0, 'empty code')
    assert(empty.json.data.total === 0 && (empty.json.data.items?.length ?? 0) === 0, 'empty list')
    pass('SE-AC4', 'no match returns empty page')

    const noKw = await req('GET', '/api/articles?page=1&size=10')
    assert(noKw.json.code === 0 && Array.isArray(noKw.json.data.items), 'list without keyword')
    pass('SE-AC3', 'blank keyword list still works')
  } catch (e) {
    fail('SE-run', e.message)
  }

  const failed = results.filter((r) => !r.ok)
  console.log(`\n${results.length - failed.length}/${results.length} passed`)
  if (failed.length) process.exit(1)
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
