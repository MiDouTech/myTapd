import type { PageOutput } from '@/types/common'
import type {
  AlertEventLogOutput,
  AlertEventLogPageInput,
  AlertRuleMappingCreateInput,
  AlertRuleMappingOutput,
  AlertRuleMappingPageInput,
  AlertRuleMappingUpdateInput,
  AlertTokenOutput,
} from '@/types/alert'
import request from '@/utils/request'

/**
 * 分页查询告警规则映射
 * 接口编号：API000701
 * 产品文档功能：告警接入 - 映射配置列表
 */
export function getAlertMappingPage(
  params: AlertRuleMappingPageInput,
): Promise<PageOutput<AlertRuleMappingOutput>> {
  return request.get<PageOutput<AlertRuleMappingOutput>>('/alert-mapping/page', { params })
}

/**
 * 创建告警规则映射
 * 接口编号：API000702
 * 产品文档功能：告警接入 - 新增映射配置
 */
export function createAlertMapping(data: AlertRuleMappingCreateInput): Promise<number> {
  return request.post<number>('/alert-mapping/create', data)
}

/**
 * 更新告警规则映射
 * 接口编号：API000703
 * 产品文档功能：告警接入 - 编辑映射配置
 */
export function updateAlertMapping(data: AlertRuleMappingUpdateInput): Promise<void> {
  return request.put<void>('/alert-mapping/update', data)
}

/**
 * 删除告警规则映射
 * 接口编号：API000704
 * 产品文档功能：告警接入 - 删除映射配置
 */
export function deleteAlertMapping(id: number): Promise<void> {
  return request.del<void>(`/alert-mapping/delete/${id}`)
}

/**
 * 获取告警规则映射详情
 * 接口编号：API000705
 * 产品文档功能：告警接入 - 映射配置详情
 */
export function getAlertMappingDetail(id: number): Promise<AlertRuleMappingOutput> {
  return request.get<AlertRuleMappingOutput>(`/alert-mapping/detail/${id}`)
}

/**
 * 分页查询告警事件日志
 * 接口编号：API000706
 * 产品文档功能：告警接入 - 告警事件日志
 */
export function getAlertEventLogPage(
  params: AlertEventLogPageInput,
): Promise<PageOutput<AlertEventLogOutput>> {
  return request.get<PageOutput<AlertEventLogOutput>>('/alert-mapping/event-log/page', { params })
}

/**
 * 获取告警接入Token
 * 接口编号：API000707
 * 产品文档功能：告警接入 - 获取Webhook地址与Token
 */
export function getAlertToken(): Promise<AlertTokenOutput> {
  return request.get<AlertTokenOutput>('/alert-mapping/token')
}

/**
 * 重置告警接入Token
 * 接口编号：API000708
 * 产品文档功能：告警接入 - 重新生成Token
 */
export function resetAlertToken(): Promise<AlertTokenOutput> {
  return request.post<AlertTokenOutput>('/alert-mapping/token/reset')
}
