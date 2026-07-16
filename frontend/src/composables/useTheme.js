import { computed, ref, watch } from 'vue'

const STORAGE_KEY = 'blog.theme'
const THEMES = new Set(['light', 'dark'])

const theme = ref('light')
let initialized = false

function readStored() {
  try {
    const v = localStorage.getItem(STORAGE_KEY)
    return THEMES.has(v) ? v : null
  } catch {
    return null
  }
}

function writeStored(value) {
  try {
    localStorage.setItem(STORAGE_KEY, value)
  } catch {
    // ignore quota / private mode
  }
}

function applyDom(value) {
  if (typeof document === 'undefined') return
  document.documentElement.dataset.theme = value
}

/**
 * Resolve visitor theme: localStorage → site defaultTheme → light
 */
export function resolveTheme(siteDefaultTheme) {
  const stored = readStored()
  if (stored) return stored
  if (THEMES.has(siteDefaultTheme)) return siteDefaultTheme
  return 'light'
}

export function useTheme() {
  if (!initialized) {
    theme.value = resolveTheme(null)
    applyDom(theme.value)
    initialized = true
  }

  const isDark = computed(() => theme.value === 'dark')

  function setTheme(next) {
    const value = THEMES.has(next) ? next : 'light'
    theme.value = value
    writeStored(value)
    applyDom(value)
  }

  function toggleTheme() {
    setTheme(theme.value === 'dark' ? 'light' : 'dark')
  }

  /** Call after site settings load to apply default when no local preference. */
  function syncFromSiteDefault(siteDefaultTheme) {
    if (readStored()) {
      applyDom(theme.value)
      return
    }
    setTheme(resolveTheme(siteDefaultTheme))
  }

  watch(theme, (v) => applyDom(v), { immediate: true })

  return {
    theme,
    isDark,
    setTheme,
    toggleTheme,
    syncFromSiteDefault,
  }
}

export { STORAGE_KEY as THEME_STORAGE_KEY }
