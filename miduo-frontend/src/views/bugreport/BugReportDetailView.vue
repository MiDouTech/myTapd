<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  approveBugReport,
  getBugReportDetail,
  rejectBugReport,
  submitBugReport,
  voidBugReport,
} from '@/api/bugreport'
import { getUserList } from '@/api/user'
import EmptyState from '@/components/common/EmptyState.vue'
import { useAuthStore } from '@/stores/auth'
import type {
  BugReportAttachmentOutput,
  BugReportDetailOutput,
  BugReportRelatedTicketOutput,
} from '@/types/bugreport'
import type { UserListOutput } from '@/types/user'
import { confirmAction, notifySuccess, notifyWarning } from '@/utils/feedback'
import { formatDateTime, formatFileSize } from '@/utils/formatter'
import { getTicketStatusLabel, normalizeTicketStatusCode } from '@/utils/ticketStatus'
import {
  canReviewBugReport,
  canSubmitBugReport,
  canVoidBugReport,
  getBugReportStatusLabel,
  getBugReportStatusTagType,
  isBugReportEditable,
} from '@/utils/bugreport'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const MOBILE_BREAKPOINT = 768
const loading = ref(false)
const submitLoading = ref(false)
const isMobile = ref(false)
const detail = ref<BugReportDetailOutput>()
const users = ref<UserListOutput[]>([])

const submitDialogVisible = ref(false)
const reviewDialogVisible = ref(false)
const reviewAction = ref<'approve' | 'reject'>('approve')

const submitForm = reactive({
  reviewerId: undefined as number | undefined,
  remark: '',
})

const reviewForm = reactive({
  reviewComment: '',
})

const reportId = computed(() => {
  const raw = Number(route.params.id)
  return Number.isFinite(raw) && raw > 0 ? raw : 0
})

const roleCodes = computed(() =>
  (authStore.userInfo?.roleCodes ?? []).map((item) => String(item).toUpperCase()),
)

const canEdit = computed(() => isBugReportEditable(detail.value?.status))
const canSubmit = computed(() => canSubmitBugReport(detail.value?.status))

/** P0/P1/P2 需要走审核流程；P3/P4 提交后直接归档 */
const isHighSeverityDetail = computed(() => {
  const level = detail.value?.severityLevel?.toUpperCase()
  return level === 'P0' || level === 'P1' || level === 'P2'
})
const canVoid = computed(() => canVoidBugReport(detail.value?.status))
const canReview = computed(() => {
  if (!canReviewBugReport(detail.value?.status)) {
    return false
  }
  if (hasRole('ADMIN', 'TICKET_ADMIN', 'QA_MANAGER', 'QA_LEAD', 'REPORT_REVIEWER')) {
    return true
  }
  return Boolean(authStore.userInfo?.id && authStore.userInfo.id === detail.value?.reviewerId)
})

/** 与编辑页一致：根据关联工单状态判断展示哪一类解决信息 */
const bugReportResolutionMode = computed<'temp' | 'complete' | 'unknown'>(() => {
  const tickets = detail.value?.tickets ?? []
  if (tickets.length === 0) {
    return 'unknown'
  }
  if (tickets.some((t) => normalizeTicketStatusCode(t.status) === 'temp_resolved')) {
    return 'temp'
  }
  if (tickets.every((t) => normalizeTicketStatusCode(t.status) === 'completed')) {
    return 'complete'
  }
  return 'unknown'
})

const bugReportResolutionDisplayMode = computed<'temp' | 'complete' | 'unknown'>(() => {
  const m = bugReportResolutionMode.value
  if (m !== 'unknown') {
    return m
  }
  const d = detail.value
  if (d?.resolveTime) {
    return 'complete'
  }
  if (d?.tempResolveDate || d?.tempSolution || (d?.resolveDate && d?.solution)) {
    return 'temp'
  }
  return 'unknown'
})

function hasRole(...targets: string[]): boolean {
  return targets.some((target) => roleCodes.value.includes(target))
}

