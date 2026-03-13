import type {
  HandlerGroupCreateInput,
  HandlerGroupListOutput,
  WorkflowDetailOutput,
  WorkflowListOutput,
  AvailableActionOutput,
  TransitInput,
  TransferInput,
  ReturnInput,
  TicketFlowRecordOutput,
} from '@/types/workflow'
import request from '@/utils/request'

interface WorkflowListRawOutput {
  id: number
  name: string
  mode: string
  modeLabel?: string
  description?: string
  isBuiltin?: number
  isActive: number
  stateCount?: number
  transitionCount?: number
  createTime?: string
  updateTime?: string
}

interface WorkflowDetailRawOutput {
  id: number
  name: string
  mode: string
  modeLabel?: string
  description?: string
  isBuiltin?: number
  isActive: number
  states?: WorkflowDetailOutput['states']
  transitions?: WorkflowDetailOutput['transitions']
  createTime?: string
  updateTime?: string
}

interface HandlerGroupRawOutput {
  id: number
  name: string
  description?: string
  skillTags?: string
  isActive: number
  leaderId?: number
  leaderName?: string
  memberCount?: number
  members?: HandlerGroupListOutput['members']
  createTime?: string
  updateTime?: string
}

function normalizeWorkflowListItem(item: WorkflowListRawOutput): WorkflowListOutput {
  return {
    id: item.id,
    name: item.name,
    mode: item.mode,
    modeLabel: item.modeLabel,
    description: item.description,
    isBuiltin: item.isBuiltin,
    isActive: item.isActive,
    stateCount: item.stateCount ?? 0,
    transitionCount: item.transitionCount ?? 0,
    createTime: item.createTime,
    updateTime: item.updateTime || item.createTime,
  }
}

function normalizeWorkflowDetail(item: WorkflowDetailRawOutput): WorkflowDetailOutput {
  return {
    id: item.id,
    name: item.name,
    mode: item.mode,
    modeLabel: item.modeLabel,
    description: item.description,
    isBuiltin: item.isBuiltin,
    isActive: item.isActive,
    states: item.states || [],
    transitions: item.transitions || [],
    createTime: item.createTime,
    updateTime: item.updateTime || item.createTime,
  }
}

function normalizeHandlerGroup(item: HandlerGroupRawOutput): HandlerGroupListOutput {
  return {
    id: item.id,
    name: item.name,
    description: item.description,
    skillTags: item.skillTags,
    isActive: item.isActive,
    leaderId: item.leaderId,
    leaderName: item.leaderName || '-',
    memberCount: item.memberCount ?? item.members?.length ?? 0,
    members: item.members || [],
    createTime: item.createTime,
    updateTime: item.updateTime || item.createTime,
  }
}

/**
 * 查询工作流列表
 * 接口编号：API000012
 * 产品文档功能：4.4 工作流引擎 - 工作流列表
 */
export async function getWorkflowList(): Promise<WorkflowListOutput[]> {
  const result = await request.get<WorkflowListRawOutput[]>('/workflow/list')
  return result.map(normalizeWorkflowListItem)
}

/**
 * 查询工作流详情
 * 接口编号：API000013
 * 产品文档功能：4.4 工作流引擎 - 工作流详情
 */
export async function getWorkflowDetail(id: number): Promise<WorkflowDetailOutput> {
  const result = await request.get<WorkflowDetailRawOutput>(`/workflow/detail/${id}`)
  return normalizeWorkflowDetail(result)
}

/**
 * 查询处理组列表
 * 接口编号：API000018
 * 产品文档功能：4.5 分派与路由 - 处理组管理
 */
export async function getHandlerGroupList(): Promise<HandlerGroupListOutput[]> {
  const result = await request.get<HandlerGroupRawOutput[]>('/handler-group/list')
  return result.map(normalizeHandlerGroup)
}

/**
 * 创建处理组
 * 接口编号：API000019
 * 产品文档功能：4.5 分派与路由 - 处理组创建
 */
export function createHandlerGroup(data: HandlerGroupCreateInput): Promise<number> {
  return request.post<number>('/handler-group/create', data)
}

// ============================================================
// 工单流转相关接口
// ============================================================

/**
 * 获取工单可用操作列表（动态渲染操作按钮，不硬编码）
 * 接口编号：API000014
 * 产品文档功能：4.4 工作流引擎 - 可用操作
 */
export function getAvailableActions(ticketId: number): Promise<AvailableActionOutput> {
  return request.get<AvailableActionOutput>(`/ticket/${ticketId}/available-actions`)
}

/**
 * 执行状态流转
 * 接口编号：API000015
 * 产品文档功能：4.4 工作流引擎 - 状态流转
 */
export function transitTicket(ticketId: number, data: TransitInput): Promise<void> {
  return request.put<void>(`/ticket/transit/${ticketId}`, data)
}

/**
 * 转派工单
 * 接口编号：API000016
 * 产品文档功能：4.5 分派与路由 - 转派
 */
export function transferTicket(ticketId: number, data: TransferInput): Promise<void> {
  return request.put<void>(`/ticket/transfer/${ticketId}`, data)
}

/**
 * 退回上一节点
 * 接口编号：API000017
 * 产品文档功能：4.5 分派与路由 - 退回
 */
export function returnTicket(ticketId: number, data: ReturnInput): Promise<void> {
  return request.put<void>(`/ticket/return/${ticketId}`, data)
}

/**
 * 查询工单流转历史
 * 接口编号：API000018
 * 产品文档功能：4.4 工作流引擎 - 流转历史
 */
export function getFlowHistory(ticketId: number): Promise<TicketFlowRecordOutput[]> {
  return request.get<TicketFlowRecordOutput[]>(`/ticket/${ticketId}/flow-history`)
}
