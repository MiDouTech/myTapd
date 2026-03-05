export interface WecomGroupBindingListOutput {
  id: number
  chatId: string
  chatName?: string
  defaultCategoryId?: number
  defaultCategoryName?: string
  webhookUrl?: string
  isActive: number
  createBy?: string
  updateBy?: string
  createTime?: string
  updateTime?: string
}

export interface WecomGroupBindingCreateInput {
  chatId: string
  chatName?: string
  defaultCategoryId?: number
  webhookUrl?: string
  isActive?: number
}

export interface WecomGroupBindingUpdateInput {
  chatName?: string
  defaultCategoryId?: number
  webhookUrl?: string
  isActive?: number
}

export interface WecomConfigOutput {
  id?: number
  corpId?: string
  agentId?: string
  corpSecretMasked?: string
  apiBaseUrl?: string
  connectTimeoutMs?: number
  readTimeoutMs?: number
  scheduleEnabled?: boolean
  scheduleCron?: string
  retryCount?: number
  batchSize?: number
  enabled?: boolean
  updateTime?: string
}

export interface WecomConfigUpdateInput {
  corpId: string
  agentId: string
  corpSecret: string
  apiBaseUrl: string
  connectTimeoutMs: number
  readTimeoutMs: number
  scheduleEnabled: boolean
  scheduleCron?: string
  retryCount: number
  batchSize: number
  enabled: boolean
}

export interface WecomConnectionTestOutput {
  success: boolean
  message: string
  departmentCount: number
}
