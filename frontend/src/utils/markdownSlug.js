/**
 * Pure heading slug helpers (browser + Node safe).
 */

/**
 * @param {string} text
 * @returns {string}
 */
export function slugifyHeading(text) {
  let slug = String(text || '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^\p{L}\p{N}-]+/gu, '')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '')

  if (!slug) slug = 'section'
  if (/^\d/.test(slug)) slug = `h-${slug}`
  return slug
}

/**
 * @param {string} base
 * @param {Map<string, number>} used
 * @returns {string}
 */
export function uniqueHeadingId(base, used) {
  const count = used.get(base) || 0
  used.set(base, count + 1)
  if (count === 0) return base
  return `${base}-${count + 1}`
}
