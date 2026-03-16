import type { PageOutput } from '@/types/common'
import type {
  AppCodeOutput,
  OperationLogDetailOutput,
  OperationLogListOutput,
  OperationLogPageInput,
  OperationLogStatisticsOutput,
} from '@/types/operationLog'
import request from '@/utils/request'

/**
 * 分页查询操作日志
 * 接口编号：API000600
 * 产品文档功能：PRD §3.1 日志列表多条件查询
 */
export function getOperationLogPage(
  params: OperationLogPageInput,
): Promise<PageOutput<OperationLogListOutput>> {
  return request.get<PageOutput<OperationLogListOutput>>('/operation-log/page', { params })
}

/**
 * 获取操作日志详情
 * 接口编号：API000601
 * 产品文档功能：PRD §3.2 日志详情抽屉
 */
export function getOperationLogDetail(id: number): Promise<OperationLogDetailOutput> {
  return request.get<OperationLogDetailOutput>(`/operation-log/detail/${id}`)
}

/**
 * 获取日志统计概览
 * 接口编号：API000602
 * 产品文档功能：PRD §3.3 今日日志统计卡片
 */
export function getOperationLogStatistics(): Promise<OperationLogStatisticsOutput> {
  return request.get<OperationLogStatisticsOutput>('/operation-log/statistics')
}

/**
 * 获取操作模块枚举列表
 * 接口编号：API000604
 * 产品文档功能：PRD §3.1 操作模块下拉选项
 */
export function getOperationLogModuleList(): Promise<string[]> {
  return request.get<string[]>('/operation-log/module/list')
}

/**
 * 获取所属应用枚举列表
 * 接口编号：API000605
 * 产品文档功能：PRD §3.1 所属应用下拉选项
 */
export function getOperationLogAppList(): Promise<AppCodeOutput[]> {
  return request.get<AppCodeOutput[]>('/operation-log/app/list')
}
