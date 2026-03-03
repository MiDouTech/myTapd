<script setup lang="ts">
import { storeToRefs } from 'pinia'
import {
  ArrowDown,
  Bell,
  DataAnalysis,
  Document,
  Files,
  Fold,
  Grid,
  Histogram,
  Plus,
  Setting,
  Tickets,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import EmptyState from '@/components/common/EmptyState.vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import type { NotificationOutput } from '@/types/notification'
import { formatDateTime } from '@/utils/formatter'

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
      { index: '/manage/category', title: '分类管理', icon: Setting },
      { index: '/manage/workflow', title: '工作流管理', icon: Setting },
      { index: '/manage/sla', title: 'SLA管理', icon: Setting },
      { index: '/manage/user', title: '用户管理', icon: Setting },
      { index: '/manage/settings', title: '系统设置', icon: Setting },
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

const breadcrumbs = computed(() => {
  const matchedRoutes = route.matched
    .filter((item) => item.meta.title && item.path !== '/')
    .map((item, index, arr) => ({
      path: item.path.startsWith('/') ? item.path : `/${item.path}`,
      title: String(item.meta.title),
      canJump: index < arr.length - 1 && !item.path.includes(':'),
    }))
  return matchedRoutes
})

function handleMenuSelect(index: string): void {
  router.push(index)
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

async function handleLogout(): Promise<void> {
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
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  notificationStore.teardown()
})
</script>

<template>
  <el-container class="layout-root">
    <el-aside class="sidebar" :width="collapsed ? '64px' : '220px'">
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
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-button text @click="collapsed = !collapsed">
            <el-icon><Fold /></el-icon>
          </el-button>
          <div class="header-title">{{ currentTitle }}</div>
        </div>
        <div class="header-right">
          <el-input class="search-input" placeholder="搜索工单编号/标题" clearable />
          <el-button type="primary" :icon="Plus" @click="router.push('/ticket/create')">
            新建工单
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
              <span class="username">{{ authStore.userInfo?.name || '未登录用户' }}</span>
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
      <el-main class="main">
        <el-breadcrumb class="breadcrumb" separator="/">
          <el-breadcrumb-item
            v-for="breadcrumb in breadcrumbs"
            :key="breadcrumb.path"
            :to="breadcrumb.canJump ? { path: breadcrumb.path } : undefined"
          >
            {{ breadcrumb.title }}
          </el-breadcrumb-item>
        </el-breadcrumb>
        <div class="page-container">
          <RouterView />
        </div>
      </el-main>
      <el-drawer v-model="notificationDrawerVisible" title="通知消息" size="420px">
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
}

.sidebar {
  background: #ffffff;
  border-right: 1px solid #e5e7eb;
  transition: width 0.2s;
}

.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  color: #1675d1;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #eef2f7;
}

.menu {
  border-right: none;
  height: calc(100vh - 56px);
}

.header {
  height: 56px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 280px;
}

.notification-badge {
  display: inline-flex;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  color: #303133;
}

.main {
  padding: 12px 16px 16px;
}

.breadcrumb {
  margin-bottom: 12px;
}

.page-container {
  background: #ffffff;
  border-radius: 8px;
  min-height: calc(100vh - 140px);
  padding: 16px;
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
  padding: 12px;
  margin-bottom: 10px;
  background: #ffffff;
}

.notification-item.unread {
  border-color: #bcdcff;
  background: #f7fbff;
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
</style>
