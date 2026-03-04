import type { PageQuery } from '@/types/common'

export interface DepartmentTreeNode {
  id: number
  name: string
  parentId?: number
  wecomDeptId?: number
  sortOrder?: number
  deptStatus?: number
  syncStatus?: number
  syncTime?: string
  directUserCount?: number
  totalUserCount?: number
  children: DepartmentTreeNode[]
}

export interface EmployeePageQuery extends PageQuery {
  departmentId?: number
  keyword?: string
  accountStatus?: number
  gender?: number
}

export interface EmployeePageRecord {
  id: number
  name: string
  employeeNo?: string
  departmentId?: number
  departmentName?: string
  emailMasked?: string
  phoneMasked?: string
  position?: string
  gender?: number
  genderName?: string
  avatarUrl?: string
  wecomUseridMasked?: string
  accountStatus?: number
  accountStatusName?: string
  createTime?: string
}

export interface EmployeeDetailOutput extends EmployeePageRecord {
  roleCodes?: string[]
}

export interface SyncManualOutput {
  syncStatus: string
  totalCount: number
  successCount: number
  failCount: number
  errorMessage?: string
  startTime?: string
  endTime?: string
  durationMs?: number
}

export interface SyncStatusOutput {
  syncType: string
  syncMode: string
  syncStatus: string
  totalCount: number
  successCount: number
  failCount: number
  retryCount: number
  triggerBy?: string
  errorMessage?: string
  startTime?: string
  endTime?: string
  durationMs?: number
}

export interface SyncLogPageQuery extends PageQuery {
  syncMode?: string
  syncStatus?: string
}
