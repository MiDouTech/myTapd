import type { PageQuery } from './common'

export interface NotificationPageInput extends PageQuery {
  type?: string
  isRead?: number
}

export interface NotificationOutput {
  id: number
  userId?: number
  ticketId?: number
  reportId?: number
  type?: string
  typeLabel?: string
  channel?: string
  channelLabel?: string
  title?: string
  content?: string
  isRead?: number
  readAt?: string
  createTime?: string
}

export interface NotificationUnreadCountOutput {
  unreadCount: number
}

export interface NotificationPreferenceOutput {
  id?: number
  userId?: number
  eventType: string
  eventTypeLabel?: string
  siteEnabled: number
  wecomEnabled: number
  emailEnabled: number
}

export interface NotificationPreferenceUpdateItem {
  eventType: string
  siteEnabled?: number
  wecomEnabled?: number
  emailEnabled?: number
}

export interface NotificationPreferenceUpdateInput {
  items: NotificationPreferenceUpdateItem[]
}
