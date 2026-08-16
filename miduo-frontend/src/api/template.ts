import type {
  TemplateAdminListOutput,
  TemplateCreateInput,
  TemplateDetailOutput,
  TemplateListOutput,
  TemplateUpdateInput,
} from '@/types/template'
import request from '@/utils/request'

/**
 * 按分类查询模板列表
 * 接口编号：API000005
 * 产品文档功能：4.3.2 工单模板与自定义字段 - 模板列表
 */
export function getTemplateList(categoryId?: number): Promise<TemplateListOutput[]> {
  return request.get<TemplateListOutput[]>('/template/list', {
    params: { categoryId },
  })
}

export function getTemplateAdminList(params?: {
  keyword?: string
  isActive?: number
}): Promise<TemplateAdminListOutput[]> {
  return request.get<TemplateAdminListOutput[]>('/template/admin/list', { params })
}

export function getTemplateDetail(id: number): Promise<TemplateDetailOutput> {
  return request.get<TemplateDetailOutput>(`/template/admin/${id}`)
}

export function createTemplate(data: TemplateCreateInput): Promise<TemplateDetailOutput> {
  return request.post<TemplateDetailOutput>('/template/admin/create', data)
}

export function updateTemplate(data: TemplateUpdateInput): Promise<TemplateDetailOutput> {
  return request.put<TemplateDetailOutput>('/template/admin/update', data)
}

export function deleteTemplate(id: number): Promise<void> {
  return request.del<void>(`/template/admin/delete/${id}`)
}
