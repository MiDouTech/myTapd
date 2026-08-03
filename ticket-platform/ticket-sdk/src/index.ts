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

interface PluginTicketMessage {
  id: number
  userName?: string
  content: string
  type: string
  createTime: string
}

interface PluginTicketDetail {
  ticketId: number
  ticketNo: string
  title: string
  description: string
  status: string
  statusLabel?: string
  createTime: string
  updateTime: string
  urgeCount: number
  canUrge: boolean
  urgeDisabledReason?: string
  messages: PluginTicketMessage[]
}

interface SupplementUpload {
  url: string
  name: string
  type: 'image' | 'video'
}

type TicketAttachment = SupplementUpload

const DEFAULT_API_BASE = ''

class TicketSdkImpl {
  private options: TicketSdkInitOptions | null = null
  private config: TicketSdkConfig | null = null
  private context: TicketSdkContext = {}
  private floatEl: HTMLElement | null = null
  private floatObserver: MutationObserver | null = null
  private overlayEl: HTMLElement | null = null
  private handlers = new Map<TicketSdkEvent, Set<EventHandler>>()
  private autoReporter = new HttpAutoReporter()
  private launchTokenWatchTimer: number | null = null

  async init(options: TicketSdkInitOptions): Promise<void> {
    this.options = { ...options, mode: options.mode ?? 'float' }
    // Mount the entry before the first await so host-side compatibility checks
    // performed immediately after TicketSDK.init() can discover it reliably.
    this.mountEntry()
    this.config = await this.fetchConfig()
    this.applyFloatTheme()
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

  refreshLaunchToken(launchToken: string): void {
    const normalized = (launchToken ?? '').trim()
    if (!this.options || !normalized) {
      return
    }
    this.options.launchToken = normalized
    if (this.overlayEl) {
      const panel = this.overlayEl.firstElementChild as HTMLElement | null
      if (panel) {
        this.clearLaunchTokenWatch()
        this.hideLaunchTokenHint(panel)
        this.scheduleLaunchTokenWatch(panel)
      }
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
    this.clearLaunchTokenWatch()
    this.autoReporter.stop()
    this.floatObserver?.disconnect()
    this.floatEl?.remove()
    this.floatStyleEl?.remove()
    this.overlayEl?.remove()
    this.floatEl = null
    this.floatObserver = null
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
    this.floatEl?.remove()
    // Only remove roots that are explicitly owned by TicketSDK. Never inspect,
    // hide, or remove a host application's fallback controls.
    document
      .querySelectorAll(
        '[data-ticket-sdk-entry="float"][data-ticket-sdk-owner="TicketSDK"], [data-miduo-ticket-launcher]',
      )
      .forEach((element) => element.remove())

    const primary = this.config?.theme?.primaryColor ?? '#1675d1'
    const host = document.createElement('div')
    host.className = 'ticket-sdk-float miduo-ticket-launcher'
    host.setAttribute('data-ticket-sdk-entry', 'float')
    host.setAttribute('data-ticket-sdk-owner', 'TicketSDK')
    host.dataset.miduoTicketLauncher = 'vortex-v2'
    host.style.cssText =
      'position:fixed;right:24px;bottom:24px;z-index:99998;width:64px;height:64px;display:block;'
    const shadow = host.attachShadow({ mode: 'open' })
    const style = document.createElement('style')
    style.textContent = `
      :host{all:initial!important;position:fixed!important;right:24px!important;bottom:24px!important;z-index:99998!important;width:64px!important;height:64px!important;display:block!important}
      .miduo-ticket-float{position:relative;display:block;box-sizing:border-box;width:64px;height:64px;padding:0;border:0;border-radius:50%;appearance:none;background:transparent;cursor:pointer;box-shadow:0 4px 12px rgba(22,117,209,.35);transition:transform .25s cubic-bezier(.34,1.56,.64,1),box-shadow .25s ease;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;isolation:isolate}
      .miduo-ticket-float:hover{transform:scale(1.08);box-shadow:0 6px 20px color-mix(in srgb,var(--miduo-ticket-primary) 50%,transparent)}
      .miduo-ticket-float:active{transform:scale(.96)}
      .miduo-ticket-float:focus-visible{outline:3px solid color-mix(in srgb,var(--miduo-ticket-primary) 38%,white);outline-offset:4px}
      .miduo-ticket-float__glow,.miduo-ticket-float__ring,.miduo-ticket-float__ring-inner,.miduo-ticket-float__core{position:absolute;border-radius:50%;pointer-events:none}
      .miduo-ticket-float__glow{inset:-8px;z-index:-1;background:radial-gradient(circle,rgba(64,158,255,.45) 0%,rgba(96,165,250,.2) 40%,transparent 70%);filter:blur(6px);animation:miduo-ticket-breathe 3s ease-in-out infinite}
      .miduo-ticket-float__ring{inset:0;background:conic-gradient(from 0deg,transparent 0deg,rgba(102,177,255,.35) 35deg,rgba(64,158,255,.75) 95deg,rgba(37,99,168,.95) 155deg,rgba(64,158,255,.7) 215deg,rgba(102,177,255,.4) 275deg,transparent 325deg);-webkit-mask:radial-gradient(circle,transparent 25%,#000 28%);mask:radial-gradient(circle,transparent 25%,#000 28%);animation:miduo-ticket-spin 10s linear infinite}
      .miduo-ticket-float__ring-inner{inset:6px;background:conic-gradient(from 180deg,transparent 0deg,rgba(155,200,255,.4) 55deg,rgba(64,158,255,.8) 145deg,rgba(37,99,168,.75) 235deg,transparent 305deg);-webkit-mask:radial-gradient(circle,transparent 35%,#000 38%);mask:radial-gradient(circle,transparent 35%,#000 38%);animation:miduo-ticket-spin-reverse 6s linear infinite}
      .miduo-ticket-float__core{inset:13px;display:flex;align-items:center;justify-content:center;background:var(--miduo-ticket-primary);background:radial-gradient(circle,#2563a8 0%,var(--miduo-ticket-primary) 55%,#66b1ff 100%);box-shadow:inset 0 0 12px rgba(0,50,100,.3),0 0 10px rgba(59,130,246,.4)}
      .miduo-ticket-float__label{position:relative;z-index:1;color:#fff;font-size:13px;font-weight:600;letter-spacing:1px;text-shadow:0 1px 2px rgba(0,0,0,.3)}
      .miduo-ticket-float:hover .miduo-ticket-float__glow{filter:blur(8px);animation-duration:1.5s}.miduo-ticket-float:hover .miduo-ticket-float__ring{animation-duration:3s}.miduo-ticket-float:hover .miduo-ticket-float__ring-inner{animation-duration:2s}
      @keyframes miduo-ticket-breathe{0%,100%{opacity:.7;transform:scale(1)}50%{opacity:1;transform:scale(1.12)}}@keyframes miduo-ticket-spin{to{transform:rotate(360deg)}}@keyframes miduo-ticket-spin-reverse{from{transform:rotate(360deg)}to{transform:rotate(0)}}
      @media(max-width:640px){:host{right:16px!important;bottom:16px!important;width:56px!important;height:56px!important}.miduo-ticket-float{width:56px;height:56px}.miduo-ticket-float__core{inset:11px}.miduo-ticket-float__label{font-size:12px}}
      @media(prefers-reduced-motion:reduce){.miduo-ticket-float,.miduo-ticket-float__glow,.miduo-ticket-float__ring,.miduo-ticket-float__ring-inner{animation:none;transition:none}}
    `
    const button = document.createElement('button')
    button.type = 'button'
    button.className = 'miduo-ticket-float'
    button.dataset.sdkFloatVersion = 'vortex-v2'
    button.setAttribute('aria-label', '提交工单')
    button.title = '提交工单'
    button.style.setProperty('--miduo-ticket-primary', primary)
    button.innerHTML = `
      <span class="miduo-ticket-float__glow" aria-hidden="true"></span>
      <span class="miduo-ticket-float__ring" aria-hidden="true"></span>
      <span class="miduo-ticket-float__ring-inner" aria-hidden="true"></span>
      <span class="miduo-ticket-float__core" aria-hidden="true"><span class="miduo-ticket-float__label">工单</span></span>
    `
    button.addEventListener('click', () => this.openModal())
    shadow.append(style, button)
    document.body.appendChild(host)
    this.floatEl = host
  }

  private applyFloatTheme(): void {
    const button = this.floatEl?.shadowRoot?.querySelector<HTMLElement>('.miduo-ticket-float')
    button?.style.setProperty('--miduo-ticket-primary', this.config?.theme?.primaryColor ?? '#1675d1')
  }

  private openModal(myTickets = false, prefillDescription?: string, autoCaptured = false): void {
    if (!this.options) return
    this.overlayEl?.remove()
    const primary = this.config?.theme?.primaryColor ?? '#1675d1'
    const overlay = document.createElement('div')
    overlay.style.cssText =
      'position:fixed;inset:0;background:rgba(0,0,0,.45);z-index:99999;display:flex;align-items:center;justify-content:center;'
    const panel = document.createElement('div')
    panel.style.cssText = myTickets
      ? 'width:420px;max-width:92vw;max-height:92vh;background:#fff;border-radius:8px;box-shadow:0 8px 24px rgba(0,0,0,.18);padding:20px;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif;display:flex;flex-direction:column;overflow:hidden;'
      : 'width:420px;max-width:92vw;max-height:92vh;box-sizing:border-box;background:#fff;border-radius:8px;box-shadow:0 8px 24px rgba(0,0,0,.18);font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif;display:flex;flex-direction:column;overflow:hidden;'
    if (!myTickets) panel.setAttribute('data-ticket-submit-panel', '')
    panel.innerHTML = myTickets
      ? `<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;flex:0 0 auto;">
           <strong style="font-size:16px;">我的工单</strong>
           <button type="button" data-action="close" style="border:none;background:transparent;font-size:20px;cursor:pointer;">×</button>
         </div>
         <div data-role="list-container" style="flex:1 1 auto;min-height:0;overflow:auto;">
           <div data-role="list" style="min-height:120px;color:#606266;">加载中...</div>
         </div>`
      : `<div style="display:flex;justify-content:space-between;align-items:center;padding:20px;border-bottom:1px solid #ebeef5;flex:0 0 auto;">
           <strong style="font-size:16px;line-height:1.2;color:#303133;">提交工单</strong>
           <div style="display:flex;align-items:center;gap:18px;">
             <button type="button" data-action="open-my-tickets" style="padding:0;border:none;background:transparent;color:${primary};font-size:14px;line-height:1.5;cursor:pointer;">我的工单</button>
             <button type="button" data-action="close" aria-label="关闭" style="padding:0;border:none;background:transparent;color:#909399;font-size:20px;line-height:1;cursor:pointer;">×</button>
           </div>
         </div>
         <style>
           [data-ticket-submit-panel] [data-role="description"]:empty::before{content:attr(data-placeholder);white-space:pre-line;color:#a8abb2;pointer-events:none;}
           [data-ticket-submit-panel] [data-role="description"]:focus,[data-ticket-submit-panel] [data-role="category"]:focus{border-color:${primary}!important;box-shadow:0 0 0 2px rgba(22,117,209,.12);}
           [data-ticket-submit-panel] [data-action="pick-attachment"]:hover{border-color:${primary}!important;background:#f5faff!important;}
           [data-ticket-submit-panel] [data-action="open-my-tickets"]:hover{color:#409eff!important;}
           [data-ticket-submit-panel] [data-action="close"]:hover{color:#606266!important;border-color:#c6e2ff!important;}
           [data-ticket-submit-panel] [data-action="submit"]:not(:disabled):hover{filter:brightness(.95);}
           [data-ticket-submit-panel] [data-action="submit"]:focus-visible,[data-ticket-submit-panel] [data-action="close"]:focus-visible,[data-ticket-submit-panel] [data-action="open-my-tickets"]:focus-visible,[data-ticket-submit-panel] [data-action="pick-attachment"]:focus-visible{outline:2px solid ${primary};outline-offset:2px;}
         </style>
         <div data-role="submit-scroll" style="flex:1 1 auto;min-height:0;overflow:auto;padding:20px;">
           ${autoCaptured ? '<div data-role="hint" style="margin-bottom:10px;padding:8px 10px;background:#f0f9ff;border:1px solid #b3d8ff;border-radius:4px;font-size:13px;color:#1675d1;">检测到接口异常，已自动填写问题描述，请确认后提交。</div>' : ''}
           <label for="miduo-ticket-category" style="display:block;margin-bottom:6px;font-size:14px;color:#303133;"><span style="color:#f56c6c;">*</span> 问题分类</label>
           <select id="miduo-ticket-category" data-role="category" style="width:100%;height:36px;box-sizing:border-box;margin-bottom:16px;padding:0 10px;border:1px solid #dcdfe6;border-radius:4px;background:#fff;color:#303133;outline:none;font-size:14px;">
             <option value="">请选择问题分类</option>
             <option value="功能异常/Bug">功能异常/Bug</option>
             <option value="需求建议">需求建议</option>
           </select>
           <label for="miduo-ticket-description" style="display:block;margin-bottom:6px;font-size:14px;color:#303133;"><span style="color:#f56c6c;">*</span> 问题描述</label>
           <div id="miduo-ticket-description" data-role="description" contenteditable="true" aria-label="问题描述" data-placeholder="请详细描述您遇到的问题，包括：&#10;1. 问题出现的具体操作步骤&#10;2. 期望的正常结果是什么&#10;3. 实际出现的结果是什么&#10;4. 相关的截图或错误信息（可在下方上传）" style="width:100%;height:140px;box-sizing:border-box;padding:10px;border:1px solid #dcdfe6;border-radius:4px;overflow:auto;outline:none;line-height:1.6;font-size:14px;word-break:break-word;overflow-wrap:anywhere;"></div>
           <div style="margin-top:20px;margin-bottom:8px;font-size:14px;color:#303133;">附件上传</div>
           <input data-role="attachment-input" type="file" accept="image/*,video/*" multiple style="display:none;" />
           <button type="button" data-action="pick-attachment" aria-label="上传附件，支持点击、粘贴或拖拽图片和视频" style="width:100%;height:140px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;box-sizing:border-box;border:1px dashed #dcdfe6;background:#fff;border-radius:8px;cursor:pointer;color:inherit;transition:border-color .2s,background .2s;">
             <span aria-hidden="true" style="font-size:28px;line-height:1;color:#606266;">📎</span>
             <span style="font-size:14px;color:${primary};">上传附件</span>
             <span style="font-size:12px;color:#909399;">支持点击、粘贴或拖拽图片和视频（jpg/png/mp4 等格式），单个文件不超过 50MB</span>
           </button>
           <div data-role="attachment-list" style="margin-top:8px;font-size:12px;line-height:1.6;color:#909399;word-break:break-all;">未上传附件</div>
           <div data-role="message" style="margin-top:8px;font-size:13px;color:#67c23a;"></div>
         </div>
         <div style="padding:16px 20px;display:flex;align-items:center;justify-content:flex-end;gap:8px;border-top:1px solid #ebeef5;flex:0 0 auto;">
           <button type="button" data-action="close" style="padding:8px 14px;border:1px solid #dcdfe6;background:#fff;border-radius:4px;cursor:pointer;color:#606266;font-size:14px;">取消</button>
           <button type="button" data-action="submit" disabled style="padding:8px 14px;border:none;color:#fff;border-radius:4px;cursor:not-allowed;background:#a8d2fa;font-size:14px;">提交</button>
         </div>`

    const uploadedAttachments: TicketAttachment[] = []
    const hasUnsavedDraft = (): boolean => {
      if (myTickets) {
        return false
      }
      const descriptionEl = panel.querySelector('[data-role="description"]') as HTMLDivElement | null
      const descriptionHtml = (descriptionEl?.innerHTML ?? '').trim()
      const sanitized = this.sanitizeDescriptionHtml(descriptionHtml)
      const hasInlineImage = /<img[\s>]/i.test(descriptionHtml)
      const category = (panel.querySelector('[data-role="category"]') as HTMLSelectElement | null)?.value
      return !!category || !!sanitized.plainText || hasInlineImage || uploadedAttachments.length > 0
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
        descriptionEl.innerHTML = ''
      }
      if (prefillDescription) {
        const content = escapeHtml(prefillDescription).replace(/\n/g, '<br/>')
        if (descriptionEl) {
          descriptionEl.innerHTML = content
        }
      }
      this.bindDescriptionEditorEvents(panel, descriptionEl, uploadedAttachments)
      const categoryEl = panel.querySelector('[data-role="category"]') as HTMLSelectElement
      const attachmentInput = panel.querySelector('[data-role="attachment-input"]') as HTMLInputElement
      const submitButton = panel.querySelector('[data-action="submit"]') as HTMLButtonElement
      const updateSubmitState = (): void => {
        const hasDescription = !!this.sanitizeDescriptionHtml(descriptionEl.innerHTML).plainText
        const enabled = !!categoryEl.value && hasDescription
        submitButton.disabled = !enabled
        submitButton.style.background = enabled ? primary : '#a8d2fa'
        submitButton.style.cursor = enabled ? 'pointer' : 'not-allowed'
      }
      categoryEl.addEventListener('change', updateSubmitState)
      descriptionEl.addEventListener('input', updateSubmitState)
      updateSubmitState()
      const attachmentDropzone = panel.querySelector('[data-action="pick-attachment"]') as HTMLButtonElement
      attachmentDropzone?.addEventListener('click', () => {
        attachmentInput?.click()
      })
      attachmentInput?.addEventListener('change', async () => {
        const files = Array.from(attachmentInput.files ?? [])
        await this.uploadAttachmentFiles(panel, descriptionEl, uploadedAttachments, files)
        attachmentInput.value = ''
      })
      attachmentDropzone?.addEventListener('paste', (event: ClipboardEvent) => {
        const files = this.extractClipboardMediaFiles(event)
        if (!files.length) return
        event.preventDefault()
        void this.uploadAttachmentFiles(panel, descriptionEl, uploadedAttachments, files)
      })
      attachmentDropzone?.addEventListener('dragover', (event: DragEvent) => {
        event.preventDefault()
        if (event.dataTransfer) event.dataTransfer.dropEffect = 'copy'
        attachmentDropzone.style.borderColor = primary
        attachmentDropzone.style.background = '#f5faff'
      })
      attachmentDropzone?.addEventListener('dragleave', () => {
        attachmentDropzone.style.borderColor = '#dcdfe6'
        attachmentDropzone.style.background = '#fff'
      })
      attachmentDropzone?.addEventListener('drop', (event: DragEvent) => {
        event.preventDefault()
        attachmentDropzone.style.borderColor = '#dcdfe6'
        attachmentDropzone.style.background = '#fff'
        void this.uploadAttachmentFiles(
          panel,
          descriptionEl,
          uploadedAttachments,
          Array.from(event.dataTransfer?.files ?? []),
        )
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
    this.scheduleLaunchTokenWatch(panel)
  }

  private parseLaunchTokenExpiryMs(token?: string): number | null {
    const normalized = (token ?? '').trim()
    if (!normalized) {
      return null
    }
    try {
      const payloadPart = normalized.split('.')[1]
      if (!payloadPart) {
        return null
      }
      const base64 = payloadPart.replace(/-/g, '+').replace(/_/g, '/')
      const payload = JSON.parse(atob(base64)) as { exp?: number }
      return typeof payload.exp === 'number' ? payload.exp * 1000 : null
    } catch {
      return null
    }
  }

  private clearLaunchTokenWatch(): void {
    if (this.launchTokenWatchTimer != null) {
      window.clearTimeout(this.launchTokenWatchTimer)
      this.launchTokenWatchTimer = null
    }
  }

  private scheduleLaunchTokenWatch(panel: HTMLElement): void {
    this.clearLaunchTokenWatch()
    const expMs = this.parseLaunchTokenExpiryMs(this.options?.launchToken)
    if (!expMs) {
      return
    }
    const check = (): void => {
      if (!this.overlayEl || this.overlayEl.firstElementChild !== panel) {
        this.clearLaunchTokenWatch()
        return
      }
      const remainingMs = expMs - Date.now()
      if (remainingMs <= 0) {
        this.showLaunchTokenHint(
          panel,
          '登录凭证已过期，弹窗不会自动关闭。请刷新页面或联系管理员重新获取凭证后再提交。',
          '#fef0f0',
          '#f56c6c',
        )
        return
      }
      if (remainingMs <= 2 * 60 * 1000) {
        const minutes = Math.max(1, Math.ceil(remainingMs / 60000))
        this.showLaunchTokenHint(
          panel,
          `登录凭证将在约 ${minutes} 分钟后过期，建议尽快提交；也可由业务系统调用 TicketSDK.refreshLaunchToken() 续期。`,
          '#fdf6ec',
          '#e6a23c',
        )
      } else {
        this.hideLaunchTokenHint(panel)
      }
      const nextCheckMs = Math.min(remainingMs, 30 * 1000)
      this.launchTokenWatchTimer = window.setTimeout(check, nextCheckMs)
    }
    check()
  }

  private showLaunchTokenHint(
    panel: HTMLElement,
    message: string,
    backgroundColor: string,
    borderColor: string,
  ): void {
    let hintEl = panel.querySelector('[data-role="launch-token-hint"]') as HTMLElement | null
    if (!hintEl) {
      hintEl = document.createElement('div')
      hintEl.setAttribute('data-role', 'launch-token-hint')
      hintEl.style.cssText =
        'margin-bottom:10px;padding:8px 10px;border-radius:4px;font-size:13px;line-height:1.5;flex:0 0 auto;'
      const header = panel.firstElementChild
      if (header?.nextElementSibling) {
        panel.insertBefore(hintEl, header.nextElementSibling)
      } else {
        panel.appendChild(hintEl)
      }
    }
    hintEl.style.background = backgroundColor
    hintEl.style.border = `1px solid ${borderColor}`
    hintEl.style.color = borderColor
    hintEl.textContent = message
  }

  private hideLaunchTokenHint(panel: HTMLElement): void {
    panel.querySelector('[data-role="launch-token-hint"]')?.remove()
  }

  private closeModal(): void {
    this.clearLaunchTokenWatch()
    this.overlayEl?.remove()
    this.overlayEl = null
  }

  private async submitTicket(panel: HTMLElement, attachments: TicketAttachment[]): Promise<void> {
    const categoryEl = panel.querySelector('[data-role="category"]') as HTMLSelectElement
    const descriptionEl = panel.querySelector('[data-role="description"]') as HTMLDivElement
    const descriptionHtml = (descriptionEl?.innerHTML ?? '').trim()
    const sanitizedDescription = this.sanitizeDescriptionHtml(descriptionHtml)
    const descriptionText = sanitizedDescription.plainText
    const messageEl = panel.querySelector('[data-role="message"]') as HTMLElement
    const category = categoryEl.value.trim()
    if (!category || !descriptionText) {
      messageEl.style.color = '#f56c6c'
      messageEl.textContent = '请选择问题分类并填写问题描述'
      return
    }
    if (sanitizedDescription.removedInlineImageCount > 0 && attachments.length === 0) {
      messageEl.style.color = '#f56c6c'
      messageEl.textContent = '检测到未上传成功的内联图片，请重新粘贴或点击“上传附件”后再提交'
      return
    }
    const characters = Array.from(descriptionText)
    const title = `${characters.slice(0, 30).join('')}${characters.length > 30 ? '…' : ''}`
    const content = `<p>【${escapeHtml(category)}】</p>${sanitizedDescription.html}`
    console.log({ title, content })
    messageEl.style.color = '#909399'
    messageEl.textContent = '提交中...'
    try {
      const output = await this.createTicket(title, content, attachments)
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
    attachments: TicketAttachment[],
  ): void {
    if (!descriptionEl) {
      return
    }
    this.normalizeEditorImages(descriptionEl)
    descriptionEl.addEventListener('input', () => {
      this.normalizeEditorImages(descriptionEl)
      if (!(descriptionEl.textContent ?? '').trim() && !descriptionEl.querySelector('img')) {
        descriptionEl.innerHTML = ''
      }
    })
    descriptionEl.addEventListener('paste', (event: ClipboardEvent) => {
      const mediaFiles = this.extractClipboardMediaFiles(event)
      if (!mediaFiles.length) {
        window.setTimeout(() => this.normalizeEditorImages(descriptionEl), 0)
        return
      }
      event.preventDefault()
      void (async () => {
        for (const imageFile of imageFiles) {
          // 为什么直接上传粘贴图片：避免 dataURL 直接进描述字段，导致内容过长或显示异常。
          await this.uploadAttachment(panel, descriptionEl, attachments, imageFile)
        }
      })()
    })
  }

  private extractClipboardMediaFiles(event: ClipboardEvent): File[] {
    const clipboard = event.clipboardData
    if (!clipboard || !clipboard.items || !clipboard.items.length) return []
    return Array.from(clipboard.items)
      .filter((item) => item.kind === 'file' && (item.type.startsWith('image/') || item.type.startsWith('video/')))
      .map((item) => item.getAsFile())
      .filter((file): file is File => !!file)
  }

  private extractClipboardImageFiles(event: ClipboardEvent): File[] {
    return this.extractClipboardMediaFiles(event).filter((file) => file.type.startsWith('image/'))
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
          void this.renderTicketDetail(panel, ticketNo)
        })
      })
    } catch (error) {
      listEl.style.color = '#f56c6c'
      listEl.textContent = error instanceof Error ? error.message : '加载失败'
    }
  }

