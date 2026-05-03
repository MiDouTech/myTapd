<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { ArrowRight, Document as DocumentOutlined, Edit } from '@element-plus/icons-vue'

import { getCategoryTree } from '@/api/category'
import {
  getTicketDetail,
  getTicketNodeDuration,
  getTicketPage,
  getTicketTimeTrack,
} from '@/api/ticket'
import { getFlowHistory } from '@/api/workflow'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import BugChangeHistory from '@/views/ticket/components/bug/BugChangeHistory.vue'
import BugDetailInfoPanel from '@/views/ticket/components/bug/BugDetailInfoPanel.vue'
import TicketTimeTrackPanel from '@/views/ticket/components/TicketTimeTrackPanel.vue'
import type { CategoryTreeOutput } from '@/types/category'
import type {
  BugChangeHistoryOutput,
  TicketDetailOutput,
  TicketListOutput,
  TicketNodeDurationItem,
  TicketPageInput,
  TicketTimeTrackItem,
  TicketView,
} from '@/types/ticket'
import type { TicketFlowRecordOutput } from '@/types/workflow'
import {
  consumeTicketListKeywordClearFromHeader,
  layoutTicketSearchKeyword,
  persistLayoutTicketSearch,
} from '@/stores/layoutTicketSearch'
import { formatDateTime, formatDurationSec, formatFileSize, formatRoleLabel } from '@/utils/formatter'
import { parseProblemScreenshotUrls } from '@/utils/problem-screenshot-urls'
import { formatTicketDescriptionForDisplay } from '@/utils/ticket-description-display'
import { notifyWarning } from '@/utils/feedback'
const route = useRoute()
const router = useRouter()

const isMobile = ref(false)
const MOBILE_BREAKPOINT = 768
const loading = ref(false)
const categoryTree = ref<CategoryTreeOutput[]>([])
const tableData = ref<TicketListOutput[]>([])
const total = ref(0)
const selectedRows = ref<TicketListOutput[]>([])

const previewDrawerVisible = ref(false)
const previewLoading = ref(false)
const previewDetail = ref<TicketDetailOutput | null>(null)
const previewTicketId = ref<number | null>(null)

const previewDescriptionHtml = computed(() =>
  formatTicketDescriptionForDisplay(previewDetail.value?.description),
)

const previewTimeTrackItems = ref<TicketTimeTrackItem[]>([])
const previewTimeTrackStandalone = ref<BugChangeHistoryOutput[]>([])
const previewNodeDurationItems = ref<TicketNodeDurationItem[]>([])
const previewFlowHistory = ref<TicketFlowRecordOutput[]>([])

const previewChangeHistoryCount = ref(0)
const previewActiveMainTab = ref('detail')
const previewActiveBugTab = ref('customer')

const IMPACT_SCOPE_LABEL_MAP: Record<string, string> = {
  SINGLE: '单一商户',
  PARTIAL: '部分商户',
  ALL: '全部商户',
}

const SEVERITY_LABEL_MAP: Record<string, string> = {
  P0: '致命',
  P1: '严重',
  P2: '一般',
  P3: '轻微',
  P4: '建议',
}

const STATUS_LABEL_MAP: Record<string, string> = {
  pending: '待处理',
  pending_assign: '待分派',
  pending_accept: '待受理',
  alert_triggered: '待认领',
  alert_acknowledged: '处置中',
  alert_stable: '待确认',
  alert_resolved: '已解决',
  alert_suppressed: '已抑制',
  processing: '处理中',
  suspended: '已挂起',
  pending_verify: '待验收',
  completed: '已完成',
  closed: '已关闭',
  pending_test_accept: '待测试受理',
  testing: '测试复现中',
  investigating: '排查中',
  pending_dev_accept: '待开发受理',
  developing: '开发解决中',
  temp_resolved: '临时解决',
  pending_cs_confirm: '待客服确认',
  submitted: '已提交',
  dept_approval: '部门审批',
  executing: '执行中',
  rejected: '已驳回',
}

const query = reactive<TicketPageInput>({
  pageNum: 1,
  pageSize: 20,
  view: 'my_todo',
  keyword: '',
  ticketNo: '',
  title: '',
  categoryId: undefined,
  statuses: [],
  priority: '',
  creatorId: undefined,
  assigneeId: undefined,
  orderBy: undefined,
  asc: false,
  slaStatus: '',
})

const timeRange = ref<string[]>([])

const viewTabs: Array<{ label: string; value: TicketView }> = [
  { label: '我待办的', value: 'my_todo' },
  { label: '我创建的', value: 'my_created' },
  { label: '我参与的', value: 'my_participated' },
  { label: '我关注的', value: 'my_followed' },
  { label: '待出简报工单', value: 'my_brief_todo' },
  { label: '所有工单', value: 'all' },
]

const isBriefTodoView = computed(() => query.view === 'my_brief_todo')
const selectedBriefTicketIds = computed(() =>
  Array.from(
    new Set(
      selectedRows.value
        .map((item) => item.id)
        .filter((id): id is number => Number.isFinite(id) && id > 0),
    ),
  ),
)

const categoryOptions = computed(() => {
  const options: Array<{ label: string; value: number }> = []
  const walk = (nodes: CategoryTreeOutput[], prefix = ''): void => {
    nodes.forEach((node) => {
      const label = prefix ? `${prefix} / ${node.name}` : node.name
      options.push({ label, value: node.id })
      if (node.children?.length) {
        walk(node.children, label)
      }
    })
  }
  walk(categoryTree.value)
  return options
})

function normalizeViewFromRoute(): TicketView {
  if (route.path === '/ticket/all') {
    return 'all'
  }
  const routeView = route.query.view
  const values = viewTabs.map((item) => item.value)
  if (typeof routeView === 'string' && values.includes(routeView as TicketView)) {
    return routeView as TicketView
  }
  return 'my_todo'
}

async function loadCategoryTree(): Promise<void> {
  categoryTree.value = await getCategoryTree()
}

