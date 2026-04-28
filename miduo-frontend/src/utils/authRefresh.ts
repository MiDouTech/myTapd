import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'

import type { ApiResult } from '@/types/common'
import type { LoginOutput } from '@/types/auth'
import { getRefreshToken } from '@/utils/storage'

const REFRESH_PATH = '/auth/refresh'

/** 使用裸 axios，避免走带拦截器的实例造成死循环 */
const refreshClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

let isRefreshing = false
type QueueItem = {
  config: AxiosRequestConfig
  resolve: (value: unknown) => void
  reject: (reason?: unknown) => void
}
let failedQueue: QueueItem[] = []

function processQueue(error: Error | null, token: string | null): void {
  const q = failedQueue
  failedQueue = []
  for (const { config, resolve, reject } of q) {
    if (error) {
      reject(error)
    } else if (token) {
      config.headers = config.headers ?? {}
      config.headers.Authorization = `Bearer ${token}`
      refreshClient.request(config).then(resolve).catch(reject)
    }
  }
}

function isRefreshOrLoginRequest(config: AxiosRequestConfig): boolean {
  const path = `${config.baseURL ?? ''}${config.url ?? ''}`
  return (
    path.includes(REFRESH_PATH) ||
    path.includes('/auth/wecom/login') ||
    path.includes('/auth/dev/login') ||
    path.includes('/auth/local/login') ||
    path.includes('/auth/sso/callback')
  )
}

/**
 * Access Token 过期时：用 refresh 换新 token 并重放原请求。
 * 并发 401 时只发一次 refresh，其余进入队列。
 */
export async function tryRefreshAndReplay(
  config: AxiosRequestConfig,
  replay: (cfg: AxiosRequestConfig) => Promise<unknown>,
): Promise<unknown> {
  if (isRefreshOrLoginRequest(config)) {
    return Promise.reject(new Error('skip silent refresh'))
  }

  const refresh = getRefreshToken()
  if (!refresh) {
    return Promise.reject(new Error('no refresh token'))
  }

  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      failedQueue.push({ config, resolve, reject })
    })
  }

  isRefreshing = true
  try {
    const { data } = await refreshClient.post<ApiResult<LoginOutput>>(REFRESH_PATH, {
      refreshToken: refresh,
    })
    if (data.code !== 200 || !data.data) {
      throw new Error(data.message || 'Refresh failed')
    }
    const { useAuthStore } = await import('@/stores/auth')
    useAuthStore().setLoginState(data.data)
    const newToken = data.data.accessToken
    processQueue(null, newToken)
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${newToken}`
    return replay(config)
  } catch (e) {
    processQueue(e instanceof Error ? e : new Error('Refresh failed'), null)
    return Promise.reject(e)
  } finally {
    isRefreshing = false
  }
}