  private async renderTicketDetail(panel: HTMLElement, ticketNo: string): Promise<void> {
    const container = panel.querySelector('[data-role="list-container"]') as HTMLElement
    container.innerHTML = '<div style="padding:20px 0;color:#909399;text-align:center;">加载中...</div>'
    try {
      const detail = await this.fetchTicketDetail(ticketNo)
      container.innerHTML = `
        <div style="display:flex;flex-direction:column;gap:14px;">
          <button type="button" data-action="back-to-tickets" style="align-self:flex-start;border:none;background:transparent;color:#606266;cursor:pointer;padding:0;">← 返回我的工单</button>
          <div>
            <div style="font-size:16px;font-weight:600;color:#303133;">${escapeHtml(detail.title)}</div>
            <div style="font-size:12px;color:#909399;margin-top:5px;">${escapeHtml(detail.ticketNo)} · ${escapeHtml(detail.statusLabel || detail.status)}</div>
          </div>
          <div style="background:#f5f7fa;border-radius:6px;padding:10px;font-size:13px;line-height:1.6;color:#606266;white-space:pre-wrap;">${escapeHtml(htmlToPlainText(detail.description) || '暂无问题描述')}</div>
          <div>
            <div style="font-weight:600;font-size:14px;margin-bottom:8px;">最近沟通</div>
            <div data-role="ticket-messages">${this.renderTicketMessages(
              Array.isArray(detail.messages) ? detail.messages.slice(-5) : [],
            )}</div>
          </div>
          <div data-role="supplement-editor" style="display:none;border-top:1px solid #ebeef5;padding-top:12px;">
            <textarea data-role="supplement-content" maxlength="4000" placeholder="补充问题说明、最新复现情况等（可直接粘贴截图）" style="width:100%;box-sizing:border-box;min-height:110px;padding:8px;border:1px solid #dcdfe6;border-radius:4px;resize:vertical;font:inherit;"></textarea>
            <div style="display:flex;align-items:center;gap:8px;margin-top:8px;flex-wrap:wrap;">
              <button type="button" data-action="pick-supplement-file" style="padding:6px 12px;border:1px solid #dcdfe6;background:#fff;border-radius:4px;cursor:pointer;">上传图片/视频</button>
              <span style="font-size:12px;color:#909399;">支持粘贴图片；最多9张图片、3个视频</span>
            </div>
            <input data-role="supplement-files" type="file" accept="image/*,video/*" multiple style="display:none;" />
            <div data-role="supplement-upload-list" style="margin-top:8px;font-size:12px;color:#606266;"></div>
            <div data-role="supplement-message" style="font-size:12px;margin-top:6px;color:#909399;"></div>
            <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:10px;">
              <button type="button" data-action="cancel-supplement" style="padding:7px 12px;border:1px solid #dcdfe6;background:#fff;border-radius:4px;cursor:pointer;">取消</button>
              <button type="button" data-action="submit-supplement" style="padding:7px 12px;border:none;background:${this.config?.theme?.primaryColor ?? '#1675d1'};color:#fff;border-radius:4px;cursor:pointer;">提交补充</button>
            </div>
          </div>
          <div data-role="ticket-actions" style="display:flex;flex-wrap:wrap;gap:8px;border-top:1px solid #ebeef5;padding-top:12px;">
            <button type="button" data-action="show-supplement" style="padding:8px 14px;border:none;background:${this.config?.theme?.primaryColor ?? '#1675d1'};color:#fff;border-radius:4px;cursor:pointer;">补充信息</button>
            <button type="button" data-action="urge-ticket" ${detail.canUrge ? '' : 'disabled'} title="${escapeHtml(detail.urgeDisabledReason || '')}" style="padding:8px 14px;border:1px solid #dcdfe6;background:#fff;border-radius:4px;cursor:${detail.canUrge ? 'pointer' : 'not-allowed'};color:${detail.canUrge ? '#606266' : '#c0c4cc'};">催单${detail.urgeCount ? `（${detail.urgeCount}）` : ''}</button>
            <button type="button" data-action="open-public-detail" style="padding:8px 14px;border:none;background:transparent;color:#1675d1;cursor:pointer;">查看完整详情</button>
          </div>
          <div data-role="detail-message" style="font-size:13px;color:#67c23a;"></div>
        </div>`

      {
        const supplementUploads: SupplementUpload[] = []
        container.querySelector('[data-action="back-to-tickets"]')?.addEventListener('click', () => {
          container.innerHTML = '<div data-role="list" style="min-height:120px;color:#606266;">加载中...</div>'
          void this.renderMyTickets(panel)
        })
        container.querySelector('[data-action="open-public-detail"]')?.addEventListener('click', () => this.openPublicTicket(detail.ticketNo))
        container.querySelector('[data-action="show-supplement"]')?.addEventListener('click', () => {
          ;(container.querySelector('[data-role="supplement-editor"]') as HTMLElement).style.display = 'block'
          ;(container.querySelector('[data-role="ticket-actions"]') as HTMLElement).style.display = 'none'
        })
        container.querySelector('[data-action="cancel-supplement"]')?.addEventListener('click', () => {
          ;(container.querySelector('[data-role="supplement-editor"]') as HTMLElement).style.display = 'none'
          ;(container.querySelector('[data-role="ticket-actions"]') as HTMLElement).style.display = 'flex'
        })
        const supplementContentEl = container.querySelector('[data-role="supplement-content"]') as HTMLTextAreaElement
        const supplementFilesEl = container.querySelector('[data-role="supplement-files"]') as HTMLInputElement
        container.querySelector('[data-action="pick-supplement-file"]')?.addEventListener('click', () => supplementFilesEl.click())
        supplementFilesEl.addEventListener('change', () => {
          const files = Array.from(supplementFilesEl.files ?? [])
          if (files.length) void this.uploadSupplementFiles(panel, files, supplementUploads)
          supplementFilesEl.value = ''
        })
        supplementContentEl.addEventListener('paste', (event) => {
          const imageFiles = this.extractClipboardImageFiles(event)
          if (!imageFiles.length) return
          event.preventDefault()
          void this.uploadSupplementFiles(panel, imageFiles, supplementUploads)
        })
        container.querySelector('[data-action="submit-supplement"]')?.addEventListener('click', () => {
          void this.submitTicketSupplement(panel, detail.ticketNo, supplementUploads)
        })
        container.querySelector('[data-action="urge-ticket"]')?.addEventListener('click', () => {
          void this.submitTicketUrge(panel, detail.ticketNo)
        })
      }
    } catch (error) {
      container.innerHTML = `<div style="color:#f56c6c;">${escapeHtml(error instanceof Error ? error.message : '加载失败')}</div>`
    }
  }

