import {
  AutoReportOptions,
  HttpAutoReporter,
  HttpErrorCapture,
  buildHttpErrorDescription,
  createAxiosErrorInterceptor,
  normalizeAutoReportOptions,
} from './auto-report'

export type { AutoReportOptions, HttpErrorCapture }
export { createAxiosErrorInterceptor }

export interface TicketSdkUser {
  id: string
  name: string
  dept?: string
  role?: string
  mobile?: string
}

export interface TicketSdkContext {
  module?: string
  page?: string
  pageUrl?: string
  pageTitle?: string
  bizId?: string
  bizType?: string
  bizNo?: string
  merchantNo?: string
  companyName?: string
  merchantAccount?: string
  sceneCode?: string
  expectedResult?: string
  extra?: Record<string, unknown>
}

export interface TicketSdkInitOptions {
  appKey: string
  system: string
  launchToken: string
  apiBase?: string
  user: TicketSdkUser
  mode?: 'modal' | 'sidebar' | 'float'
  entry?: string
  autoReport?: AutoReportOptions
}

export interface TicketSdkConfig {
  appName: string
  systemCode: string
  defaultPriority: string
  showPriorityPicker: boolean
  theme?: { primaryColor?: string }
}

export interface TicketUpdatedEvent {
  ticketNo: string
  status: string
}

type TicketSdkEvent = 'ticket:updated'
type EventHandler = (payload: TicketUpdatedEvent) => void

interface SanitizedDescriptionResult {
  html: string
  plainText: string
  removedInlineImageCount: number
}

interface EnvInfo {
  browser: string
  os: string
  screen: string
  userAgent: string
  network: string
}

const DEFAULT_API_BASE = ''

class TicketSdkImpl {
  private options: TicketSdkInitOptions | null = null
  private config: TicketSdkConfig | null = null
  private context: TicketSdkContext = {}
  private floatEl: HTMLElement | null = null
  private overlayEl: HTMLElement | null = null
  private handlers = new Map<TicketSdkEvent, Set<EventHandler>>()
  private autoReporter = new HttpAutoReporter()

  async init(options: TicketSdkInitOptions): Promise<void> {
    this.options = { ...options, mode: options.mode ?? 'float' }
    this.config = await this.fetchConfig()
    this.mountEntry()
    this.setupAutoReport(options.autoReport)
  }

  setContext(context: TicketSdkContext): void {
    this.context = { ...this.context, ...context }
  }

  setUser(user: TicketSdkUser): void {
    if (this.options) {
      this.options.user = user
    }
  }

  open(prefillDescription?: string): void {
    this.openModal(false, prefillDescription)
  }

  openMyTickets(): void {
    this.openModal(true)
  }

  /** 手动上报 HTTP 错误（供 axios 拦截器或业务代码调用） */
  reportHttpError(error: HttpErrorCapture): void {
    this.handleHttpErrorCapture(error)
  }

  on(event: TicketSdkEvent, handler: EventHandler): void {
    if (!this.handlers.has(event)) {
      this.handlers.set(event, new Set())
    }
    this.handlers.get(event)!.add(handler)
  }

  destroy(): void {
    this.autoReporter.stop()
    this.floatEl?.remove()
    this.overlayEl?.remove()
    this.floatEl = null
    this.overlayEl = null
    this.handlers.clear()
  }

  private get apiBase(): string {
    return (this.options?.apiBase ?? DEFAULT_API_BASE).replace(/\/$/, '')
  }

  private async fetchConfig(): Promise<TicketSdkConfig> {
    const appKey = encodeURIComponent(this.options!.appKey)
    const response = await fetch(`${this.apiBase}/api/open/v1/plugin/config?appKey=${appKey}`, {
      credentials: 'include',
    })
    const result = await response.json()
    if (!response.ok || result.code !== 200) {
      throw new Error(result.message || '加载插件配置失败')
    }
    return result.data as TicketSdkConfig
  }

  private mountEntry(): void {
    if (!this.options) return
    if (this.options.entry) {
      const button = document.querySelector(this.options.entry)
      if (button) {
        button.addEventListener('click', () => this.openModal())
      }
    }
    if (this.options.mode === 'float' || !this.options.entry) {
      this.mountFloatButton()
    }
  }

