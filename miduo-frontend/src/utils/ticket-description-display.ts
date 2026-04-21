/**
 * 工单描述展示：企微自然语言建单常把多段字段拼成一行，这里在常见「字段名：」前补换行，
 * 并把换行渲染为 <br>；已是带段落/换行标签的富文本时保持原样，避免破坏排版。
 */

const RICH_HTML_PATTERN =
  /<\s*(p|div|ul|ol|li|table|thead|tbody|tr|td|th|img|h[1-6])\b|<br\s*\/?>/i

/** 企微单行模板里多个「字段名：值」连在一起时在字段前换行，并保留用户使用的半角/全角冒号 */
const WECOM_FIELD_LABEL_PATTERN =
  /(?<![\r\n])\s+((?:商户编号|公司名称|商户账号|场景码|问题描述|预期结果)\s*[:：])/g

const ROBOT_PREFIX_PATTERN =
  /^机器人\s+(?=(?:商户编号|公司名称|商户账号|场景码|问题描述|预期结果)\s*[:：])/

function looksLikeRichTicketDescription(raw: string): boolean {
  const t = raw.trim()
  if (!t.includes('<')) {
    return false
  }
  return RICH_HTML_PATTERN.test(t)
}

function insertWecomFieldLineBreaks(plain: string): string {
  return plain.replace(ROBOT_PREFIX_PATTERN, '机器人\n').replace(WECOM_FIELD_LABEL_PATTERN, '\n$1')
}

function stripHtmlToPlain(html: string): string {
  if (typeof DOMParser === 'undefined') {
    return html.replace(/<[^>]+>/g, ' ')
  }
  const doc = new DOMParser().parseFromString(html, 'text/html')
  return doc.body?.textContent ?? ''
}

/**
 * 将工单内富文本（客服/测试等字段）转为单行纯文本，避免简报「问题描述」自动拼接时出现裸 HTML 标签。
 */
export function ticketRichTextToPlainLine(raw: string | null | undefined): string {
  if (raw == null || String(raw).trim() === '') {
    return ''
  }
  const text = stripHtmlToPlain(String(raw))
    .replace(/\r\n/g, '\n')
    .replace(/\n+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  return text
}

/**
 * 富文本转纯文本并保留换行，供 `pre-wrap` 展示（简报详情「问题描述」等）。
 */
export function ticketRichTextToPlainMultiline(raw: string | null | undefined): string {
  if (raw == null || String(raw).trim() === '') {
    return ''
  }
  return stripHtmlToPlain(String(raw))
    .replace(/\r\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

/**
 * 将接口返回的工单描述格式化为可安全 v-html 的片段（换行可见）。
 */
export function formatTicketDescriptionForDisplay(raw: string | null | undefined): string {
  if (raw == null || raw.trim() === '') {
    return ''
  }
  const trimmed = raw.trim()
  if (looksLikeRichTicketDescription(trimmed)) {
    return trimmed
  }
  const plain = insertWecomFieldLineBreaks(stripHtmlToPlain(trimmed).replace(/\r\n/g, '\n')).trim()
  if (!plain) {
    return ''
  }
  return escapeHtml(plain).replace(/\n/g, '<br />')
}
