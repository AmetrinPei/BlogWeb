import { computed, inject, onMounted, provide, ref } from 'vue'
import { fetchSite } from '@/api/site'
import { siteConfig } from '@/config/site'

const SITE_KEY = Symbol('siteSettings')

function mapSocials(links) {
  if (!Array.isArray(links) || !links.length) {
    return siteConfig.socials
  }
  return links
    .filter((item) => item?.url)
    .map((item) => ({
      label: item.name || item.url,
      href: item.url,
    }))
}

function isHttpUrl(url) {
  if (!url || typeof url !== 'string') return false
  const lower = url.trim().toLowerCase()
  return lower.startsWith('http://') || lower.startsWith('https://')
}

function mapFriendLinks(links) {
  if (!Array.isArray(links) || !links.length) {
    return []
  }
  return links
    .filter((item) => item?.name && isHttpUrl(item.url))
    .map((item) => ({
      name: item.name,
      url: item.url.trim(),
      description: item.description || '',
      sortOrder: item.sortOrder ?? 0,
    }))
}

function buildSite(remote) {
  const hasRemote = remote != null
  const highlights = hasRemote
    ? Array.isArray(remote.aboutHighlights)
      ? remote.aboutHighlights
      : []
    : siteConfig.about.highlights

  const aboutTitle = hasRemote
    ? remote.aboutTitle?.trim() || '关于我'
    : siteConfig.about.title

  const aboutText = hasRemote
    ? remote.aboutText ?? ''
    : siteConfig.about.intro

  const author = hasRemote
    ? remote.aboutDisplayName?.trim() || siteConfig.author
    : siteConfig.author

  return {
    name: remote?.siteName || siteConfig.name,
    tagline: remote?.tagline || siteConfig.tagline,
    heroSubtitle: remote?.aboutText || siteConfig.heroSubtitle,
    aboutText,
    aboutTitle,
    author,
    avatar: siteConfig.avatar,
    about: {
      title: aboutTitle,
      intro: aboutText || siteConfig.about.intro,
      highlights,
    },
    aboutHighlights: highlights,
    friendLinks: mapFriendLinks(remote?.friendLinks),
    socials: remote?.socialLinks?.length
      ? mapSocials(remote.socialLinks)
      : siteConfig.socials,
    defaultTheme: remote?.defaultTheme || 'light',
    backgroundMode: remote?.backgroundMode || 'theme',
    backgroundColor: remote?.backgroundColor || null,
    backgroundGradient: remote?.backgroundGradient || null,
    backgroundImageUrl: remote?.backgroundImageUrl || null,
    aboutAvatarUrl: remote?.aboutAvatarUrl || null,
    homeHeroUrl: remote?.homeHeroUrl || null,
    // Missing field (old backend / fetch fail) → treat as open (Plan)
    publicRegistrationEnabled:
      remote == null || remote.publicRegistrationEnabled !== false,
  }
}

export function provideSiteSettings() {
  const loading = ref(true)
  const remote = ref(null)

  const site = computed(() => buildSite(remote.value))

  async function reload() {
    loading.value = true
    try {
      remote.value = await fetchSite()
    } catch {
      remote.value = null
    } finally {
      loading.value = false
    }
  }

  onMounted(reload)

  const ctx = { site, loading, reload }
  provide(SITE_KEY, ctx)
  return ctx
}

export function useSiteSettings() {
  const ctx = inject(SITE_KEY, null)
  if (ctx) return ctx

  return {
    site: computed(() => buildSite(null)),
    loading: ref(false),
    reload: () => Promise.resolve(),
  }
}
