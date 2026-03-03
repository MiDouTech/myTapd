import type { DepartmentTreeOutput } from '@/types/department'
import request from '@/utils/request'

interface DepartmentTreeRawOutput {
  id: number
  name: string
  parentId?: number
  wecomDeptId?: number
  sortOrder?: number
  children?: DepartmentTreeRawOutput[]
}

function normalizeDepartmentTree(nodes: DepartmentTreeRawOutput[]): DepartmentTreeOutput[] {
  return nodes.map((node) => ({
    id: node.id,
    name: node.name,
    parentId: node.parentId,
    wecomDeptId: node.wecomDeptId,
    sortOrder: node.sortOrder,
    children: normalizeDepartmentTree(node.children || []),
  }))
}

/**
 * 获取组织架构树
 * 接口编号：API000404
 * 产品文档功能：4.10.1 组织架构与用户管理
 */
export async function getDepartmentTree(): Promise<DepartmentTreeOutput[]> {
  const result = await request.get<DepartmentTreeRawOutput[]>('/department/tree')
  return normalizeDepartmentTree(result)
}
