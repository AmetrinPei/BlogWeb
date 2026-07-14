import { nextTick, watch } from 'vue'

let hljsPromise = null

async function loadHljs() {
  if (!hljsPromise) {
    hljsPromise = (async () => {
      const hljs = (await import('highlight.js/lib/core')).default
      const langs = await Promise.all([
        import('highlight.js/lib/languages/javascript'),
        import('highlight.js/lib/languages/typescript'),
        import('highlight.js/lib/languages/java'),
        import('highlight.js/lib/languages/xml'),
        import('highlight.js/lib/languages/css'),
        import('highlight.js/lib/languages/json'),
        import('highlight.js/lib/languages/bash'),
        import('highlight.js/lib/languages/sql'),
        import('highlight.js/lib/languages/markdown'),
        import('highlight.js/lib/languages/python'),
      ])
      const names = [
        'javascript',
        'typescript',
        'java',
        'xml',
        'css',
        'json',
        'bash',
        'sql',
        'markdown',
        'python',
      ]
      names.forEach((name, i) => hljs.registerLanguage(name, langs[i].default))
      await import('highlight.js/styles/github.css')
      return hljs
    })()
  }
  return hljsPromise
}

/**
 * Highlight code blocks inside rootEl. No-op (and no import) if none.
 * @param {HTMLElement | null | undefined} rootEl
 */
export async function highlightCodeBlocks(rootEl) {
  if (!rootEl) return
  const blocks = rootEl.querySelectorAll('pre code')
  if (!blocks.length) return

  const hljs = await loadHljs()
  blocks.forEach((block) => {
    if (block.dataset.highlighted === 'yes') return
    hljs.highlightElement(block)
  })
}

/**
 * Watch htmlRef; after DOM update, highlight inside getRoot().
 * @param {import('vue').Ref<string>} htmlRef
 * @param {() => HTMLElement | null | undefined} getRoot
 */
export function useCodeHighlight(htmlRef, getRoot) {
  watch(
    htmlRef,
    async () => {
      await nextTick()
      await highlightCodeBlocks(getRoot())
    },
    { flush: 'post' },
  )
}
