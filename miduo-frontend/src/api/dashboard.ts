import request from '@/utils/request'
import type {
  DashboardCategoryDistributionOutput,
  DashboardEfficiencyOutput,
  DashboardOverviewOutput,
  DashboardSlaAchievementOutput,
  DashboardTrendPointOutput,
  DashboardWorkloadOutput,
} from '@/types/dashboard'

/**
 * 工单概览仪表盘
 * 接口编号：API000405
 * 产品文档功能：4.9 数据看板与报表 - 工单概览
 */
export function getDashboardOverview(): Promise<DashboardOverviewOutput> {
  return request.get<DashboardOverviewOutput>('/dashboard/overview')
}

/**
 * 工单趋势统计
 * 接口编号：API000406
 * 产品文档功能：4.9 数据看板与报表 - 工单趋势（新建/关闭/积压）
 */
export function getDashboardTrend(params?: { days?: number }): Promise<DashboardTrendPointOutput[]> {
  return request.get<DashboardTrendPointOutput[]>('/dashboard/trend', { params })
}

/**
 * 分类分布统计
 * 接口编号：API000407
 * 产品文档功能：4.9 数据看板与报表 - 分类分布
 */
export function getDashboardCategoryDistribution(): Promise<DashboardCategoryDistributionOutput[]> {
  return request.get<DashboardCategoryDistributionOutput[]>('/dashboard/category-distribution')
}

/**
 * 处理效率统计
 * 接口编号：API000408
 * 产品文档功能：4.9 数据看板与报表 - 处理效率
 */
export function getDashboardEfficiency(): Promise<DashboardEfficiencyOutput> {
  return request.get<DashboardEfficiencyOutput>('/dashboard/efficiency')
}

/**
 * SLA达成统计
 * 接口编号：API000409
 * 产品文档功能：4.9 数据看板与报表 - SLA达成率
 */
export function getDashboardSlaAchievement(): Promise<DashboardSlaAchievementOutput> {
  return request.get<DashboardSlaAchievementOutput>('/dashboard/sla-achievement')
}

/**
 * 人员工作量TOP
 * 接口编号：API000410
 * 产品文档功能：4.9 数据看板与报表 - 人员工作量TOP10
 */
export function getDashboardWorkload(params?: { limit?: number }): Promise<DashboardWorkloadOutput[]> {
  return request.get<DashboardWorkloadOutput[]>('/dashboard/workload', { params })
}
