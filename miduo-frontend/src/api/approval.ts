import type {
  ApprovalTaskOutput,
  ApprovalActionInput,
  ApprovalPendingListOutput
} from '@/types/approval'
import request from '@/utils/request'

const BASE = '/api/ticket/approval'

/**
 * 查询工单审批任务详情（含审批时间轴）
 * 接口编号：API000517
 */
export function getApprovalTasks(ticketId: number): Promise<ApprovalTaskOutput> {
  return request.get(`${BASE}/${ticketId}/tasks`)
}

/**
 * 查询当前用户待审批任务数量（角标）
 * 接口编号：API000518
 */
export function getMyApprovalPendingCount(): Promise<number> {
  return request.get(`${BASE}/my-pending-count`)
}

/**
 * 查询当前用户待审批任务列表
 * 接口编号：API000519
 */
export function getMyApprovalPendingList(params: {
  pageNum?: number
  pageSize?: number
}): Promise<ApprovalPendingListOutput> {
  return request.get(`${BASE}/my-pending`, { params })
}

/**
 * 执行审批操作（同意 / 驳回 / 转交）
 * 接口编号：API000520
 */
export function performApproval(input: ApprovalActionInput): Promise<void> {
  return request.post(`${BASE}/action`, input)
}
