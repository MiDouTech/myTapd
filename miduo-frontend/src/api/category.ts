import type {
  CategoryCreateInput,
  CategoryDetailOutput,
  CategoryTreeOutput,
  CategoryUpdateInput,
} from '@/types/category'
import request from '@/utils/request'

/**
 * 获取三级分类树
 * 接口编号：API000001
 * 产品文档功能：4.3.1 工单分类体系 - 三级分类树展示
 */
export function getCategoryTree(): Promise<CategoryTreeOutput[]> {
  return request.get<CategoryTreeOutput[]>('/category/tree')
}

/**
 * 获取分类详情
 * 接口编号：API000002
 * 产品文档功能：4.3.1 工单分类体系 - 分类详情查看
 */
export function getCategoryDetail(id: number): Promise<CategoryDetailOutput> {
  return request.get<CategoryDetailOutput>(`/category/detail/${id}`)
}

/**
 * 新增分类
 * 接口编号：API000003
 * 产品文档功能：4.3.1 工单分类体系 - 新增分类
 */
export function createCategory(data: CategoryCreateInput): Promise<number> {
  return request.post<number>('/category/create', data)
}

/**
 * 修改分类
 * 接口编号：API000004
 * 产品文档功能：4.3.1 工单分类体系 - 修改分类
 */
export function updateCategory(id: number, data: CategoryUpdateInput): Promise<void> {
  return request.put<void>(`/category/update/${id}`, data)
}

/**
 * 删除分类
 * 接口编号：API000422
 * 产品文档功能：4.3.1 工单分类体系 - 删除分类
 */
export function deleteCategory(id: number): Promise<void> {
  return request.del<void>(`/category/delete/${id}`)
}
