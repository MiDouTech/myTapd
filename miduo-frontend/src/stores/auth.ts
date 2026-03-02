import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { getCurrentUser } from '@/api/user'
import type { LoginOutput } from '@/types/auth'
import type { CurrentUserOutput } from '@/types/user'
import {
  clearAuthStorage,
  getAccessToken,
  getRefreshToken,
  setAccessToken,
  setRefreshToken,
} from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string>(getAccessToken())
  const refreshToken = ref<string>(getRefreshToken())
  const userInfo = ref<CurrentUserOutput | null>(null)
  const loginLoading = ref(false)

  const isLoggedIn = computed(() => Boolean(accessToken.value))

  function setLoginState(payload: LoginOutput): void {
    accessToken.value = payload.accessToken
    refreshToken.value = payload.refreshToken
    setAccessToken(payload.accessToken)
    setRefreshToken(payload.refreshToken)
    if (payload.userInfo) {
      userInfo.value = {
        id: payload.userInfo.id,
        name: payload.userInfo.name,
        avatarUrl: payload.userInfo.avatar,
        departmentName: payload.userInfo.department,
        roleCodes: payload.userInfo.roles,
      }
    }
  }

  function clearLoginState(): void {
    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    clearAuthStorage()
  }

  async function loadCurrentUser(force = false): Promise<void> {
    if (!accessToken.value || (userInfo.value && !force)) {
      return
    }
    const profile = await getCurrentUser()
    userInfo.value = profile
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    loginLoading,
    isLoggedIn,
    setLoginState,
    clearLoginState,
    loadCurrentUser,
  }
})
