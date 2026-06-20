export type UpdateCenterSource = 'local' | 'github' | 'none'

export interface ChangelogEntryOutput {
  type: string
  module: string
  description: string
}

export interface ChangelogFragmentOutput {
  fileName: string
  date: string
  entries: ChangelogEntryOutput[]
}

export interface CurrentWeekOutput {
  weekStart: string
  weekEnd: string
  dataSourceAvailable: boolean
  source: UpdateCenterSource
  fetchedAt: string
  totalDays: number
  totalEntries: number
  daysOffset: number
  hasMore: boolean
  fragments: ChangelogFragmentOutput[]
}

export interface ChangelogDayOutput {
  date: string
  commitTimeUtc?: string | null
  entries: ChangelogEntryOutput[]
}

export interface ChangelogReleaseOutput {
  version: string
  releaseDate: string | null
  entryCount: number
  sourceScope: string
  highlights: string[]
  entriesOmitted: boolean
  days: ChangelogDayOutput[]
}

export interface ReleasesOutput {
  dataSourceAvailable: boolean
  source: UpdateCenterSource
  fetchedAt: string
  totalReleases: number
  totalEntries: number
  releases: ChangelogReleaseOutput[]
}

export interface GitHubCoAuthorOutput {
  name: string
  matchedUsername?: string | null
  matchedDisplayName?: string | null
}

export interface GitHubLogEntryOutput {
  sha: string
  shortSha: string
  message: string
  authorName: string
  authorAvatarUrl?: string | null
  commitTimeUtc: string
  htmlUrl?: string | null
  matchedUsername?: string | null
  matchedDisplayName?: string | null
  coAuthors?: GitHubCoAuthorOutput[]
}

export interface GitHubLogsOutput {
  dataSourceAvailable: boolean
  source: UpdateCenterSource
  fetchedAt: string
  totalCount: number
  repoTotalCommitCount?: number | null
  hasMore: boolean
  nextCursor?: string | null
  logs: GitHubLogEntryOutput[]
}

export interface UpdateCenterGitHubQuery {
  limit?: number
  before?: string | null
  force?: boolean
}

export interface WeeklyReportSummaryOutput {
  fileName: string
  title: string
  reportWeek?: string | null
  period?: string | null
  updatedAt?: string | null
}

export interface WeeklyReportsOutput {
  dataSourceAvailable: boolean
  source: UpdateCenterSource
  fetchedAt: string
  totalReports: number
  reports: WeeklyReportSummaryOutput[]
}

export interface WeeklyReportDetailOutput extends WeeklyReportSummaryOutput {
  content: string
  dataSourceAvailable: boolean
  source: UpdateCenterSource
  fetchedAt: string
}
