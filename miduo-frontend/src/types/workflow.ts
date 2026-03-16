export interface WorkflowListOutput {
  id: number
  name: string
  mode: string
  modeLabel?: string
  description?: string
  isBuiltin?: number
  isActive: number
  stateCount: number
  transitionCount: number
  createTime?: string
  updateTime?: string
}

export interface WorkflowDetailStateItem {
  code: string
  name: string
  type?: string
  slaAction?: string
  order?: number
}

export interface WorkflowDetailTransitionItem {
  id?: string
  from: string
  fromName?: string
  to: string
  toName?: string
  name?: string
  allowedRoles?: string[]
  requireRemark?: boolean
  allowTransfer?: boolean
  isReturn?: boolean
}

export interface WorkflowDetailOutput {
  id: number
  name: string
  mode: string
  modeLabel?: string
  description?: string
  isBuiltin?: number
  isActive: number
  states: WorkflowDetailStateItem[]
  transitions: WorkflowDetailTransitionItem[]
  createTime?: string
  updateTime?: string
}

export interface HandlerGroupMemberOutput {
  userId: number
  userName?: string
}

export interface HandlerGroupListOutput {
  id: number
  name: string
  description?: string
  skillTags?: string
  isActive: number
  leaderId?: number
  leaderName?: string
  memberCount: number
  members: HandlerGroupMemberOutput[]
  createTime?: string
  updateTime?: string
}

export interface HandlerGroupCreateInput {
  name: string
  description?: string
  skillTags?: string
  leaderId?: number
  memberIds: number[]
}

// ---- 工单流转相关类型 ----

/** 可用操作条目（getAvailableActions 返回） */
export interface TicketActionItem {
  transitionId: string
  targetStatus: string
  targetStatusName: string
  actionName: string
  isReturn?: boolean
  requireRemark?: boolean
  allowTransfer?: boolean
  allowedRoles?: string[]
}

/** 工作流状态条目（全状态列表，用于进度展示） */
export interface TicketStatusItem {
  code: string
  name: string
  type: string
  isCurrent?: boolean
  order?: number
}

/** 获取可用操作接口返回值 */
export interface AvailableActionOutput {
  ticketId: number
  currentStatus: string
  currentStatusName: string
  isTerminal?: boolean
  actions: TicketActionItem[]
  allStatuses: TicketStatusItem[]
}

/** 状态流转请求 */
export interface TransitInput {
  transitionId?: string
  targetStatus?: string
  remark?: string
  newAssigneeId?: number
}

/** 转派请求 */
export interface TransferInput {
  targetUserId: number
  reason?: string
}

/** 退回请求 */
export interface ReturnInput {
  targetStatus?: string
  reason: string
}

/** 工单流转历史条目 */
export interface TicketFlowRecordOutput {
  id: number
  ticketId: number
  ticketNo: string
  flowType: string
  flowTypeLabel?: string
  transitionId?: string
  transitionName?: string
  fromStatus: string
  fromStatusName?: string
  toStatus: string
  toStatusName?: string
  fromAssigneeId?: number
  fromAssigneeName?: string
  toAssigneeId?: number
  toAssigneeName?: string
  operatorId: number
  operatorName?: string
  operatorRole: string
  remark?: string
  createTime: string
}

