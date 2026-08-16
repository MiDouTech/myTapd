export type CustomFieldType =
  | 'INPUT'
  | 'TEXTAREA'
  | 'SELECT'
  | 'ATTACHMENT'
  | 'DATE'
  | 'TIME'
  | 'DATETIME'
  | 'NUMBER'
  | 'RADIO'
  | 'CHECKBOX'
  | 'CASCADER'

export interface CustomFieldOption {
  label: string
  value: string
  sortOrder?: number
  enabled?: number
}

export interface CustomFieldOutput {
  id: number
  fieldCode: string
  fieldTitle: string
  fieldType: CustomFieldType
  fieldTypeLabel?: string
  description?: string
  placeholder?: string
  defaultRequired: number
  defaultValue?: string
  maxLength?: number
  options: CustomFieldOption[]
  isActive: number
  referenceCount: number
  createTime?: string
  updateTime?: string
}

export interface CustomFieldCreateInput {
  fieldTitle: string
  fieldType: CustomFieldType
  description?: string
  placeholder?: string
  defaultRequired: number
  defaultValue?: string
  maxLength?: number
  options?: CustomFieldOption[]
}

export interface CustomFieldUpdateInput extends Omit<CustomFieldCreateInput, 'fieldType'> {
  id: number
  isActive?: number
}
