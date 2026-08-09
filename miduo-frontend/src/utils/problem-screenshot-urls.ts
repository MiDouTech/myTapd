/**
 * 与工单列表预览、详情页客服区一致：问题截图字段为逗号/分号/换行分隔的 URL 列表。
 * 只用于「问题截图」业务字段，不包含附件区其它文件。
 */
function uniqStringList(values: string[]): string[] {
  return Array.from(new Set(values.filter((item) => Boolean(item && item.trim()))))
}

export function parseProblemScreenshotUrls(raw?: string | null): string[] {
  if (raw == null || !raw.trim()) {
    return []
  }
  return uniqStringList(
    raw
      .split(/[,，;\n]/)
      .map((item) => item.trim())
      .filter(Boolean),
  )
}

/** 公开页等场景只展示可直链打开的图片地址 */
export function filterHttpImageUrls(urls: string[]): string[] {
  return urls.filter((url) => isHttpMediaUrl(url) && !isVideoUrl(url))
}

const VIDEO_FILE_EXTENSION = /\.(?:mp4|webm|ogg|ogv|mov|m4v)(?:$|[?#])/i

export function isHttpMediaUrl(url: string): boolean {
  return /^https?:\/\//i.test(url.trim())
}

/** 上传接口只回传 URL，因此通过去除签名参数后的文件扩展名识别视频。 */
export function isVideoUrl(url: string): boolean {
  return VIDEO_FILE_EXTENSION.test(url.trim())
}

export function filterHttpVideoUrls(urls: string[]): string[] {
  return urls.filter((url) => isHttpMediaUrl(url) && isVideoUrl(url))
}
