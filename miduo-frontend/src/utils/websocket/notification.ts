import type { NotificationRealtimePayload } from '@/types/notification'

interface NotificationWebSocketOptions {
  userId: number
  heartbeatIntervalMs?: number
  reconnectIntervalMs?: number
  maxReconnectAttempts?: number
  onOpen?: () => void
  onClose?: () => void
  onError?: () => void
  onMessage?: (payload: NotificationRealtimePayload) => void
}

const DEFAULT_HEARTBEAT_INTERVAL = 20_000
const DEFAULT_RECONNECT_INTERVAL = 5_000
const DEFAULT_MAX_RECONNECT_ATTEMPTS = 20

function buildEndpoint(userId: number): string {
  if (
    import.meta.env.DEV &&
    import.meta.env.VITE_USE_PROXY === 'true' &&
    import.meta.env.VITE_API_PROXY_TARGET
  ) {
    try {
      const target = new URL(import.meta.env.VITE_API_PROXY_TARGET)
      target.protocol = target.protocol === 'https:' ? 'wss:' : 'ws:'
      target.pathname = '/ws/notification'
      target.search = `?userId=${encodeURIComponent(String(userId))}`
      return target.toString()
    } catch {
      // 代理地址异常时降级到当前站点
    }
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/notification?userId=${encodeURIComponent(String(userId))}`
}

export class NotificationWebSocketClient {
  private readonly options: Required<
    Pick<
      NotificationWebSocketOptions,
      'heartbeatIntervalMs' | 'reconnectIntervalMs' | 'maxReconnectAttempts'
    >
  > &
    Omit<NotificationWebSocketOptions, 'heartbeatIntervalMs' | 'reconnectIntervalMs' | 'maxReconnectAttempts'>
  private socket: WebSocket | null = null
  private heartbeatTimer: number | null = null
  private reconnectTimer: number | null = null
  private reconnectAttempts = 0
  private reconnectEnabled = true

  constructor(options: NotificationWebSocketOptions) {
    this.options = {
      ...options,
      heartbeatIntervalMs: options.heartbeatIntervalMs ?? DEFAULT_HEARTBEAT_INTERVAL,
      reconnectIntervalMs: options.reconnectIntervalMs ?? DEFAULT_RECONNECT_INTERVAL,
      maxReconnectAttempts: options.maxReconnectAttempts ?? DEFAULT_MAX_RECONNECT_ATTEMPTS,
    }
  }

  connect(): void {
    if (
      this.socket &&
      (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)
    ) {
      return
    }
    this.reconnectEnabled = true
    this.openSocket()
  }

  disconnect(): void {
    this.reconnectEnabled = false
    this.clearReconnectTimer()
    this.clearHeartbeat()
    if (this.socket && this.socket.readyState !== WebSocket.CLOSED) {
      this.socket.close()
    }
    this.socket = null
  }

  get isConnected(): boolean {
    return this.socket?.readyState === WebSocket.OPEN
  }

  private openSocket(): void {
    const endpoint = buildEndpoint(this.options.userId)
    this.socket = new WebSocket(endpoint)

    this.socket.onopen = () => {
      this.reconnectAttempts = 0
      this.clearReconnectTimer()
      this.startHeartbeat()
      this.options.onOpen?.()
    }

    this.socket.onmessage = (event: MessageEvent) => {
      if (typeof event.data !== 'string') {
        return
      }
      try {
        const payload = JSON.parse(event.data) as NotificationRealtimePayload
        this.options.onMessage?.(payload)
      } catch {
        // 后端消息格式异常时忽略，不阻断连接生命周期
      }
    }

    this.socket.onerror = () => {
      this.options.onError?.()
    }

    this.socket.onclose = () => {
      this.clearHeartbeat()
      this.socket = null
      this.options.onClose?.()
      if (this.reconnectEnabled) {
        this.scheduleReconnect()
      }
    }
  }

  private startHeartbeat(): void {
    this.clearHeartbeat()
    this.heartbeatTimer = window.setInterval(() => {
      if (this.socket?.readyState === WebSocket.OPEN) {
        this.socket.send(JSON.stringify({ type: 'PING', timestamp: Date.now() }))
      }
    }, this.options.heartbeatIntervalMs)
  }

  private clearHeartbeat(): void {
    if (this.heartbeatTimer !== null) {
      window.clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer !== null) {
      return
    }
    if (this.reconnectAttempts >= this.options.maxReconnectAttempts) {
      return
    }
    this.reconnectAttempts += 1
    const delay = this.options.reconnectIntervalMs * this.reconnectAttempts
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null
      this.openSocket()
    }, delay)
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer !== null) {
      window.clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }
}

export function createNotificationWebSocketClient(
  options: NotificationWebSocketOptions,
): NotificationWebSocketClient {
  return new NotificationWebSocketClient(options)
}
