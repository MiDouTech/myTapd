/**
 * 工单状态 code → 中文展示（与工单详情/列表页口径一致）
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

/** 与后端枚举及历史数据对齐（别名、大小写） */
export function normalizeTicketStatusCode(raw?: string | null): string {
  if (raw == null || String(raw).trim() === '') {
    return ''
  }
  let code = String(raw).trim().toLowerCase()
  if (code === 'pending_dispatch') {
    code = 'pending_assign'
  }
  if (code === 'pending_test') {
    code = 'pending_test_accept'
  }
  if (code === 'pending_dev') {
    code = 'pending_dev_accept'
  }
  return code
}

/** 将状态 code 转为中文标签；未知 code 时回退为原字符串 */
export function getTicketStatusLabel(status?: string | null): string {
  if (status == null || String(status).trim() === '') {
    return '-'
  }
  const normalized = normalizeTicketStatusCode(status)
  return TICKET_STATUS_LABEL_MAP[normalized] || String(status).trim()
}
