export interface AlertRuleMappingCreateInput {
  ruleName: string
  matchMode: string
  categoryId: number
  priorityP1?: string
  priorityP2?: string
  priorityP3?: string
  assigneeId?: number | null
  dedupWindowMinutes?: number
  enabled?: boolean
}

export interface AlertRuleMappingUpdateInput {
  id: number
  ruleName?: string
  matchMode?: string
  categoryId?: number
  priorityP1?: string
  priorityP2?: string
  priorityP3?: string
  assigneeId?: number | null
  dedupWindowMinutes?: number
  enabled?: boolean
}

export interface AlertRuleMappingOutput {
  id: number
  ruleName: string
  matchMode: string
  categoryId: number
  categoryName: string
  priorityP1: string
  priorityP2: string
  priorityP3: string
  assigneeId: number | null
  assigneeName: string
  dedupWindowMinutes: number
  enabled: boolean
  createTime: string
  updateTime: string
}

export interface AlertRuleMappingPageInput {
  pageNum?: number
  pageSize?: number
  ruleName?: string
  enabled?: boolean | null
}

export interface AlertEventLogOutput {
  id: number
  eventHash: string
  ruleId: number
  ruleName: string
  severity: number
  targetIdent: string
  triggerValue: string
  triggerTime: string
  isRecovered: boolean
  ticketId: number | null
  ticketNo: string
  processResult: string
  createTime: string
}

export interface AlertEventLogPageInput {
  pageNum?: number
  pageSize?: number
  ruleName?: string
  targetIdent?: string
  processResult?: string
}

export interface AlertTokenOutput {
  token: string
  webhookUrl: string
}