  private mountFloatButton(): void {
    const primary = this.config?.theme?.primaryColor ?? '#1675d1'
    const button = document.createElement('button')
    button.type = 'button'
    button.textContent = '工单'
    button.title = '提交工单'
    button.style.cssText = [
      'position:fixed',
      'right:24px',
      'bottom:24px',
      'z-index:99998',
      'width:52px',
      'height:52px',
      'border:none',
      'border-radius:50%',
      'color:#fff',
      `background:${primary}`,
      'box-shadow:0 4px 12px rgba(0,0,0,.15)',
      'cursor:pointer',
      'font-size:14px',
      'font-weight:500',
    ].join(';')
    button.addEventListener('click', () => this.openModal())
    document.body.appendChild(button)
    this.floatEl = button
  }

  private openModal(myTickets = false, prefillDescription?: string, autoCaptured = false): void {
    if (!this.options) return
    this.overlayEl?.remove()
    const primary = this.config?.theme?.primaryColor ?? '#1675d1'
    const overlay = document.createElement('div')
    overlay.style.cssText =
      'position:fixed;inset:0;background:rgba(0,0,0,.45);z-index:99999;display:flex;align-items:center;justify-content:center;'
    const panel = document.createElement('div')
    panel.style.cssText =
      'width:420px;max-width:92vw;max-height:92vh;background:#fff;border-radius:8px;box-shadow:0 8px 24px rgba(0,0,0,.18);padding:20px;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif;display:flex;flex-direction:column;overflow:hidden;'
    panel.innerHTML = myTickets
      ? `<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;flex:0 0 auto;">
           <strong style="font-size:16px;">我的工单</strong>
           <button type="button" data-action="close" style="border:none;background:transparent;font-size:20px;cursor:pointer;">×</button>
         </div>
         <div data-role="list-container" style="flex:1 1 auto;min-height:0;overflow:auto;">
           <div data-role="list" style="min-height:120px;color:#606266;">加载中...</div>
         </div>`
      : `<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;flex:0 0 auto;">
           <strong style="font-size:16px;">提交工单</strong>
           <button type="button" data-action="close" style="border:none;background:transparent;font-size:20px;cursor:pointer;">×</button>
         </div>
         <div data-role="submit-scroll" style="flex:1 1 auto;min-height:0;overflow:auto;padding-right:2px;">
           ${autoCaptured ? '<div data-role="hint" style="margin-bottom:10px;padding:8px 10px;background:#f0f9ff;border:1px solid #b3d8ff;border-radius:4px;font-size:13px;color:#1675d1;">检测到接口异常，已自动填写问题描述，请确认后提交。</div>' : ''}
           <div data-role="description" contenteditable="true" style="width:100%;min-height:140px;box-sizing:border-box;padding:10px;border:1px solid #dcdfe6;border-radius:4px;overflow:auto;outline:none;line-height:1.6;word-break:break-word;overflow-wrap:anywhere;"></div>
           <div style="margin-top:6px;font-size:12px;color:#909399;">支持富文本输入，可上传图片或直接粘贴图片，图片会随工单附件一起提交。</div>
           <div style="margin-top:10px;display:flex;align-items:center;justify-content:space-between;gap:10px;">
             <div data-role="attachment-list" style="font-size:12px;color:#909399;word-break:break-all;flex:1 1 auto;">未上传图片</div>
             <div style="display:flex;align-items:center;gap:8px;">
               <input data-role="image-input" type="file" accept="image/*" multiple style="display:none;" />
               <button type="button" data-action="pick-image" style="padding:6px 12px;border:1px solid #dcdfe6;background:#fff;border-radius:4px;cursor:pointer;">上传图片</button>
             </div>
           </div>
         </div>
         <div style="margin-top:12px;display:flex;align-items:center;justify-content:space-between;gap:8px;flex:0 0 auto;">
           <button type="button" data-action="open-my-tickets" style="padding:8px 14px;border:1px solid #dcdfe6;background:#fff;border-radius:4px;cursor:pointer;color:#606266;">我的工单</button>
           <div style="display:flex;align-items:center;gap:8px;">
             <button type="button" data-action="close" style="padding:8px 14px;border:1px solid #dcdfe6;background:#fff;border-radius:4px;cursor:pointer;">取消</button>
             <button type="button" data-action="submit" style="padding:8px 14px;border:none;color:#fff;border-radius:4px;cursor:pointer;background:${primary};">提交</button>
           </div>
         </div>
         <div data-role="message" style="margin-top:8px;font-size:13px;color:#67c23a;flex:0 0 auto;"></div>`

    const uploadedAttachments: string[] = []
    const hasUnsavedDraft = (): boolean => {
      if (myTickets) {
        return false
      }
      const descriptionEl = panel.querySelector('[data-role="description"]') as HTMLDivElement | null
      const descriptionHtml = (descriptionEl?.innerHTML ?? '').trim()
      const sanitized = this.sanitizeDescriptionHtml(descriptionHtml)
      const hasInlineImage = /<img[\s>]/i.test(descriptionHtml)
      return !!sanitized.plainText || hasInlineImage || uploadedAttachments.length > 0
    }
    const tryCloseModal = (): void => {
      if (!hasUnsavedDraft()) {
        this.closeModal()
        return
      }
      const message = '当前内容尚未提交，确认关闭后将丢失已填写内容，是否继续关闭？'
      if (typeof window.confirm !== 'function' || window.confirm(message)) {
        this.closeModal()
      }
    }

    overlay.appendChild(panel)
    // 为什么禁用遮罩点击关闭：用户反馈误触频繁，统一改为只能通过明确按钮关闭，避免内容丢失。
    panel.querySelectorAll('[data-action="close"]').forEach((node) => {
      node.addEventListener('click', () => tryCloseModal())
    })
    if (myTickets) {
      void this.renderMyTickets(panel)
    } else {
      const descriptionEl = panel.querySelector('[data-role="description"]') as HTMLDivElement
      if (descriptionEl) {
        descriptionEl.innerHTML = '<p><br/></p>'
      }
      if (prefillDescription) {
        const content = escapeHtml(prefillDescription).replace(/\n/g, '<br/>')
        if (descriptionEl) {
          descriptionEl.innerHTML = content
        }
      }
      this.bindDescriptionEditorEvents(panel, descriptionEl, uploadedAttachments)
      const imageInput = panel.querySelector('[data-role="image-input"]') as HTMLInputElement
      panel.querySelector('[data-action="pick-image"]')?.addEventListener('click', () => {
        imageInput?.click()
      })
      imageInput?.addEventListener('change', async () => {
        const files = Array.from(imageInput.files ?? [])
        if (!files.length) {
          return
        }
        for (const file of files) {
          await this.uploadImageAndAttach(panel, descriptionEl, uploadedAttachments, file)
        }
        imageInput.value = ''
      })
      panel.querySelector('[data-action="submit"]')?.addEventListener('click', () => {
        void this.submitTicket(panel, uploadedAttachments)
      })
      panel.querySelector('[data-action="open-my-tickets"]')?.addEventListener('click', () => {
        if (hasUnsavedDraft()) {
          const switchMessage = '当前内容尚未提交，切换到“我的工单”后将丢失已填写内容，是否继续？'
          if (typeof window.confirm === 'function' && !window.confirm(switchMessage)) {
            return
          }
        }
        // 为什么从提交页直接跳我的工单：用户提交后常需要立刻查看状态，减少额外点击路径。
        this.openModal(true)
      })
    }
    document.body.appendChild(overlay)
    this.overlayEl = overlay
  }