  private renderTicketMessages(messages: PluginTicketMessage[]): string {
    if (!messages.length) return '<div style="font-size:13px;color:#909399;">暂无沟通记录</div>'
    return messages.map((message) => `<div style="padding:8px 0;border-bottom:1px solid #f0f0f0;">
      <div style="display:flex;justify-content:space-between;gap:8px;font-size:12px;color:#909399;">
        <span>${escapeHtml(message.userName || '系统')}</span><span>${escapeHtml(formatSdkDate(message.createTime))}</span>
      </div>
      <div style="font-size:13px;line-height:1.6;color:#606266;margin-top:4px;white-space:pre-wrap;">${escapeHtml(htmlToPlainText(message.content))}</div>
    </div>`).join('')
  }

  private async uploadSupplementFiles(
    panel: HTMLElement,
    files: File[],
    uploads: SupplementUpload[],
  ): Promise<void> {
    const messageEl = panel.querySelector('[data-role="supplement-message"]') as HTMLElement
    const imageCount = uploads.filter((item) => item.type === 'image').length + files.filter((file) => file.type.startsWith('image/')).length
    const videoCount = uploads.filter((item) => item.type === 'video').length + files.filter((file) => file.type.startsWith('video/')).length
    if (imageCount > 9 || videoCount > 3 || files.some((file) => !file.type.startsWith('image/') && !file.type.startsWith('video/'))) {
      messageEl.style.color = '#f56c6c'
      messageEl.textContent = '仅支持图片和视频，最多9张图片、3个视频'
      return
    }
    try {
      for (const file of files) {
        messageEl.style.color = '#909399'
        messageEl.textContent = `正在上传：${file.name}`
        const output = await this.uploadPluginFile(file, 'attachment')
        uploads.push({
          url: output.url,
          name: output.fileName || file.name,
          type: file.type.startsWith('video/') ? 'video' : 'image',
        })
        this.renderSupplementUploads(panel, uploads)
      }
      messageEl.style.color = '#67c23a'
      messageEl.textContent = '文件上传成功'
    } catch (error) {
      messageEl.style.color = '#f56c6c'
      messageEl.textContent = error instanceof Error ? error.message : '文件上传失败'
    }
  }

