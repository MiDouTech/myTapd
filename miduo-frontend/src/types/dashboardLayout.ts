/**
 * 仪表盘个性化布局相关 TypeScript 类型定义
 */

/** 行组 Key 类型（与后端 DashboardRowGroupEnum 对应） */
export type DashboardRowGroupKey = 'overview' | 'trend_category' | 'efficiency_workload'

/** 单条布局配置项（GET 接口响应） */
export interface DashboardLayoutItem {
  rowGroupKey: DashboardRowGroupKey
  sortOrder: number
  isFixed: boolean
}

/** 保存布局请求体 */
export interface DashboardLayoutSaveInput {
  layouts: Array<{
    rowGroupKey: DashboardRowGroupKey
    sortOrder: number
  }>
}
