/**
 * 判断顶部搜索框输入是否按「工单编号」检索（否则按标题模糊）。
 * 与 Bug 简报 PRD 一致：支持 WO- / BUG- 前缀；无空格的单段疑似编号也走编号前缀匹配。
 */
export function isLikelyTicketNoQuery(raw: string): boolean {
  const t = raw.trim()
  if (!t) {
    return false
  }
  if (/\s/.test(t)) {
    return false
  }
  const u = t.toUpperCase()
  if (u.startsWith('WO-') || u.startsWith('BUG-')) {
    return true
  }
  // 字母数字与连字符，且含至少一段数字（如 WO20260228-001 等变体仍偏编号）
  if (/^[A-Za-z0-9-]+$/.test(t) && /\d/.test(t) && t.includes('-')) {
    return true
  }
  return false
}
