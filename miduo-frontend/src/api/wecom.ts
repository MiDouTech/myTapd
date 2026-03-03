import type {
  WecomGroupBindingCreateInput,
  WecomGroupBindingListOutput,
  WecomGroupBindingUpdateInput,
} from '@/types/wecom'
import request from '@/utils/request'

/**
 * 查询企微群绑定配置列表
 * 接口编号：API000021（与 Bug简报模块存在历史编号重复，待Task021统一治理）
 * 产品文档功能：4.6.4 企微群机器人工单 - 群与工单分类绑定管理
 */
export function getWecomGroupBindingList(): Promise<WecomGroupBindingListOutput[]> {
  return request.get<WecomGroupBindingListOutput[]>('/wecom/group-binding/list')
}

/**
 * 新增企微群绑定配置
 * 接口编号：API000022（与 Bug简报模块存在历史编号重复，待Task021统一治理）
 * 产品文档功能：4.6.4 企微群机器人工单 - 群绑定配置新增
 */
export function createWecomGroupBinding(data: WecomGroupBindingCreateInput): Promise<number> {
  return request.post<number>('/wecom/group-binding/create', data)
}

/**
 * 修改企微群绑定配置
 * 接口编号：API000023（与 Bug简报模块存在历史编号重复，待Task021统一治理）
 * 产品文档功能：4.6.4 企微群机器人工单 - 群绑定配置修改
 */
export function updateWecomGroupBinding(
  id: number,
  data: WecomGroupBindingUpdateInput,
): Promise<void> {
  return request.put<void>(`/wecom/group-binding/update/${id}`, data)
}
