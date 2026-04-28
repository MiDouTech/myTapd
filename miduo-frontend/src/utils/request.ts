import type { AxiosRequestConfig } from 'axios'
import axios from 'axios'

import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import type { ApiResult } from '@/types/common'

import { tryRefreshAndReplay } from '@/utils/authRefresh'

import { notifyError } from './feedback'
import { getAccessToken } from './storage'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

let isRedirecting = false

/** 公开页（如企微外链工单）不应因 401 被拉去登录，否则用户只看到跳转/白屏 */
function isNoLoginRedirectRoute(): boolean {
  const path = router.currentRoute.value.path
  return path.startsWith('/open/')
}

function redirectToLogin(): void {
  if (isNoLoginRedirectRoute() || isRedirecting || router.currentRoute.value.path === '/login') {
    return
  }
  isRedirecting = true
  const redirect = router.currentRoute.value.fullPath
  // Clear both in-memory store state and localStorage so the router guard
  // correctly treats the user as unauthenticated after the redirect.
  useAuthStore().clearLoginState()
  router
    .push({
      path: '/login',
      query: { redirect },
    })
    .finally(() => {
      isRedirecting = false
    })
}

const AUTH_RETRY_FLAG = '_ticketAuthRetried' as const

/** Access 过期时先试静默刷新，失败再跳转登录（否则 refresh 设再长也没用） */
async function handleUnauthorized(
  originalConfig: AxiosRequestConfig | undefined,
  replay: (cfg: AxiosRequestConfig) => Promise<unknown>,
): Promise<unknown> {
  if (isNoLoginRedirectRoute()) {
    return Promise.reject(new Error('Unauthorized'))
  }
  if (!originalConfig) {
    redirectToLogin()
    return Promise.reject(new Error('Unauthorized'))
  }
  if ((originalConfig as Record<string, unknown>)[AUTH_RETRY_FLAG]) {
    redirectToLogin()
    return Promise.reject(new Error('Unauthorized'))
  }
  ;(originalConfig as Record<string, unknown>)[AUTH_RETRY_FLAG] = true
  try {
    return await tryRefreshAndReplay(originalConfig, replay)
  } catch {
    redirectToLogin()
    return Promise.reject(new Error('Unauthorized'))
  }
}

service.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResult<unknown>
    if (typeof payload?.code === 'number') {
      if (payload.code === 200) {
        return payload.data
      }
      if (payload.code === 401) {
        return handleUnauthorized(response.config, (cfg) => service.request(cfg)).catch(() => {
          notifyError(payload.message || '请求失败')
          return Promise.reject(new Error(payload.message || 'Request failed'))
        })
      }
      notifyError(payload.message || '请求失败')
      return Promise.reject(new Error(payload.message || 'Request failed'))
    }
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      return handleUnauthorized(error.config, (cfg) => service.request(cfg)).catch(() => {
        const message = error.response?.data?.message || error.message || '网络异常'
        notifyError(message)
        return Promise.reject(error)
      })
    }
    const message = error.response?.data?.message || error.message || '网络异常'
    notifyError(message)
    return Promise.reject(error)
  },
)

function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, config)
}

function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config)
}

function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config)
}

function del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, config)
}

const request = {
  get,
  post,
  put,
  del,
}

export default request
