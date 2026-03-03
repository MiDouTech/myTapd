type BugReportTagType = 'success' | 'warning' | 'danger' | 'info' | 'primary'

export interface BugReportStatusOption {
  value: string
  label: string
  tagType: BugReportTagType
}

export const BUG_REPORT_STATUS_OPTIONS: BugReportStatusOption[] = [
  { value: 'DRAFT', label: '待填写', tagType: 'info' },
  { value: 'PENDING_REVIEW', label: '待审核', tagType: 'warning' },
  { value: 'REJECTED', label: '已退回', tagType: 'danger' },
  { value: 'ARCHIVED', label: '已归档', tagType: 'success' },
  { value: 'VOIDED', label: '已作废', tagType: 'info' },
]

const BUG_REPORT_STATUS_MAP = BUG_REPORT_STATUS_OPTIONS.reduce(
  (acc, item) => {
    acc[item.value] = item
    return acc
  },
  {} as Record<string, BugReportStatusOption>,
)

export function getBugReportStatusLabel(status?: string, fallbackLabel?: string): string {
  if (!status) {
    return '-'
  }
  return BUG_REPORT_STATUS_MAP[status]?.label || fallbackLabel || status
}

export function getBugReportStatusTagType(status?: string): BugReportTagType {
  if (!status) {
    return 'info'
  }
  return BUG_REPORT_STATUS_MAP[status]?.tagType || 'info'
}

export function isBugReportEditable(status?: string): boolean {
  return status === 'DRAFT' || status === 'REJECTED'
}

export function canSubmitBugReport(status?: string): boolean {
  return isBugReportEditable(status)
}

export function canReviewBugReport(status?: string): boolean {
  return status === 'PENDING_REVIEW'
}

export function canVoidBugReport(status?: string): boolean {
  return Boolean(status && status !== 'ARCHIVED' && status !== 'VOIDED')
}
