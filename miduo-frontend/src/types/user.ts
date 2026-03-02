export interface CurrentUserOutput {
  id: number
  name: string
  employeeNo?: string
  departmentId?: number
  departmentName?: string
  email?: string
  phone?: string
  position?: string
  avatarUrl?: string
  wecomUserid?: string
  accountStatus?: number
  roleCodes?: string[]
  createTime?: string
}

export interface UserListInput {
  departmentId?: number
  keyword?: string
  accountStatus?: number
}

export interface UserListOutput {
  id: number
  name: string
  employeeNo?: string
  departmentId?: number
  departmentName?: string
  email?: string
  phone?: string
  position?: string
  avatarUrl?: string
  accountStatus?: number
  roleCodes?: string[]
  createTime?: string
}
