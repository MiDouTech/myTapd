export interface WecomGroupBindingListOutput {
  id: number
  chatId: string
  chatName?: string
  defaultCategoryId?: number
  defaultCategoryName?: string
  webhookUrl?: string
  isActive: number
  createBy?: string
  updateBy?: string
  createTime?: string
  updateTime?: string
}

export interface WecomGroupBindingCreateInput {
  chatId: string
  chatName?: string
  defaultCategoryId?: number
  webhookUrl?: string
  isActive?: number
}

export interface WecomGroupBindingUpdateInput {
  chatName?: string
  defaultCategoryId?: number
  webhookUrl?: string
  isActive?: number
}
