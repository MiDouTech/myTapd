export interface DepartmentTreeOutput {
  id: number
  name: string
  parentId?: number
  wecomDeptId?: number
  sortOrder?: number
  children: DepartmentTreeOutput[]
}
