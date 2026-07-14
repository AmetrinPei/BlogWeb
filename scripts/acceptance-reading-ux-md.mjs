/**
 * blog-reading-ux: markdown slug / TOC / XSS sanitize checks (Node).
 * Usage: node scripts/acceptance-reading-ux-md.mjs
 * (run from repo root; uses frontend/node_modules)
 *
 * Covers Spec AC-2 (unique ids), AC-5 (no script after purify) at utility level.
 * Full UI walkthrough remains Plan §2.7.
 */
import { createRequire } from 'module'
import { dirname, join } from 'path'
import { fileURLToPath, pathToFileURL } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const frontendRoot = join(__dirname, '../frontend')
const require = createRequire(join(frontendRoot, 'package.json'))

const { JSDOM } = require('jsdom')
const { marked } = await import(
  pathToFileURL(join(frontendRoot, 'node_modules/marked/lib/marked.esm.js')).href
)
const createDOMPurify = require('dompurify')
const { slugifyHeading, uniqueHeadingId } = await import(
  pathToFileURL(join(frontendRoot, 'src/utils/markdownSlug.js')).href
)

const purifyWindow = new JSDOM('').window
const DOMPurify = createDOMPurify(purifyWindow)

function injectHeadingAnchors(sanitizedHtml) {
  if (!sanitizedHtml) return { html: '', toc: [] }
  const dom = new JSDOM(`<div id="__md_root">${sanitizedHtml}</div>`)
  const root = dom.window.document.getElementById('__md_root')
  if (!root) return { html: sanitizedHtml, toc: [] }

  const used = new Map()
  const toc = []
  root.querySelectorAll('h2, h3').forEach((el) => {
    const text = (el.textContent || '').trim()
    if (!text) return
    const level = el.tagName === 'H2' ? 2 : 3
    const id = uniqueHeadingId(slugifyHeading(text), used)
    el.setAttribute('id', id)
    toc.push({ id, text, level })
  })
  return { html: root.innerHTML, toc }
}

function renderArticleMarkdown(md) {
  if (!md) return { html: '', toc: [] }
  const rawHtml = marked.parse(md, { async: false })
  const sanitized = DOMPurify.sanitize(rawHtml, { ADD_ATTR: ['id'] })
  return injectHeadingAnchors(sanitized)
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

function main() {
  try {
    assert(slugifyHeading('Hello World') === 'hello-world', 'slug ascii')
    assert(slugifyHeading('简介 Overview') === '简介-overview', 'slug unicode')
    assert(slugifyHeading('123 start').startsWith('h-'), 'slug leading digit')
    assert(slugifyHeading('!!!') === 'section', 'slug empty fallback')
    const used = new Map()
    assert(uniqueHeadingId('same', used) === 'same', 'unique first')
    assert(uniqueHeadingId('same', used) === 'same-2', 'unique second')
    assert(uniqueHeadingId('same', used) === 'same-3', 'unique third')
    pass('RU-SLUG', 'slugify + unique ids')
  } catch (e) {
    fail('RU-SLUG', e.message)
  }

  try {
    const md = `## One\n\ntext\n\n### Nested\n\n## Two\n\n## One\n`
    const { html, toc } = renderArticleMarkdown(md)
    assert(toc.length === 4, `toc length want 4 got ${toc.length}`)
    assert(toc[0].id === 'one' && toc[0].level === 2, 'first h2')
    assert(toc[1].id === 'nested' && toc[1].level === 3, 'h3')
    assert(toc[2].id === 'two', 'second h2')
    assert(toc[3].id === 'one-2', `dup id want one-2 got ${toc[3].id}`)
    assert(html.includes('id="one"') && html.includes('id="one-2"'), 'ids in html')
    assert(html.includes('id="nested"'), 'nested id')
    pass('RU-TOC', `toc=${toc.map((t) => t.id).join(',')}`)
  } catch (e) {
    fail('RU-TOC', e.message)
  }

  try {
    const { toc } = renderArticleMarkdown('just a paragraph\n\nno headings here')
    assert(toc.length === 0, 'empty toc')
    pass('RU-TOC-EMPTY', 'no h2/h3 → empty toc')
  } catch (e) {
    fail('RU-TOC-EMPTY', e.message)
  }

  try {
    const md = `## Safe\n\n<script>alert(1)</script>\n\n<img src=x onerror="alert(2)">\n\n[ok](javascript:alert(3))\n`
    const { html } = renderArticleMarkdown(md)
    assert(!/<script/i.test(html), 'no script tag')
    assert(!/onerror\s*=/i.test(html), 'no onerror')
    assert(!/javascript:/i.test(html), 'no javascript: url')
    assert(html.includes('id="safe"'), 'heading id kept after purify')
    pass('RU-XSS', 'dangerous html stripped; heading id retained')
  } catch (e) {
    fail('RU-XSS', e.message)
  }

  const failed = results.filter((r) => !r.ok)
  console.log(`\n${results.length - failed.length}/${results.length} passed`)
  if (failed.length) process.exit(1)
}

main()
