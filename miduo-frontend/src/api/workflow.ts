import type {
  HandlerGroupCreateInput,
  HandlerGroupListOutput,
  WorkflowDetailOutput,
  WorkflowListOutput,
} from '@/types/workflow'
import request from '@/utils/request'

/**
 * 查询工作流列表
 * 接口编号：API000012
 * 产品文档功能：4.4 工作流引擎 - 工作流列表
 */
export function getWorkflowList(): Promise<WorkflowListOutput[]> {
  return request.get<WorkflowListOutput[]>('/workflow/list')
}

/**
 * 查询工作流详情
 * 接口编号：API000013
 * 产品文档功能：4.4 工作流引擎 - 工作流详情
 */
export function getWorkflowDetail(id: number): Promise<WorkflowDetailOutput> {
  return request.get<WorkflowDetailOutput>(`/workflow/detail/${id}`)
}

/**
 * 查询处理组列表
 * 接口编号：API000018
 * 产品文档功能：4.5 分派与路由 - 处理组管理
 */
export function getHandlerGroupList(): Promise<HandlerGroupListOutput[]> {
  return request.get<HandlerGroupListOutput[]>('/handler-group/list')
}

/**
 * 创建处理组
 * 接口编号：API000019
 * 产品文档功能：4.5 分派与路由 - 处理组创建
 */
export function createHandlerGroup(data: HandlerGroupCreateInput): Promise<number> {
  return request.post<number>('/handler-group/create', data)
}
