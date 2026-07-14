/**
 * Article Markdown → sanitized HTML + TOC (h2/h3 anchors).
 * Order: marked → DOMPurify → inject ids on sanitized DOM.
 */

import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { slugifyHeading, uniqueHeadingId } from '@/utils/markdownSlug'

export { slugifyHeading, uniqueHeadingId } from '@/utils/markdownSlug'

/**
 * Inject ids into h2/h3 and collect TOC from already-sanitized HTML.
 * @param {string} sanitizedHtml
 * @returns {{ html: string, toc: Array<{ id: string, text: string, level: number }> }}
 */
export function injectHeadingAnchors(sanitizedHtml) {
  if (!sanitizedHtml) return { html: '', toc: [] }

  const doc = new DOMParser().parseFromString(
    `<div id="__md_root">${sanitizedHtml}</div>`,
    'text/html',
  )
  const root = doc.getElementById('__md_root')
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

/**
 * @param {string} md
 * @returns {{ html: string, toc: Array<{ id: string, text: string, level: number }> }}
 */
export function renderArticleMarkdown(md) {
  if (!md) return { html: '', toc: [] }

  const rawHtml = marked.parse(md, { async: false })
  const sanitized = DOMPurify.sanitize(rawHtml, {
    ADD_ATTR: ['id'],
  })
  return injectHeadingAnchors(sanitized)
}
