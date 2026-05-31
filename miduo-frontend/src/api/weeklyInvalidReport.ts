import request from '@/utils/request'
import type {
  WeeklyInvalidReportConfigOutput,
  WeeklyInvalidReportConfigUpdateInput,
  WeeklyInvalidReportOutput,
} from '@/types/weeklyInvalidReport'

/**
 * 预览无效反馈周报（不推送，仅返回数据和 Markdown）
 * 接口编号：API000517
 * 产品文档功能：无效反馈周报推送 - 预览周报内容
 */
export function previewWeeklyInvalidReport(): Promise<WeeklyInvalidReportOutput> {
  return request.get<WeeklyInvalidReportOutput>('/weekly-invalid-report/preview')
}

/**
 * 手动触发无效反馈周报推送
 * 接口编号：API000518
 * 产品文档功能：无效反馈周报推送 - 手动推送周报
 */
export function pushWeeklyInvalidReport(): Promise<void> {
  return request.post('/weekly-invalid-report/push')
}

/**
 * 查询无效反馈周报配置
 * 接口编号：API000519
 * 产品文档功能：无效反馈周报推送 - 查询配置
 */
export function getWeeklyInvalidReportConfig(): Promise<WeeklyInvalidReportConfigOutput> {
  return request.get<WeeklyInvalidReportConfigOutput>('/weekly-invalid-report/config')
}

/**
 * 更新无效反馈周报配置
 * 接口编号：API000520
 * 产品文档功能：无效反馈周报推送 - 更新配置
 */
export function updateWeeklyInvalidReportConfig(
  data: WeeklyInvalidReportConfigUpdateInput,
): Promise<void> {
  return request.put('/weekly-invalid-report/config', data)
}
