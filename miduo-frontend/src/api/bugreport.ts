import type { PageOutput } from '@/types/common'
import type {
  BugReportCreateInput,
  BugReportDetailOutput,
  BugReportPageInput,
  BugReportPageOutput,
  BugReportReviewInput,
  BugReportStatisticsInput,
  BugReportStatisticsOutput,
  BugReportSubmitInput,
  BugReportUpdateInput,
  DefectCategoryOutput,
  LogicCauseTreeOutput,
} from '@/types/bugreport'
import request from '@/utils/request'

/**
 * 分页查询Bug简报
 * 接口编号：API000020
 * 产品文档功能：4.12 Bug简报管理 - 简报列表
 */
export function getBugReportPage(params: BugReportPageInput): Promise<PageOutput<BugReportPageOutput>> {
  return request.get<PageOutput<BugReportPageOutput>>('/bug-report/page', { params })
}

/**
 * 获取Bug简报详情
 * 接口编号：API000021
 * 产品文档功能：4.12 Bug简报管理 - 简报详情
 */
export function getBugReportDetail(id: number): Promise<BugReportDetailOutput> {
  return request.get<BugReportDetailOutput>(`/bug-report/detail/${id}`)
}

/**
 * 创建Bug简报
 * 接口编号：API000022
 * 产品文档功能：4.12 Bug简报管理 - 简报创建
 */
export function createBugReport(data: BugReportCreateInput): Promise<number> {
  return request.post<number>('/bug-report/create', data)
}

/**
 * 更新Bug简报
 * 接口编号：API000023
 * 产品文档功能：4.12 Bug简报管理 - 简报编辑
 */
export function updateBugReport(id: number, data: BugReportUpdateInput): Promise<void> {
  return request.put<void>(`/bug-report/update/${id}`, data)
}

/**
 * 提交审核
 * 接口编号：API000024
 * 产品文档功能：4.12 Bug简报管理 - 提交审核
 */
export function submitBugReport(id: number, data?: BugReportSubmitInput): Promise<void> {
  return request.put<void>(`/bug-report/submit/${id}`, data)
}

/**
 * 审核通过
 * 接口编号：API000025
 * 产品文档功能：4.12 Bug简报管理 - 审核通过归档
 */
export function approveBugReport(id: number, data: BugReportReviewInput): Promise<void> {
  return request.put<void>(`/bug-report/approve/${id}`, data)
}

/**
 * 审核驳回
 * 接口编号：API000026
 * 产品文档功能：4.12 Bug简报管理 - 审核不通过
 */
export function rejectBugReport(id: number, data: BugReportReviewInput): Promise<void> {
  return request.put<void>(`/bug-report/reject/${id}`, data)
}

/**
 * 作废简报
 * 接口编号：API000027
 * 产品文档功能：4.12 Bug简报管理 - 作废
 */
export function voidBugReport(id: number): Promise<void> {
  return request.put<void>(`/bug-report/void/${id}`)
}

/**
 * 获取简报统计
 * 接口编号：API000028
 * 产品文档功能：4.12 Bug简报管理 - 统计看板
 */
export function getBugReportStatistics(
  params?: BugReportStatisticsInput,
): Promise<BugReportStatisticsOutput> {
  return request.get<BugReportStatisticsOutput>('/bug-report/statistics', { params })
}

/**
 * 逻辑归因字典
 * 接口编号：API000029
 * 产品文档功能：4.12 Bug简报管理 - 逻辑归因字典
 */
export function getLogicCauseDict(): Promise<LogicCauseTreeOutput[]> {
  return request.get<LogicCauseTreeOutput[]>('/dict/logic-cause')
}

/**
 * 缺陷分类字典
 * 接口编号：API000030
 * 产品文档功能：4.12 Bug简报管理 - 缺陷分类字典
 */
export function getDefectCategoryDict(): Promise<DefectCategoryOutput[]> {
  return request.get<DefectCategoryOutput[]>('/dict/defect-category')
}
