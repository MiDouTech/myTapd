import request from '@/utils/request'
import type {
  DailyReportConfigOutput,
  DailyReportConfigUpdateInput,
  DailyReportOutput,
} from '@/types/dailyReport'

/**
 * 预览日报（不推送，仅返回数据和 Markdown）
 * 接口编号：API000501
 * 产品文档功能：日报自动推送 - 预览日报内容
 */
export function previewDailyReport(): Promise<DailyReportOutput> {
  return request.get<DailyReportOutput>('/daily-report/preview')
}

/**
 * 手动触发日报推送
 * 接口编号：API000502
 * 产品文档功能：日报自动推送 - 手动推送日报
 */
export function pushDailyReport(): Promise<void> {
  return request.post('/daily-report/push')
}

/**
 * 查询日报配置
 * 接口编号：API000503
 * 产品文档功能：日报自动推送 - 查询配置
 */
export function getDailyReportConfig(): Promise<DailyReportConfigOutput> {
  return request.get<DailyReportConfigOutput>('/daily-report/config')
}

/**
 * 更新日报配置
 * 接口编号：API000504
 * 产品文档功能：日报自动推送 - 更新配置
 */
export function updateDailyReportConfig(data: DailyReportConfigUpdateInput): Promise<void> {
  return request.put('/daily-report/config', data)
}
