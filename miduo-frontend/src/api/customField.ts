import type {
  CustomFieldCreateInput,
  CustomFieldOutput,
  CustomFieldUpdateInput,
} from '@/types/customField'
import request from '@/utils/request'

export function getCustomFieldList(): Promise<CustomFieldOutput[]> {
  return request.get<CustomFieldOutput[]>('/custom-field/list')
}

export function createCustomField(data: CustomFieldCreateInput): Promise<CustomFieldOutput> {
  return request.post<CustomFieldOutput>('/custom-field/create', data)
}

export function updateCustomField(data: CustomFieldUpdateInput): Promise<CustomFieldOutput> {
  return request.put<CustomFieldOutput>('/custom-field/update', data)
}

export function deleteCustomField(id: number): Promise<void> {
  return request.del<void>(`/custom-field/delete/${id}`)
}
