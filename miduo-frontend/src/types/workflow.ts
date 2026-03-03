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
}

export interface WorkflowDetailStateItem {
  code: string
  name: string
  type?: string
  slaAction?: string
}

export interface WorkflowDetailTransitionItem {
  from: string
  fromName?: string
  to: string
  toName?: string
  name?: string
  allowedRoles?: string[]
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
}

export interface HandlerGroupCreateInput {
  name: string
  description?: string
  skillTags?: string
  leaderId?: number
  memberIds: number[]
}