  private closeModal(): void {
    this.overlayEl?.remove()
    this.overlayEl = null
  }

  private async submitTicket(panel: HTMLElement, attachments: string[]): Promise<void> {
    const descriptionEl = panel.querySelector('[data-role="description"]') as HTMLDivElement
    const descriptionHtml = (descriptionEl?.innerHTML ?? '').trim()
    const sanitizedDescription = this.sanitizeDescriptionHtml(descriptionHtml)
    const descriptionText = sanitizedDescription.plainText
    const messageEl = panel.querySelector('[data-role="message"]') as HTMLElement
    if (sanitizedDescription.removedInlineImageCount > 0 && attachments.length === 0) {
      messageEl.style.color = '#f56c6c'
      messageEl.textContent = '检测到未上传成功的内联图片，请重新粘贴或点击“上传图片”后再提交'
      return
    }
    if (!descriptionText && attachments.length === 0) {
      messageEl.style.color = '#f56c6c'
      messageEl.textContent = '请先填写问题描述或上传图片'
      return
    }
    const description = descriptionText
      ? sanitizedDescription.html
      : '<p>用户上传了问题图片，请结合附件排查。</p>'
    messageEl.style.color = '#909399'
    messageEl.textContent = '提交中...'
    try {
      const output = await this.createTicket(description, attachments)
      messageEl.style.color = '#67c23a'
      messageEl.textContent = `提交成功：${output.ticketNo}`
      this.emit('ticket:updated', { ticketNo: output.ticketNo, status: output.status })
      window.setTimeout(() => this.closeModal(), 1200)
    } catch (error) {
      messageEl.style.color = '#f56c6c'
      messageEl.textContent = error instanceof Error ? error.message : '提交失败'
    }
  }

