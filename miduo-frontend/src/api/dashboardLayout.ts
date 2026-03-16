import request from '@/utils/request'
import type { DashboardLayoutItem, DashboardLayoutSaveInput } from '@/types/dashboardLayout'

/**
 * 获取当前用户个人仪表盘布局配置
 * 接口编号：API000411
 * 产品文档功能：仪表盘个性化布局 §3.1
 */
export function getDashboardLayout(): Promise<DashboardLayoutItem[]> {
  return request.get<DashboardLayoutItem[]>('/dashboard/layout')
}

/**
 * 保存当前用户个人仪表盘布局配置
 * 接口编号：API000412
 * 产品文档功能：仪表盘个性化布局 §3.4
 */
export function saveDashboardLayout(data: DashboardLayoutSaveInput): Promise<void> {
  return request.put<void>('/dashboard/layout', data)
}

/**
 * 恢复当前用户仪表盘为系统默认布局
 * 接口编号：API000413
 * 产品文档功能：仪表盘个性化布局 §3.6
 */
export function resetDashboardLayout(): Promise<void> {
  return request.del<void>('/dashboard/layout')
}