  private renderSupplementUploads(panel: HTMLElement, uploads: SupplementUpload[]): void {
    const listEl = panel.querySelector('[data-role="supplement-upload-list"]') as HTMLElement
    listEl.innerHTML = uploads.map((item, index) => `<div style="display:flex;align-items:center;justify-content:space-between;gap:8px;padding:4px 0;">
      <span>${item.type === 'video' ? '🎬' : '🖼️'} ${escapeHtml(item.name)}</span>
      <button type="button" data-remove-supplement-upload="${index}" style="border:none;background:transparent;color:#f56c6c;cursor:pointer;">删除</button>
    </div>`).join('')
    listEl.querySelectorAll('[data-remove-supplement-upload]').forEach((node) => {
      node.addEventListener('click', () => {
        const index = Number((node as HTMLElement).getAttribute('data-remove-supplement-upload'))
        if (Number.isInteger(index)) uploads.splice(index, 1)
        this.renderSupplementUploads(panel, uploads)
      })
    })
  }

  private async submitTicketSupplement(panel: HTMLElement, ticketNo: string, uploads: SupplementUpload[]): Promise<void> {
    const contentEl = panel.querySelector('[data-role="supplement-content"]') as HTMLTextAreaElement
    const messageEl = panel.querySelector('[data-role="supplement-message"]') as HTMLElement
    const content = contentEl.value.trim()
    if (!content && !uploads.length) {
      messageEl.style.color = '#f56c6c'
      messageEl.textContent = '请输入补充内容或上传图片/视频'
      return
    }
    try {
      messageEl.style.color = '#909399'
      messageEl.textContent = '正在提交...'
      await this.addTicketMessage(
        ticketNo,
        content,
        uploads.filter((item) => item.type === 'image').map((item) => item.url),
        uploads.filter((item) => item.type === 'video').map((item) => item.url),
      )
      await this.renderTicketDetail(panel, ticketNo)
    } catch (error) {
      messageEl.style.color = '#f56c6c'
      messageEl.textContent = error instanceof Error ? error.message : '提交失败'
    }
  }

