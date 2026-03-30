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
  callbackTokenMasked?: string
  callbackAesKeyMasked?: string
}

export interface WecomConfigUpdateInput {
  corpId: string
  agentId: string
  /** 已有配置时留空表示不修改 */
  corpSecret: string
  apiBaseUrl: string
  connectTimeoutMs: number
  readTimeoutMs: number
  scheduleEnabled: boolean
  scheduleCron?: string
  retryCount: number
  batchSize: number
  enabled: boolean
  callbackToken?: string
  callbackAesKey?: string
}

export interface WecomConnectionTestOutput {
  success: boolean
  message: string
  departmentCount: number
}

// NLP关键词管理相关类型
export interface NlpKeywordListOutput {
  id: number
  keyword: string
  matchType: number
  matchTypeLabel?: string
  targetValue: string
  confidence: number
  sortOrder: number
  isActive: number
  createTime?: string
  updateTime?: string
}

export interface NlpKeywordCreateInput {
  keyword: string
  matchType: number
  targetValue: string
  confidence: number
  sortOrder: number
  isActive: number
}

export interface NlpKeywordUpdateInput {
  keyword: string
  matchType: number
  targetValue: string
  confidence: number
  sortOrder: number
  isActive: number
}

// NLP解析日志相关类型
export interface NlpLogPageInput {
  pageNum?: number
  pageSize?: number
  parseType?: string
  chatId?: string
  fromWecomUserid?: string
  minConfidence?: number
  startTime?: string
  endTime?: string
}

export interface NlpLogPageOutput {
  id: number
  chatId?: string
  msgId?: string
  fromWecomUserid?: string
  rawMessage?: string
  status?: string
  parseType?: string
  nlpConfidence?: number
  ticketId?: number
  errorMsg?: string
  createTime?: string
}
