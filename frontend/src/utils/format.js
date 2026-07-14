/** 格式化为 YYYY-MM-DD（本地展示） */
export function formatDate(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value).slice(0, 10)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** 按分类名映射卡片 emoji，未知则默认 */
const CATEGORY_EMOJI = {
  技术: '💻',
  编程: '🧑‍💻',
  生活: '🌿',
  随笔: '✍️',
  旅行: '✈️',
  读书: '📚',
  设计: '🎨',
  前端: '✨',
  后端: '🧩',
  Java: '☕',
  Spring: '🌱',
}

export function categoryEmoji(categoryName) {
  if (!categoryName) return '📝'
  return CATEGORY_EMOJI[categoryName] || '📝'
}
