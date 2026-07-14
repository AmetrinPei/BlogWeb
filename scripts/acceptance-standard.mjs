/**
 * Standard edition acceptance: content enhance, auth RBAC, comment/like, site experience.
 * Usage: node scripts/acceptance-standard.mjs
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
  // Admin login
  const login = await req('POST', '/api/auth/login', {
    body: { username: 'admin', password: 'admin123' },
  })
  assert(login.json.code === 0 && login.json.data?.token, 'admin login failed')
  assert(login.json.data.role === 'ADMIN', 'admin role missing')
  const adminToken = login.json.data.token
  pass('AUTH-login-admin', 'admin login + role')

  // Ensure category
  let cats = (await req('GET', '/api/admin/categories', { token: adminToken })).json.data || []
  let cat = cats[0]
  if (!cat) {
    const created = await req('POST', '/api/admin/categories', {
      token: adminToken,
      body: { name: '标准验收分类' },
    })
    assert(created.json.code === 0, 'create category')
    cat = created.json.data
  }

  // Content enhance: draft / published / offline
  const draft = await req('POST', '/api/admin/articles', {
    token: adminToken,
    body: {
      title: `草稿-${Date.now()}`,
      content: '# Draft\nsecret',
      categoryId: cat.id,
      status: 'DRAFT',
    },
  })
  assert(draft.json.code === 0, 'create draft')
  const draftId = draft.json.data.id

  const published = await req('POST', '/api/admin/articles', {
    token: adminToken,
    body: {
      title: `已发布-${Date.now()}`,
      content: '# Hello\n\n- item\n\n<script>alert(1)</script>',
      categoryId: cat.id,
      status: 'PUBLISHED',
      summary: '摘要测试',
      coverUrl: 'https://example.com/cover.png',
      pinned: true,
      recommended: true,
      publishedAt: new Date().toISOString().slice(0, 19),
    },
  })
  assert(published.json.code === 0, 'create published')
  const pubId = published.json.data.id
  pass('CE-create', 'draft + published created')

  const offline = await req('POST', '/api/admin/articles', {
    token: adminToken,
    body: {
      title: `下架-${Date.now()}`,
      content: 'offline',
      categoryId: cat.id,
      status: 'OFFLINE',
    },
  })
  assert(offline.json.code === 0, 'create offline')
  const offlineId = offline.json.data.id

  const pubList = await req('GET', '/api/articles?page=1&size=50')
  assert(pubList.json.code === 0, 'public list')
  const ids = (pubList.json.data.items || []).map((a) => a.id)
  assert(!ids.includes(draftId), 'draft not in public list')
  assert(!ids.includes(offlineId), 'offline not in public list')
  assert(ids.includes(pubId), 'published in public list')
  pass('CE-AC-2', 'public list status isolation')

  const draftDetail = await req('GET', `/api/articles/${draftId}`)
  assert(draftDetail.json.code === 404, 'draft detail 404')
  pass('CE-draft-404', 'draft detail hidden')

  const d1 = await req('GET', `/api/articles/${pubId}`)
  assert(d1.json.code === 0, 'published detail')
  const v1 = d1.json.data.viewCount
  const d2 = await req('GET', `/api/articles/${pubId}`)
  assert(d2.json.data.viewCount === v1 + 1, `viewCount ${v1} -> ${d2.json.data.viewCount}`)
  pass('CE-AC-5', 'viewCount increments')

  const pinnedFirst = pubList.json.data.items[0]
  assert(pinnedFirst.pinned === true || ids.indexOf(pubId) === 0 || pinnedFirst.id === pubId
    || pubList.json.data.items.some((a) => a.pinned), 'pinned present')
  pass('CE-AC-6', 'pinned field in list')

  // Register author
  const uname = `author_${Date.now()}`
  const reg = await req('POST', '/api/auth/register', {
    body: { username: uname, password: 'pass1234' },
  })
  assert(reg.json.code === 0 && reg.json.data.role === 'AUTHOR', 'register author')
  const authorToken = reg.json.data.token
  pass('AUTH-AC-1', 'register AUTHOR')

  const authorArticle = await req('POST', '/api/admin/articles', {
    token: authorToken,
    body: {
      title: `作者文-${Date.now()}`,
      content: 'mine',
      categoryId: cat.id,
      status: 'PUBLISHED',
      publishedAt: new Date().toISOString().slice(0, 19),
    },
  })
  assert(authorArticle.json.code === 0, 'author create')
  assert(authorArticle.json.data.authorName === uname, 'author bound')
  const authorArtId = authorArticle.json.data.id
  pass('AUTH-AC-3', 'article author bound')

  const forbid = await req('PUT', `/api/admin/articles/${pubId}`, {
    token: authorToken,
    body: {
      title: 'hack',
      content: 'no',
      categoryId: cat.id,
      status: 'PUBLISHED',
    },
  })
  assert(forbid.json.code === 403, `expect 403 got ${forbid.json.code}`)
  pass('AUTH-AC-4', 'author cannot edit others')

  const adminEdit = await req('PUT', `/api/admin/articles/${authorArtId}`, {
    token: adminToken,
    body: {
      title: authorArticle.json.data.title,
      content: 'admin updated',
      categoryId: cat.id,
      status: 'PUBLISHED',
    },
  })
  assert(adminEdit.json.code === 0, 'admin can edit any')
  pass('AUTH-AC-5', 'admin edits any article')

  const catForbid = await req('POST', '/api/admin/categories', {
    token: authorToken,
    body: { name: `nope-${Date.now()}` },
  })
  assert(catForbid.json.code === 403 || catForbid.status === 403, 'author cannot manage categories')
  pass('AUTH-admin-only', 'category admin-only')

  // Comments & likes
  const noAuthComment = await req('POST', `/api/articles/${pubId}/comments`, {
    body: { content: 'hi' },
  })
  assert(noAuthComment.json.code === 401 || noAuthComment.status === 401, 'comment needs auth')
  pass('CL-AC-5', 'unauth comment 401')

  const comment = await req('POST', `/api/articles/${pubId}/comments`, {
    token: authorToken,
    body: { content: '一级评论内容' },
  })
  assert(comment.json.code === 0, 'create comment')
  const commentId = comment.json.data.id
  pass('CL-AC-1', 'create first-level comment')

  const comments = await req('GET', `/api/articles/${pubId}/comments`)
  assert(comments.json.code === 0 && comments.json.data.some((c) => c.id === commentId), 'list comments')
  pass('CL-AC-2', 'list comments')

  const like1 = await req('POST', '/api/likes/toggle', {
    token: authorToken,
    body: { targetType: 'ARTICLE', targetId: pubId },
  })
  assert(like1.json.code === 0 && like1.json.data.liked === true && like1.json.data.count >= 1, 'like article')
  const like2 = await req('POST', '/api/likes/toggle', {
    token: authorToken,
    body: { targetType: 'ARTICLE', targetId: pubId },
  })
  assert(like2.json.data.liked === false && like2.json.data.count === like1.json.data.count - 1, 'unlike')
  pass('CL-AC-4', 'like toggle idempotent')

  const likeC = await req('POST', '/api/likes/toggle', {
    token: authorToken,
    body: { targetType: 'COMMENT', targetId: commentId },
  })
  assert(likeC.json.code === 0 && likeC.json.data.liked === true, 'like comment')

  // Site experience
  const siteGet = await req('GET', '/api/site')
  assert(siteGet.json.code === 0 && siteGet.json.data.siteName, 'get site')
  const newName = `站名${Date.now()}`
  const sitePut = await req('PUT', '/api/admin/site', {
    token: adminToken,
    body: {
      siteName: newName,
      tagline: 'tagline',
      aboutText: 'about',
      socialLinks: [{ name: 'GitHub', url: 'https://github.com' }],
    },
  })
  assert(sitePut.json.code === 0 && sitePut.json.data.siteName === newName, 'update site')
  const siteGet2 = await req('GET', '/api/site')
  assert(siteGet2.json.data.siteName === newName, 'site reflected')
  pass('SITE-AC-4', 'site config CRUD')

  const featured = await req('GET', '/api/articles/featured')
  assert(featured.json.code === 0, 'featured')
  assert(featured.json.data.some((a) => a.id === pubId), 'recommended in featured')
  pass('SITE-AC-1', 'featured list')

  const archive = await req('GET', '/api/articles/archive')
  assert(archive.json.code === 0 && Array.isArray(archive.json.data), 'archive')
  assert(archive.json.data.length >= 1, 'archive has months')
  pass('SITE-AC-2', 'archive months')

  // cleanup soft: offline draft articles left is fine
  await req('DELETE', `/api/admin/articles/${draftId}`, { token: adminToken })
  await req('DELETE', `/api/admin/articles/${offlineId}`, { token: adminToken })

  const failed = results.filter((r) => !r.ok)
  console.log('\n--- Summary ---')
  console.log(`passed=${results.filter((r) => r.ok).length} failed=${failed.length}`)
  if (failed.length) process.exit(1)
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
