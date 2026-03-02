export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageOutput<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

export interface PageQuery {
  pageNum: number
  pageSize: number
  orderBy?: string
  asc?: boolean
}

export interface SelectOption<T = string | number> {
  label: string
  value: T
}
