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

  async init(options: TicketSdkInitOptions): Promise<void> {
    this.options = { ...options, mode: options.mode ?? 'float' }
    this.config = await this.fetchConfig()
    this.mountEntry()
  }

  setContext(context: TicketSdkContext): void {
    this.context = { ...this.context, ...context }
  }

  setUser(user: TicketSdkUser): void {
    if (this.options) {
      this.options.user = user
    }
  }

  open(): void {
    this.openModal()
  }

  openMyTickets(): void {
    this.openModal(true)
  }

  on(event: TicketSdkEvent, handler: EventHandler): void {
    if (!this.handlers.has(event)) {
      this.handlers.set(event, new Set())
    }
    this.handlers.get(event)!.add(handler)
  }

  destroy(): void {
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

  private openModal(myTickets = false): void {
    if (!this.options) return
    this.overlayEl?.remove()
    const primary = this.config?.theme?.primaryColor ?? '#1675d1'
    const overlay = document.createElement('div')
    overlay.style.cssText =
      'position:fixed;inset:0;background:rgba(0,0,0,.45);z-index:99999;display:flex;align-items:center;justify-content:center;'
    const panel = document.createElement('div')
    panel.style.cssText =
      'width:420px;max-width:92vw;background:#fff;border-radius:8px;box-shadow:0 8px 24px rgba(0,0,0,.18);padding:20px;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif;'
    panel.innerHTML = myTickets
      ? `<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
           <strong style="font-size:16px;">我的工单</strong>
           <button type="button" data-action="close" style="border:none;background:transparent;font-size:20px;cursor:pointer;">×</button>
         </div>
         <div data-role="list" style="min-height:120px;color:#606266;">加载中...</div>`
      : `<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
           <strong style="font-size:16px;">提交工单</strong>
           <button type="button" data-action="close" style="border:none;background:transparent;font-size:20px;cursor:pointer;">×</button>
         </div>
         <textarea data-role="description" rows="6" placeholder="请描述遇到的问题" style="width:100%;box-sizing:border-box;padding:10px;border:1px solid #dcdfe6;border-radius:4px;resize:vertical;"></textarea>
         <div style="margin-top:12px;display:flex;justify-content:flex-end;gap:8px;">
           <button type="button" data-action="close" style="padding:8px 14px;border:1px solid #dcdfe6;background:#fff;border-radius:4px;cursor:pointer;">取消</button>
           <button type="button" data-action="submit" style="padding:8px 14px;border:none;color:#fff;border-radius:4px;cursor:pointer;background:${primary};">提交</button>
         </div>
         <div data-role="message" style="margin-top:8px;font-size:13px;color:#67c23a;"></div>`

    overlay.appendChild(panel)
    overlay.addEventListener('click', (event) => {
      if (event.target === overlay) this.closeModal()
    })
    panel.querySelectorAll('[data-action="close"]').forEach((node) => {
      node.addEventListener('click', () => this.closeModal())
    })
    if (myTickets) {
      void this.renderMyTickets(panel)
    } else {
      panel.querySelector('[data-action="submit"]')?.addEventListener('click', () => {
        void this.submitTicket(panel)
      })
    }
    document.body.appendChild(overlay)
    this.overlayEl = overlay
  }

  private closeModal(): void {
    this.overlayEl?.remove()
    this.overlayEl = null
  }

  private async submitTicket(panel: HTMLElement): Promise<void> {
    const description = (panel.querySelector('[data-role="description"]') as HTMLTextAreaElement)?.value?.trim()
    const messageEl = panel.querySelector('[data-role="message"]') as HTMLElement
    if (!description) {
      messageEl.style.color = '#f56c6c'
      messageEl.textContent = '请先填写问题描述'
      return
    }
    messageEl.style.color = '#909399'
    messageEl.textContent = '提交中...'
    try {
      const output = await this.createTicket(description)
      messageEl.style.color = '#67c23a'
      messageEl.textContent = `提交成功：${output.ticketNo}`
      this.emit('ticket:updated', { ticketNo: output.ticketNo, status: output.status })
      window.setTimeout(() => this.closeModal(), 1200)
    } catch (error) {
      messageEl.style.color = '#f56c6c'
      messageEl.textContent = error instanceof Error ? error.message : '提交失败'
    }
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
          (item) => `<div style="padding:10px 0;border-bottom:1px solid #ebeef5;">
            <div style="font-weight:500;">${escapeHtml(item.title)}</div>
            <div style="font-size:12px;color:#909399;margin-top:4px;">${escapeHtml(item.ticketNo)} · ${escapeHtml(item.statusLabel || item.status)}</div>
          </div>`,
        )
        .join('')
    } catch (error) {
      listEl.style.color = '#f56c6c'
      listEl.textContent = error instanceof Error ? error.message : '加载失败'
    }
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
      user: this.options?.user,
      env,
      extra: this.context.extra,
      clientTime: new Date().toISOString(),
    }
  }

  private async createTicket(description: string): Promise<{ ticketNo: string; status: string }> {
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
  open: () => ticketSdk.open(),
  openMyTickets: () => ticketSdk.openMyTickets(),
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