  private sanitizeDescriptionHtml(descriptionHtml: string): SanitizedDescriptionResult {
    const trimmed = descriptionHtml.trim()
    if (!trimmed) {
      return { html: '', plainText: '', removedInlineImageCount: 0 }
    }

    const parser = new DOMParser()
    const doc = parser.parseFromString(`<div data-role="root">${trimmed}</div>`, 'text/html')
    const root = doc.querySelector('[data-role="root"]') as HTMLDivElement | null
    if (!root) {
      return { html: '', plainText: '', removedInlineImageCount: 0 }
    }

    let removedInlineImageCount = 0
    root.querySelectorAll('img').forEach((img) => {
      const src = (img.getAttribute('src') ?? '').trim().toLowerCase()
      // 为什么这里主动移除：dataURL 图片会把超长 base64 带进 description，导致后端入库失败。
      if (src.startsWith('data:image/')) {
        img.remove()
        removedInlineImageCount += 1
      }
    })

    root.querySelectorAll('script,style').forEach((node) => node.remove())
    const html = root.innerHTML.trim()
    const plainText = (root.textContent ?? '').replace(/\s+/g, ' ').trim()
    return { html, plainText, removedInlineImageCount }
  }

  private bindDescriptionEditorEvents(
    panel: HTMLElement,
    descriptionEl: HTMLDivElement,
    attachments: string[],
  ): void {
    if (!descriptionEl) {
      return
    }
    this.normalizeEditorImages(descriptionEl)
    descriptionEl.addEventListener('input', () => {
      this.normalizeEditorImages(descriptionEl)
    })
    descriptionEl.addEventListener('paste', (event: ClipboardEvent) => {
      const imageFiles = this.extractClipboardImageFiles(event)
      if (!imageFiles.length) {
        window.setTimeout(() => this.normalizeEditorImages(descriptionEl), 0)
        return
      }
      event.preventDefault()
      void (async () => {
        for (const imageFile of imageFiles) {
          // 为什么直接上传粘贴图片：避免 dataURL 直接进描述字段，导致内容过长或显示异常。
          await this.uploadImageAndAttach(panel, descriptionEl, attachments, imageFile)
        }
      })()
    })
  }

  private extractClipboardImageFiles(event: ClipboardEvent): File[] {
    const clipboard = event.clipboardData
    if (!clipboard || !clipboard.items || !clipboard.items.length) {
      return []
    }
    const imageFiles: File[] = []
    Array.from(clipboard.items).forEach((item) => {
      if (item.kind !== 'file' || !item.type || !item.type.startsWith('image/')) {
        return
      }
      const file = item.getAsFile()
      if (file) {
        imageFiles.push(file)
      }
    })
    return imageFiles
  }

  private normalizeEditorImages(editor: HTMLDivElement): void {
    if (!editor) {
      return
    }
    editor.querySelectorAll('img').forEach((imageNode) => {
      const image = imageNode as HTMLImageElement
      image.style.maxWidth = '100%'
      image.style.height = 'auto'
      image.style.display = 'block'
      image.style.borderRadius = '4px'
    })
  }

  private async renderMyTickets(panel: HTMLElement): Promise<void> {
    const listEl = panel.querySelector('[data-role="list"]') as HTMLElement
    try {
      const page = await this.fetchMineTickets()
      if (!page.records.length) {
        listEl.textContent = '暂无工单'
        return
      }
      listEl.innerHTML = page.records
        .map(
          (item) => `<div data-action="open-ticket-item" data-ticket-no="${escapeHtml(item.ticketNo || '')}" style="padding:10px 0;border-bottom:1px solid #ebeef5;cursor:pointer;">
            <div style="font-weight:500;">${escapeHtml(item.title)}</div>
            <div style="font-size:12px;color:#909399;margin-top:4px;">${escapeHtml(item.ticketNo)} · ${escapeHtml(item.statusLabel || item.status)}</div>
          </div>`,
        )
        .join('')
      listEl.querySelectorAll('[data-action="open-ticket-item"]').forEach((node) => {
        node.addEventListener('click', () => {
          const ticketNo = (node as HTMLElement).getAttribute('data-ticket-no') ?? ''
          this.openPublicTicket(ticketNo)
        })
      })
    } catch (error) {
      listEl.style.color = '#f56c6c'
      listEl.textContent = error instanceof Error ? error.message : '加载失败'
    }
  }

