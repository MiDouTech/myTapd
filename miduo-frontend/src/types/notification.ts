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