  private async submitTicketUrge(panel: HTMLElement, ticketNo: string): Promise<void> {
    if (typeof window.confirm === 'function' && !window.confirm('确认催办此工单？催办后将通知当前处理人。')) return
    const messageEl = panel.querySelector('[data-role="detail-message"]') as HTMLElement
    try {
      messageEl.style.color = '#909399'
      messageEl.textContent = '正在催办...'
      await this.urgePluginTicket(ticketNo)
      await this.renderTicketDetail(panel, ticketNo)
    } catch (error) {
      messageEl.style.color = '#f56c6c'
      messageEl.textContent = error instanceof Error ? error.message : '催办失败'
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

  private async uploadAttachment(
    panel: HTMLElement,
    descriptionEl: HTMLDivElement,
    attachments: TicketAttachment[],
    file: File,
  ): Promise<void> {
    const messageEl = panel.querySelector('[data-role="message"]') as HTMLElement
    if (file.size > 50 * 1024 * 1024) {
      messageEl.style.color = '#f56c6c'
      messageEl.textContent = '单个附件不能超过 50MB'
      return
    }
    if (!file.type.startsWith('image/') && !file.type.startsWith('video/')) {
      messageEl.style.color = '#f56c6c'
      messageEl.textContent = '仅支持上传图片或视频附件'
      return
    }
    messageEl.style.color = '#909399'
    messageEl.textContent = `上传中：${file.name}`
    try {
      if (!file.type.startsWith('image/') && !file.type.startsWith('video/')) {
        throw new Error('仅支持上传图片或视频附件')
      }
      const output = await this.uploadPluginFile(file, file.type.startsWith('image/') ? 'screenshot' : 'attachment')
      if (output.url) {
        const type = file.type.startsWith('video/') ? 'video' : 'image'
        attachments.push({ url: output.url, name: output.fileName || file.name, type })
        this.renderAttachmentList(panel, attachments)
        if (type === 'image') this.insertImageToEditor(descriptionEl, output.url)
      }
      messageEl.style.color = '#67c23a'
      messageEl.textContent = `上传成功：${output.fileName || file.name}`
    } catch (error) {
      messageEl.style.color = '#f56c6c'
      messageEl.textContent = error instanceof Error ? error.message : '附件上传失败'
    }
  }

  private async uploadAttachmentFiles(
    panel: HTMLElement,
    descriptionEl: HTMLDivElement,
    attachments: TicketAttachment[],
    files: File[],
  ): Promise<void> {
    for (const file of files) {
      await this.uploadAttachment(panel, descriptionEl, attachments, file)
    }
  }

  private renderAttachmentList(panel: HTMLElement, attachments: TicketAttachment[]): void {
    const listEl = panel.querySelector('[data-role="attachment-list"]') as HTMLElement
    if (!listEl) {
      return
    }
    if (!attachments.length) {
      listEl.textContent = '未上传附件'
      return
    }
    listEl.innerHTML = attachments
      .map((item, index) => {
        const url = escapeHtml(item.url)
        const name = escapeHtml(item.name || `${item.type === 'video' ? '视频' : '图片'}${index + 1}`)
        const preview =
          item.type === 'video'
            ? `<video src="${url}" controls preload="metadata" style="display:block;width:100%;max-height:180px;border-radius:6px;background:#000;"></video>`
            : `<img src="${url}" alt="${name}" loading="lazy" style="display:block;width:100%;max-height:180px;object-fit:contain;border-radius:6px;background:#f5f7fa;" />`
        return `<div style="margin-bottom:10px;padding:8px;border:1px solid #ebeef5;border-radius:6px;">
          <a href="${url}" target="_blank" rel="noopener noreferrer" style="display:block;margin-bottom:6px;color:#1675d1;word-break:break-all;">${name}</a>
          ${preview}
        </div>`
      })
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

  private resolveUploadFileName(file: File): string {
    const originalName = (file?.name ?? '').trim()
    if (/\.[A-Za-z0-9]+$/.test(originalName)) {
      return originalName
    }
    const ext = this.resolveImageExtension(file?.type)
    // 为什么这里补后缀：部分浏览器粘贴截图会传无扩展名文件名（如 image/blob），旧后端按后缀处理时容易报错。
    return `pasted-image-${Date.now()}-${Math.floor(Math.random() * 1000)}.${ext}`
  }

  private resolveImageExtension(contentType?: string): string {
    const normalized = (contentType ?? '').split(';')[0].trim().toLowerCase()
    switch (normalized) {
      case 'image/jpeg':
      case 'image/jpg':
        return 'jpg'
      case 'image/gif':
        return 'gif'
      case 'image/webp':
        return 'webp'
      case 'image/bmp':
        return 'bmp'
      case 'image/png':
      default:
        return 'png'
    }
  }

  private resolvePluginApiErrorMessage(result: { code?: number; message?: string }, fallback: string): string {
    if (result.code === 8102 || result.code === 8103) {
      return '登录凭证已过期或失效，弹窗不会自动关闭。请刷新页面后重试，或联系管理员重新获取凭证。'
    }
    return result.message || fallback
  }

  private async uploadPluginFile(
    file: File,
    uploadPurpose: 'screenshot' | 'attachment',
  ): Promise<{ url: string; fileName?: string; fileSize?: number; fileType?: string }> {
    const formData = new FormData()
    formData.append('file', file, this.resolveUploadFileName(file))
    formData.append('uploadPurpose', uploadPurpose)
    const response = await fetch(`${this.apiBase}/api/open/v1/plugin/attachments/image`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${this.options!.launchToken}`,
      },
      body: formData,
    })
    const result = await response.json()
    if (!response.ok || result.code !== 200 || !result.data?.url) {
      throw new Error(this.resolvePluginApiErrorMessage(result, '图片上传失败'))
    }
    return result.data
  }

  private async createTicket(title: string, content: string, attachments: TicketAttachment[]): Promise<{ ticketNo: string; status: string }> {
    const response = await fetch(`${this.apiBase}/api/open/v1/plugin/tickets`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${this.options!.launchToken}`,
      },
      body: JSON.stringify({
        title,
        content,
        description: content,
        priority: this.config?.defaultPriority ?? 'medium',
        pluginContext: this.buildPluginContext(),
        attachments: attachments.map((item) => item.url),
      }),
    })
    const result = await response.json()
    if (!response.ok || result.code !== 200) {
      throw new Error(this.resolvePluginApiErrorMessage(result, '创建工单失败'))
    }
    return result.data
  }

  private async fetchMineTickets(): Promise<{ records: Array<{ title: string; ticketNo: string; status: string; statusLabel?: string }> }> {
    const response = await fetch(`${this.apiBase}/api/open/v1/plugin/tickets/mine?pageNum=1&pageSize=20`, {
      headers: { Authorization: `Bearer ${this.options!.launchToken}` },
    })
    const result = await response.json()
    if (!response.ok || result.code !== 200) {
      throw new Error(this.resolvePluginApiErrorMessage(result, '加载工单失败'))
    }
    return result.data
  }

  private async fetchTicketDetail(ticketNo: string): Promise<PluginTicketDetail> {
    const response = await fetch(`${this.apiBase}/api/open/v1/plugin/tickets/${encodeURIComponent(ticketNo)}/detail`, {
      headers: { Authorization: `Bearer ${this.options!.launchToken}` },
    })
    const result = await response.json()
    if (!response.ok || result.code !== 200) {
      throw new Error(this.resolvePluginApiErrorMessage(result, '加载工单详情失败'))
    }
    return result.data
  }

  private async addTicketMessage(
    ticketNo: string,
    content: string,
    attachments: string[],
    videos: string[],
  ): Promise<void> {
    const response = await fetch(`${this.apiBase}/api/open/v1/plugin/tickets/${encodeURIComponent(ticketNo)}/messages`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${this.options!.launchToken}`,
      },
      body: JSON.stringify({ content, attachments, videos }),
    })
    const result = await response.json()
    if (!response.ok || result.code !== 200) {
      throw new Error(this.resolvePluginApiErrorMessage(result, '补充信息失败'))
    }
  }

  private async urgePluginTicket(ticketNo: string): Promise<void> {
    const response = await fetch(`${this.apiBase}/api/open/v1/plugin/tickets/${encodeURIComponent(ticketNo)}/urge`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${this.options!.launchToken}` },
    })
    const result = await response.json()
    if (!response.ok || result.code !== 200) {
      throw new Error(this.resolvePluginApiErrorMessage(result, '催办失败'))
    }
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

function escapeHtml(value: unknown): string {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function htmlToPlainText(value?: unknown): string {
  if (!value) return ''
  const element = document.createElement('div')
  element.innerHTML = String(value)
  return (element.textContent ?? '').replace(/\s+/g, ' ').trim()
}

function formatSdkDate(value?: unknown): string {
  if (!value) return ''
  const normalizedValue = typeof value === 'number' || typeof value === 'string' ? value : String(value)
  const date = new Date(normalizedValue)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

const ticketSdk = new TicketSdkImpl()

export const TicketSDK = {
  init: (options: TicketSdkInitOptions) => ticketSdk.init(options),
  setContext: (context: TicketSdkContext) => ticketSdk.setContext(context),
  setUser: (user: TicketSdkUser) => ticketSdk.setUser(user),
  refreshLaunchToken: (launchToken: string) => ticketSdk.refreshLaunchToken(launchToken),
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