function getReviewActionLabel(action?: string): string {
  const map: Record<string, string> = {
    CREATE: '创建',
    EDIT: '编辑',
    SUBMIT: '提交审核',
    APPROVE: '审核通过',
    REJECT: '审核驳回',
    VOID: '作废',
  }
  if (!action) {
    return '-'
  }
  return map[action] || action
}

async function loadUsers(): Promise<void> {
  users.value = await getUserList({})
}

async function loadDetail(): Promise<void> {
  if (!reportId.value) {
    return
  }
  loading.value = true
  try {
    const data = await getBugReportDetail(reportId.value)
    detail.value = data
    submitForm.reviewerId = data.reviewerId
  } finally {
    loading.value = false
  }
}

function updateViewportState(): void {
  isMobile.value = window.innerWidth <= MOBILE_BREAKPOINT
}

function handleBack(): void {
  router.push('/bug-report')
}

function handleEdit(): void {
  if (!detail.value?.id) {
    return
  }
  router.push(`/bug-report/edit/${detail.value.id}`)
}

function openSubmitDialog(): void {
  submitForm.reviewerId = detail.value?.reviewerId
  submitForm.remark = ''
  submitDialogVisible.value = true
}

async function handleSubmitReport(): Promise<void> {
  if (!reportId.value) {
    return
  }
  const reviewerId = submitForm.reviewerId || detail.value?.reviewerId
  if (isHighSeverityDetail.value && !reviewerId) {
    notifyWarning('P0/P1/P2 级别简报提交前请先选择审核人')
    return
  }
  submitLoading.value = true
  try {
    await submitBugReport(reportId.value, {
      reviewerId,
      remark: submitForm.remark.trim() || undefined,
    })
    const successMsg = isHighSeverityDetail.value ? '简报已提交，等待审核' : '简报已提交并直接归档（P3/P4级别无需审核）'
    notifySuccess(successMsg)
    submitDialogVisible.value = false
    await loadDetail()
  } finally {
    submitLoading.value = false
  }
}

function openReviewDialog(action: 'approve' | 'reject'): void {
  reviewAction.value = action
  reviewForm.reviewComment = ''
  reviewDialogVisible.value = true
}

async function handleReview(): Promise<void> {
  if (!reportId.value) {
    return
  }
  const reviewComment = reviewForm.reviewComment.trim()
  if (!reviewComment) {
    notifyWarning('请输入审核意见')
    return
  }
  submitLoading.value = true
  try {
    if (reviewAction.value === 'approve') {
      await approveBugReport(reportId.value, { reviewComment })
      notifySuccess('审核通过成功')
    } else {
      await rejectBugReport(reportId.value, { reviewComment })
      notifySuccess('审核驳回成功')
    }
    reviewDialogVisible.value = false
    await loadDetail()
  } finally {
    submitLoading.value = false
  }
}

async function handleVoid(): Promise<void> {
  if (!detail.value?.id || !detail.value.reportNo) {
    return
  }
  await confirmAction(`确认作废简报【${detail.value.reportNo}】吗？`)
  submitLoading.value = true
  try {
    await voidBugReport(detail.value.id)
    notifySuccess('简报已作废')
    await loadDetail()
  } finally {
    submitLoading.value = false
  }
}

type DetailMobileActionCommand = 'edit' | 'submit' | 'approve' | 'reject' | 'void'

function handleMobileActionCommand(command: DetailMobileActionCommand): void {
  if (command === 'edit') {
    handleEdit()
    return
  }
  if (command === 'submit') {
    openSubmitDialog()
    return
  }
  if (command === 'approve') {
    openReviewDialog('approve')
    return
  }
  if (command === 'reject') {
    openReviewDialog('reject')
    return
  }
  void handleVoid()
}

function openTicketDetail(ticket: BugReportRelatedTicketOutput): void {
  if (!ticket.ticketId) {
    return
  }
  router.push(`/ticket/detail/${ticket.ticketId}`)
}

