export interface SlaPolicyOutput {
  id: number
  name: string
  priority: string
  priorityLabel?: string
  responseTime: number
  resolveTime: number
  warningPct: number
  criticalPct: number
  description?: string
  isActive: number
  createTime?: string
  updateTime?: string
}

export interface SlaPolicyCreateInput {
  name: string
  priority: string
  responseTime: number
  resolveTime: number
  warningPct?: number
  criticalPct?: number
  description?: string
}

export interface SlaPolicyUpdateInput {
  id: number
  name?: string
  priority?: string
  responseTime?: number
  resolveTime?: number
  warningPct?: number
  criticalPct?: number
  description?: string
  isActive?: number
}
