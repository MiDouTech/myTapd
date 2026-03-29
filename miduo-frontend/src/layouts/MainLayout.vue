<script setup lang="ts">
import { storeToRefs } from 'pinia'
import {
  ArrowDown,
  Bell,
  Calendar,
  Connection,
  DataAnalysis,
  Document,
  Expand,
  Files,
  Fold,
  Grid,
  Histogram,
  Menu as MenuIcon,
  Notebook,
  Plus,
  Setting,
  Tickets,
  Timer,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import EmptyState from '@/components/common/EmptyState.vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import type { NotificationOutput } from '@/types/notification'
import { formatDateTime } from '@/utils/formatter'
import { createInertiaWheelScroll } from '@/utils/inertiaWheelScroll'

interface MenuItem {
  index: string
  title: string
  icon: unknown
  children?: MenuItem[]
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const { unreadCount, unreadBadge, recentNotifications, recentLoading, realtimeConnected } =
  storeToRefs(notificationStore)
const collapsed = ref(false)
const notificationDrawerVisible = ref(false)
const isMobile = ref(false)
const mobileSidebarVisible = ref(false)
const MOBILE_BREAKPOINT = 768
const DESKTOP_POINTER_MEDIA_QUERY = '(hover: hover) and (pointer: fine)'
const mainScrollRef = ref<HTMLElement | { $el?: Element | null } | null>(null)
let inertiaWheelController: { destroy: () => void } | null = null
let desktopPointerMedia: MediaQueryList | null = null

const menuItems: MenuItem[] = [
  { index: '/dashboard', title: '仪表盘', icon: DataAnalysis },
  { index: '/ticket/mine', title: '我的工单', icon: Tickets },
  { index: '/ticket/all', title: '所有工单', icon: Files },
  { index: '/ticket/kanban', title: '工单看板', icon: Grid },
  { index: '/report', title: '报表中心', icon: Histogram },
  {
    index: 'bugReport',
    title: 'Bug简报',
    icon: Document,
    children: [
      { index: '/bug-report', title: '简报列表', icon: Document },
      { index: '/bug-report/statistics', title: '统计看板', icon: Histogram },
    ],
  },
  { index: '/notification', title: '通知中心', icon: Bell },
  {
    index: 'manage',
    title: '管理',
    icon: Setting,
    children: [
      { index: '/manage/category', title: '分类管理', icon: MenuIcon },
      { index: '/manage/workflow', title: '工作流管理', icon: Connection },
      { index: '/manage/sla', title: 'SLA管理', icon: Timer },
      { index: '/manage/user', title: '组织账号管理', icon: User },
      { index: '/manage/settings', title: '系统设置', icon: Setting },
      { index: '/manage/operation-log', title: '工单日志', icon: Notebook },
      { index: '/manage/daily-report', title: '日报管理', icon: Calendar },
    ],
  },
]

const currentTitle = computed(() => String(route.meta.title || '工单系统'))

const activeMenu = computed(() => {
  if (route.path.startsWith('/ticket/detail/')) {
    return '/ticket/all'
  }
  if (route.path === '/ticket/create') {
    return '/ticket/mine'
  }
  if (route.path.startsWith('/bug-report/detail/')) {
    return '/bug-report'
  }
  if (route.path.startsWith('/bug-report/edit/')) {
    return '/bug-report'
  }
  if (route.path === '/bug-report/edit') {
    return '/bug-report'
  }
  return route.path
})


function handleMenuSelect(index: string): void {
  router.push(index)
  if (isMobile.value) {
    mobileSidebarVisible.value = false
  }
}

function handleSidebarTrigger(): void {
  if (isMobile.value) {
    mobileSidebarVisible.value = true
    return
  }
  collapsed.value = !collapsed.value
}

function handleNotificationBellClick(): void {
  notificationDrawerVisible.value = true
  void notificationStore.refreshOverview()
}

async function handleOpenNotification(notification: NotificationOutput): Promise<void> {
  if (notification.id && notification.isRead !== 1) {
    await notificationStore.markAsRead(notification.id)
  }

  notificationDrawerVisible.value = false

  if (notification.ticketId) {
    await router.push(`/ticket/detail/${notification.ticketId}`)
    return
  }
  if (notification.reportId) {
    await router.push(`/bug-report/detail/${notification.reportId}`)
    return
  }
  await router.push('/notification')
}

async function handleMarkRecentAsRead(notification: NotificationOutput): Promise<void> {
  if (!notification.id) {
    return
  }
  await notificationStore.markAsRead(notification.id)
}

async function handleMarkAllAsRead(): Promise<void> {
  await notificationStore.markAllAsRead()
}

async function handleGoNotificationCenter(): Promise<void> {
  notificationDrawerVisible.value = false
  await router.push('/notification')
}

function handleVisibilityChange(): void {
  if (document.visibilityState === 'visible') {
    void notificationStore.loadUnreadCount()
  }
}

function updateViewportState(): void {
  isMobile.value = window.innerWidth <= MOBILE_BREAKPOINT
  if (!isMobile.value) {
    mobileSidebarVisible.value = false
  }
}

function resolveMainScrollElement(): HTMLElement | null {
  const target = mainScrollRef.value
  if (!target) {
    return null
  }
  if (target instanceof HTMLElement) {
    return target
  }
  const maybeElement = target.$el
  if (maybeElement instanceof HTMLElement) {
    return maybeElement
  }
  return null
}

function teardownInertiaWheelScroll(): void {
  inertiaWheelController?.destroy()
  inertiaWheelController = null
}

async function setupInertiaWheelScroll(): Promise<void> {
  teardownInertiaWheelScroll()
  if (isMobile.value) {
    return
  }
  if (!window.matchMedia(DESKTOP_POINTER_MEDIA_QUERY).matches) {
    return
  }
  await nextTick()
  const mainScrollElement = resolveMainScrollElement()
  if (!mainScrollElement) {
    return
  }
  inertiaWheelController = createInertiaWheelScroll(mainScrollElement, {
    lerpFactor: 0.14,
    maxDeltaPerFrame: 220,
  })
}

async function handleLogout(): Promise<void> {
  try {
    const { ssoLogout } = await import('@/api/sso')
    await ssoLogout()
  } catch {
    // SSO 登出失败不影响本地登出流程
  }
  notificationStore.teardown()
  authStore.clearLoginState()
  await router.push('/login')
}

watch(
  () => authStore.userInfo?.id,
  (userId) => {
    if (!userId) {
      notificationStore.teardown()
      return
    }
    notificationStore.connectRealtime(userId)
    void notificationStore.refreshOverview()
  },
  { immediate: true },
)

watch(
  () => route.fullPath,
  () => {
    void notificationStore.loadUnreadCount()
  },
)

onMounted(() => {
  updateViewportState()
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('resize', updateViewportState)
  desktopPointerMedia = window.matchMedia(DESKTOP_POINTER_MEDIA_QUERY)
  desktopPointerMedia.addEventListener('change', setupInertiaWheelScroll)
  void setupInertiaWheelScroll()
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('resize', updateViewportState)
  desktopPointerMedia?.removeEventListener('change', setupInertiaWheelScroll)
  desktopPointerMedia = null
  teardownInertiaWheelScroll()
  notificationStore.teardown()
})

watch(
  () => route.fullPath,
  () => {
    if (isMobile.value) {
      mobileSidebarVisible.value = false
    }
  },
)

watch(
  () => isMobile.value,
  () => {
    void setupInertiaWheelScroll()
  },
)
</script>

<template>
  <el-container class="layout-root">
    <el-aside v-if="!isMobile" class="sidebar" :width="collapsed ? '64px' : '220px'">
      <div class="logo">{{ collapsed ? 'MD' : '米多工单系统' }}</div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :unique-opened="true"
        :collapse-transition="false"
        class="menu"
        @select="handleMenuSelect"
      >
        <template v-for="item in menuItems" :key="item.index">
          <el-sub-menu v-if="item.children?.length" :index="item.index">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item v-for="sub in item.children" :key="sub.index" :index="sub.index">
              <el-icon><component :is="sub.icon" /></el-icon>
              <span>{{ sub.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.index">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-drawer
      v-model="mobileSidebarVisible"
      direction="ltr"
      :with-header="false"
      size="240px"
      class="mobile-sidebar-drawer"
      append-to-body
      destroy-on-close
    >
      <div class="mobile-sidebar-panel">
        <div class="logo">米多工单系统</div>
        <el-menu :default-active="activeMenu" :unique-opened="true" class="menu" @select="handleMenuSelect">
          <template v-for="item in menuItems" :key="item.index">
            <el-sub-menu v-if="item.children?.length" :index="item.index">
              <template #title>
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
              </template>
              <el-menu-item v-for="sub in item.children" :key="sub.index" :index="sub.index">
                <el-icon><component :is="sub.icon" /></el-icon>
                <span>{{ sub.title }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="item.index">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </div>
    </el-drawer>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-button text @click="handleSidebarTrigger">
            <el-icon><component :is="isMobile ? Expand : Fold" /></el-icon>
          </el-button>
          <div class="header-title">{{ currentTitle }}</div>
        </div>
        <div class="header-right">
          <el-input v-if="!isMobile" class="search-input" placeholder="搜索工单编号/标题" clearable />
          <el-button type="primary" :icon="Plus" @click="router.push('/ticket/create')">
            <span class="new-ticket-text">新建工单</span>
          </el-button>
          <el-badge :value="unreadBadge" :hidden="unreadCount === 0" class="notification-badge">
            <el-button text @click="handleNotificationBellClick">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-dropdown>
            <span class="user-dropdown">
              <el-avatar :size="28" :src="authStore.userInfo?.avatarUrl">
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <span class="username" :class="{ 'is-hidden': isMobile }">
                {{ authStore.userInfo?.name || '未登录用户' }}
              </span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :icon="User">个人信息</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main ref="mainScrollRef" class="main">
        <div class="page-container">
          <RouterView />
        </div>
      </el-main>
      <el-drawer v-model="notificationDrawerVisible" title="通知消息" :size="isMobile ? '92vw' : '420px'">
        <div class="notification-drawer-toolbar">
          <el-tag :type="realtimeConnected ? 'success' : 'warning'">
            {{ realtimeConnected ? '实时推送中' : '轮询兜底中' }}
          </el-tag>
          <el-space>
            <el-button type="primary" link @click="notificationStore.refreshOverview">刷新</el-button>
            <el-button type="primary" link @click="handleMarkAllAsRead">全部已读</el-button>
          </el-space>
        </div>

        <EmptyState v-if="!recentLoading && recentNotifications.length === 0" description="暂无通知消息" />
        <el-scrollbar v-else height="calc(100vh - 220px)">
          <div
            v-for="item in recentNotifications"
            :key="item.id"
            class="notification-item"
            :class="{ unread: item.isRead !== 1 }"
          >
            <div class="notification-item-header">
              <span class="notification-item-title">{{ item.title || '系统通知' }}</span>
              <el-tag size="small" :type="item.isRead === 1 ? 'success' : 'info'">
                {{ item.isRead === 1 ? '已读' : '未读' }}
              </el-tag>
            </div>
            <div class="notification-item-content">
              {{ item.content || '-' }}
            </div>
            <div class="notification-item-footer">
              <span>{{ formatDateTime(item.createTime) }}</span>
              <el-space size="small">
                <el-button
                  v-if="item.isRead !== 1"
                  type="primary"
                  link
                  @click="handleMarkRecentAsRead(item)"
                >
                  标记已读
                </el-button>
                <el-button type="primary" link @click="handleOpenNotification(item)">查看</el-button>
              </el-space>
            </div>
          </div>
        </el-scrollbar>

        <template #footer>
          <div class="notification-drawer-footer">
            <el-button @click="notificationDrawerVisible = false">关闭</el-button>
            <el-button type="primary" @click="handleGoNotificationCenter">进入通知中心</el-button>
          </div>
        </template>
      </el-drawer>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.layout-root {
  height: 100vh;
  background: #f5f7fa;
  overflow: hidden;

  > .el-container {
    min-width: 0;
    overflow: hidden;
  }
}

.sidebar {
  background: #ffffff;
  border-right: 1px solid var(--md-border-color);
  transition: width 0.2s ease;
  box-shadow: 1px 0 4px rgba(0, 0, 0, 0.02);
}

.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  color: #1675d1;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
  border-bottom: 1px solid #eef2f7;
  background: linear-gradient(180deg, #fafcff 0%, #ffffff 100%);
}

.menu {
  border-right: none;
  height: calc(100vh - 56px);
  overflow-y: auto;
  padding: 6px 0;
  -webkit-overflow-scrolling: touch;
  touch-action: pan-y;

  :deep(.el-menu-item) {
    margin: 2px 8px;
    border-radius: 6px;
    height: 42px;
    line-height: 42px;
    transition: all 0.15s ease;

    &.is-active {
      background: #e8f2fc;
      color: #1675d1;
      font-weight: 500;
    }

    &:hover:not(.is-active) {
      background: #f5f7fa;
    }
  }

  :deep(.el-sub-menu .el-menu-item) {
    margin: 2px 8px;
    border-radius: 6px;
    min-width: auto;
  }

  :deep(.el-sub-menu__title) {
    margin: 2px 8px;
    border-radius: 6px;
    height: 42px;
    line-height: 42px;

    &:hover {
      background: #f5f7fa;
    }
  }
}

.mobile-sidebar-panel {
  height: 100%;
  background: #ffffff;
}

.header {
  height: 56px;
  background: #ffffff;
  border-bottom: 1px solid var(--md-border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
  max-width: min(60vw, 360px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.search-input {
  width: 280px;

  :deep(.el-input__wrapper) {
    border-radius: 20px;
    background: #f5f7fa;
    box-shadow: none !important;
    border: 1px solid transparent;
    transition: all 0.2s ease;

    &:hover,
    &.is-focus {
      background: #ffffff;
      border-color: #1675d1;
      box-shadow: 0 0 0 2px rgba(22, 117, 209, 0.1) !important;
    }
  }
}

.notification-badge {
  display: inline-flex;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.15s ease;

  &:hover {
    background: #f5f7fa;
  }
}

.username {
  color: #303133;
  font-size: 14px;
}

.username.is-hidden {
  display: none;
}

.main {
  padding: 16px 20px 20px;
  overflow: auto;
  background: #f5f7fa;
  min-width: 0;
  overscroll-behavior-y: contain;
  -webkit-overflow-scrolling: touch;
  touch-action: pan-y pinch-zoom;
}

.page-container {
  min-height: calc(100vh - 140px);
  min-width: 0;
  overflow: hidden;
}

.notification-drawer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.notification-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 10px;
  background: #ffffff;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  cursor: pointer;

  &:hover {
    border-color: #c8ddf5;
    box-shadow: 0 2px 6px rgba(22, 117, 209, 0.06);
  }
}

.notification-item.unread {
  border-color: #bcdcff;
  background: linear-gradient(135deg, #f7fbff 0%, #f0f7ff 100%);
  border-left: 3px solid #1675d1;
}

.notification-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.notification-item-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.notification-item-content {
  margin-top: 8px;
  font-size: 13px;
  line-height: 20px;
  color: #606266;
}

.notification-item-footer {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
}

.notification-drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 768px) {
  .header {
    padding: 0 10px;
  }

  .header-right {
    gap: 4px;
  }

  .new-ticket-text {
    display: none;
  }

  .main {
    padding: 8px;
  }

  .page-container {
    min-height: calc(100vh - 76px);
  }

  .notification-item-header {
    align-items: flex-start;
  }

  .notification-item-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