async function loadTickets(): Promise<void> {
  loading.value = true
  try {
    const params: TicketPageInput = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      view: query.view,
    }
    if (query.keyword?.trim()) {
      params.keyword = query.keyword.trim()
    } else {
      if (query.ticketNo?.trim()) params.ticketNo = query.ticketNo.trim()
      if (query.title?.trim()) params.title = query.title.trim()
    }
    if (query.categoryId) params.categoryId = query.categoryId
    if (query.statuses?.length) {
      params.statuses = query.statuses
    }
    if (query.slaStatus) params.slaStatus = query.slaStatus
    if (query.priority) params.priority = query.priority
    if (query.creatorId) params.creatorId = query.creatorId
    if (query.assigneeId) params.assigneeId = query.assigneeId
    if (query.orderBy) {
      params.orderBy = query.orderBy
      params.asc = query.asc
    }
    if (timeRange.value?.[0]) params.createTimeStart = timeRange.value[0]
    if (timeRange.value?.[1]) params.createTimeEnd = timeRange.value[1]

    const response = await getTicketPage(params)
    tableData.value = response.records
    total.value = response.total
    // 列表刷新后清空选择，避免跨页/筛选后仍保留旧勾选导致误写简报
    selectedRows.value = []
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  query.pageNum = 1
  query.keyword = ''
  const kw = (query.ticketNo || '').trim() || (query.title || '').trim()
  layoutTicketSearchKeyword.value = kw
  persistLayoutTicketSearch(kw)
  const next = { ...route.query }
  delete next.q
  const targetPath = router.resolve({ path: route.path, query: next }).fullPath
  if (targetPath === route.fullPath) {
    loadTickets()
    return
  }
  void router.replace({ path: route.path, query: next })
}

function applyKeywordToQuery(keyword: string): void {
  const k = keyword.trim()
  if (!k) {
    query.keyword = ''
    query.ticketNo = ''
    query.title = ''
    return
  }
  query.keyword = k
  query.ticketNo = ''
  query.title = ''
}

function handleReset(): void {
  query.keyword = ''
  query.ticketNo = ''
  query.title = ''
  query.categoryId = undefined
  query.statuses = []
  query.priority = ''
  query.slaStatus = ''
  query.creatorId = undefined
  query.assigneeId = undefined
  timeRange.value = []
  query.pageNum = 1
  layoutTicketSearchKeyword.value = ''
  persistLayoutTicketSearch('')
  const next = { ...route.query }
  delete next.q
  delete next.status
  delete next.slaStatus
  void router.replace({ path: route.path, query: next })
}

function handleTabChange(value: string | number): void {
  const view = value as TicketView
  const qTrim = typeof route.query.q === 'string' ? route.query.q.trim() : ''
  const qPart = qTrim ? { q: qTrim } : {}
  if (view === 'all') {
    router.push({ path: '/ticket/all', query: qPart })
    return
  }
  router.push({
    path: '/ticket/mine',
    query: {
      view,
      ...qPart,
    },
  })
}

function handleSortChange(payload: {
  prop: string
  order: 'ascending' | 'descending' | null
}): void {
  const sortMap: Record<string, string> = {
    createTime: 'create_time',
    updateTime: 'update_time',
    priority: 'priority',
  }
  query.orderBy = payload.order && payload.prop ? sortMap[payload.prop] || payload.prop : undefined
  query.asc = payload.order === 'ascending'
  loadTickets()
}

function handleSelectionChange(rows: unknown[]): void {
  selectedRows.value = rows as TicketListOutput[]
}

function handleCreateBugBrief(): void {
  if (!isBriefTodoView.value) {
    return
  }
  const ticketIds = selectedBriefTicketIds.value
  if (ticketIds.length === 0) {
    notifyWarning('请至少选择一条工单后再去写简报')
    return
  }
  void router.push({
    path: '/bug-report/edit',
    query: {
      ticketIds: ticketIds.join(','),
    },
  })
}

function handlePaginationChange(payload: { pageNum: number; pageSize: number }): void {
  query.pageNum = payload.pageNum
  query.pageSize = payload.pageSize
  loadTickets()
}

function openDetail(row: TicketListOutput): void {
  router.push(`/ticket/detail/${row.id}`)
}

function reproduceEnvLabel(code?: string): string {
  if (code === 'PRODUCTION') return '生产环境'
  if (code === 'TEST') return '测试环境'
  if (code === 'BOTH') return '均可复现'
  return code || '-'
}

function normalizeStatus(status?: string): string {
  if (!status) {
    return ''
  }
  const code = status.trim().toLowerCase()
  if (code === 'pending_dispatch') return 'pending_assign'
  if (code === 'pending_test') return 'pending_test_accept'
  if (code === 'pending_dev') return 'pending_dev_accept'
  return code
}

function getStatusLabel(status?: string): string {
  if (!status) return '-'
  const normalized = normalizeStatus(status)
  return STATUS_LABEL_MAP[normalized] || status
}

function impactScopeLabel(code?: string): string {
  if (!code) return '-'
  return IMPACT_SCOPE_LABEL_MAP[code.toUpperCase()] || code
}

function severityLabel(code?: string): string {
  if (!code) return '-'
  const u = code.trim().toUpperCase()
  return SEVERITY_LABEL_MAP[u] || code
}

function isImageFile(fileType?: string): boolean {
  if (!fileType) return false
  return fileType.startsWith('image/')
}

function isVideoFile(fileType?: string): boolean {
  if (!fileType) return false
  return fileType.startsWith('video/')
}

const previewAttachmentImageUrls = computed(() => {
  return (previewDetail.value?.attachments ?? [])
    .filter((a) => isImageFile(a.fileType))
    .map((a) => a.filePath || '')
})

function getPreviewAttachmentImageIndex(filePath?: string): number {
  if (!filePath) return 0
  const idx = previewAttachmentImageUrls.value.indexOf(filePath)
  return idx >= 0 ? idx : 0
}

async function refreshPreviewDetail(): Promise<void> {
  const id = previewDetail.value?.id
  if (id == null) {
    return
  }
  previewLoading.value = true
  try {
    previewDetail.value = await getTicketDetail(id)
    await loadPreviewExtra(id)
  } finally {
    previewLoading.value = false
  }
}

function resetPreviewExtra(): void {
  previewTimeTrackItems.value = []
  previewTimeTrackStandalone.value = []
  previewNodeDurationItems.value = []
  previewFlowHistory.value = []
  previewChangeHistoryCount.value = 0
  previewActiveMainTab.value = 'detail'
  previewActiveBugTab.value = 'customer'
}

