import { createRouter, createWebHistory } from 'vue-router'
import { getRole, isLoggedIn, isSafeAdminRedirect, isSafePublicRedirect } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/PublicLayout.vue'),
      meta: { area: 'public' },
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
        },
        {
          path: 'articles',
          name: 'articles',
          component: () => import('@/views/ArticlesView.vue'),
        },
        {
          path: 'articles/:id',
          name: 'article-detail',
          component: () => import('@/views/ArticleDetailView.vue'),
        },
        {
          path: 'archive',
          name: 'archive',
          component: () => import('@/views/ArchiveView.vue'),
        },
        {
          path: 'about',
          name: 'about',
          component: () => import('@/views/AboutView.vue'),
        },
        {
          path: 'register',
          name: 'register',
          component: () => import('@/views/RegisterView.vue'),
          meta: { public: true },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue'),
          meta: { requiresLogin: true },
        },
      ],
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/admin/LoginView.vue'),
      meta: { area: 'public', public: true },
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('@/views/admin/LoginView.vue'),
      meta: { area: 'admin', public: true },
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { area: 'admin', requiresAuth: true },
      children: [
        {
          path: '',
          redirect: { name: 'admin-articles' },
        },
        {
          path: 'articles',
          name: 'admin-articles',
          component: () => import('@/views/admin/ArticlesView.vue'),
          meta: { roles: ['ADMIN', 'AUTHOR'] },
        },
        {
          path: 'categories',
          name: 'admin-categories',
          component: () => import('@/views/admin/CategoriesView.vue'),
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'tags',
          name: 'admin-tags',
          component: () => import('@/views/admin/TagsView.vue'),
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'comments',
          name: 'admin-comments',
          component: () => import('@/views/admin/CommentsModerationView.vue'),
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'sensitive-words',
          name: 'admin-sensitive-words',
          component: () => import('@/views/admin/SensitiveWordsView.vue'),
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'site',
          name: 'admin-site',
          component: () => import('@/views/admin/SiteSettingsView.vue'),
          meta: { roles: ['ADMIN'] },
        },
      ],
    },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  const needsAuth = to.matched.some((record) => record.meta.requiresAuth)
  const needsLogin = to.matched.some((record) => record.meta.requiresLogin)
  const loggedIn = isLoggedIn()
  const role = getRole()

  if (needsAuth && !loggedIn) {
    return {
      name: 'admin-login',
      query: { redirect: to.fullPath },
    }
  }

  if (needsLogin && !loggedIn) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  if (needsAuth && loggedIn) {
    const requiredRoles = [
      ...new Set(
        to.matched.flatMap((record) => record.meta.roles || []),
      ),
    ]
    if (requiredRoles.length && !requiredRoles.includes(role)) {
      return { name: 'admin-articles' }
    }
  }

  if (to.name === 'admin-login' && loggedIn) {
    const raw = to.query.redirect
    if (isSafeAdminRedirect(raw)) {
      return raw
    }
    return { name: 'admin-articles' }
  }

  if (to.name === 'login' && loggedIn) {
    const raw = to.query.redirect
    if (isSafePublicRedirect(raw)) {
      return raw
    }
    return { name: 'home' }
  }

  return true
})

export default router
