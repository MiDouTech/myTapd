import type { BasicSettingsOutput, BasicSettingsUpdateInput } from '@/types/systemConfig'
import request from '@/utils/request'

/**
 * 查询基础参数配置
 * 接口编号：API000505
 * 产品文档功能：系统设置 - 基础参数查询
 */
export function getBasicSettings(): Promise<BasicSettingsOutput> {
  return request.get<BasicSettingsOutput>('/system-config/basic')
}

/**
 * 更新基础参数配置
 * 接口编号：API000506
 * 产品文档功能：系统设置 - 基础参数更新
 */
export function updateBasicSettings(data: BasicSettingsUpdateInput): Promise<void> {
  return request.put<void>('/system-config/basic', data)
}
