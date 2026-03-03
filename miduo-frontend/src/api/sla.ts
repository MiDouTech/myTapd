import type { SlaPolicyCreateInput, SlaPolicyOutput, SlaPolicyUpdateInput } from '@/types/sla'
import request from '@/utils/request'

/**
 * 查询SLA策略列表
 * 接口编号：API000001
 * 产品文档功能：4.7 SLA管理 - 策略列表查询
 */
export function getSlaPolicyList(): Promise<SlaPolicyOutput[]> {
  return request.get<SlaPolicyOutput[]>('/sla/policy/list')
}

/**
 * 创建SLA策略
 * 接口编号：API000002
 * 产品文档功能：4.7 SLA管理 - 新增SLA策略
 */
export function createSlaPolicy(data: SlaPolicyCreateInput): Promise<SlaPolicyOutput> {
  return request.post<SlaPolicyOutput>('/sla/policy/create', data)
}

/**
 * 更新SLA策略
 * 接口编号：API000003
 * 产品文档功能：4.7 SLA管理 - 更新SLA策略
 */
export function updateSlaPolicy(data: SlaPolicyUpdateInput): Promise<SlaPolicyOutput> {
  return request.put<SlaPolicyOutput>('/sla/policy/update', data)
}