  private openPublicTicket(ticketNo: string): void {
    const normalizedTicketNo = (ticketNo || '').trim()
    if (!normalizedTicketNo) {
      return
    }
    window.open(this.buildPublicTicketUrl(normalizedTicketNo), '_blank', 'noopener')
  }

  private buildPublicTicketUrl(ticketNo: string): string {
    const normalizedTicketNo = encodeURIComponent((ticketNo || '').trim())
    if (!normalizedTicketNo) {
      return `${this.apiBase}/open/ticket/`
    }
    return `${this.apiBase}/open/ticket/${normalizedTicketNo}`
  }

  private buildPluginContext(): Record<string, unknown> {
    const env = collectEnv()
    return {
      system: this.options?.system,
      module: this.context.module,
      page: this.context.page ?? document.title,
      pageUrl: this.context.pageUrl ?? window.location.href,
      pageTitle: this.context.pageTitle ?? document.title,
      bizId: this.context.bizId,
      bizType: this.context.bizType,
      bizNo: this.context.bizNo,
      merchantNo: this.context.merchantNo,
      companyName: this.context.companyName,
      merchantAccount: this.context.merchantAccount,
      sceneCode: this.context.sceneCode,
      expectedResult: this.context.expectedResult,
      user: this.options?.user,
      env,
      extra: this.context.extra,
      clientTime: new Date().toISOString(),
    }
  }

  private async uploadImageAndAttach(
    panel: HTMLElement,
    descriptionEl: HTMLDivElement,
    attachments: string[],
    file: File,
  ): Promise<void> {
    const messageEl = panel.querySelector('[data-role="message"]') as HTMLElement
    messageEl.style.color = '#909399'
    messageEl.textContent = `上传中：${file.name}`
    try {
      const output = await this.uploadPluginImage(file)
      if (output.url) {
        attachments.push(output.url)
        this.renderAttachmentList(panel, attachments)
        this.insertImageToEditor(descriptionEl, output.url)
      }
      messageEl.style.color = '#67c23a'
      messageEl.textContent = `上传成功：${output.fileName || file.name}`
    } catch (error) {
      messageEl.style.color = '#f56c6c'
      messageEl.textContent = error instanceof Error ? error.message : '图片上传失败'
    }
  }

  private renderAttachmentList(panel: HTMLElement, attachments: string[]): void {
    const listEl = panel.querySelector('[data-role="attachment-list"]') as HTMLElement
    if (!listEl) {
      return
    }
    if (!attachments.length) {
      listEl.textContent = '未上传图片'
      return
    }
    listEl.innerHTML = attachments
      .map((url, index) => `<div style="margin-bottom:4px;">图片${index + 1}：<a href="${escapeHtml(url)}" target="_blank" rel="noopener noreferrer">查看</a></div>`)
      .join('')
  }

  private insertImageToEditor(editor: HTMLDivElement, imageUrl: string): void {
    if (!editor || !imageUrl) {
      return
    }
    editor.focus()
    const html = `<p><img src="${escapeHtml(imageUrl)}" alt="问题截图" style="max-width:100%;height:auto;border-radius:4px;" /></p>`
    try {
      document.execCommand('insertHTML', false, html)
    } catch (error) {
      editor.innerHTML += html
    }
  }

