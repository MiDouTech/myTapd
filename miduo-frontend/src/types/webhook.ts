import type { PageQuery } from './common'

export interface WebhookConfigPageInput extends PageQuery {
  keyword?: string
  eventType?: string
  isActive?: number
}

export interface WebhookConfigOutput {
  id: number
  url: string
  secret?: string
  eventTypes: string[]
  isActive: number
  timeoutMs: number
  maxRetryTimes: number
  description?: string
  lastSuccessTime?: string
  lastFailTime?: string
  lastFailReason?: string
  createTime?: string
  updateTime?: string
}

export interface WebhookConfigCreateInput {
  url: string
  secret?: string
  eventTypes: string[]
  isActive: number
  timeoutMs: number
  maxRetryTimes: number
  description?: string
}

export type WebhookConfigUpdateInput = WebhookConfigCreateInput
