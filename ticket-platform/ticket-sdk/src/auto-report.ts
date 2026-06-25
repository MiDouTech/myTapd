export interface HttpErrorCapture {
  statusCode: number
  url: string
  method: string
  responseMessage?: string
}

export interface AutoReportOptions {
  /** 是否启用 HTTP 自动捕获，默认 false */
  enabled?: boolean
  /** 捕获的 HTTP 状态码，默认 [403, 500, 502, 503] */
  httpStatus?: number[]
  /** 仅监控 URL 包含这些前缀的请求；为空则监控全部（排除插件自身接口） */
  apiPatterns?: string[]
  /** 同一错误防抖间隔（毫秒），默认 300000（5 分钟） */
  debounceMs?: number
  /**
   * 捕获后是否弹窗让用户确认再提交，默认 true（推荐）
   * false 时仍会打开弹窗并预填描述，与 true 行为一致（不做静默建单）
   */
  confirmBeforeSubmit?: boolean
}

export interface AutoReportRuntimeOptions {
  debounceMs: number
  httpStatus: Set<number>
  apiPatterns: string[]
  onCapture: (error: HttpErrorCapture) => void
  shouldIgnoreUrl: (url: string) => boolean
}

const DEFAULT_HTTP_STATUS = [403, 500, 502, 503]
const DEFAULT_DEBOUNCE_MS = 300_000

export function normalizeAutoReportOptions(options?: AutoReportOptions): AutoReportRuntimeOptions | null {
  if (!options?.enabled) {
    return null
  }
  const httpStatus = new Set(
    (options.httpStatus && options.httpStatus.length > 0 ? options.httpStatus : DEFAULT_HTTP_STATUS).map(
      (code) => Number(code),
    ),
  )
  return {
    debounceMs: options.debounceMs ?? DEFAULT_DEBOUNCE_MS,
    httpStatus,
    apiPatterns: (options.apiPatterns ?? []).map((item) => item.trim()).filter(Boolean),
    onCapture: () => undefined,
    shouldIgnoreUrl: () => false,
  }
}

export function buildHttpErrorDescription(error: HttpErrorCapture): string {
  const method = (error.method || 'GET').toUpperCase()
  const path = simplifyUrl(error.url)
  const hint =
    error.statusCode === 403
      ? '（可能是权限不足或登录失效，请确认是否属于系统故障）'
      : ''
  return `接口返回 ${error.statusCode}：${method} ${path}${hint}`
}

function simplifyUrl(rawUrl: string): string {
  try {
    const parsed = new URL(rawUrl, window.location.origin)
    return `${parsed.pathname}${parsed.search}`
  } catch {
    return rawUrl
  }
}

export class HttpAutoReporter {
  private runtime: AutoReportRuntimeOptions | null = null
  private recentKeys = new Map<string, number>()
  private originalFetch: typeof fetch | null = null
  private xhrPatched = false
  private originalXhrOpen: typeof XMLHttpRequest.prototype.open | null = null
  private originalXhrSend: typeof XMLHttpRequest.prototype.send | null = null

  start(runtime: AutoReportRuntimeOptions): void {
    this.stop()
    this.runtime = runtime
    this.patchFetch()
    this.patchXhr()
  }

  stop(): void {
    if (this.originalFetch) {
      window.fetch = this.originalFetch
      this.originalFetch = null
    }
    if (this.xhrPatched && this.originalXhrOpen && this.originalXhrSend) {
      XMLHttpRequest.prototype.open = this.originalXhrOpen
      XMLHttpRequest.prototype.send = this.originalXhrSend
      this.originalXhrOpen = null
      this.originalXhrSend = null
      this.xhrPatched = false
    }
    this.runtime = null
    this.recentKeys.clear()
  }

  /** 供 axios 等外部拦截器手动上报 */
  report(error: HttpErrorCapture): void {
    if (!this.runtime) {
      return
    }
    this.handleCapture(error)
  }

  updateOnCapture(onCapture: (error: HttpErrorCapture) => void): void {
    if (this.runtime) {
      this.runtime.onCapture = onCapture
    }
  }

  private patchFetch(): void {
    if (this.originalFetch) {
      return
    }
    this.originalFetch = window.fetch.bind(window)
    const reporter = this
    window.fetch = async function patchedFetch(
      input: RequestInfo | URL,
      init?: RequestInit,
    ): Promise<Response> {
      const method = (init?.method ?? 'GET').toUpperCase()
      const url = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url
      const response = await reporter.originalFetch!(input, init)
      reporter.inspectResponse(url, method, response.status, response.clone())
      return response
    }
  }

