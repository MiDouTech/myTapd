export interface TemplateListOutput {
  id: number
  name: string
  categoryId?: number
  categoryName?: string
  fieldsConfig?: string
  description?: string
  isActive?: number
  createTime?: string
}

export interface TemplateFieldOption {
  label: string
  value: string
}

export interface TemplateFieldConfigItem {
  key: string
  label: string
  type?: 'input' | 'textarea' | 'select' | 'date'
  placeholder?: string
  required?: boolean
  options?: TemplateFieldOption[]
}
