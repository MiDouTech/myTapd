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

export interface TicketBugCustomerInfoInput {
  merchantNo?: string
  companyName?: string
  merchantAccount?: string
  problemDesc?: string
  expectedResult?: string
  sceneCode?: string
  problemScreenshot?: string
}

export interface TicketBugTestInfoInput {
  reproduceEnv?: string
  reproduceSteps?: string
  actualResult?: string
  impactScope?: string
  severityLevel?: string
  moduleName?: string
  testRemark?: string
}

export interface TicketModuleOutput {
  id: number
  name: string
  sort: number
}

export interface TicketModuleInput {
  name: string
}

export interface TicketBugDevInfoInput {
  rootCause?: string
  fixSolution?: string
  gitBranch?: string
  impactAssessment?: string
  devRemark?: string
}

export interface TicketTimeTrackItem {
  id: number
  userId?: number
  userName?: string
  userRole?: string
  action?: string
  actionLabel?: string
  fromStatus?: string
  toStatus?: string
  fromUserId?: number
  fromUserName?: string
  toUserId?: number
  toUserName?: string
  remark?: string
  isFirstRead?: boolean
  timestamp?: string
}

export interface TicketTimeTrackOutput {
  ticketId: number
  tracks?: TicketTimeTrackItem[]
}

export interface TicketNodeDurationItem {
  id: number
  nodeName?: string
  assigneeId?: number
  assigneeName?: string
  assigneeRole?: string
  arriveAt?: string
  firstReadAt?: string
  startProcessAt?: string
  leaveAt?: string
  waitDurationSec?: number
  processDurationSec?: number
  totalDurationSec?: number
}

export interface TicketNodeDurationOutput {
  ticketId: number
  nodes?: TicketNodeDurationItem[]
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

export interface ImageUploadOutput {
  url: string
  fileName?: string
  fileSize?: number
  fileType?: string
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
  bugCustomerInfo?: TicketBugCustomerInfoInput
  bugTestInfo?: TicketBugTestInfoInput
  bugDevInfo?: TicketBugDevInfoInput
  isFollowed?: boolean
  /** 缺陷维度摘要信息（从 Bug 简报关联获取，工单未关联简报时为 null） */
  bugSummaryInfo?: BugSummaryInfoOutput
}

/** 单个字段变更明细 */
export interface BugFieldChangeItem {
  fieldName: string
  fieldLabel: string
  oldValue: string | null
  oldLabel: string | null
  newValue: string | null
  newLabel: string | null
}

/** 变更历史条目（API000501） */
export interface BugChangeHistoryOutput {
  id: number
  seq: number
  changeTime: string
  changeByUserId: number
  changeByUserName: string
  changeByAvatar: string | null
  changeType: string
  changeTypeLabel: string
  fields: BugFieldChangeItem[]
}

/** 缺陷维度摘要信息（从 Bug 简报关联获取） */
export interface BugSummaryInfoOutput {
  bugReportId: number | null
  bugReportNo: string | null
  defectCategory: string | null
  defectCategoryLabel: string | null
  isValidReport: string | null
  isValidReportLabel: string | null
  responsibleUserName: string | null
  isOverdue: boolean | null
}

/** 变更历史查询参数 */
export interface BugChangeHistoryQuery {
  changeType?: string
  fieldName?: string
}

/** 工单公开详情（无需登录，外网访问） */
export interface TicketPublicCommentOutput {
  id: number
  userName?: string
  content?: string
  type?: string
  createTime?: string
}

export interface TicketPublicDetailOutput {
  id: number
  ticketNo: string
  title?: string
  description?: string
  categoryName?: string
  categoryFullPath?: string
  priority?: string
  priorityLabel?: string
  status?: string
  statusLabel?: string
  creatorName?: string
  assigneeName?: string
  source?: string
  sourceLabel?: string
  expectedTime?: string
  resolvedAt?: string
  closedAt?: string
  createTime?: string
  updateTime?: string
  comments?: TicketPublicCommentOutput[]
}
