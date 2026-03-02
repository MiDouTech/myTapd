export interface DashboardOverviewOutput {
  pendingAcceptCount: number
  processingCount: number
  suspendedCount: number
  completedCount: number
  slaBreachedCount: number
  totalCount: number
}

export interface DashboardTrendPointOutput {
  day: string
  createdCount: number
  closedCount: number
  backlogCount: number
}

export interface DashboardCategoryDistributionOutput {
  categoryId?: number
  categoryName: string
  ticketCount: number
  percentage: number
}

export interface DashboardEfficiencyOutput {
  avgResponseMinutes: number
  avgResolveMinutes: number
  completedCount: number
  totalCount: number
  completionRate: number
}

export interface DashboardSlaAchievementOutput {
  totalCount: number
  achievedCount: number
  breachedCount: number
  achievementRate: number
}

export interface DashboardWorkloadOutput {
  assigneeId?: number
  assigneeName?: string
  totalCount: number
  processingCount: number
  completedCount: number
}
