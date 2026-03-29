import { ref } from 'vue'

/** 顶部栏工单搜索框文案，与工单列表筛选联动 */
export const layoutTicketSearchKeyword = ref('')

const STORAGE_KEY = 'md-layout-ticket-search'
/** 头部提交空关键词时置位，列表页消费后清除，用于清空编号/标题条件 */
const CLEAR_FLAG_KEY = 'md-ticket-list-clear-keyword'

export function persistLayoutTicketSearch(value: string): void {
  const v = value.trim()
  if (!v) {
    sessionStorage.removeItem(STORAGE_KEY)
    return
  }
  sessionStorage.setItem(STORAGE_KEY, v)
}

export function readPersistedLayoutTicketSearch(): string {
  return sessionStorage.getItem(STORAGE_KEY)?.trim() ?? ''
}

export function markTicketListKeywordClearFromHeader(): void {
  sessionStorage.setItem(CLEAR_FLAG_KEY, '1')
}

export function consumeTicketListKeywordClearFromHeader(): boolean {
  if (sessionStorage.getItem(CLEAR_FLAG_KEY) !== '1') {
    return false
  }
  sessionStorage.removeItem(CLEAR_FLAG_KEY)
  return true
}
