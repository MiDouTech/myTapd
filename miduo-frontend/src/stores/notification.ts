import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  getNotificationPage,
  getNotificationUnreadCount,
  markAllNotificationsAsRead,
  markNotificationAsRead,
} from '@/api/notification'
import type {
  NotificationOutput,
  NotificationPageInput,
  NotificationRealtimePayload,
} from '@/types/notification'
import {
  NotificationWebSocketClient,
  createNotificationWebSocketClient,
} from '@/utils/websocket/notification'

const RECENT_PAGE_SIZE = 8
const FALLBACK_POLLING_INTERVAL = 30_000

function ensureNonNegative(value: number): number {
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : 0
}

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const recentNotifications = ref<NotificationOutput[]>([])
  const unreadLoading = ref(false)
  const recentLoading = ref(false)
  const realtimeConnected = ref(false)
  const pollingFallbackActive = ref(false)
  const lastRealtimeAt = ref<number>()

  let wsClient: NotificationWebSocketClient | null = null
  let pollingTimer: number | null = null
  let connectedUserId: number | null = null

  const unreadBadge = computed(() => (unreadCount.value > 99 ? '99+' : String(unreadCount.value)))

  async function loadUnreadCount(): Promise<void> {
    unreadLoading.value = true
    try {
      const result = await getNotificationUnreadCount()
      unreadCount.value = ensureNonNegative(result.unreadCount)
    } catch {
      // 全局拦截器已提示错误，保留当前未读数
    } finally {
      unreadLoading.value = false
    }
  }

  async function loadRecentNotifications(pageSize = RECENT_PAGE_SIZE): Promise<void> {
    recentLoading.value = true
    try {
      const params: NotificationPageInput = {
        pageNum: 1,
        pageSize,
      }
      const result = await getNotificationPage(params)
      recentNotifications.value = result.records || []
    } catch {
      // 全局拦截器已提示错误，保留当前列表
    } finally {
      recentLoading.value = false
    }
  }

  async function refreshOverview(pageSize = RECENT_PAGE_SIZE): Promise<void> {
    await Promise.all([loadUnreadCount(), loadRecentNotifications(pageSize)])
  }

  function applyReadLocally(notificationId: number): void {
    const target = recentNotifications.value.find((item) => item.id === notificationId)
    if (!target || target.isRead === 1) {
      return
    }
    target.isRead = 1
    target.readAt = new Date().toISOString()
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }

  async function markAsRead(notificationId: number): Promise<void> {
    await markNotificationAsRead(notificationId)
    applyReadLocally(notificationId)
    await loadUnreadCount()
  }

  async function markAllAsRead(): Promise<void> {
    await markAllNotificationsAsRead()
    unreadCount.value = 0
    recentNotifications.value = recentNotifications.value.map((item) => ({
      ...item,
      isRead: 1,
      readAt: item.readAt || new Date().toISOString(),
    }))
  }

  function onRealtimeMessage(payload: NotificationRealtimePayload): void {
    lastRealtimeAt.value = payload.timestamp ?? Date.now()
    void refreshOverview()
  }

  function startPollingFallback(): void {
    if (pollingTimer !== null) {
      return
    }
    pollingFallbackActive.value = true
    void loadUnreadCount()
    pollingTimer = window.setInterval(() => {
      void loadUnreadCount()
    }, FALLBACK_POLLING_INTERVAL)
  }

  function stopPollingFallback(): void {
    if (pollingTimer !== null) {
      window.clearInterval(pollingTimer)
      pollingTimer = null
    }
    pollingFallbackActive.value = false
  }

  function connectRealtime(userId: number): void {
    if (wsClient && connectedUserId === userId) {
      return
    }

    disconnectRealtime()
    startPollingFallback()

    connectedUserId = userId
    wsClient = createNotificationWebSocketClient({
      userId,
      onOpen: () => {
        realtimeConnected.value = true
        stopPollingFallback()
      },
      onClose: () => {
        realtimeConnected.value = false
        startPollingFallback()
      },
      onError: () => {
        realtimeConnected.value = false
        startPollingFallback()
      },
      onMessage: onRealtimeMessage,
    })
    wsClient.connect()
  }

  function disconnectRealtime(): void {
    if (wsClient) {
      wsClient.disconnect()
      wsClient = null
    }
    connectedUserId = null
    realtimeConnected.value = false
  }

  function resetState(): void {
    unreadCount.value = 0
    recentNotifications.value = []
    lastRealtimeAt.value = undefined
  }

  function teardown(): void {
    disconnectRealtime()
    stopPollingFallback()
    resetState()
  }

  return {
    unreadCount,
    unreadBadge,
    unreadLoading,
    recentLoading,
    recentNotifications,
    realtimeConnected,
    pollingFallbackActive,
    lastRealtimeAt,
    loadUnreadCount,
    loadRecentNotifications,
    refreshOverview,
    markAsRead,
    markAllAsRead,
    connectRealtime,
    disconnectRealtime,
    teardown,
  }
})
