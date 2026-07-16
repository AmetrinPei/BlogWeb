/**
 * blog-ops-docker acceptance
 * 1) Always: static checks on deploy configs
 * 2) Optional: if OPS_BASE is set, smoke HTTPS health / site / feed
 *
 * Usage:
 *   node scripts/acceptance-ops-docker.mjs
 *   $env:OPS_BASE="https://localhost"; $env:NODE_TLS_REJECT_UNAUTHORIZED="0"; node scripts/acceptance-ops-docker.mjs
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, '..')

function assert(cond, msg) {
  if (!cond) throw new Error(msg)
}

function pass(id, detail) {
  console.log(`PASS  ${id}  ${detail}`)
}

function read(rel) {
  return fs.readFileSync(path.join(root, rel), 'utf8')
}

function exists(rel) {
  return fs.existsSync(path.join(root, rel))
}

async function main() {
  assert(exists('deploy/docker-compose.yml'), 'missing deploy/docker-compose.yml')
  assert(exists('backend/Dockerfile'), 'missing backend/Dockerfile')
  assert(exists('deploy/nginx/Dockerfile'), 'missing deploy/nginx/Dockerfile')
  assert(exists('deploy/nginx/default.conf'), 'missing deploy/nginx/default.conf')
  assert(exists('docx/部署方式.md'), 'missing docx/部署方式.md')
  pass('OD-1', 'compose / Dockerfiles / nginx conf / deploy doc present')

  const conf = read('deploy/nginx/default.conf')
  assert(conf.includes('location /api/'), 'nginx must proxy /api/')
  assert(conf.includes('location /uploads/'), 'nginx must proxy /uploads/')
  assert(conf.includes('location = /feed.xml') || conf.includes('location /feed.xml'), 'nginx must proxy feed.xml')
  assert(conf.includes('try_files'), 'nginx must SPA try_files')
  assert(conf.includes('listen 443') && conf.includes('ssl'), 'nginx must terminate HTTPS')
  assert(conf.includes('return 301 https://'), 'nginx should redirect HTTP to HTTPS')
  pass('OD-2', 'nginx conf has api/uploads/feed/SPA/HTTPS')

  const compose = read('deploy/docker-compose.yml')
  assert(compose.includes('uploads_data'), 'compose must define uploads volume')
  assert(compose.includes('mysql'), 'compose must include mysql')
  assert(compose.includes('JWT_SECRET'), 'compose must inject JWT_SECRET')
  assert(compose.includes('BLOG_SITE_BASE_URL'), 'compose must inject BLOG_SITE_BASE_URL')
  assert(compose.includes('BLOG_UPLOAD_DIR'), 'compose must set upload dir')
  pass('OD-3', 'compose wires mysql, secrets, upload volume')

  const base = process.env.OPS_BASE
  if (!base) {
    console.log('SKIP  OD-4  set OPS_BASE to smoke a running stack')
    console.log('All static acceptance checks passed.')
    return
  }

  const health = await fetch(`${base.replace(/\/$/, '')}/api/health`)
  const healthJson = await health.json()
  assert(healthJson.code === 0 && healthJson.data?.status === 'UP', `health: ${JSON.stringify(healthJson)}`)
  pass('OD-4', `${base}/api/health UP`)

  const site = await fetch(`${base.replace(/\/$/, '')}/api/site`)
  const siteJson = await site.json()
  assert(siteJson.code === 0 && siteJson.data?.siteName, `site: ${JSON.stringify(siteJson)}`)
  pass('OD-5', 'GET /api/site via reverse proxy')

  const feed = await fetch(`${base.replace(/\/$/, '')}/feed.xml`)
  const feedText = await feed.text()
  assert(feed.ok && feedText.includes('<rss'), `feed status=${feed.status}`)
  pass('OD-6', 'GET /feed.xml via reverse proxy')

  console.log('All acceptance checks passed.')
}

main().catch((err) => {
  console.error('FAIL', err.message || err)
  process.exit(1)
})