function openAttachment(attachment: BugReportAttachmentOutput): void {
  if (!attachment.filePath) {
    notifyWarning('该附件暂无可用下载地址')
    return
  }
  window.open(attachment.filePath, '_blank')
}

function formatDateDisplay(value?: string | null): string {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

function getSeverityLabel(level?: string): string {
  const map: Record<string, string> = {
    P0: 'P0（致命）',
    P1: 'P1（重大）',
    P2: 'P2（严重）',
    P3: 'P3（一般）',
    P4: 'P4（轻微）',
  }
  return level ? (map[level] || level) : '-'
}

function getSeverityTagType(level?: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    P0: 'danger',
    P1: 'warning',
    P2: '',
    P3: 'success',
    P4: 'info',
  }
  return level ? (map[level] ?? 'info') : 'info'
}

function getTicketSourceLabel(isAutoCreated?: number): string {
  return isAutoCreated === 1 ? '自动关联' : '手动关联'
}

function buildCopyText(): string {
  const d = detail.value
  if (!d) return ''
  const responsibleNames = (d.responsibleUsers || []).map((u) => u.userName || String(u.userId)).join('、') || '-'
  const lines: string[] = [
    `问题描述：${d.problemDesc || '-'}`,
    `逻辑归因：${[d.logicCauseLevel1, d.logicCauseLevel2].filter(Boolean).join(' / ') || '-'}`,
    `缺陷分类：${d.defectCategory || '-'}`,
    `引入项目：${d.introducedProject || '-'}`,
    `开始时间：${formatDateDisplay(d.startDate)}`,
  ]
  if (bugReportResolutionDisplayMode.value === 'complete') {
    lines.push(`解决时间：${d.resolveTime ? formatDateTime(d.resolveTime) : '-'}`)
  } else if (bugReportResolutionDisplayMode.value === 'temp') {
    lines.push(`临时解决时间：${formatDateDisplay(d.tempResolveDate)}`)
    lines.push(`临时解决方案：${d.tempSolution || '-'}`)
    lines.push(`彻底解决日期：${formatDateDisplay(d.resolveDate)}`)
    lines.push(`彻底解决方案：${d.solution || '-'}`)
  } else {
    lines.push(`临时解决时间：${formatDateDisplay(d.tempResolveDate)}`)
    lines.push(`临时解决方案：${d.tempSolution || '-'}`)
    lines.push(`彻底解决时间：${formatDateDisplay(d.resolveDate)}`)
    lines.push(`彻底解决方案：${d.solution || '-'}`)
  }
  lines.push(
    `影响范围：${d.impactScope || '-'}`,
    `缺陷等级：${d.severityLevel || '-'}`,
    `反馈人：${d.reporterName || '-'}`,
    `审核人：${d.reviewerName || '-'}`,
    `责任人：${responsibleNames}`,
  )
  return lines.join('\n')
}

function handleCopyReport(): void {
  const text = buildCopyText()
  if (!text) return
  navigator.clipboard.writeText(text).then(
    () => notifySuccess('Bug简报已复制到剪贴板'),
    () => notifyWarning('复制失败，请手动复制'),
  )
}

onMounted(async () => {
  updateViewportState()
  window.addEventListener('resize', updateViewportState)
  await Promise.all([loadUsers(), loadDetail()])
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewportState)
})

watch(
  () => reportId.value,
  async (value, oldValue) => {
    if (!value || value === oldValue) {
      return
    }
    await loadDetail()
  },
)
</script>

