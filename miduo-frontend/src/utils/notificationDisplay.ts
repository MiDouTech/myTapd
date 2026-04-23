export interface NotificationKvLine {
  label: string
  value: string
}

/**
 * 将「标签：值」按行排列的通知正文解析为键值列表（与工单状态类通知格式一致）。
 * 若任一行不符合该格式，返回 null，由调用方回退为纯文本展示。
 */
export function tryParseNotificationKeyValueContent(
  content: string | null | undefined,
): NotificationKvLine[] | null {
  if (content == null || !content.trim()) {
    return null
  }
  const rawLines = content.split('\n')
  const lines: NotificationKvLine[] = []
  const kvPattern = /^([^:\n：]+)[：:]\s*(.*)$/
  for (const raw of rawLines) {
    const line = raw.trim()
    if (!line) {
      continue
    }
    const m = line.match(kvPattern)
    if (!m) {
      return null
    }
    lines.push({ label: m[1].trim(), value: (m[2] ?? '').trim() || '-' })
  }
  return lines.length > 0 ? lines : null
}

export type NotificationDisplayBody =
  | { mode: 'kv'; lines: NotificationKvLine[] }
  | { mode: 'text'; text: string }

export function notificationDisplayBody(content: string | null | undefined): NotificationDisplayBody {
  const lines = tryParseNotificationKeyValueContent(content)
  if (lines) {
    return { mode: 'kv', lines }
  }
  const text = content != null && content.trim() ? content.trim() : '-'
  return { mode: 'text', text }
}
