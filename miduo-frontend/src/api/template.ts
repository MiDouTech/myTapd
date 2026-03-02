import type { TemplateListOutput } from '@/types/template'
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