  private async uploadPluginImage(file: File): Promise<{ url: string; fileName?: string; fileSize?: number; fileType?: string }> {
    const formData = new FormData()
    formData.append('file', file)
    const response = await fetch(`${this.apiBase}/api/open/v1/plugin/attachments/image`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${this.options!.launchToken}`,
      },
      body: formData,
    })
    const result = await response.json()
    if (!response.ok || result.code !== 200 || !result.data?.url) {
      throw new Error(result.message || '图片上传失败')
    }
    return result.data
  }

  private async createTicket(description: string, attachments: string[]): Promise<{ ticketNo: string; status: string }> {
    const response = await fetch(`${this.apiBase}/api/open/v1/plugin/tickets`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${this.options!.launchToken}`,
      },
      body: JSON.stringify({
        description,
        priority: this.config?.defaultPriority ?? 'medium',
        pluginContext: this.buildPluginContext(),
        attachments,
      }),
    })
    const result = await response.json()
    if (!response.ok || result.code !== 200) {
      throw new Error(result.message || '创建工单失败')
    }
    return result.data
  }

  private async fetchMineTickets(): Promise<{ records: Array<{ title: string; ticketNo: string; status: string; statusLabel?: string }> }> {
    const response = await fetch(`${this.apiBase}/api/open/v1/plugin/tickets/mine?pageNum=1&pageSize=20`, {
      headers: { Authorization: `Bearer ${this.options!.launchToken}` },
    })
    const result = await response.json()
    if (!response.ok || result.code !== 200) {
      throw new Error(result.message || '加载工单失败')
    }
    return result.data
  }

  private emit(event: TicketSdkEvent, payload: TicketUpdatedEvent): void {
    this.handlers.get(event)?.forEach((handler) => handler(payload))
  }

  private setupAutoReport(options?: AutoReportOptions): void {
    const runtime = normalizeAutoReportOptions(options)
    if (!runtime) {
      this.autoReporter.stop()
      return
    }
    const apiBase = this.apiBase
    runtime.shouldIgnoreUrl = (url: string) => {
      return url.includes('/api/open/v1/plugin/') || (!!apiBase && url.startsWith(apiBase) && url.includes('/plugin/'))
    }
    runtime.onCapture = (error) => this.handleHttpErrorCapture(error)
    this.autoReporter.start(runtime)
  }

  private handleHttpErrorCapture(error: HttpErrorCapture): void {
    if (!this.options) {
      return
    }
    this.setContext({
      extra: {
        ...(this.context.extra ?? {}),
        errorType: 'http',
        statusCode: error.statusCode,
        url: error.url,
        method: error.method,
        responseMessage: error.responseMessage,
        autoCaptured: true,
      },
    })
    const description = buildHttpErrorDescription(error)
    if (this.overlayEl) {
      this.closeModal()
    }
    this.openModal(false, description, true)
  }

  /** 创建 axios 响应错误拦截器，内部调用 reportHttpError */
  createAxiosInterceptor(options?: Pick<AutoReportOptions, 'httpStatus' | 'apiPatterns'>) {
    return createAxiosErrorInterceptor((error) => this.reportHttpError(error), options)
  }
}

function collectEnv(): EnvInfo {
  const ua = navigator.userAgent
  return {
    browser: detectBrowser(ua),
    os: detectOs(ua),
    screen: `${window.screen.width}x${window.screen.height}`,
    userAgent: ua,
    network: (navigator as Navigator & { connection?: { effectiveType?: string } }).connection?.effectiveType ?? 'unknown',
  }
}

function detectBrowser(ua: string): string {
  if (ua.includes('Chrome/')) return `Chrome ${ua.split('Chrome/')[1]?.split(' ')[0] ?? ''}`.trim()
  if (ua.includes('Firefox/')) return `Firefox ${ua.split('Firefox/')[1]?.split(' ')[0] ?? ''}`.trim()
  if (ua.includes('Safari/')) return 'Safari'
  return 'Unknown'
}

function detectOs(ua: string): string {
  if (ua.includes('Windows')) return 'Windows'
  if (ua.includes('Mac OS')) return 'macOS'
  if (ua.includes('Linux')) return 'Linux'
  if (ua.includes('Android')) return 'Android'
  if (ua.includes('iPhone') || ua.includes('iPad')) return 'iOS'
  return 'Unknown'
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

const ticketSdk = new TicketSdkImpl()

export const TicketSDK = {
  init: (options: TicketSdkInitOptions) => ticketSdk.init(options),
  setContext: (context: TicketSdkContext) => ticketSdk.setContext(context),
  setUser: (user: TicketSdkUser) => ticketSdk.setUser(user),
  open: (prefillDescription?: string) => ticketSdk.open(prefillDescription),
  openMyTickets: () => ticketSdk.openMyTickets(),
  reportHttpError: (error: HttpErrorCapture) => ticketSdk.reportHttpError(error),
  createAxiosInterceptor: (options?: Pick<AutoReportOptions, 'httpStatus' | 'apiPatterns'>) =>
    ticketSdk.createAxiosInterceptor(options),
  on: (event: TicketSdkEvent, handler: EventHandler) => ticketSdk.on(event, handler),
  destroy: () => ticketSdk.destroy(),
}

declare global {
  interface Window {
    TicketSDK: typeof TicketSDK
  }
}

if (typeof window !== 'undefined') {
  window.TicketSDK = TicketSDK
}
