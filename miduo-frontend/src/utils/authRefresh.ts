import axios from 'axios'

import type { ApiResult } from '@/types/common'
import type { LoginOutput } from '@/types/auth'
import { getRefreshToken, setAccessToken, setRefreshToken } from '@/utils/storage'

let refreshPromise: Promise<LoginOutput> | null = null

/**
 * 使用 RefreshToken 换取新的双 Token（绕过主 request 实例，避免拦截器递归）
 */
export function refreshAccessToken(): Promise<LoginOutput> {
  if (refreshPromise) {
    return refreshPromise
  }
  const rt = getRefreshToken()
  if (!rt) {
    return Promise.reject(new Error('No refresh token'))
  }

  refreshPromise = (async () => {
    try {
      const res = await axios.post<ApiResult<LoginOutput>>(
        `${import.meta.env.VITE_API_BASE_URL}/auth/refresh`,
        { refreshToken: rt },
        {
          headers: { 'Content-Type': 'application/json' },
          timeout: 15000,
        },
      )
      const payload = res.data
      if (typeof payload?.code === 'number' && payload.code === 200 && payload.data) {
        const data = payload.data
        setAccessToken(data.accessToken)
        setRefreshToken(data.refreshToken)
        return data
      }
      throw new Error(payload?.message || 'Refresh failed')
    } finally {
      refreshPromise = null
    }
  })()

  return refreshPromise
}

export function isAuthRefreshExcludedUrl(url: string): boolean {
  return (
    url.includes('/auth/refresh') ||
    url.includes('/auth/dev/login') ||
    url.includes('/auth/wecom/login') ||
    url.includes('/auth/sso/callback')
  )
}
