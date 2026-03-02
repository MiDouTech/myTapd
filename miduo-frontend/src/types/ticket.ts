import type { PageQuery } from './common'

export type TicketView = 'my_created' | 'my_todo' | 'my_participated' | 'my_followed' | 'all'

export interface TicketPageInput extends PageQuery {
  view?: TicketView
  ticketNo?: string
  title?: string
  categoryId?: number
  status?: string
  priority?: string
  creatorId?: number
  assigneeId?: number
  createTimeStart?: string
  createTimeEnd?: string
}

export interface TicketListOutput {
  id: number
  ticketNo: string
  title: string
  categoryId?: number
  categoryName?: string
  priority?: string
  priorityLabel?: string
  status?: string
  statusLabel?: string
  creatorId?: number
  creatorName?: string
  assigneeId?: number
  assigneeName?: string
  source?: string
  sourceLabel?: string
  expectedTime?: string
  createTime?: string
  updateTime?: string
  resolvedAt?: string
  closedAt?: string
}

export interface TicketCreateInput {
  title: string
  description?: string
  categoryId: number
  priority: string
  expectedTime?: string
  assigneeId?: number
  source?: string
  sourceChatId?: string
  customFields?: Record<string, string>
}

export interface TicketAssignInput {
  assigneeId: number
  remark?: string
}

export interface TicketProcessInput {
  targetStatus: string
  targetUserId?: number
  remark?: string
}

export interface TicketCloseInput {
  remark?: string
}

export interface TicketAttachmentOutput {
  id: number
  fileName: string
  filePath?: string
  fileSize?: number
  fileType?: string
  uploadedBy?: number
  uploadedByName?: string
  createTime?: string
}

export interface TicketCommentOutput {
  id: number
  userId?: number
  userName?: string
  userAvatar?: string
  content?: string
  type?: string
  createTime?: string
}

export interface TicketLogOutput {
  id: number
  userId?: number
  userName?: string
  action?: string
  actionLabel?: string
  oldValue?: string
  newValue?: string
  remark?: string
  createTime?: string
}

export interface TicketBugReportOutput {
  id: number
  reportNo?: string
  status?: string
  statusLabel?: string
  isAutoCreated?: number
  createTime?: string
}

export interface TicketDetailOutput {
  id: number
  ticketNo: string
  title: string
  description?: string
  categoryId?: number
  categoryName?: string
  categoryFullPath?: string
  templateId?: number
  templateName?: string
  workflowId?: number
  priority?: string
  priorityLabel?: string
  status?: string
  statusLabel?: string
  creatorId?: number
  creatorName?: string
  assigneeId?: number
  assigneeName?: string
  source?: string
  sourceLabel?: string
  expectedTime?: string
  resolvedAt?: string
  closedAt?: string
  createTime?: string
  updateTime?: string
  customFields?: Record<string, string>
  attachments?: TicketAttachmentOutput[]
  comments?: TicketCommentOutput[]
  logs?: TicketLogOutput[]
  bugReports?: TicketBugReportOutput[]
  isFollowed?: boolean
}