async function loadPreviewExtra(ticketId: number): Promise<void> {
  try {
    const [trackOutput, nodeOutput, flow] = await Promise.all([
      getTicketTimeTrack(ticketId),
      getTicketNodeDuration(ticketId),
      getFlowHistory(ticketId),
    ])
    previewTimeTrackItems.value = trackOutput.tracks || []
    previewTimeTrackStandalone.value = trackOutput.standaloneFieldChanges || []
    previewNodeDurationItems.value = nodeOutput.nodes || []
    previewFlowHistory.value = flow || []
  } catch {
    previewTimeTrackItems.value = []
    previewTimeTrackStandalone.value = []
    previewNodeDurationItems.value = []
    previewFlowHistory.value = []
  }
}

const previewCustomFieldEntries = computed(() => {
  if (!previewDetail.value?.customFields) {
    return []
  }
  return Object.entries(previewDetail.value.customFields).map(([key, value]) => ({ key, value }))
})

const previewProblemScreenshotUrls = computed(() =>
  parseProblemScreenshotUrls(previewDetail.value?.bugCustomerInfo?.problemScreenshot),
)

const previewProblemScreenshotImageUrls = computed(() =>
  previewProblemScreenshotUrls.value.filter((u) => u.startsWith('http')),
)

function openBugReportDetail(reportId: number): void {
  router.push(`/bug-report/detail/${reportId}`)
}

async function openTitlePreview(row: TicketListOutput): Promise<void> {
  previewTicketId.value = row.id
  previewDetail.value = null
  resetPreviewExtra()
  previewDrawerVisible.value = true
  previewLoading.value = true
  try {
    previewDetail.value = await getTicketDetail(row.id)
    await loadPreviewExtra(row.id)
  } finally {
    previewLoading.value = false
  }
}

function goPreviewEdit(): void {
  const id = previewDetail.value?.id ?? previewTicketId.value
  if (id == null) {
    return
  }
  previewDrawerVisible.value = false
  router.push(`/ticket/detail/${id}`)
}

function getStatusType(status?: string): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  if (!status) return 'info'
  // 终态：完成/关闭/驳回
  if (['completed', 'closed', 'rejected', 'alert_resolved', 'alert_suppressed'].includes(status)) {
    return 'success'
  }
  // 待处理类（需要人工介入）
  if (
    [
      'pending_assign',
      'pending_accept',
      'pending_test_accept',
      'pending_dev_accept',
      'pending_verify',
      'pending_cs_confirm',
      'alert_triggered',
      'alert_stable',
    ].includes(status)
  ) {
    return 'warning'
  }
  // 进行中类
  if (['processing', 'testing', 'developing', 'executing', 'alert_acknowledged'].includes(status)) {
    return 'primary'
  }
  // 挂起类
  if (status === 'suspended') {
    return 'danger'
  }
  return 'info'
}

function getPriorityType(priority?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (priority === 'urgent') {
    return 'danger'
  }
  if (priority === 'high') {
    return 'warning'
  }
  if (priority === 'low') {
    return 'success'
  }
  return 'info'
}

function updateViewportState(): void {
  isMobile.value = window.innerWidth <= MOBILE_BREAKPOINT
}

watch(
  () => route.fullPath,
  () => {
    const rawQ = typeof route.query.q === 'string' ? route.query.q.trim() : ''
    if (rawQ) {
      applyKeywordToQuery(rawQ)
      layoutTicketSearchKeyword.value = rawQ
      persistLayoutTicketSearch(rawQ)
    } else if (consumeTicketListKeywordClearFromHeader()) {
      query.keyword = ''
      query.ticketNo = ''
      query.title = ''
    }

    const normalized = normalizeViewFromRoute()
    query.view = normalized
    query.pageNum = 1
    selectedRows.value = []

    // 从仪表盘卡片跳转时携带的状态/SLA过滤参数（status 支持单值或多值 query）
    const rawStatusQ = route.query.status
    const routeStatuses: string[] = Array.isArray(rawStatusQ)
      ? rawStatusQ.filter((s): s is string => typeof s === 'string').map((s) => s.trim()).filter(Boolean)
      : typeof rawStatusQ === 'string' && rawStatusQ.trim()
        ? [rawStatusQ.trim()]
        : []
    const routeSlaStatus = typeof route.query.slaStatus === 'string' ? route.query.slaStatus : ''
    query.statuses = routeStatuses
    query.slaStatus = routeSlaStatus

    // 进入「我的工单」时补齐默认 view，避免仅路径 /ticket/mine、无 query 时刷新/分享链接与当前 Tab 不一致
    if (route.path === '/ticket/mine') {
      const raw = route.query.view
      const q = typeof raw === 'string' ? raw : undefined
      if (q !== normalized) {
        router.replace({ path: '/ticket/mine', query: { ...route.query, view: normalized } })
        return
      }
    }
    loadTickets()
  },
  { immediate: true },
)

watch(
  () => [query.keyword, query.ticketNo, query.title] as const,
  ([kw, no, tit]) => {
    const k = (kw || '').trim()
    const n = (no || '').trim()
    const t = (tit || '').trim()
    const next = k || n || t
    if (layoutTicketSearchKeyword.value.trim() !== next) {
      layoutTicketSearchKeyword.value = next
      persistLayoutTicketSearch(next)
    }
  },
)

onMounted(() => {
  updateViewportState()
  window.addEventListener('resize', updateViewportState)
  loadCategoryTree()
})

onUnmounted(() => {
  window.removeEventListener('resize', updateViewportState)
})
</script>

