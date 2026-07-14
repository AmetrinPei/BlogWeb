/**
 * blog-rss-feed acceptance: public RSS 2.0 feed.
 * Usage: node scripts/acceptance-rss-feed.mjs
 * Requires backend at API_BASE (default http://localhost:8080)
 */
const BASE = process.env.API_BASE || 'http://localhost:8080'

async function req(method, path, { token, body, accept } = {}) {
  const headers = { Accept: accept || 'application/json' }
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
    json = null
  }
  return { status: res.status, headers: res.headers, text, json }
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

function countTag(xml, tag) {
  const re = new RegExp(`<${tag}[\\s>]`, 'g')
  return (xml.match(re) || []).length
}

async function main() {
  const login = await req('POST', '/api/auth/login', {
    body: { username: 'admin', password: 'admin123' },
  })
  assert(login.json?.code === 0 && login.json.data?.token, 'admin login failed')
  const token = login.json.data.token

  let cats = (await req('GET', '/api/admin/categories', { token })).json.data || []
  let cat = cats[0]
  if (!cat) {
    const created = await req('POST', '/api/admin/categories', {
      token,
      body: { name: 'RSS验收分类' },
    })
    assert(created.json.code === 0, 'create category')
    cat = created.json.data
  }

  const suffix = Date.now()
  const siteName = `RSS站点_${suffix}`

  const siteUp = await req('PUT', '/api/admin/site', {
    token,
    body: {
      siteName,
      tagline: `简介_${suffix}`,
      aboutText: '',
      socialLinks: [],
    },
  })
  assert(siteUp.json?.code === 0, `update site: ${JSON.stringify(siteUp.json)}`)

  const published = await req('POST', '/api/admin/articles', {
    token,
    body: {
      title: `RSS已发布 & 特殊_${suffix}`,
      content: 'rss published body',
      summary: `摘要 & 内容_${suffix}`,
      categoryId: cat.id,
      status: 'PUBLISHED',
      publishedAt: '2026-07-01T10:00:00',
    },
  })
  assert(published.json?.code === 0, `create published: ${JSON.stringify(published.json)}`)
  const pubId = published.json.data.id

  const draft = await req('POST', '/api/admin/articles', {
    token,
    body: {
      title: `RSS草稿_${suffix}`,
      content: 'draft should not appear',
      categoryId: cat.id,
      status: 'DRAFT',
    },
  })
  assert(draft.json?.code === 0, `create draft: ${JSON.stringify(draft.json)}`)

  try {
    const feed = await req('GET', '/feed.xml', { accept: 'application/rss+xml' })
    assert(feed.status === 200, `feed status=${feed.status}`)
    const ct = feed.headers.get('content-type') || ''
    assert(ct.includes('rss+xml') || ct.includes('xml'), `content-type=${ct}`)
    assert(feed.text.includes('<rss version="2.0"'), 'rss root')
    assert(!feed.text.trimStart().startsWith('{'), 'not JSON wrapper')
    pass('RSS-AC1', 'GET /feed.xml returns XML 200')

    assert(feed.text.includes(`<title>${siteName}</title>`), 'channel title from site')
    assert(feed.text.includes(`简介_${suffix}`), 'channel description from tagline')
    pass('RSS-AC6', 'channel meta from site settings')

    assert(feed.text.includes(`RSS已发布 &amp; 特殊_${suffix}`), 'escaped title')
    assert(feed.text.includes(`摘要 &amp; 内容_${suffix}`), 'escaped summary')
    assert(!feed.text.includes(`RSS已发布 & 特殊_${suffix}`), 'raw ampersand not in title')
    pass('RSS-AC8', 'XML special chars escaped')

    assert(feed.text.includes(`/articles/${pubId}`), 'item link has article id')
    assert(!feed.text.includes(`RSS草稿_${suffix}`), 'draft hidden')
    pass('RSS-AC3', 'only published articles; draft excluded')

    assert(feed.text.includes('<item>'), 'has items')
    assert(countTag(feed.text, 'item') >= 1, 'at least one item')
    pass('RSS-AC5', 'items include title/link/description')

    const emptyCheck = feed.text.includes('</channel>')
    assert(emptyCheck, 'channel closed')
    pass('RSS-AC2', 'valid RSS 2.0 structure')
  } catch (e) {
    fail('RSS-run', e.message)
  }

  const failed = results.filter((r) => !r.ok)
  console.log(`\n${results.length - failed.length}/${results.length} passed`)
  if (failed.length) process.exit(1)
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
