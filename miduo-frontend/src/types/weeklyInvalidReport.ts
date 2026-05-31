export interface WeeklyInvalidReporterItem {
  reporterId: number | null
  reporterName: string
  invalidCount: number
}

export interface WeeklyInvalidTicketItem {
  id: number
  ticketNo: string
  title: string
  reporterId: number | null
  reporterName: string
  closedTime: string | null
}

export interface WeeklyInvalidReportSummary {
  invalidTotalCount: number
  reporterCount: number
  detailDisplayCount: number
  detailLimitCount: number
}

export interface WeeklyInvalidReportOutput {
  reportDate: string
  weekRangeLabel: string
  summary: WeeklyInvalidReportSummary
  reporterStats: WeeklyInvalidReporterItem[]
  ticketDetails: WeeklyInvalidTicketItem[]
  markdownContent: string
}

export interface WeeklyInvalidReportConfigOutput {
  enabled: boolean
  cronList: string[]
  webhookUrls: string[]
  statCategoryIds: number[]
  maxDetailCount: number
  timezone: string
}

export interface WeeklyInvalidReportConfigUpdateInput {
  enabled?: boolean
  cronList?: string[]
  webhookUrls?: string[]
  statCategoryIds?: number[]
  maxDetailCount?: number
  timezone?: string
}
