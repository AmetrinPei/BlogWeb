/**
 * blog-friend-links acceptance
 * Usage: node scripts/acceptance-friend-links.mjs
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
  assert(Array.isArray(d.friendLinks), 'missing friendLinks array')
  assert(Array.isArray(d.aboutHighlights), 'missing aboutHighlights array')
  assert('aboutTitle' in d, 'missing aboutTitle key')
  assert('aboutDisplayName' in d, 'missing aboutDisplayName key')
  pass('FL-1', `public site friend/about fields present (friends=${d.friendLinks.length})`)

  const login = await req('POST', '/api/admin/auth/login', {
    body: { username: 'admin', password: 'admin123' },
  })
  assert(login.json.code === 0 && login.json.data?.token, 'admin login failed')
  const token = login.json.data.token
  pass('FL-2', 'admin login ok')

  const snapshot = {
    siteName: d.siteName,
    tagline: d.tagline || '',
    aboutText: d.aboutText || '',
    socialLinks: d.socialLinks || [],
    friendLinks: d.friendLinks || [],
    aboutTitle: d.aboutTitle || '',
    aboutDisplayName: d.aboutDisplayName || '',
    aboutHighlights: d.aboutHighlights || [],
    defaultTheme: d.defaultTheme || 'light',
    backgroundMode: d.backgroundMode || 'theme',
    backgroundColor: d.backgroundColor || '',
    backgroundGradient: d.backgroundGradient || '',
    backgroundImageUrl: d.backgroundImageUrl || '',
    aboutAvatarUrl: d.aboutAvatarUrl || '',
    homeHeroUrl: d.homeHeroUrl || '',
  }

  const putFriends = await req('PUT', '/api/admin/site', {
    token,
    body: {
      ...snapshot,
      aboutTitle: '验收关于标题',
      aboutDisplayName: '验收作者',
      aboutHighlights: ['亮点一', '亮点二'],
      friendLinks: [
        { name: '友链乙', url: 'https://beta.example.com', description: '第二', sortOrder: 99 },
        { name: '友链甲', url: 'https://alpha.example.com', description: '第一', sortOrder: 1 },
      ],
      socialLinks: [{ name: 'SocialKeep', url: 'https://social.example.com' }],
    },
  })
  assert(putFriends.json.code === 0, `PUT friends failed: ${JSON.stringify(putFriends.json)}`)
  assert(putFriends.json.data.friendLinks?.length === 2, 'expected 2 friend links')
  assert(putFriends.json.data.friendLinks[0].name === '友链乙', 'order should follow array, first=友链乙')
  assert(putFriends.json.data.friendLinks[0].sortOrder === 0, 'sortOrder rewritten to 0')
  assert(putFriends.json.data.friendLinks[1].name === '友链甲', 'second=友链甲')
  assert(putFriends.json.data.aboutTitle === '验收关于标题', 'aboutTitle not saved')
  assert(putFriends.json.data.socialLinks?.[0]?.name === 'SocialKeep', 'socialLinks overwritten incorrectly')
  pass('FL-3', 'ADMIN saved friendLinks order + about fields; socialLinks intact')

  const pub2 = await req('GET', '/api/site')
  assert(pub2.json.data.friendLinks[0].name === '友链乙', 'public order mismatch')
  assert(pub2.json.data.aboutTitle === '验收关于标题', 'public aboutTitle mismatch')
  assert(pub2.json.data.aboutHighlights?.length === 2, 'public highlights mismatch')
  pass('FL-4', 'public GET reflects friend order and about title')

  const badUrl = await req('PUT', '/api/admin/site', {
    token,
    body: {
      ...snapshot,
      friendLinks: [{ name: 'x', url: 'javascript:alert(1)', description: '' }],
    },
  })
  assert(badUrl.json.code === 400, `illegal url should 400: ${JSON.stringify(badUrl.json)}`)
  pass('FL-5', 'illegal friend url → 400')

  const clear = await req('PUT', '/api/admin/site', {
    token,
    body: {
      ...snapshot,
      friendLinks: [],
      aboutTitle: '',
      aboutDisplayName: '',
      aboutHighlights: [],
    },
  })
  assert(clear.json.code === 0, `clear failed: ${JSON.stringify(clear.json)}`)
  assert(Array.isArray(clear.json.data.friendLinks) && clear.json.data.friendLinks.length === 0, 'friends not cleared')
  pass('FL-6', 'cleared friendLinks')

  const restore = await req('PUT', '/api/admin/site', {
    token,
    body: snapshot,
  })
  assert(restore.json.code === 0, `restore failed: ${JSON.stringify(restore.json)}`)
  pass('FL-7', 'restored previous site settings')

  const noAuth = await req('PUT', '/api/admin/site', {
    body: { siteName: 'x', tagline: '', aboutText: '', socialLinks: [], friendLinks: [] },
  })
  assert(noAuth.json.code === 401, 'unauth PUT should 401')
  pass('FL-8', 'PUT without token → 401')

  console.log('\nAll friend-links acceptance checks passed.')
}

main().catch((err) => {
  console.error('FAIL ', err.message || err)
  process.exit(1)
})
