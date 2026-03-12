import type { AxiosRequestConfig } from 'axios'
import axios from 'axios'

import router from '@/router'
import type { ApiResult } from '@/types/common'

import { notifyError } from './feedback'
import { clearAuthStorage, getAccessToken } from './storage'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

/** Axios instance for public endpoints that require no authentication and should never trigger login redirect */
const publicService = axios.create({
  timeout: 15000,
})

let isRedirecting = false

function redirectToLogin(): void {
  if (isRedirecting || router.currentRoute.value.path === '/login') {
    return
  }
  isRedirecting = true
  const redirect = router.currentRoute.value.fullPath
  clearAuthStorage()
  router
    .push({
      path: '/login',
      query: { redirect },
    })
    .finally(() => {
      isRedirecting = false
    })
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
        redirectToLogin()
      }
      notifyError(payload.message || '请求失败')
      return Promise.reject(new Error(payload.message || 'Request failed'))
    }
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      redirectToLogin()
      return Promise.reject(error)
    }
    const message = error.response?.data?.message || error.message || '网络异常'
    notifyError(message)
    return Promise.reject(error)
  },
)

publicService.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResult<unknown>
    if (typeof payload?.code === 'number') {
      if (payload.code === 200) {
        return payload.data
      }
      return Promise.reject(new Error(payload.message || 'Request failed'))
    }
    return response.data
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络异常'
    return Promise.reject(new Error(message))
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

function publicGet<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return publicService.get(url, config)
}

const request = {
  get,
  post,
  put,
  del,
  publicGet,
}

export default request
