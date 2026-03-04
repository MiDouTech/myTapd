import type { PageOutput } from '@/types/common'
import type {
  DepartmentTreeNode,
  EmployeeDetailOutput,
  EmployeePageQuery,
  EmployeePageRecord,
  SyncLogPageQuery,
  SyncManualOutput,
  SyncStatusOutput,
} from '@/types/organization'
import request from '@/utils/request'

interface DepartmentTreeRawNode extends Omit<DepartmentTreeNode, 'children'> {
  children?: DepartmentTreeRawNode[]
}

function normalizeDepartmentTree(nodes: DepartmentTreeRawNode[]): DepartmentTreeNode[] {
  return nodes.map((node) => ({
    ...node,
    children: normalizeDepartmentTree(node.children || []),
  }))
}

/**
 * 获取组织架构树（含成员统计）
 * 接口编号：API000428
 * 产品文档功能：SSO一期-组织查询-部门树
 */
export async function getOrganizationDepartmentTree(): Promise<DepartmentTreeNode[]> {
  const result = await request.get<DepartmentTreeRawNode[]>('/v1/departments/tree')
  return normalizeDepartmentTree(result)
}

/**
 * 分页查询员工列表
 * 接口编号：API000429
 * 产品文档功能：SSO一期-组织查询-员工分页
 */
export function getOrganizationEmployeePage(
  params: EmployeePageQuery,
): Promise<PageOutput<EmployeePageRecord>> {
  return request.get<PageOutput<EmployeePageRecord>>('/v1/employees/page', { params })
}

/**
 * 获取员工详情
 * 接口编号：API000430
 * 产品文档功能：SSO一期-组织查询-员工详情
 */
export function getOrganizationEmployeeDetail(id: number): Promise<EmployeeDetailOutput> {
  return request.get<EmployeeDetailOutput>(`/v1/employees/detail/${id}`)
}

/**
 * 手动触发企微组织同步
 * 接口编号：API000425
 * 产品文档功能：SSO一期-手动同步
 */
export function manualSyncOrganization(): Promise<SyncManualOutput> {
  return request.post<SyncManualOutput>('/v1/sync/manual')
}

/**
 * 查询最近同步状态
 * 接口编号：API000426
 * 产品文档功能：SSO一期-同步状态查询
 */
export function getLatestOrganizationSyncStatus(): Promise<SyncStatusOutput | null> {
  return request.get<SyncStatusOutput | null>('/v1/sync/status')
}

/**
 * 分页查询同步日志
 * 接口编号：API000427
 * 产品文档功能：SSO一期-同步日志查询
 */
export function getOrganizationSyncLogPage(
  params: SyncLogPageQuery,
): Promise<PageOutput<SyncStatusOutput>> {
  return request.get<PageOutput<SyncStatusOutput>>('/v1/sync/log/page', { params })
}
