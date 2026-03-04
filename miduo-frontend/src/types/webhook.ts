import type { PageQuery } from './common'

export interface WebhookConfigPageInput extends PageQuery {
  keyword?: string
  eventType?: string
  isActive?: number
}

export interface WebhookDispatchLogPageInput extends PageQuery {
  webhookConfigId?: number
  ticketId?: number
  eventType?: string
  status?: string
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
  createBy?: string
  updateBy?: string
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

export interface WebhookDispatchLogOutput {
  id: number
  webhookConfigId?: number
  eventType: string
  ticketId?: number
  requestUrl?: string
  attemptNo: number
  maxAttempts: number
  status: string
  responseCode?: number
  failReason?: string
  durationMs?: number
  dispatchTime?: string
  createTime?: string
}
