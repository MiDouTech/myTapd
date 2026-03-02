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

  if (requiresAuth && !authStore.isLoggedIn) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath,
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
