import type { PageQuery } from './common'

/**
 * 操作日志分页查询参数
 * 接口编号：API000600
 * PRD §6.2
 */
export interface OperationLogPageInput extends PageQuery {
  startTime?: string
  endTime?: string
  accountId?: number
  operatorName?: string
  operatorIp?: string
  logLevel?: string
  moduleName?: string
  operationItem?: string
  operationDetail?: string
  executeResult?: string
  sortField?: string
  sortOrder?: string
}

/**
 * 操作日志列表行输出
 * 接口编号：API000600
 */
export interface OperationLogListOutput {
  id: number
  operateTime: string
  accountId: number
  operatorName: string
  operatorIp: string
  logLevel: string
  logLevelDesc: string
  moduleName: string
  requestPath: string
  operationItem: string
  executeResult: string
  executeResultDesc: string
}

/**
 * 变更记录项
 * PRD §3.2.3
 */
export interface ChangeRecordItem {
  fieldName: string
  beforeValue: string
  afterValue: string
}

/**
 * 操作日志详情输出
 * 接口编号：API000601
 */
export interface OperationLogDetailOutput {
  id: number
  operateTime: string
  accountId: number
  operatorName: string
  operatorIp: string
  userAgent: string
  logLevel: string
  logLevelDesc: string
  moduleName: string
  requestPath: string
  requestMethod: string
  operationItem: string
  requestParams: string | null
  executeResult: string
  executeResultDesc: string
  costMillis: number
  changeRecords: ChangeRecordItem[] | null
  errorCode: string | null
  errorMessage: string | null
  errorStack: string | null
}

/**
 * 日志统计概览输出
 * 接口编号：API000602
 */
export interface OperationLogStatisticsOutput {
  todayTotalCount: number
  todayFailureCount: number
  todayActiveUserCount: number
  todaySecurityAlertCount: number
}