  private patchXhr(): void {
    if (this.xhrPatched) {
      return
    }
    this.xhrPatched = true
    const reporter = this
    this.originalXhrOpen = XMLHttpRequest.prototype.open
    this.originalXhrSend = XMLHttpRequest.prototype.send
    const originalOpen = this.originalXhrOpen
    const originalSend = this.originalXhrSend

    XMLHttpRequest.prototype.open = function open(
      this: XMLHttpRequest & { __ticketSdkMethod?: string; __ticketSdkUrl?: string },
      method: string,
      url: string | URL,
      ...rest: unknown[]
    ) {
      this.__ticketSdkMethod = (method || 'GET').toUpperCase()
      this.__ticketSdkUrl = typeof url === 'string' ? url : url.toString()
      return originalOpen.call(this, method, url, ...(rest as [boolean?, string?, string?]))
    }

    XMLHttpRequest.prototype.send = function send(this: XMLHttpRequest & {
      __ticketSdkMethod?: string
      __ticketSdkUrl?: string
    }, ...args: unknown[]) {
      this.addEventListener('loadend', () => {
        if (this.__ticketSdkUrl) {
          reporter.inspectResponse(this.__ticketSdkUrl, this.__ticketSdkMethod ?? 'GET', this.status, null, this)
        }
      })
      return originalSend.apply(this, args as [Document | XMLHttpRequestBodyInit | null | undefined])
    }
  }

  private async inspectResponse(
    rawUrl: string,
    method: string,
    statusCode: number,
    responseClone: Response | null,
    xhr?: XMLHttpRequest,
  ): Promise<void> {
    if (!this.runtime || !this.runtime.httpStatus.has(statusCode)) {
      return
    }
    const url = this.resolveUrl(rawUrl)
    if (this.runtime.shouldIgnoreUrl(url)) {
      return
    }
    if (!this.matchApiPattern(url)) {
      return
    }
    const responseMessage = await this.extractResponseMessage(responseClone, xhr)
    this.handleCapture({ statusCode, url, method, responseMessage })
  }

  private handleCapture(error: HttpErrorCapture): void {
    if (!this.runtime) {
      return
    }
    const debounceKey = `${error.statusCode}:${error.method}:${simplifyUrl(error.url)}`
    const now = Date.now()
    const lastAt = this.recentKeys.get(debounceKey)
    if (lastAt != null && now - lastAt < this.runtime.debounceMs) {
      return
    }
    this.recentKeys.set(debounceKey, now)
    this.runtime.onCapture(error)
  }

  private resolveUrl(rawUrl: string): string {
    try {
      return new URL(rawUrl, window.location.origin).toString()
    } catch {
      return rawUrl
    }
  }

  private matchApiPattern(url: string): boolean {
    if (!this.runtime || this.runtime.apiPatterns.length === 0) {
      return true
    }
    const path = simplifyUrl(url)
    return this.runtime.apiPatterns.some((pattern) => path.includes(pattern) || url.includes(pattern))
  }

  private async extractResponseMessage(
    responseClone: Response | null,
    xhr?: XMLHttpRequest,
  ): Promise<string | undefined> {
    try {
      if (responseClone) {
        const contentType = responseClone.headers.get('content-type') ?? ''
        if (contentType.includes('application/json')) {
          const json = await responseClone.json()
          if (json && typeof json === 'object') {
            const message = (json as { message?: string }).message
            return typeof message === 'string' ? message.slice(0, 200) : undefined
          }
        }
        const text = await responseClone.text()
        return text ? text.slice(0, 200) : undefined
      }
      if (xhr?.responseText) {
        return xhr.responseText.slice(0, 200)
      }
    } catch {
      return undefined
    }
    return undefined
  }
}

export function createAxiosErrorInterceptor(
  report: (error: HttpErrorCapture) => void,
  options?: Pick<AutoReportOptions, 'httpStatus' | 'apiPatterns'>,
) {
  const statusSet = new Set(
    (options?.httpStatus && options.httpStatus.length > 0 ? options.httpStatus : DEFAULT_HTTP_STATUS).map(Number),
  )
  const patterns = (options?.apiPatterns ?? []).map((item) => item.trim()).filter(Boolean)

  return (error: {
    response?: { status?: number; data?: { message?: string } }
    config?: { url?: string; method?: string }
  }) => {
    const statusCode = error.response?.status
    const url = error.config?.url
    if (statusCode == null || !url || !statusSet.has(statusCode)) {
      return Promise.reject(error)
    }
    const path = simplifyUrl(url)
    if (patterns.length > 0 && !patterns.some((pattern) => path.includes(pattern) || url.includes(pattern))) {
      return Promise.reject(error)
    }
    report({
      statusCode,
      url,
      method: (error.config?.method ?? 'GET').toUpperCase(),
      responseMessage:
        typeof error.response?.data?.message === 'string' ? error.response.data.message : undefined,
    })
    return Promise.reject(error)
  }
}
