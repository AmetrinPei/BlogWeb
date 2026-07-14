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

function buildSite(remote) {
  return {
    name: remote?.siteName || siteConfig.name,
    tagline: remote?.tagline || siteConfig.tagline,
    heroSubtitle: remote?.aboutText || siteConfig.heroSubtitle,
    aboutText: remote?.aboutText || siteConfig.about.intro,
    author: siteConfig.author,
    avatar: siteConfig.avatar,
    about: siteConfig.about,
    socials: remote?.socialLinks?.length
      ? mapSocials(remote.socialLinks)
      : siteConfig.socials,
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
