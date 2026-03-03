import type {
  CurrentUserOutput,
  DepartmentTreeOutput,
  UserListInput,
  UserListOutput,
} from '@/types/user'
import request from '@/utils/request'

/**
 * 获取当前用户信息
 * 接口编号：API000402
 * 产品文档功能：4.10.1 用户管理
 */
export function getCurrentUser(): Promise<CurrentUserOutput> {
  return request.get<CurrentUserOutput>('/user/current')
}

/**
 * 用户列表（按部门筛选）
 * 接口编号：API000403
 * 产品文档功能：4.10.1 用户管理
 */
export function getUserList(params?: UserListInput): Promise<UserListOutput[]> {
  return request.get<UserListOutput[]>('/user/list', { params })
}

/**
 * 组织架构树
 * 接口编号：API000404
 * 产品文档功能：4.10.1 组织架构与用户管理
 */
export function getDepartmentTree(): Promise<DepartmentTreeOutput[]> {
  return request.get<DepartmentTreeOutput[]>('/department/tree')
}
