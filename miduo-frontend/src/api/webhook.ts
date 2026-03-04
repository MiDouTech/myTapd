import type { PageOutput } from '@/types/common'
import type {
  WebhookConfigCreateInput,
  WebhookConfigOutput,
  WebhookConfigPageInput,
  WebhookConfigUpdateInput,
  WebhookDispatchLogOutput,
  WebhookDispatchLogPageInput,
} from '@/types/webhook'
import request from '@/utils/request'

interface WebhookConfigRawOutput extends Omit<WebhookConfigOutput, 'eventTypes'> {
  eventTypes?: string[]
}

function normalizeWebhookConfig(item: WebhookConfigRawOutput): WebhookConfigOutput {
  return {
    ...item,
    eventTypes: item.eventTypes || [],
  }
}

/**
 * 分页查询Webhook配置
 * 接口编号：API000417
 * 产品文档功能：4.11 开放能力 - Webhook配置管理
 */
export async function getWebhookConfigPage(
  params: WebhookConfigPageInput,
): Promise<PageOutput<WebhookConfigOutput>> {
  const result = await request.get<PageOutput<WebhookConfigRawOutput>>('/webhook/config/page', { params })
  return {
    ...result,
    records: result.records.map(normalizeWebhookConfig),
  }
}

/**
 * 查询Webhook配置详情
 * 接口编号：API000418
 * 产品文档功能：4.11 开放能力 - Webhook配置详情
 */
export async function getWebhookConfigDetail(id: number): Promise<WebhookConfigOutput> {
  const result = await request.get<WebhookConfigRawOutput>(`/webhook/config/detail/${id}`)
  return normalizeWebhookConfig(result)
}

/**
 * 创建Webhook配置
 * 接口编号：API000419
 * 产品文档功能：4.11 开放能力 - 新增Webhook配置
 */
export function createWebhookConfig(data: WebhookConfigCreateInput): Promise<number> {
  return request.post<number>('/webhook/config/create', data)
}

/**
 * 更新Webhook配置
 * 接口编号：API000420
 * 产品文档功能：4.11 开放能力 - 更新Webhook配置
 */
export function updateWebhookConfig(id: number, data: WebhookConfigUpdateInput): Promise<void> {
  return request.put<void>(`/webhook/config/update/${id}`, data)
}

/**
 * 删除Webhook配置
 * 接口编号：API000421
 * 产品文档功能：4.11 开放能力 - 删除Webhook配置
 */
export function deleteWebhookConfig(id: number): Promise<void> {
  return request.del<void>(`/webhook/config/delete/${id}`)
}

/**
 * 分页查询Webhook推送日志
 * 接口编号：API000431
 * 产品文档功能：4.11 开放能力 - Webhook推送日志排障
 */
export function getWebhookDispatchLogPage(
  params: WebhookDispatchLogPageInput,
): Promise<PageOutput<WebhookDispatchLogOutput>> {
  return request.get<PageOutput<WebhookDispatchLogOutput>>('/webhook/config/dispatch-log/page', { params })
}
