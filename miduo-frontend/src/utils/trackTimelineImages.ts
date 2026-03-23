/**
 * 从字段文本中提取可预览的图片 URL（截图字段、富文本 img、裸链等）
 */
export function extractImageUrlsFromText(text: string | null | undefined): string[] {
  if (!text) return []
  const found = new Set<string>()

  const urlRe = /https?:\/\/[^\s"'<>]+/gi
  let m: RegExpExecArray | null
  while ((m = urlRe.exec(text)) !== null) {
    const u = m[0].replace(/[.,;)}]+$/g, '')
    if (isLikelyImageUrl(u)) {
      found.add(u)
    }
  }

  const imgSrcRe = /src\s*=\s*["'](https?:[^"']+)["']/gi
  while ((m = imgSrcRe.exec(text)) !== null) {
    const src = m[1]
    if (src && isLikelyImageUrl(src)) {
      found.add(src)
    }
  }

  for (const part of text.split(/[,，\n]+/)) {
    const p = part.trim()
    if (/^https?:\/\//i.test(p) && isLikelyImageUrl(p)) {
      found.add(p)
    }
  }

  return [...found]
}

function isLikelyImageUrl(url: string): boolean {
  if (!url || url.length > 2048) return false
  if (/\.(jpe?g|png|gif|webp|bmp|svg)(\?|#|$)/i.test(url)) return true
  if (/qiniudn\.com|qiniup\.com|clouddn\.com|qnssl\.com|qpic\.cn/i.test(url)) return true
  return false
}
