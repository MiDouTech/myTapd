import type { PageOutput } from '@/types/common'
import type {
  IntegrationAppCreateInput,
  IntegrationAppOutput,
  IntegrationAppPageInput,
  IntegrationAppRotateSecretOutput,
  IntegrationAppUpdateInput,
} from '@/types/integration'
import request from '@/utils/request'

/**
 * 分页查询接入应用
 * 接口编号：API000527
 * 产品文档功能：业务原生工单插件 - 接入应用管理
 */
export function getIntegrationAppPage(
  params: IntegrationAppPageInput,
): Promise<PageOutput<IntegrationAppOutput>> {
  return request.get<PageOutput<IntegrationAppOutput>>('/integration/app/page', { params })
}

/**
 * 查询接入应用详情
 * 产品文档功能：业务原生工单插件 - 接入应用详情
 */
export function getIntegrationAppDetail(id: number): Promise<IntegrationAppOutput> {
  return request.get<IntegrationAppOutput>(`/integration/app/detail/${id}`)
}

/**
 * 创建接入应用
 * 接口编号：API000528
 * 产品文档功能：业务原生工单插件 - 新建接入应用
 */
export function createIntegrationApp(data: IntegrationAppCreateInput): Promise<number> {
  return request.post<number>('/integration/app/create', data)
}

/**
 * 更新接入应用
 * 接口编号：API000529
 * 产品文档功能：业务原生工单插件 - 更新接入应用
 */
export function updateIntegrationApp(id: number, data: IntegrationAppUpdateInput): Promise<void> {
  return request.put<void>(`/integration/app/update/${id}`, data)
}

/**
 * 轮换 AppSecret
 * 接口编号：API000530
 * 产品文档功能：业务原生工单插件 - 轮换接入应用密钥
 */
export function rotateIntegrationAppSecret(id: number): Promise<IntegrationAppRotateSecretOutput> {
  return request.post<IntegrationAppRotateSecretOutput>(`/integration/app/rotate-secret/${id}`)
}
