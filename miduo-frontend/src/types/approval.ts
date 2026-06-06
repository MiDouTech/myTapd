// 工单审批任务相关类型定义
// 对应后端 TicketApprovalAppService 的输出

export interface ApprovalTaskItem {
  taskId: number
  assigneeId: number
  assigneeName: string
  taskStatus: string
  taskStatusLabel: string
  remark: string | null
  operateTime: string | null
  dueTime: string | null
  sortOrder: number
}

export interface ApprovalNodeGroup {
  nodeKey: string
  nodeName: string
  approveMode: string
  tasks: ApprovalTaskItem[]
}

export interface ApprovalRecordItem {
  recordId: number
  taskId: number
  nodeKey: string
  nodeName: string
  actionType: string
  actionLabel: string
  operatorId: number
  operatorName: string
  remark: string | null
  targetAssigneeId: number | null
  targetAssigneeName: string | null
  createTime: string
}

export interface ApprovalTaskOutput {
  ticketId: number
  nodes: ApprovalNodeGroup[]
  records: ApprovalRecordItem[]
  hasPendingTask: boolean
  myPendingTaskId: number | null
}

export interface ApprovalActionInput {
  taskId: number
  actionType: 'approve' | 'reject' | 'transfer'
  remark?: string
  targetAssigneeId?: number
}

export interface ApprovalPendingItem {
  taskId: number
  ticketId: number
  ticketNo: string | null
  ticketTitle: string | null
  ticketStatus: string | null
  ticketStatusLabel: string | null
  creatorName: string | null
  nodeName: string
  approveMode: string
  waitMinutes: number | null
  createTime: string
  dueTime: string | null
  isOverdue: boolean
}

export interface ApprovalPendingListOutput {
  totalCount: number
  items: ApprovalPendingItem[]
}

export const APPROVAL_MODE_LABELS: Record<string, string> = {
  single: '单人审批',
  countersign: '会签',
  orsign: '或签',
  sequential: '依次审批'
}

export const APPROVAL_TASK_STATUS_LABELS: Record<string, string> = {
  pending: '待审批',
  waiting: '等待中',
  approved: '已通过',
  rejected: '已驳回',
  transferred: '已转交',
  skipped: '已跳过'
}

export const APPROVAL_TASK_STATUS_TYPES: Record<string, string> = {
  pending: 'warning',
  waiting: 'info',
  approved: 'success',
  rejected: 'danger',
  transferred: 'info',
  skipped: ''
}
