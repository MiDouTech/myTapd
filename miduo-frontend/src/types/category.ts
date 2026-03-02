export interface CategoryTreeOutput {
  id: number
  name: string
  parentId?: number
  level: number
  path?: string
  templateId?: number
  workflowId?: number
  slaPolicyId?: number
  defaultGroupId?: number
  sortOrder?: number
  isActive?: number
  children?: CategoryTreeOutput[]
}

export interface CategoryDetailOutput {
  id: number
  name: string
  parentId?: number
  parentName?: string
  level: number
  path?: string
  fullPathName?: string
  templateId?: number
  templateName?: string
  workflowId?: number
  workflowName?: string
  slaPolicyId?: number
  slaPolicyName?: string
  defaultGroupId?: number
  defaultGroupName?: string
  sortOrder?: number
  isActive?: number
  createTime?: string
  updateTime?: string
}

export interface CategoryCreateInput {
  name: string
  parentId?: number
  level: number
  templateId?: number
  workflowId?: number
  slaPolicyId?: number
  defaultGroupId?: number
  sortOrder?: number
}

export interface CategoryUpdateInput {
  name?: string
  templateId?: number
  workflowId?: number
  slaPolicyId?: number
  defaultGroupId?: number
  sortOrder?: number
  isActive?: number
}
