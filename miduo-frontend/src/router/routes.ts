import type { RouteRecordRaw } from 'vue-router'

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: {
      title: '登录',
      requiresAuth: false,
    },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    meta: {
      requiresAuth: true,
    },
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: {
          title: '仪表盘',
        },
      },
      {
        path: 'ticket/mine',
        name: 'ticketMine',
        component: () => import('@/views/ticket/TicketListView.vue'),
        meta: {
          title: '我的工单',
        },
      },
      {
        path: 'ticket/all',
        name: 'ticketAll',
        component: () => import('@/views/ticket/TicketListView.vue'),
        meta: {
          title: '所有工单',
        },
      },
      {
        path: 'ticket/kanban',
        name: 'ticketKanban',
        component: () => import('@/views/ticket/KanbanView.vue'),
        meta: {
          title: '工单看板',
        },
      },
      {
        path: 'ticket/create',
        name: 'ticketCreate',
        component: () => import('@/views/ticket/TicketCreateView.vue'),
        meta: {
          title: '新建工单',
        },
      },
      {
        path: 'ticket/detail/:id',
        name: 'ticketDetail',
        component: () => import('@/views/ticket/TicketDetailView.vue'),
        meta: {
          title: '工单详情',
        },
      },
      {
        path: 'report',
        name: 'report',
        component: () => import('@/views/report/ReportView.vue'),
        meta: {
          title: '报表中心',
        },
      },
      {
        path: 'bug-report',
        name: 'bugReportList',
        component: () => import('@/views/bugreport/BugReportListView.vue'),
        meta: {
          title: 'Bug简报',
        },
      },
      {
        path: 'bug-report/detail/:id',
        name: 'bugReportDetail',
        component: () => import('@/views/bugreport/BugReportDetailView.vue'),
        meta: {
          title: 'Bug简报详情',
        },
      },
      {
        path: 'bug-report/edit/:id?',
        name: 'bugReportEdit',
        component: () => import('@/views/bugreport/BugReportEditView.vue'),
        meta: {
          title: 'Bug简报编辑',
        },
      },
      {
        path: 'bug-report/statistics',
        name: 'bugReportStatistics',
        component: () => import('@/views/bugreport/BugReportStatisticsView.vue'),
        meta: {
          title: 'Bug简报统计',
        },
      },
      {
        path: 'notification',
        name: 'notificationCenter',
        component: () => import('@/views/notification/NotificationCenterView.vue'),
        meta: {
          title: '通知中心',
        },
      },
      {
        path: 'manage/category',
        name: 'manageCategory',
        component: () => import('@/views/manage/CategoryManageView.vue'),
        meta: {
          title: '分类管理',
        },
      },
      {
        path: 'manage/workflow',
        name: 'manageWorkflow',
        component: () => import('@/views/manage/WorkflowManageView.vue'),
        meta: {
          title: '工作流管理',
        },
      },
      {
        path: 'manage/sla',
        name: 'manageSla',
        component: () => import('@/views/manage/SlaManageView.vue'),
        meta: {
          title: 'SLA管理',
        },
      },
      {
        path: 'manage/user',
        name: 'manageUser',
        component: () => import('@/views/manage/UserManageView.vue'),
        meta: {
          title: '组织账号管理',
        },
      },
      {
        path: 'manage/settings',
        name: 'manageSettings',
        component: () => import('@/views/manage/SettingsView.vue'),
        meta: {
          title: '系统设置',
        },
      },
      {
        path: 'manage/operation-log',
        name: 'manageOperationLog',
        component: () => import('@/views/manage/OperationLogView.vue'),
        meta: {
          title: '工单日志',
        },
      },
      {
        path: 'design-system',
        name: 'designSystem',
        component: () => import('@/views/demo/DesignSystemView.vue'),
        meta: {
          title: '设计系统示例',
        },
      },
    ],
  },
  {
    path: '/open/ticket/:ticketNo',
    name: 'ticketPublicDetail',
    component: () => import('@/views/ticket/TicketPublicView.vue'),
    meta: {
      title: '工单详情',
      requiresAuth: false,
    },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'notFound',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: {
      title: '页面不存在',
      requiresAuth: false,
    },
  },
]
