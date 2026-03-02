import request from '@/utils/request'
import type { KanbanColumnOutput, KanbanMoveInput } from '@/types/kanban'

/**
 * 获取看板数据
 * 接口编号：API000411
 * 产品文档功能：5.3 看板视图 - 按状态分列展示工单
 */
export function getKanbanData(params?: { limit?: number }): Promise<KanbanColumnOutput[]> {
  return request.get<KanbanColumnOutput[]>('/ticket/kanban', { params })
}

/**
 * 看板拖拽变更状态
 * 接口编号：API000412
 * 产品文档功能：5.3 看板视图 - 拖拽卡片变更状态
 */
export function moveKanbanTicket(data: KanbanMoveInput): Promise<void> {
  return request.put<void>('/ticket/kanban/move', data)
}
