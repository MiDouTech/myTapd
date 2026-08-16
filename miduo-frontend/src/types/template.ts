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

export interface TemplateAdminListOutput {
  id: number
  templateCode: string
  name: string
  description?: string
  isSystem: number
  isActive: number
  fieldCount: number
  createTime?: string
  updateTime?: string
}

export interface TemplateFieldOutput {
  fieldSource: 'SYSTEM' | 'CUSTOM'
  customFieldId?: number
  fieldCode: string
  fieldTitle: string
  fieldType: string
  fieldTypeLabel: string
  sortOrder: number
  isEnabled: number
  isRequired: number
  placeholder?: string
  defaultValue?: string
  options?: TemplateFieldOption[]
  deletable: boolean
  definitionActive: boolean
}

export interface TemplateDetailOutput {
  id: number
  templateCode: string
  name: string
  description?: string
  isSystem: number
  isActive: number
  fieldCount: number
  createTime?: string
  updateTime?: string
  fields: TemplateFieldOutput[]
}

export interface TemplateCustomFieldInput {
  customFieldId: number
  sortOrder: number
  isEnabled: number
  isRequired: number
  placeholderOverride?: string
  defaultValueOverride?: string
}

export interface TemplateCreateInput {
  name: string
  description?: string
  customFields: TemplateCustomFieldInput[]
}

export interface TemplateUpdateInput extends Partial<TemplateCreateInput> {
  id: number
  isActive?: number
}
