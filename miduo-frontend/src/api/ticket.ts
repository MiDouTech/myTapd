import type { PageOutput } from '@/types/common'
import type {
  TicketAssignInput,
  TicketBugCustomerInfoInput,
  TicketBugDevInfoInput,
  TicketBugTestInfoInput,
  TicketCloseInput,
  TicketCreateInput,
  TicketDetailOutput,
  TicketListOutput,
  TicketNodeDurationOutput,
  TicketPageInput,
  TicketProcessInput,
  TicketTimeTrackOutput,
} from '@/types/ticket'
import request from '@/utils/request'

/**
 * 创建工单
 * 接口编号：API000006
 * 产品文档功能：4.2.1 工单创建 - 选择分类→加载模板→填写→提交
 */
export function createTicket(data: TicketCreateInput): Promise<number> {
  return request.post<number>('/ticket/create', data)
}

/**
 * 分页查询工单列表
 * 接口编号：API000007
 * 产品文档功能：4.2.2 工单列表与筛选 - 多视图+筛选+排序+分页
 */
export function getTicketPage(params: TicketPageInput): Promise<PageOutput<TicketListOutput>> {
  return request.get<PageOutput<TicketListOutput>>('/ticket/page', { params })
}

/**
 * 获取工单详情
 * 接口编号：API000008
 * 产品文档功能：4.2.3 工单详情与操作 - 完整信息展示
 */
export function getTicketDetail(id: number): Promise<TicketDetailOutput> {
  return request.get<TicketDetailOutput>(`/ticket/detail/${id}`)
}

/**
 * 手动分派工单
 * 接口编号：API000009
 * 产品文档功能：4.5.1 分派策略 - 手动分派指定处理人
 */
export function assignTicket(id: number, data: TicketAssignInput): Promise<void> {
  return request.put<void>(`/ticket/assign/${id}`, data)
}

/**
 * 处理工单并流转
 * 接口编号：API000010
 * 产品文档功能：4.2.3 核心操作 - 处理/转派/挂起/恢复/验收
 */
export function processTicket(id: number, data: TicketProcessInput): Promise<void> {
  return request.put<void>(`/ticket/process/${id}`, data)
}

/**
 * 关闭工单
 * 接口编号：API000011
 * 产品文档功能：4.2.3 核心操作 - 关闭工单
 */
export function closeTicket(id: number, data?: TicketCloseInput): Promise<void> {
  return request.put<void>(`/ticket/close/${id}`, data)
}

/**
 * 关注工单
 * 接口编号：API000012
 * 产品文档功能：4.2.3 核心操作 - 关注工单动态
 */
export function followTicket(id: number): Promise<void> {
  return request.post<void>(`/ticket/follow/${id}`)
}

/**
 * 取消关注工单
 * 接口编号：API000013
 * 产品文档功能：4.2.3 核心操作 - 取消关注工单
 */
export function unfollowTicket(id: number): Promise<void> {
  return request.del<void>(`/ticket/follow/${id}`)
}

/**
 * 记录工单首次阅读轨迹
 * 接口编号：API000020
 * 产品文档功能：4.4.6 全链路时间追踪 - 首次阅读记录
 */
export function trackTicketRead(id: number): Promise<void> {
  return request.post<void>(`/ticket/${id}/track/read`)
}

/**
 * 更新缺陷工单客服信息
 * 接口编号：API000021
 * 产品文档功能：4.2.3 缺陷工单详情页 - 客服信息区
 */
export function updateBugCustomerInfo(id: number, data: TicketBugCustomerInfoInput): Promise<void> {
  return request.put<void>(`/ticket/bug/customer-info/${id}`, data)
}

/**
 * 更新缺陷工单测试信息
 * 接口编号：API000022
 * 产品文档功能：4.2.3 缺陷工单详情页 - 测试信息区
 */
export function updateBugTestInfo(id: number, data: TicketBugTestInfoInput): Promise<void> {
  return request.put<void>(`/ticket/bug/test-info/${id}`, data)
}

/**
 * 更新缺陷工单开发信息
 * 接口编号：API000023
 * 产品文档功能：4.2.3 缺陷工单详情页 - 开发信息区
 */
export function updateBugDevInfo(id: number, data: TicketBugDevInfoInput): Promise<void> {
  return request.put<void>(`/ticket/bug/dev-info/${id}`, data)
}

/**
 * 获取工单时间追踪链
 * 接口编号：API000024
 * 产品文档功能：4.4.6 全链路时间追踪 - 时间链
 */
export function getTicketTimeTrack(id: number): Promise<TicketTimeTrackOutput> {
  return request.get<TicketTimeTrackOutput>(`/ticket/${id}/time-track`)
}

/**
 * 获取工单节点耗时统计
 * 接口编号：API000025
 * 产品文档功能：4.4.6 全链路时间追踪 - 节点耗时
 */
export function getTicketNodeDuration(id: number): Promise<TicketNodeDurationOutput> {
  return request.get<TicketNodeDurationOutput>(`/ticket/${id}/node-duration`)
}
