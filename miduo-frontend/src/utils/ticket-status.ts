/**
 * 工单状态码与中文展示（多处复用：工单列表/详情、Bug 简报关联工单等）
 */

const TICKET_STATUS_LABEL_MAP: Record<string, string> = {
  pending: '待处理',
  pending_assign: '待分派',
  pending_accept: '待受理',
  alert_triggered: '待认领',
  alert_acknowledged: '处置中',
  alert_stable: '待确认',
  alert_resolved: '已解决',
  alert_suppressed: '已抑制',
  processing: '处理中',
  suspended: '已挂起',
  pending_verify: '待验收',
  completed: '已完成',
  closed: '已关闭',
  pending_test_accept: '待测试受理',
  testing: '测试复现中',
  investigating: '排查中',
  pending_dev_accept: '待开发受理',
  developing: '开发解决中',
  temp_resolved: '临时解决',
  pending_cs_confirm: '待客服确认',
  submitted: '已提交',
  dept_approval: '部门审批',
  executing: '执行中',
  rejected: '已驳回',
}

/** 与后端 TicketStatus 及历史数据对齐 */
export function normalizeTicketStatusCode(status?: string): string {
  if (!status) {
    return ''
  }
  const code = status.trim().toLowerCase()
  if (code === 'pending_dispatch') return 'pending_assign'
  if (code === 'pending_test') return 'pending_test_accept'
  if (code === 'pending_dev') return 'pending_dev_accept'
  return code
}

export function getTicketStatusLabel(status?: string): string {
  if (!status) {
    return '-'
  }
  const normalized = normalizeTicketStatusCode(status)
  return TICKET_STATUS_LABEL_MAP[normalized] || status
}