<template>
  <div class="ticket-list-page">
    <el-card shadow="never" class="ticket-list-card">
      <div v-if="isMobile" class="mobile-view-switch">
        <el-select
          :model-value="query.view"
          placeholder="请选择内容"
          class="mobile-view-select"
          @change="handleTabChange"
        >
          <el-option v-for="tab in viewTabs" :key="tab.value" :label="tab.label" :value="tab.value" />
        </el-select>
      </div>
      <el-tabs v-else :model-value="query.view" class="ticket-view-tabs" @tab-change="handleTabChange">
        <el-tab-pane
          v-for="tab in viewTabs"
          :key="tab.value"
          :label="tab.label"
          :name="tab.value"
        />
      </el-tabs>
      <el-form
        :inline="!isMobile"
        :label-width="isMobile ? 'auto' : '72px'"
        class="query-form"
        @submit.prevent="handleSearch"
      >
        <el-form-item label="工单编号" class="query-form-item">
          <el-input v-model="query.ticketNo" class="query-input" placeholder="支持模糊匹配" clearable />
        </el-form-item>
        <el-form-item label="标题" class="query-form-item">
          <el-input v-model="query.title" class="query-input" placeholder="支持模糊匹配" clearable />
        </el-form-item>
        <el-form-item label="分类" class="query-form-item">
          <el-select
            v-model="query.categoryId"
            class="query-input"
            placeholder="请选择内容"
            clearable
            filterable
          >
            <el-option
              v-for="option in categoryOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" class="query-form-item">
          <el-select
            v-model="query.statuses"
            class="query-input"
            placeholder="请选择内容"
            clearable
            multiple
            collapse-tags
            collapse-tags-tooltip
          >
            <!-- 通用工单状态 -->
            <el-option label="待分派" value="pending_assign" />
            <el-option label="待受理" value="pending_accept" />
            <el-option label="告警·待认领" value="alert_triggered" />
            <el-option label="告警·处置中" value="alert_acknowledged" />
            <el-option label="告警·待确认" value="alert_stable" />
            <el-option label="告警·已解决" value="alert_resolved" />
            <el-option label="告警·已抑制" value="alert_suppressed" />
            <el-option label="处理中" value="processing" />
            <el-option label="已挂起" value="suspended" />
            <el-option label="待验收" value="pending_verify" />
            <el-option label="已完成" value="completed" />
            <el-option label="已关闭" value="closed" />
            <!-- 缺陷工单专属状态 -->
            <el-option label="待测试受理" value="pending_test_accept" />
            <el-option label="测试复现中" value="testing" />
            <el-option label="待开发受理" value="pending_dev_accept" />
            <el-option label="开发解决中" value="developing" />
            <el-option label="待客服确认" value="pending_cs_confirm" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" class="query-form-item">
          <el-select v-model="query.priority" class="query-input" placeholder="请选择内容" clearable>
            <el-option label="紧急" value="urgent" />
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围" class="query-form-item">
          <el-date-picker
            v-model="timeRange"
            class="query-input"
            type="daterange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item class="query-form-item query-form-actions">
          <el-space class="query-action-buttons">
            <el-button type="primary" native-type="submit" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-space>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="ticket-list-card">
      <div v-if="isBriefTodoView" class="brief-toolbar">
        <div class="brief-toolbar-left">
          <span class="brief-toolbar-title">个人待出简报工单</span>
          <span class="brief-toolbar-desc">勾选后可批量带入简报编辑页</span>
        </div>
        <el-button
          type="primary"
          :disabled="selectedBriefTicketIds.length === 0"
          @click="handleCreateBugBrief"
        >
          去写简报（{{ selectedBriefTicketIds.length }}）
        </el-button>
      </div>
      <EmptyState v-if="!loading && tableData.length === 0" description="暂无工单数据" />
      <template v-else>
        <BaseTable
          :data="tableData"
          :loading="loading"
          :show-selection="isBriefTodoView"
          class="ticket-table"
          @selection-change="handleSelectionChange"
          @sort-change="handleSortChange"
        >
          <el-table-column prop="ticketNo" label="工单编号" width="160" sortable="custom">
            <template #default="{ row }">
              <el-button type="primary" link class="cell-link" @click="openDetail(row)">
                {{ row.ticketNo }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column
            prop="companyName"
            label="公司名称"
            min-width="140"
            :show-overflow-tooltip="true"
          >
            <template #default="{ row }">
              {{ row.companyName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="220" :show-overflow-tooltip="true">
            <template #default="{ row }">
              <el-tooltip :content="row.title" placement="top" :disabled="!row.title">
                <el-button type="primary" link class="cell-link title-cell-btn" @click="openTitlePreview(row)">
                  <span class="title-cell-text">{{ row.title }}</span>
                </el-button>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="分类" min-width="140" />
          <el-table-column label="优先级" width="100" sortable="custom" prop="priority">
            <template #default="{ row }">
              <el-tag :type="getPriorityType(row.priority)">{{
                row.priorityLabel || row.priority
              }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">{{ row.statusLabel || row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="SLA" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.slaStatus === 'BREACHED'" type="danger" size="small" effect="dark">
                {{ row.slaStatusLabel || '已超时' }}
              </el-tag>
              <el-tag v-else-if="row.slaStatus === 'WARNING'" type="warning" size="small" effect="dark">
                {{ row.slaStatusLabel || '预警中' }}
              </el-tag>
              <span v-else-if="row.slaStatus === 'NORMAL'" class="sla-normal">正常</span>
              <span v-else class="sla-none">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="creatorName" label="创建人" width="120" />
          <el-table-column prop="assigneeName" label="处理人" min-width="160" :show-overflow-tooltip="true" />
          <el-table-column prop="createTime" label="创建时间" width="180" sortable="custom">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="180" sortable="custom">
            <template #default="{ row }">
              {{ formatDateTime(row.updateTime) }}
            </template>
          </el-table-column>
          <el-table-column
            label="操作"
            :width="isMobile ? 90 : 120"
            align="center"
            :fixed="isMobile ? undefined : 'right'"
          >
            <template #default="{ row }">
              <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </BaseTable>
        <BasePagination
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          @update="handlePaginationChange"
        />
      </template>
    </el-card>

    <el-drawer
      v-model="previewDrawerVisible"
      direction="rtl"
      size="min(960px, 96vw)"
      destroy-on-close
      class="ticket-preview-drawer"
      :show-close="true"
    >
      <template #header>
        <div class="preview-drawer-header">
          <div class="preview-drawer-header-main">
            <div class="preview-drawer-meta">
              <el-tag
                v-if="previewDetail?.status"
                :type="getStatusType(previewDetail.status)"
                size="small"
                effect="dark"
                class="preview-status-tag"
              >
                {{ previewDetail.statusLabel || previewDetail.status }}
              </el-tag>
              <span class="preview-ticket-no">{{ previewDetail?.ticketNo || '—' }}</span>
            </div>
            <h3 class="preview-title" :title="previewDetail?.title">{{ previewDetail?.title || '加载中…' }}</h3>
          </div>
          <el-button type="primary" :disabled="!previewDetail" @click="goPreviewEdit">
            <el-icon class="preview-edit-icon"><Edit /></el-icon>
            编辑
          </el-button>
        </div>
      </template>

      <div v-loading="previewLoading" class="preview-drawer-body">
        <template v-if="previewDetail">
          <div class="preview-layout">
            <div class="preview-main">
              <div v-if="previewDescriptionHtml" class="preview-block">
                <div class="preview-block-label">描述</div>
                <!-- eslint-disable-next-line vue/no-v-html -->
                <div class="preview-html" v-html="previewDescriptionHtml" />
              </div>

              <el-tabs v-model="previewActiveMainTab" class="preview-tabs">
                <el-tab-pane label="详细信息" name="detail">
                  <el-tabs v-model="previewActiveBugTab" class="preview-inner-tabs">
                    <el-tab-pane label="客服信息" name="customer">
                      <el-descriptions :column="1" border size="small" class="preview-desc">
                        <el-descriptions-item label="商户编号">
                          {{ previewDetail.bugCustomerInfo?.merchantNo || '-' }}
                        </el-descriptions-item>
                        <el-descriptions-item label="公司名称">
                          {{ previewDetail.bugCustomerInfo?.companyName || '-' }}
                        </el-descriptions-item>
                        <el-descriptions-item label="商户账号">
                          {{ previewDetail.bugCustomerInfo?.merchantAccount || '-' }}
                        </el-descriptions-item>
                        <el-descriptions-item label="场景码">
                          {{ previewDetail.bugCustomerInfo?.sceneCode || '-' }}
                        </el-descriptions-item>
                        <el-descriptions-item label="问题描述">
                          <span class="preview-plain">{{ previewDetail.bugCustomerInfo?.problemDesc || '-' }}</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="预期结果">
                          <span class="preview-plain">{{ previewDetail.bugCustomerInfo?.expectedResult || '-' }}</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="问题截图">
                          <div v-if="previewProblemScreenshotUrls.length" class="preview-screenshot-block">
                            <div v-for="url in previewProblemScreenshotUrls" :key="url" class="preview-screenshot-row">
                              <a
                                v-if="url.startsWith('http')"
                                :href="url"
                                target="_blank"
                                rel="noopener noreferrer"
                                class="preview-screenshot-link"
                              >{{ url }}</a>
                              <span v-else class="preview-plain">{{ url }}</span>
                              <el-image
                                v-if="url.startsWith('http')"
                                :src="url"
                                :preview-src-list="previewProblemScreenshotImageUrls"
                                :initial-index="previewProblemScreenshotImageUrls.indexOf(url) >= 0 ? previewProblemScreenshotImageUrls.indexOf(url) : 0"
                                fit="cover"
                                class="preview-screenshot-thumb"
                                preview-teleported
                              />
                            </div>
                          </div>
                          <span v-else>-</span>
                        </el-descriptions-item>
                      </el-descriptions>
                    </el-tab-pane>
                    <el-tab-pane label="测试信息" name="test">
                      <!-- eslint-disable vue/no-v-html — 与工单详情页一致，展示后端富文本字段 -->
                      <el-descriptions :column="1" border size="small" class="preview-desc">
                        <el-descriptions-item label="复现环境">
                          {{ reproduceEnvLabel(previewDetail.bugTestInfo?.reproduceEnv) }}
                        </el-descriptions-item>
                        <el-descriptions-item label="复现步骤">
                          <div
                            v-if="previewDetail.bugTestInfo?.reproduceSteps"
                            class="preview-html preview-html--compact"
                            v-html="previewDetail.bugTestInfo.reproduceSteps"
                          />
                          <span v-else>-</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="实际结果">
                          <div
                            v-if="previewDetail.bugTestInfo?.actualResult"
                            class="preview-html preview-html--compact"
                            v-html="previewDetail.bugTestInfo.actualResult"
                          />
                          <span v-else>-</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="影响范围">
                          {{ impactScopeLabel(previewDetail.bugTestInfo?.impactScope) }}
                        </el-descriptions-item>
                        <el-descriptions-item label="缺陷等级">
                          {{ severityLabel(previewDetail.bugTestInfo?.severityLevel) }}
                        </el-descriptions-item>
                        <el-descriptions-item label="所属模块">
                          {{ previewDetail.bugTestInfo?.moduleName || '-' }}
                        </el-descriptions-item>
                        <el-descriptions-item label="测试备注">
                          <div
                            v-if="previewDetail.bugTestInfo?.testRemark"
                            class="preview-html preview-html--compact"
                            v-html="previewDetail.bugTestInfo.testRemark"
                          />
                          <span v-else>-</span>
                        </el-descriptions-item>
                      </el-descriptions>
                      <!-- eslint-enable vue/no-v-html -->
                    </el-tab-pane>
                    <!-- eslint-disable vue/no-v-html — 与工单详情页一致，开发备注为富文本 -->
                    <el-tab-pane label="开发信息" name="dev">
                      <el-descriptions :column="1" border size="small" class="preview-desc">
                        <el-descriptions-item label="缺陷原因">
                          <span class="preview-plain">{{ previewDetail.bugDevInfo?.rootCause || '-' }}</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="修复方案">
                          <span class="preview-plain">{{ previewDetail.bugDevInfo?.fixSolution || '-' }}</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="关联分支">
                          {{ previewDetail.bugDevInfo?.gitBranch || '-' }}
                        </el-descriptions-item>
                        <el-descriptions-item label="影响评估">
                          <span class="preview-plain">{{ previewDetail.bugDevInfo?.impactAssessment || '-' }}</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="开发备注">
                          <div
                            v-if="previewDetail.bugDevInfo?.devRemark"
                            class="preview-html preview-html--compact"
                            v-html="previewDetail.bugDevInfo.devRemark"
                          />
                          <span v-else>-</span>
                        </el-descriptions-item>
                      </el-descriptions>
                    </el-tab-pane>
                    <!-- eslint-enable vue/no-v-html -->
                    <el-tab-pane label="时间追踪" name="track">
                      <TicketTimeTrackPanel
                        :tracks="previewTimeTrackItems"
                        :standalone-field-changes="previewTimeTrackStandalone"
                        :node-duration-items="previewNodeDurationItems"
                        :status-label-fn="getStatusLabel"
                        :role-label-fn="formatRoleLabel"
                        :format-duration="formatDurationSec"
                      />
                    </el-tab-pane>
                  </el-tabs>
                </el-tab-pane>
                <el-tab-pane :label="`变更历史(${previewChangeHistoryCount})`" name="history">
                  <BugChangeHistory
                    v-if="previewDetail.id"
                    :ticket-id="previewDetail.id"
                    @count-update="previewChangeHistoryCount = $event"
                  />
                </el-tab-pane>
                <el-tab-pane label="流转历史" name="flow-history">
                  <div class="preview-flow-history">
                    <el-empty v-if="!previewFlowHistory.length" description="暂无流转记录" />
                    <el-timeline v-else>
                      <el-timeline-item
                        v-for="record in previewFlowHistory"
                        :key="record.id"
                        :timestamp="record.createTime"
                        placement="top"
                      >
                        <div class="preview-flow-record">
                          <div class="preview-flow-record-main">
                            <el-tag size="small" :type="record.flowType === 'RETURN' ? 'warning' : 'primary'">
                              {{ record.flowTypeLabel || record.flowType }}
                            </el-tag>
                            <span v-if="record.fromStatusName" class="preview-flow-status">{{ record.fromStatusName }}</span>
                            <el-icon v-if="record.toStatus !== record.fromStatus" class="preview-flow-arrow">
                              <ArrowRight />
                            </el-icon>
                            <span v-if="record.toStatus !== record.fromStatus" class="preview-flow-status preview-flow-status-to">
                              {{ record.toStatusName }}
                            </span>
                            <el-divider direction="vertical" />
                            <span class="preview-flow-operator">
                              操作人：{{ record.operatorName || record.operatorId }}（{{ formatRoleLabel(record.operatorRole) }}）
                            </span>
                            <template v-if="record.fromAssigneeName !== record.toAssigneeName && record.toAssigneeName">
                              <el-divider direction="vertical" />
                              <span class="preview-flow-operator">
                                处理人：{{ record.fromAssigneeName || '-' }} → {{ record.toAssigneeName }}
                              </span>
                            </template>
                          </div>
                          <div v-if="record.remark" class="preview-flow-remark">{{ record.remark }}</div>
                        </div>
                      </el-timeline-item>
                    </el-timeline>
                  </div>
                </el-tab-pane>
              </el-tabs>
            </div>

            <aside class="preview-side">
              <BugDetailInfoPanel
                :detail="previewDetail"
                :ticket-id="previewDetail.id"
                @refresh="refreshPreviewDetail"
              />
            </aside>
          </div>

          <el-card v-if="previewCustomFieldEntries.length > 0" shadow="never" class="preview-section-card">
            <template #header>
              <span class="preview-section-title">自定义字段</span>
            </template>
            <el-table
              :data="previewCustomFieldEntries"
              :border="false"
              :stripe="true"
              :header-cell-style="{ backgroundColor: '#f5f7fa' }"
            >
              <el-table-column prop="key" label="字段名" align="center" />
              <el-table-column prop="value" label="字段值" align="center" />
            </el-table>
          </el-card>

          <el-card shadow="never" class="preview-section-card">
            <template #header>
              <span class="preview-section-title">附件</span>
            </template>
            <EmptyState v-if="!previewDetail.attachments?.length" description="暂无附件" />
            <div v-else class="preview-attachment-list">
              <div
                v-for="attachment in previewDetail.attachments"
                :key="attachment.id"
                class="preview-attachment-item"
              >
                <div class="preview-attachment-preview">
                  <el-image
                    v-if="isImageFile(attachment.fileType)"
                    :src="attachment.filePath"
                    :preview-src-list="previewAttachmentImageUrls"
                    :initial-index="getPreviewAttachmentImageIndex(attachment.filePath)"
                    fit="cover"
                    class="preview-attachment-thumb"
                    preview-teleported
                    lazy
                  />
                  <video
                    v-else-if="isVideoFile(attachment.fileType)"
                    :src="attachment.filePath"
                    controls
                    class="preview-attachment-video"
                    preload="metadata"
                  />
                  <el-icon v-else class="preview-attachment-icon"><DocumentOutlined /></el-icon>
                </div>
                <div class="preview-attachment-info">
                  <div class="preview-attachment-name" :title="attachment.fileName">{{ attachment.fileName }}</div>
                  <div class="preview-attachment-meta">
                    <el-tag v-if="attachment.source === 'WECOM_BOT'" type="success" size="small" effect="plain">企微</el-tag>
                    <el-tag v-else type="info" size="small" effect="plain">Web</el-tag>
                    <span>{{ formatFileSize(attachment.fileSize) }}</span>
                    <span class="preview-meta-dot">·</span>
                    <span>{{ attachment.uploadedByName || '-' }}</span>
                    <span class="preview-meta-dot">·</span>
                    <span>{{ formatDateTime(attachment.createTime) }}</span>
                  </div>
                </div>
                <el-link
                  v-if="isImageFile(attachment.fileType) && attachment.filePath"
                  type="primary"
                  :href="attachment.filePath"
                  target="_blank"
                  rel="noopener noreferrer"
                >查看</el-link>
                <el-link
                  v-if="isVideoFile(attachment.fileType) && attachment.filePath"
                  type="primary"
                  :href="attachment.filePath"
                  target="_blank"
                  rel="noopener noreferrer"
                >播放</el-link>
              </div>
            </div>
          </el-card>

          <el-card v-if="previewDetail.bugReports?.length" shadow="never" class="preview-section-card">
            <template #header>
              <span class="preview-section-title">关联Bug简报</span>
            </template>
            <el-table
              :data="previewDetail.bugReports"
              :border="false"
              :stripe="true"
              :header-cell-style="{ backgroundColor: '#f5f7fa' }"
            >
              <el-table-column prop="reportNo" label="简报编号" min-width="160" align="center" />
              <el-table-column label="状态" width="120" align="center">
                <template #default="{ row }">
                  {{ row.statusLabel || row.status || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="关联方式" width="100" align="center">
                <template #default="{ row }">
                  {{ row.isAutoCreated === 1 ? '自动' : '手动' }}
                </template>
              </el-table-column>
              <el-table-column label="关联时间" width="170" align="center">
                <template #default="{ row }">
                  {{ formatDateTime(row.createTime) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="112" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button
                    type="primary"
                    size="small"
                    plain
                    class="bug-report-view-btn"
                    @click="openBugReportDetail(row.id)"
                  >查看简报</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <el-card shadow="never" class="preview-section-card">
            <template #header>
              <div class="preview-section-header">
                <span class="preview-section-title">评论与处理记录</span>
                <span class="preview-section-count">{{ previewDetail.comments?.length || 0 }} 条</span>
              </div>
            </template>
            <EmptyState v-if="!previewDetail.comments?.length" description="暂无评论" />
            <div v-else class="preview-comment-list">
              <div
                v-for="comment in previewDetail.comments"
                :key="comment.id"
                class="preview-comment-item"
                :class="{ 'preview-comment-operation': comment.type === 'OPERATION' }"
              >
                <el-avatar :size="32" class="preview-comment-avatar">
                  {{ comment.userName?.charAt(0) || '?' }}
                </el-avatar>
                <div class="preview-comment-body">
                  <div class="preview-comment-head">
                    <span class="preview-comment-user">{{ comment.userName || '-' }}</span>
                    <el-tag v-if="comment.type === 'OPERATION'" type="info" size="small" effect="plain">操作记录</el-tag>
                    <span class="preview-comment-time">{{ formatDateTime(comment.createTime) }}</span>
                  </div>
                  <div v-if="comment.type === 'OPERATION'" class="preview-comment-text preview-comment-text-plain">
                    {{ comment.content || '-' }}
                  </div>
                  <!-- eslint-disable-next-line vue/no-v-html — 与工单详情页一致，评论为富文本 -->
                  <div v-else class="preview-comment-text" v-html="comment.content || '-'" />
                </div>
              </div>
            </div>
          </el-card>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.ticket-list-page {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #fff;
}

.ticket-list-card {
  width: 100%;
}

.brief-toolbar {
  margin-bottom: 12px;
  padding: 12px 14px;
  border: 1px solid #e4ecf5;
  border-radius: 8px;
  background: #f7fbff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.brief-toolbar-left {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brief-toolbar-title {
  font-size: 14px;
  font-weight: 500;
  color: #1d2129;
}

.brief-toolbar-desc {
  font-size: 12px;
  color: #606266;
}

.mobile-view-switch {
  margin-bottom: 12px;
}

.mobile-view-select {
  width: 100%;
}

.ticket-view-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 16px;
  }

  :deep(.el-tabs__item) {
    font-size: 14px;
    font-weight: 500;
    padding: 0 18px;
    height: 40px;
    line-height: 40px;
  }

  :deep(.el-tabs__active-bar) {
    height: 3px;
    border-radius: 2px 2px 0 0;
  }
}

.query-form {
  width: 100%;
  padding: 14px 16px;
  background: var(--md-bg-panel, #f9fafb);
  border-radius: 8px;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px 16px;
}

.query-form-item {
  margin-bottom: 0;
  margin-right: 0;
}

.query-input {
  width: 210px;
  max-width: 100%;
}

.query-form-actions {
  margin-left: auto;
  margin-right: 0;
}

.query-action-buttons {
  width: auto;
}

.query-action-buttons :deep(.el-button) {
  min-width: 88px;
}

.cell-link {
  padding: 0;
  font-weight: 500;

  &:hover {
    text-decoration: underline;
  }
}

.title-cell-btn {
  display: block;
  max-width: 100%;
  height: auto;
  min-height: 0;
  padding: 0;
  line-height: 1.4;
  text-align: left;
}

.title-cell-text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sla-normal {
  color: #67c23a;
  font-size: 13px;
}

.sla-none {
  color: #c0c4cc;
  font-size: 13px;
}

.ticket-table {
  :deep(.el-table) {
    min-width: 1460px;
  }
}

// ===== Drawer 全局样式覆盖 =====
:deep(.ticket-preview-drawer) {
  .el-drawer__header {
    padding: 20px 24px 16px;
    margin-bottom: 0;
    border-bottom: 1px solid #ebedf0;
    background: #fafbfc;
  }

  .el-drawer__body {
    padding: 0;
  }

  .el-drawer__close-btn {
    top: 20px;
  }
}

// ===== Drawer 头部 =====
.preview-drawer-header {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
  padding-right: 8px;
}

.preview-drawer-header-main {
  flex: 1;
  min-width: 0;
}

.preview-drawer-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 8px;
}

.preview-status-tag {
  font-weight: 500;
  letter-spacing: 0.5px;
}

.preview-ticket-no {
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #909399;
  background: #f0f2f5;
  padding: 2px 8px;
  border-radius: 4px;
}

.preview-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.45;
  color: #1d2129;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.preview-edit-icon {
  margin-right: 4px;
  vertical-align: middle;
}

// ===== Drawer 主体 =====
.preview-drawer-body {
  min-height: 200px;
  padding: 20px 24px 24px;
}

// ===== 左右分栏 =====
.preview-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.preview-main {
  flex: 1;
  min-width: 0;
}

.preview-side {
  flex: 0 0 280px;
  position: sticky;
  top: 0;
  max-height: calc(100vh - 140px);
  overflow: auto;
  padding: 12px;
  border: 1px solid #ebedf0;
  border-radius: 10px;
  background: #fafbfc;
}

.preview-side :deep(.bug-detail-info-panel) {
  padding: 0;
}

// ===== 描述区块 =====
.preview-block {
  margin-bottom: 20px;
  padding: 14px 16px;
  background: #f8f9fb;
  border: 1px solid #ebedf0;
  border-radius: 8px;
}

.preview-block-label {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.preview-html {
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
  word-break: break-word;
  overflow: hidden;
  white-space: pre-line;

  :deep(p),
  :deep(div) {
    max-width: 100%;
    overflow-wrap: break-word;
  }

  :deep(img) {
    display: block !important;
    max-width: 100% !important;
    width: auto !important;
    height: auto !important;
    border-radius: 4px;
  }
}

.preview-html--compact {
  max-height: 280px;
  padding: 10px 12px;
  overflow: auto;
  background: #f5f7fa;
  border-radius: 6px;
  border: 1px solid #ebedf0;
}

.preview-plain {
  white-space: pre-wrap;
  word-break: break-word;
}

// ===== 选项卡 =====
.preview-tabs {
  margin-top: 4px;

  :deep(.el-tabs__header) {
    margin-bottom: 16px;
  }

  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
    background: #ebedf0;
  }

  :deep(.el-tabs__item) {
    font-size: 14px;
    font-weight: 500;
  }
}

.preview-inner-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 14px;
  }

  :deep(.el-tabs__item) {
    font-size: 13px;
  }
}

.preview-desc {
  margin-top: 8px;

  :deep(.el-descriptions__label) {
    width: 100px;
    font-weight: 500;
    color: #909399;
    background: #fafbfc;
  }

  :deep(.el-descriptions__content) {
    color: #303133;
  }
}

// ===== 分区卡片 =====
.preview-section-card {
  margin-top: 20px;
  border-radius: 10px;

  :deep(.el-card__header) {
    padding: 14px 18px;
    background: #fafbfc;
    border-bottom: 1px solid #ebedf0;
  }

  :deep(.el-card__body) {
    padding: 16px 18px;
  }
}

.preview-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.preview-section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
}

.preview-section-count {
  font-size: 12px;
  color: #909399;
  background: #f0f2f5;
  padding: 2px 10px;
  border-radius: 10px;
}

/* 关联 Bug 简报「查看简报」：浅色底 + 主色字，避免表格内像禁用态 */
.bug-report-view-btn {
  font-weight: 500;
  color: var(--md-primary-color) !important;
  border-color: rgba(22, 117, 209, 0.45) !important;
  background-color: var(--md-primary-light) !important;

  &:hover,
  &:focus {
    color: #fff !important;
    border-color: var(--md-primary-color) !important;
    background-color: var(--md-primary-color) !important;
  }
}

// ===== 问题截图 =====
.preview-screenshot-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-screenshot-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 8px;
}

.preview-screenshot-link {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  word-break: break-all;
  color: #1675d1;
}

.preview-screenshot-thumb {
  width: 72px;
  height: 72px;
  border-radius: 6px;
  flex-shrink: 0;
  border: 1px solid #ebedf0;
}

// ===== 流转历史 =====
.preview-flow-history {
  padding: 8px 0;
}

.preview-flow-record {
  background: #f8f9fb;
  border: 1px solid #ebedf0;
  border-radius: 8px;
  padding: 10px 14px;
  transition: box-shadow 0.15s;

  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
}

.preview-flow-record-main {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.preview-flow-status {
  color: #606266;
}

.preview-flow-status-to {
  font-weight: 600;
  color: #1675d1;
}

.preview-flow-arrow {
  font-size: 14px;
  color: #c0c4cc;
}

.preview-flow-operator {
  color: #909399;
  font-size: 12px;
}

.preview-flow-remark {
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
  padding-left: 2px;
}

// ===== 附件 =====
.preview-attachment-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-attachment-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 12px;
  border: 1px solid #ebedf0;
  border-radius: 8px;
  background: #fafbfc;
  transition: background 0.15s, box-shadow 0.15s;

  &:hover {
    background: #f0f7ff;
    box-shadow: 0 1px 4px rgba(22, 117, 209, 0.08);
  }
}

.preview-attachment-preview {
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  border-radius: 6px;
  overflow: hidden;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e4e7ed;
}

.preview-attachment-thumb {
  width: 52px;
  height: 52px;
}

.preview-attachment-video {
  width: 104px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  background-color: #000;
}

.preview-attachment-icon {
  font-size: 24px;
  color: #c0c4cc;
}

.preview-attachment-info {
  flex: 1;
  min-width: 0;
}

.preview-attachment-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-attachment-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.preview-meta-dot {
  color: #dcdfe6;
}

// ===== 评论列表 =====
.preview-comment-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.preview-comment-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.preview-comment-operation {
  .preview-comment-body {
    background: #f8f9fb;
    border: 1px solid #ebedf0;
  }
}

.preview-comment-avatar {
  flex-shrink: 0;
  background: #e8f0fe;
  color: #1675d1;
  font-weight: 500;
}

.preview-comment-body {
  flex: 1;
  min-width: 0;
  background: #f5f7fa;
  border-radius: 0 10px 10px 10px;
  padding: 12px 16px;
}

.preview-comment-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.preview-comment-user {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
}

.preview-comment-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-left: auto;
}

