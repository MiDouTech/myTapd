<script setup lang="ts">
import {
  ArrowDown,
  Bell,
  DataAnalysis,
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
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

interface MenuItem {
  index: string
  title: string
  icon: unknown
  children?: MenuItem[]
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const collapsed = ref(false)

const menuItems: MenuItem[] = [
  { index: '/dashboard', title: '仪表盘', icon: DataAnalysis },
  { index: '/ticket/mine', title: '我的工单', icon: Tickets },
  { index: '/ticket/all', title: '所有工单', icon: Files },
  { index: '/ticket/kanban', title: '工单看板', icon: Grid },
  { index: '/report', title: '报表中心', icon: Histogram },
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

async function handleLogout(): Promise<void> {
  authStore.clearLoginState()
  await router.push('/login')
}
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
          <el-button text>
            <el-icon><Bell /></el-icon>
          </el-button>
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
</style>
