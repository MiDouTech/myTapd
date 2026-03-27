import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

import { routes } from './routes'

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  const requiresAuth = to.matched.every((record) => record.meta.requiresAuth !== false)

  const ssoLandingPaths = ['/', '/dashboard']
  if (ssoLandingPaths.includes(to.path) && typeof to.query.token === 'string' && to.query.token) {
    return {
      path: '/sso/callback',
      query: {
        token: to.query.token,
        state: (to.query.state as string) || undefined,
        app_code: (to.query.app_code as string) || undefined,
        source: (to.query.source as string) || undefined,
        redirect: (to.query.redirect as string) || undefined,
      },
    }
  }

  if (requiresAuth && !authStore.isLoggedIn) {
    const hasTestParam = to.query.test === 'test'
    return {
      path: '/login',
      query: {
        redirect: to.fullPath,
        ...(hasTestParam ? { test: 'test' } : {}),
      },
    }
  }

  if (to.path === '/login' && authStore.isLoggedIn) {
    const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/dashboard'
    return redirect
  }

  if (requiresAuth && authStore.isLoggedIn && !authStore.userInfo) {
    try {
      await authStore.loadCurrentUser()
    } catch {
      authStore.clearLoginState()
      return {
        path: '/login',
        query: {
          redirect: to.fullPath,
        },
      }
    }
  }

  document.title = `${to.meta.title || '工单系统'} - ${import.meta.env.VITE_APP_TITLE}`
  return true
})

export default router
