import type { AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
import axios from 'axios'

import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import type { ApiResult } from '@/types/common'

import { isAuthRefreshExcludedUrl, refreshAccessToken } from './authRefresh'
import { notifyError } from './feedback'
import { getAccessToken } from './storage'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

type RetryableConfig = InternalAxiosRequestConfig & { _retry?: boolean }

let isRedirecting = false

function redirectToLogin(): void {
  if (isRedirecting || router.currentRoute.value.path === '/login') {
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

async function tryRefreshAndRetry(config: RetryableConfig): Promise<unknown> {
  config._retry = true
  const loginOutput = await refreshAccessToken()
  useAuthStore().setLoginState(loginOutput)
  const token = getAccessToken()
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return service.request(config)
}

service.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  async (response) => {
    const payload = response.data as ApiResult<unknown>
    if (typeof payload?.code === 'number') {
      if (payload.code === 200) {
        return payload.data
      }
      if (payload.code === 401) {
        const config = response.config as RetryableConfig
        const url = config.url ?? ''
        if (!config._retry && !isAuthRefreshExcludedUrl(url)) {
          try {
            return await tryRefreshAndRetry(config)
          } catch {
            redirectToLogin()
            return Promise.reject(new Error(payload.message || 'Request failed'))
          }
        }
        redirectToLogin()
        return Promise.reject(new Error(payload.message || 'Request failed'))
      }
      notifyError(payload.message || '请求失败')
      return Promise.reject(new Error(payload.message || 'Request failed'))
    }
    return response.data
  },
  async (error: AxiosError<ApiResult<unknown>>) => {
    const originalRequest = error.config as RetryableConfig | undefined
    const url = originalRequest?.url ?? ''
    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !isAuthRefreshExcludedUrl(url)
    ) {
      try {
        return await tryRefreshAndRetry(originalRequest)
      } catch {
        redirectToLogin()
        return Promise.reject(error)
      }
    }
    if (error.response?.status === 401) {
      redirectToLogin()
      return Promise.reject(error)
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
