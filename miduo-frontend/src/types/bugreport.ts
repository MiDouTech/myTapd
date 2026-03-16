import type { PageQuery } from './common'

export interface BugReportPageInput extends PageQuery {
  reportNo?: string
  status?: string
  defectCategory?: string
  reviewerId?: number
  responsibleUserId?: number
  ticketId?: number
  createTimeStart?: string
  createTimeEnd?: string
}

export interface BugReportPageOutput {
  id: number
  reportNo: string
  status?: string
  statusLabel?: string
  defectCategory?: string
  severityLevel?: string
  reviewerId?: number
  reviewerName?: string
  submittedAt?: string
  createTime?: string
  updateTime?: string
}

export interface BugReportCreateInput {
  problemDesc?: string
  logicCauseLevel1?: string
  logicCauseLevel2?: string
  logicCauseDetail?: string
  defectCategory?: string
  introducedProject?: string
  startDate?: string
  resolveDate?: string
  tempResolveDate?: string
  solution?: string
  tempSolution?: string
  impactScope?: string
  severityLevel?: string
  reporterId?: number
  reviewerId?: number
  remark?: string
  ticketIds: number[]
  responsibleUserIds?: number[]
  autoPrefill?: boolean
}

export interface BugReportUpdateInput {
  problemDesc?: string
  logicCauseLevel1?: string
  logicCauseLevel2?: string
  logicCauseDetail?: string
  defectCategory?: string
  introducedProject?: string
  startDate?: string
  resolveDate?: string
  tempResolveDate?: string
  solution?: string
  tempSolution?: string
  impactScope?: string
  severityLevel?: string
  reporterId?: number
  reviewerId?: number
  remark?: string
  ticketIds?: number[]
  responsibleUserIds?: number[]
  autoPrefill?: boolean
}

export interface BugReportSubmitInput {
  reviewerId?: number
  remark?: string
}

export interface BugReportReviewInput {
  reviewComment: string
}

export interface BugReportResponsibleOutput {
  userId: number
  userName?: string
}

export interface BugReportRelatedTicketOutput {
  ticketId: number
  ticketNo?: string
  title?: string
  status?: string
  isAutoCreated?: number
}

export interface BugReportLogOutput {
  id: number
  userId?: number
  userName?: string
  action?: string
  oldStatus?: string
  newStatus?: string
  remark?: string
  createTime?: string
}

export interface BugReportAttachmentOutput {
  id: number
  fileName?: string
  filePath?: string
  fileSize?: number
  uploadedBy?: number
  uploadedByName?: string
  createTime?: string
}

export interface BugReportDetailOutput {
  id: number
  reportNo: string
  status?: string
  statusLabel?: string
  problemDesc?: string
  logicCauseLevel1?: string
  logicCauseLevel2?: string
  logicCauseDetail?: string
  defectCategory?: string
  introducedProject?: string
  startDate?: string
  resolveDate?: string
  tempResolveDate?: string
  solution?: string
  tempSolution?: string
  impactScope?: string
  severityLevel?: string
  reporterId?: number
  reporterName?: string
  reviewerId?: number
  reviewerName?: string
  remark?: string
  submittedAt?: string
  reviewedAt?: string
  reviewComment?: string
  createdByUserId?: number
  createTime?: string
  updateTime?: string
  responsibleUsers?: BugReportResponsibleOutput[]
  tickets?: BugReportRelatedTicketOutput[]
  logs?: BugReportLogOutput[]
  attachments?: BugReportAttachmentOutput[]
}

export interface BugReportDistributionItem {
  name: string
  count: number
  /** 占比百分比，如 35.5 表示 35.5% */
  rate?: number
}

export interface BugReportResponsibleStatItem {
  userId: number
  userName?: string
  count: number
}

export interface BugReportStatisticsInput {
  createTimeStart?: string
  createTimeEnd?: string
}

export interface BugReportStatisticsOutput {
  logicCauseDistribution: BugReportDistributionItem[]
  defectCategoryDistribution: BugReportDistributionItem[]
  introducedProjectTop: BugReportDistributionItem[]
  responsibleStatistics: BugReportResponsibleStatItem[]
  timelyCount: number
  totalCount: number
  timelyRate: number
}

export interface LogicCauseTreeOutput {
  id: number
  name: string
  children?: LogicCauseTreeOutput[]
}

export interface DefectCategoryOutput {
  id: number
  name: string
  description?: string
}
