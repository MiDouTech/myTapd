import type { PageQuery } from './common'

export interface IntegrationAppPageInput extends PageQuery {
  keyword?: string
  status?: number
}

export interface IntegrationAppOutput {
  id: number
  appName: string
  appKey?: string
  appSecret?: string
  systemCode: string
  defaultCategoryId: number
  categoryMapping?: Record<string, number>
  callbackUrl?: string
  allowedOrigins?: string
  permissions?: string
  status: number
  createTime?: string
  updateTime?: string
}

export interface IntegrationAppCreateInput {
  appName: string
  systemCode: string
  defaultCategoryId: number
  categoryMapping?: Record<string, number>
  callbackUrl?: string
  callbackSecret?: string
  allowedOrigins?: string
  permissions?: string
}

export interface IntegrationAppUpdateInput {
  appName: string
  defaultCategoryId: number
  categoryMapping?: Record<string, number>
  callbackUrl?: string
  callbackSecret?: string
  allowedOrigins?: string
  permissions?: string
  status: number
}

export interface IntegrationAppRotateSecretOutput {
  appKey: string
  appSecret: string
}