<template>
  <div class="bug-report-detail-page">
    <el-space direction="vertical" fill :size="16">
      <el-card shadow="never" v-loading="loading">
        <template #header>
          <div class="header">
            <div class="subtitle">
              <el-tag :type="getBugReportStatusTagType(detail?.status)">
                {{ getBugReportStatusLabel(detail?.status, detail?.statusLabel) }}
              </el-tag>
              <span>简报编号：{{ detail?.reportNo || '-' }}</span>
              <span>创建时间：{{ formatDateTime(detail?.createTime) }}</span>
              <span>更新时间：{{ formatDateTime(detail?.updateTime) }}</span>
            </div>
            <el-space class="header-actions" wrap>
              <el-button @click="handleBack">返回列表</el-button>
              <el-button type="default" @click="handleCopyReport">一键复制</el-button>
              <template v-if="isMobile">
                <el-dropdown
                  trigger="click"
                  @command="(command: DetailMobileActionCommand) => handleMobileActionCommand(command)"
                >
                  <el-button type="primary" plain class="mobile-more-trigger">更多操作</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-if="canEdit" command="edit">编辑</el-dropdown-item>
                      <el-dropdown-item v-if="canSubmit" command="submit">提交审核</el-dropdown-item>
                      <el-dropdown-item v-if="canReview" command="approve">审核通过</el-dropdown-item>
                      <el-dropdown-item v-if="canReview" command="reject">审核驳回</el-dropdown-item>
                      <el-dropdown-item v-if="canVoid" command="void">作废</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
              <template v-else>
                <el-button v-if="canEdit" type="primary" plain @click="handleEdit">编辑</el-button>
                <el-button v-if="canSubmit" type="primary" @click="openSubmitDialog">提交审核</el-button>
                <el-button v-if="canReview" type="success" @click="openReviewDialog('approve')">
                  审核通过
                </el-button>
                <el-button v-if="canReview" type="warning" @click="openReviewDialog('reject')">
                  审核驳回
                </el-button>
                <el-button v-if="canVoid" type="danger" plain @click="handleVoid">作废</el-button>
              </template>
            </el-space>
          </div>
        </template>

        <div v-if="isMobile" class="mobile-detail-panel">
          <div class="mobile-detail-row">
            <span class="mobile-detail-label">状态</span>
            <span class="mobile-detail-value">
              <el-tag :type="getBugReportStatusTagType(detail?.status)">
                {{ getBugReportStatusLabel(detail?.status, detail?.statusLabel) }}
              </el-tag>
            </span>
          </div>
          <div class="mobile-detail-row">
            <span class="mobile-detail-label">反馈人</span>
            <span class="mobile-detail-value">{{ detail?.reporterName || '-' }}</span>
          </div>
          <div class="mobile-detail-row">
            <span class="mobile-detail-label">审核人</span>
            <span class="mobile-detail-value">{{ detail?.reviewerName || '-' }}</span>
          </div>
          <div class="mobile-detail-row">
            <span class="mobile-detail-label">缺陷分类</span>
            <span class="mobile-detail-value">{{ detail?.defectCategory || '-' }}</span>
          </div>
          <div class="mobile-detail-row">
            <span class="mobile-detail-label">严重级别</span>
            <span class="mobile-detail-value">
              <el-tag
                v-if="detail?.severityLevel"
                :type="getSeverityTagType(detail.severityLevel)"
                size="small"
              >
                {{ getSeverityLabel(detail.severityLevel) }}
              </el-tag>
              <span v-else>-</span>
            </span>
          </div>
          <div class="mobile-detail-row">
            <span class="mobile-detail-label">引入项目</span>
            <span class="mobile-detail-value">{{ detail?.introducedProject || '-' }}</span>
          </div>
          <div class="mobile-detail-row">
            <span class="mobile-detail-label">影响范围</span>
            <span class="mobile-detail-value">{{ detail?.impactScope || '-' }}</span>
          </div>
          <div class="mobile-detail-row">
            <span class="mobile-detail-label">开始时间</span>
            <span class="mobile-detail-value">{{ formatDateDisplay(detail?.startDate) }}</span>
          </div>
          <div v-if="bugReportResolutionDisplayMode === 'complete'" class="mobile-detail-row">
            <span class="mobile-detail-label">解决时间</span>
            <span class="mobile-detail-value">{{ detail?.resolveTime ? formatDateTime(detail.resolveTime) : '-' }}</span>
          </div>
          <template v-else-if="bugReportResolutionDisplayMode === 'temp'">
            <div class="mobile-detail-row">
              <span class="mobile-detail-label">临时解决时间</span>
              <span class="mobile-detail-value">{{ formatDateDisplay(detail?.tempResolveDate) }}</span>
            </div>
            <div class="mobile-detail-row">
              <span class="mobile-detail-label">彻底解决日期</span>
              <span class="mobile-detail-value">{{ formatDateDisplay(detail?.resolveDate) }}</span>
            </div>
          </template>
          <template v-else>
            <div class="mobile-detail-row">
              <span class="mobile-detail-label">临时解决时间</span>
              <span class="mobile-detail-value">{{ formatDateDisplay(detail?.tempResolveDate) }}</span>
            </div>
            <div class="mobile-detail-row">
              <span class="mobile-detail-label">彻底解决时间</span>
              <span class="mobile-detail-value">{{ formatDateDisplay(detail?.resolveDate) }}</span>
            </div>
          </template>
          <div class="mobile-detail-row">
            <span class="mobile-detail-label">提交时间</span>
            <span class="mobile-detail-value">{{ formatDateTime(detail?.submittedAt) }}</span>
          </div>
          <div class="mobile-detail-row">
            <span class="mobile-detail-label">审核完成时间</span>
            <span class="mobile-detail-value">{{ formatDateTime(detail?.reviewedAt) }}</span>
          </div>
          <div class="mobile-detail-row">
            <span class="mobile-detail-label">逻辑归因</span>
            <span class="mobile-detail-value">
              {{ detail?.logicCauseLevel1 || '-' }} / {{ detail?.logicCauseLevel2 || '-' }}
            </span>
          </div>
          <div class="mobile-detail-row">
            <span class="mobile-detail-label">归因明细</span>
            <span class="mobile-detail-value pre-wrap">{{ detail?.logicCauseDetail || '-' }}</span>
          </div>
          <div class="mobile-detail-row">
            <span class="mobile-detail-label">审核意见</span>
            <span class="mobile-detail-value pre-wrap">{{ detail?.reviewComment || '-' }}</span>
          </div>
          <div class="mobile-detail-block">
            <div class="mobile-detail-block-title">问题描述</div>
            <div class="pre-wrap">{{ detail?.problemDesc || '-' }}</div>
          </div>
          <template v-if="bugReportResolutionDisplayMode === 'temp'">
            <div v-if="detail?.tempSolution" class="mobile-detail-block">
              <div class="mobile-detail-block-title">临时解决方案</div>
              <div class="pre-wrap">{{ detail.tempSolution }}</div>
            </div>
            <div class="mobile-detail-block">
              <div class="mobile-detail-block-title">彻底解决方案</div>
              <div class="pre-wrap">{{ detail?.solution || '-' }}</div>
            </div>
          </template>
          <div v-else-if="bugReportResolutionDisplayMode === 'complete'" class="mobile-detail-block">
            <div class="mobile-detail-block-title">说明</div>
            <div class="pre-wrap">本简报按「处理完成」归档：仅记录解决时间，不展示临时/彻底方案栏位。</div>
          </div>
          <template v-else>
            <div v-if="detail?.tempSolution" class="mobile-detail-block">
              <div class="mobile-detail-block-title">临时解决方案</div>
              <div class="pre-wrap">{{ detail.tempSolution }}</div>
            </div>
            <div class="mobile-detail-block">
              <div class="mobile-detail-block-title">彻底解决方案</div>
              <div class="pre-wrap">{{ detail?.solution || '-' }}</div>
            </div>
          </template>
          <div class="mobile-detail-block">
            <div class="mobile-detail-block-title">备注</div>
            <div class="pre-wrap">{{ detail?.remark || '-' }}</div>
          </div>
        </div>

        <el-descriptions v-else :column="2" border class="detail-descriptions">
          <el-descriptions-item label="简报编号">{{ detail?.reportNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            {{ getBugReportStatusLabel(detail?.status, detail?.statusLabel) }}
          </el-descriptions-item>
          <el-descriptions-item label="反馈人">{{ detail?.reporterName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核人">{{ detail?.reviewerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="缺陷分类">
            {{ detail?.defectCategory || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="严重级别">
            <el-tag
              v-if="detail?.severityLevel"
              :type="getSeverityTagType(detail.severityLevel)"
              size="small"
            >
              {{ getSeverityLabel(detail.severityLevel) }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="引入项目">
            {{ detail?.introducedProject || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="影响范围">
            {{ detail?.impactScope || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">
            {{ formatDateDisplay(detail?.startDate) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="bugReportResolutionDisplayMode === 'complete'" label="解决时间">
            {{ detail?.resolveTime ? formatDateTime(detail.resolveTime) : '-' }}
          </el-descriptions-item>
          <template v-else-if="bugReportResolutionDisplayMode === 'temp'">
            <el-descriptions-item label="临时解决时间">
              {{ formatDateDisplay(detail?.tempResolveDate) }}
            </el-descriptions-item>
            <el-descriptions-item label="彻底解决日期">
              {{ formatDateDisplay(detail?.resolveDate) }}
            </el-descriptions-item>
          </template>
          <template v-else>
            <el-descriptions-item label="临时解决时间">
              {{ formatDateDisplay(detail?.tempResolveDate) }}
            </el-descriptions-item>
            <el-descriptions-item label="彻底解决时间">
              {{ formatDateDisplay(detail?.resolveDate) }}
            </el-descriptions-item>
          </template>
          <el-descriptions-item label="提交时间">
            {{ formatDateTime(detail?.submittedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="逻辑归因">
            {{ detail?.logicCauseLevel1 || '-' }} / {{ detail?.logicCauseLevel2 || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="归因明细">
            {{ detail?.logicCauseDetail || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="审核完成时间">
            {{ formatDateTime(detail?.reviewedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="审核意见">
            {{ detail?.reviewComment || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2">
            <span class="pre-wrap">{{ detail?.problemDesc || '-' }}</span>
          </el-descriptions-item>
          <template v-if="bugReportResolutionDisplayMode === 'temp'">
            <el-descriptions-item v-if="detail?.tempSolution" label="临时解决方案" :span="2">
              <span class="pre-wrap">{{ detail.tempSolution }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="彻底解决方案" :span="2">
              <span class="pre-wrap">{{ detail?.solution || '-' }}</span>
            </el-descriptions-item>
          </template>
          <el-descriptions-item
            v-else-if="bugReportResolutionDisplayMode === 'complete'"
            label="说明"
            :span="2"
          >
            本简报按「处理完成」归档：仅记录解决时间，不展示临时/彻底方案栏位。
          </el-descriptions-item>
          <template v-else>
            <el-descriptions-item v-if="detail?.tempSolution" label="临时解决方案" :span="2">
              <span class="pre-wrap">{{ detail.tempSolution }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="彻底解决方案" :span="2">
              <span class="pre-wrap">{{ detail?.solution || '-' }}</span>
            </el-descriptions-item>
          </template>
          <el-descriptions-item label="备注" :span="2">{{ detail?.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="section-title">责任人</div>
        </template>
        <EmptyState v-if="!detail?.responsibleUsers?.length" description="暂无责任人信息" />
        <el-space v-else wrap>
          <el-tag v-for="item in detail.responsibleUsers" :key="item.userId">
            {{ item.userName || item.userId }}
          </el-tag>
        </el-space>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="section-title">关联工单</div>
        </template>
        <EmptyState v-if="!detail?.tickets?.length" description="暂无关联工单" />
        <div v-else-if="isMobile" class="mobile-block-list">
          <div v-for="row in detail.tickets" :key="row.ticketId" class="mobile-block-item">
            <div class="mobile-block-head">
              <div class="mobile-block-title">{{ row.ticketNo || '-' }}</div>
              <el-tag size="small">{{ getTicketStatusLabel(row.status) }}</el-tag>
            </div>
            <div class="mobile-block-desc">{{ row.title || '-' }}</div>
            <div class="mobile-block-meta">来源：{{ getTicketSourceLabel(row.isAutoCreated) }}</div>
            <el-button type="primary" link class="mobile-block-action" @click="openTicketDetail(row)">
              查看工单
            </el-button>
          </div>
        </div>
        <el-table
          v-else
          :data="detail.tickets"
          :border="false"
          :stripe="true"
          :header-cell-style="{ backgroundColor: '#f5f7fa' }"
        >
          <el-table-column prop="ticketNo" label="工单编号" min-width="180" align="center" />
          <el-table-column prop="title" label="工单标题" min-width="260" align="center" />
          <el-table-column label="工单状态" width="130" align="center">
            <template #default="{ row }">
              {{ getTicketStatusLabel(row.status) }}
            </template>
          </el-table-column>
          <el-table-column label="来源" width="120" align="center">
            <template #default="{ row }">
              {{ getTicketSourceLabel(row.isAutoCreated) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openTicketDetail(row)">查看工单</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="section-title">附件</div>
        </template>
        <EmptyState v-if="!detail?.attachments?.length" description="暂无附件" />
        <div v-else-if="isMobile" class="mobile-block-list">
          <div v-for="row in detail.attachments" :key="row.id" class="mobile-block-item">
            <div class="mobile-block-title">{{ row.fileName || '-' }}</div>
            <div class="mobile-block-meta">大小：{{ formatFileSize(row.fileSize) }}</div>
            <div class="mobile-block-meta">上传人：{{ row.uploadedByName || '-' }}</div>
            <div class="mobile-block-meta">上传时间：{{ formatDateTime(row.createTime) }}</div>
            <el-button type="primary" link class="mobile-block-action" @click="openAttachment(row)">
              下载/预览
            </el-button>
          </div>
        </div>
        <el-table
          v-else
          :data="detail.attachments"
          :border="false"
          :stripe="true"
          :header-cell-style="{ backgroundColor: '#f5f7fa' }"
        >
          <el-table-column prop="fileName" label="文件名" min-width="240" align="center" />
          <el-table-column label="大小" width="120" align="center">
            <template #default="{ row }">
              {{ formatFileSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="uploadedByName" label="上传人" width="140" align="center" />
          <el-table-column label="上传时间" width="180" align="center">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openAttachment(row)">下载/预览</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="section-title">状态日志</div>
        </template>
        <EmptyState v-if="!detail?.logs?.length" description="暂无日志记录" />
        <div v-else-if="isMobile" class="mobile-block-list">
          <div v-for="item in detail.logs" :key="item.id" class="mobile-block-item">
            <div class="log-title">
              {{ getReviewActionLabel(item.action) }}
              <span class="log-user">（{{ item.userName || '-' }}）</span>
            </div>
            <div class="mobile-block-meta">时间：{{ formatDateTime(item.createTime) }}</div>
            <div class="log-content">
              状态：{{ getBugReportStatusLabel(item.oldStatus) }} →
              {{ getBugReportStatusLabel(item.newStatus) }}
            </div>
            <div class="log-content" v-if="item.remark">备注：{{ item.remark }}</div>
          </div>
        </div>
        <el-timeline v-else>
          <el-timeline-item
            v-for="item in detail.logs"
            :key="item.id"
            :timestamp="formatDateTime(item.createTime)"
          >
            <div class="log-item">
              <div class="log-title">
                {{ getReviewActionLabel(item.action) }}
                <span class="log-user">（{{ item.userName || '-' }}）</span>
              </div>
              <div class="log-content">
                状态：{{ getBugReportStatusLabel(item.oldStatus) }} →
                {{ getBugReportStatusLabel(item.newStatus) }}
              </div>
              <div class="log-content" v-if="item.remark">备注：{{ item.remark }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </el-space>
  </div>

  <el-dialog
    v-model="submitDialogVisible"
    :title="isHighSeverityDetail ? '提交审核' : '提交归档'"
    :width="isMobile ? '92vw' : '520px'"
  >
    <el-form :label-width="isMobile ? 'auto' : '90px'">
      <el-form-item :label="isHighSeverityDetail ? '审核人' : '审核人（可选）'" :required="isHighSeverityDetail">
        <el-select v-model="submitForm.reviewerId" placeholder="请选择审核人" filterable clearable>
          <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
        </el-select>
        <div v-if="!isHighSeverityDetail" style="font-size: 12px; color: #909399; margin-top: 4px;">
          P3/P4级别简报提交后直接归档，无需审核
        </div>
      </el-form-item>
      <el-form-item label="备注">
        <el-input
          v-model="submitForm.remark"
          type="textarea"
          :rows="3"
          placeholder="可选：补充提交说明"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmitReport">确认提交</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="reviewDialogVisible"
    :title="reviewAction === 'approve' ? '审核通过' : '审核驳回'"
    :width="isMobile ? '92vw' : '520px'"
  >
    <el-form :label-width="isMobile ? 'auto' : '90px'">
      <el-form-item label="审核意见" required>
        <el-input
          v-model="reviewForm.reviewComment"
          type="textarea"
          :rows="4"
          :placeholder="reviewAction === 'approve' ? '请输入通过意见' : '请输入驳回原因'"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reviewDialogVisible = false">取消</el-button>
      <el-button
        :type="reviewAction === 'approve' ? 'success' : 'warning'"
        :loading="submitLoading"
        @click="handleReview"
      >
        确认{{ reviewAction === 'approve' ? '通过' : '驳回' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.bug-report-detail-page {
  width: 100%;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.subtitle {
  display: flex;
  gap: 12px;
  color: #606266;
  align-items: center;
  flex-wrap: wrap;
}

.header-actions {
  width: auto;
  flex: 0 0 auto;
}

.section-title {
  font-size: 17px;
  font-weight: 600;
  color: #1d2129;
}

.detail-descriptions :deep(.el-descriptions__label) {
  width: 112px;
}

.mobile-detail-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mobile-detail-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f1f2f5;
}

.mobile-detail-label {
  color: #86909c;
  font-size: 13px;
  min-width: 92px;
}

.mobile-detail-value {
  flex: 1;
  color: #1d2129;
  text-align: right;
  word-break: break-word;
}

.mobile-detail-block {
  background: #f8fafc;
  border-radius: 8px;
  padding: 10px 12px;
}

.mobile-detail-block-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.mobile-block-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mobile-block-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fff;
}

.mobile-block-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.mobile-block-title {
  font-weight: 600;
  color: #1d2129;
  word-break: break-word;
}

.mobile-block-desc {
  margin-top: 8px;
  color: #303133;
  word-break: break-word;
}

.mobile-block-meta {
  margin-top: 6px;
  color: #606266;
  font-size: 13px;
  word-break: break-word;
}

.mobile-block-action {
  margin-top: 8px;
  min-height: 44px;
}

.log-item {
  background: #f8fafc;
  border-radius: 6px;
  padding: 8px 12px;
}

.log-title {
  font-weight: 600;
}

.log-user {
  font-weight: 400;
  color: #606266;
}

.log-content {
  margin-top: 4px;
  color: #606266;
}

.pre-wrap {
  white-space: pre-wrap;
  word-break: break-word;
}

.mobile-more-trigger {
  min-height: 44px;
}

@media (max-width: 991px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .subtitle {
    gap: 8px;
  }

  .header-actions :deep(.el-button) {
    min-height: 44px;
  }
}
</style>
