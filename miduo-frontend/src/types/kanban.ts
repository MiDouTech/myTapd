export interface KanbanTicketOutput {
  id: number
  ticketNo: string
  title: string
  priority?: string
  priorityLabel?: string
  status: string
  statusLabel?: string
  categoryName?: string
  assigneeId?: number
  assigneeName?: string
  updateTime?: string
}

export interface KanbanColumnOutput {
  status: string
  statusLabel?: string
  tickets: KanbanTicketOutput[]
}

export interface KanbanMoveInput {
  ticketId: number
  targetStatus: string
  remark?: string
}
