import request from '@/utils/request'

export interface UserApiKeyCreateInput {
  name: string
}

export interface UserApiKeyCreateOutput {
  id: number
  apiKey: string
}

export interface UserApiKeyListOutput {
  id: number
  name: string
  keyPrefixDisplay: string
  status: number
  lastUsedAt?: string
  createTime?: string
}

/**
 * 创建个人 API 密钥
 * 接口编号：API000509
 */
export function createUserApiKey(data: UserApiKeyCreateInput): Promise<UserApiKeyCreateOutput> {
  return request.post<UserApiKeyCreateOutput>('/user/api-key/create', data)
}

/**
 * 个人 API 密钥列表
 * 接口编号：API000510
 */
export function listUserApiKeys(): Promise<UserApiKeyListOutput[]> {
  return request.get<UserApiKeyListOutput[]>('/user/api-key/list')
}

/**
 * 禁用个人 API 密钥
 * 接口编号：API000511
 */
export function disableUserApiKey(id: number): Promise<void> {
  return request.put<void>(`/user/api-key/disable/${id}`)
}

/**
 * 删除个人 API 密钥
 * 接口编号：API000512
 */
export function deleteUserApiKey(id: number): Promise<void> {
  return request.del<void>(`/user/api-key/delete/${id}`)
}
