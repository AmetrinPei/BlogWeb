/**
 * blog-comment-thread acceptance: nested replies, depth limit, cascade delete.
 * Usage: node scripts/acceptance-comment-thread.mjs
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

function findComment(nodes, id) {
  for (const node of nodes || []) {
    if (node.id === id) return node
    const hit = findComment(node.replies, id)
    if (hit) return hit
  }
  return null
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
      body: { name: '楼中楼验收分类' },
    })
    assert(created.json.code === 0, 'create category')
    cat = created.json.data
  }

  const suffix = Date.now()
  const article = await req('POST', '/api/admin/articles', {
    token,
    body: {
      title: `楼中楼文章_${suffix}`,
      content: 'thread body',
      categoryId: cat.id,
      status: 'PUBLISHED',
      publishedAt: '2026-06-01T10:00:00',
    },
  })
  assert(article.json.code === 0, `create article: ${JSON.stringify(article.json)}`)
  const articleId = article.json.data.id

  try {
    const noAuth = await req('POST', `/api/articles/${articleId}/comments`, {
      body: { content: '未登录' },
    })
    assert(noAuth.json.code === 401 || noAuth.status === 401, 'unauth should 401')
    pass('CT-AC-7', 'unauth create 401')

    const root = await req('POST', `/api/articles/${articleId}/comments`, {
      token,
      body: { content: `根评论_${suffix}` },
    })
    assert(root.json.code === 0 && root.json.data?.id, 'create root')
    assert(root.json.data.parentId == null, 'root parentId null')
    const rootId = root.json.data.id
    pass('CT-AC-5', 'create root without parentId')

    const reply1 = await req('POST', `/api/articles/${articleId}/comments`, {
      token,
      body: { content: `回复1_${suffix}`, parentId: rootId },
    })
    assert(reply1.json.code === 0 && reply1.json.data?.parentId === rootId, 'create reply1')
    const reply1Id = reply1.json.data.id

    const reply2 = await req('POST', `/api/articles/${articleId}/comments`, {
      token,
      body: { content: `回复2_${suffix}`, parentId: rootId },
    })
    assert(reply2.json.code === 0, 'create reply2')
    pass('CT-AC-2', 'create replies under root')

    const list = await req('GET', `/api/articles/${articleId}/comments`)
    assert(list.json.code === 0, 'list ok')
    assert(Array.isArray(list.json.data) && list.json.data.length === 1, 'one root')
    const treeRoot = list.json.data[0]
    assert(treeRoot.id === rootId && Array.isArray(treeRoot.replies), 'tree shape')
    assert(treeRoot.replies.length === 2, 'two replies')
    assert(treeRoot.replies.every((r) => r.parentId === rootId), 'reply parentIds')
    assert(findComment(list.json.data, reply1Id), 'reply1 in tree')
    pass('CT-AC-4', 'list returns parent + replies')

    const deep = await req('POST', `/api/articles/${articleId}/comments`, {
      token,
      body: { content: '三层', parentId: reply1Id },
    })
    assert(deep.json.code === 400, `depth reject got ${deep.json.code}`)
    pass('CT-AC-3', 'reject reply-to-reply')

    const likeRoot = await req('POST', '/api/likes/toggle', {
      token,
      body: { targetType: 'COMMENT', targetId: rootId },
    })
    assert(likeRoot.json.code === 0 && likeRoot.json.data.liked === true, 'like root')
    const likeReply = await req('POST', '/api/likes/toggle', {
      token,
      body: { targetType: 'COMMENT', targetId: reply1Id },
    })
    assert(likeReply.json.code === 0 && likeReply.json.data.liked === true, 'like reply')
    const listLiked = await req('GET', `/api/articles/${articleId}/comments`, { token })
    const likedRoot = findComment(listLiked.json.data, rootId)
    assert(likedRoot?.likeCount >= 1, 'root likeCount')
    pass('CT-AC-9', 'like root and reply')

    const delRoot = await req('DELETE', `/api/comments/${rootId}`, { token })
    assert(delRoot.json.code === 0, 'delete root')
    const afterDel = await req('GET', `/api/articles/${articleId}/comments`)
    assert(afterDel.json.code === 0 && afterDel.json.data.length === 0, 'cascade cleared')
    const likeGone = await req('POST', '/api/likes/toggle', {
      token,
      body: { targetType: 'COMMENT', targetId: reply1Id },
    })
    assert(likeGone.json.code === 404, 'child like target gone')
    pass('CT-AC-6', 'cascade delete root + replies + likes')

    const legacy = await req('POST', `/api/articles/${articleId}/comments`, {
      token,
      body: { content: `兼容根_${suffix}` },
    })
    const legacyId = legacy.json.data.id
    const legacyList = await req('GET', `/api/articles/${articleId}/comments`)
    assert(findComment(legacyList.json.data, legacyId)?.parentId == null, 'legacy root')
    pass('CT-AC-10', 'null parentId roots still listed')
  } catch (e) {
    fail('CT-run', e.message)
  } finally {
    await req('DELETE', `/api/admin/articles/${articleId}`, { token })
  }

  const failed = results.filter((r) => !r.ok)
  console.log('\n--- Summary ---')
  console.log(`passed=${results.filter((r) => r.ok).length} failed=${failed.length}`)
  if (failed.length) process.exit(1)
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
