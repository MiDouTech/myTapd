export function formatDateTime(input?: string): string {
  if (!input) {
    return '-'
  }
  const date = new Date(input)
  if (Number.isNaN(date.getTime())) {
    return input
  }
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function formatFileSize(size?: number): string {
  if (!size || size <= 0) {
    return '-'
  }
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(2)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

/** 将秒数格式化为可读耗时（工单节点耗时、时间追踪等） */
export function formatDurationSec(seconds?: number | null): string {
  if (seconds === undefined || seconds === null) {
    return '-'
  }
  const total = Math.max(0, Math.floor(seconds))
  const hour = Math.floor(total / 3600)
  const minute = Math.floor((total % 3600) / 60)
  const second = total % 60
  if (hour > 0) {
    return `${hour}h ${minute}m ${second}s`
  }
  if (minute > 0) {
    return `${minute}m ${second}s`
  }
  return `${second}s`
}
