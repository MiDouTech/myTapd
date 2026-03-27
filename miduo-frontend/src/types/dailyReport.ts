export interface DailyReportTicketItem {
  id: number
  ticketNo: string
  title: string
  status: string
  statusLabel: string
  priority: string
  assigneeName: string
  categoryName: string
  severityLevel: string
}

export interface DailyReportSubSection {
  title: string
  count: number
  tickets: DailyReportTicketItem[]
}

export interface DailyReportSection {
  title: string
  count: number
  subSections: DailyReportSubSection[]
}

export interface DailyReportSummary {
  totalFeedbackCount: number
  pendingResolveCount: number
  tempResolvedCount: number
  resolvedCount: number
  suspendedCount: number
}

export interface DailyReportOutput {
  reportDate: string
  summary: DailyReportSummary
  pendingSection: DailyReportSection
  tempResolvedSection: DailyReportSection
  resolvedSection: DailyReportSection
  suspendedSection: DailyReportSection | null
  markdownContent: string
}

export interface DailyReportConfigOutput {
  enabled: boolean
  cron: string
  webhookUrls: string[]
  includeDefectDetail: boolean
  includeSuspended: boolean
}

export interface DailyReportConfigUpdateInput {
  enabled?: boolean
  cron?: string
  webhookUrls?: string[]
  includeDefectDetail?: boolean
  includeSuspended?: boolean
}
