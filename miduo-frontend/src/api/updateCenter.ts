import request from '@/utils/request'
import type {
  ChangelogReleaseOutput,
  CurrentWeekOutput,
  GitHubLogsOutput,
  ReleasesOutput,
  UpdateCenterGitHubQuery,
  WeeklyReportDetailOutput,
  WeeklyReportsOutput,
} from '@/types/updateCenter'

/**
 * 查询待发布更新
 * 接口编号：API000521
 * 产品文档功能：管理 / 更新中心 - 待发布更新列表
 */
export function getUpdateCenterCurrentWeek(params?: {
  daysLimit?: number
  daysOffset?: number
  force?: boolean
}): Promise<CurrentWeekOutput> {
  return request.get<CurrentWeekOutput>('/update-center/current-week', { params })
}

/**
 * 查询已发布更新
 * 接口编号：API000522
 * 产品文档功能：管理 / 更新中心 - 已发布版本列表
 */
export function getUpdateCenterReleases(params?: {
  limit?: number
  summary?: boolean
  force?: boolean
}): Promise<ReleasesOutput> {
  return request.get<ReleasesOutput>('/update-center/releases', { params })
}

/**
 * 查询指定版本更新详情
 * 接口编号：API000523
 * 产品文档功能：管理 / 更新中心 - 版本更新详情
 */
export function getUpdateCenterReleaseDetail(
  version: string,
  force = false,
): Promise<ChangelogReleaseOutput> {
  return request.get<ChangelogReleaseOutput>(
    `/update-center/releases/detail/${encodeURIComponent(version)}`,
    {
      params: force ? { force: true } : undefined,
    },
  )
}

/**
 * 查询Git提交日志
 * 接口编号：API000524
 * 产品文档功能：管理 / 更新中心 - Git提交记录
 */
export function getUpdateCenterGitHubLogs(
  params?: UpdateCenterGitHubQuery,
): Promise<GitHubLogsOutput> {
  return request.get<GitHubLogsOutput>('/update-center/github-logs', { params })
}

/**
 * 查询周报列表
 * 接口编号：API000525
 * 产品文档功能：管理 / 更新中心 - 周报列表
 */
export function getUpdateCenterWeeklyReports(params?: {
  force?: boolean
}): Promise<WeeklyReportsOutput> {
  return request.get<WeeklyReportsOutput>('/update-center/weekly-reports', { params })
}

/**
 * 查询周报详情
 * 接口编号：API000526
 * 产品文档功能：管理 / 更新中心 - 周报详情
 */
export function getUpdateCenterWeeklyReportDetail(
  fileName: string,
  force = false,
): Promise<WeeklyReportDetailOutput> {
  return request.get<WeeklyReportDetailOutput>(
    `/update-center/weekly-reports/detail/${encodeURIComponent(fileName)}`,
    {
      params: force ? { force: true } : undefined,
    },
  )
}