.preview-comment-text {
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
  word-break: break-word;
  overflow: hidden;
  max-width: 100%;
  box-sizing: border-box;

  :deep(p) {
    margin: 0 0 6px;
    max-width: 100%;
  }

  :deep(p:last-child) {
    margin-bottom: 0;
  }

  :deep(img) {
    display: block !important;
    max-width: 100% !important;
    width: 100% !important;
    height: auto !important;
    border-radius: 4px;
    object-fit: contain;
  }

  :deep(figure) {
    display: block !important;
    max-width: 100% !important;
    width: 100% !important;
    margin: 0 0 8px !important;
    overflow: hidden;
    box-sizing: border-box;
  }

  :deep(figure img) {
    display: block !important;
    max-width: 100% !important;
    width: 100% !important;
    height: auto !important;
    object-fit: contain;
  }
}

.preview-comment-text-plain {
  white-space: pre-wrap;
  color: #606266;
}

// ===== 响应式 =====
@media (max-width: 900px) {
  .preview-layout {
    flex-direction: column;
  }

  .preview-side {
    flex: none;
    width: 100%;
    max-height: none;
    position: static;
  }
}

@media (max-width: 768px) {
  .brief-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .query-form {
    gap: 0;
  }

  .query-form-item {
    width: 100%;
    margin-bottom: 12px;
    margin-right: 0;
  }

  .query-form-item :deep(.el-form-item__content) {
    width: 100%;
    margin-left: 0 !important;
  }

  .query-input {
    width: 100%;
  }

  .query-action-buttons :deep(.el-space__item) {
    width: calc(50% - 4px);
  }

  .query-action-buttons :deep(.el-button) {
    width: 100%;
  }

  .ticket-view-tabs :deep(.el-tabs__nav-wrap) {
    overflow-x: auto;
  }
}
</style>
