<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
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

const loading = ref(false)
const submitLoading = ref(false)
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
  if (!reviewerId) {
    notifyWarning('提交审核前请先选择审核人')
    return
  }
  submitLoading.value = true
  try {
    await submitBugReport(reportId.value, {
      reviewerId,
      remark: submitForm.remark.trim() || undefined,
    })
    notifySuccess('提交审核成功')
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
    `临时解决时间：${formatDateDisplay(d.tempResolveDate)}`,
    `临时解决方案：${d.tempSolution || '-'}`,
    `彻底解决时间：${formatDateDisplay(d.resolveDate)}`,
    `彻底解决方案：${d.solution || '-'}`,
    `影响范围：${d.impactScope || '-'}`,
    `缺陷等级：${d.severityLevel || '-'}`,
    `反馈人：${d.reporterName || '-'}`,
    `审核人：${d.reviewerName || '-'}`,
    `责任人：${responsibleNames}`,
  ]
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
  await Promise.all([loadUsers(), loadDetail()])
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
  <el-space direction="vertical" fill :size="16">
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="header">
          <div>
            <div class="title">Bug简报：{{ detail?.reportNo || '-' }}</div>
            <div class="subtitle">
              <el-tag :type="getBugReportStatusTagType(detail?.status)">
                {{ getBugReportStatusLabel(detail?.status, detail?.statusLabel) }}
              </el-tag>
              <span>创建时间：{{ formatDateTime(detail?.createTime) }}</span>
              <span>更新时间：{{ formatDateTime(detail?.updateTime) }}</span>
            </div>
          </div>
          <el-space>
            <el-button @click="handleBack">返回列表</el-button>
            <el-button type="default" @click="handleCopyReport">一键复制</el-button>
            <el-button v-if="canEdit" type="primary" plain @click="handleEdit">编辑</el-button>
            <el-button v-if="canSubmit" type="primary" @click="openSubmitDialog">提交审核</el-button>
            <el-button v-if="canReview" type="success" @click="openReviewDialog('approve')"
              >审核通过</el-button
            >
            <el-button v-if="canReview" type="warning" @click="openReviewDialog('reject')"
              >审核驳回</el-button
            >
            <el-button v-if="canVoid" type="danger" plain @click="handleVoid">作废</el-button>
          </el-space>
        </div>
      </template>

      <el-descriptions :column="2" border>
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
        <el-descriptions-item label="临时解决时间">
          {{ formatDateDisplay(detail?.tempResolveDate) }}
        </el-descriptions-item>
        <el-descriptions-item label="彻底解决时间">
          {{ formatDateDisplay(detail?.resolveDate) }}
        </el-descriptions-item>
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
        <el-descriptions-item v-if="detail?.tempSolution" label="临时解决方案" :span="2">
          <span class="pre-wrap">{{ detail.tempSolution }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="彻底解决方案" :span="2">
          <span class="pre-wrap">{{ detail?.solution || '-' }}</span>
        </el-descriptions-item>
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
      <el-table
        v-else
        :data="detail.tickets"
        :border="false"
        :stripe="true"
        :header-cell-style="{ backgroundColor: '#f5f7fa' }"
      >
        <el-table-column prop="ticketNo" label="工单编号" min-width="180" align="center" />
        <el-table-column prop="title" label="工单标题" min-width="260" align="center" />
        <el-table-column prop="status" label="工单状态" width="130" align="center" />
        <el-table-column label="来源" width="120" align="center">
          <template #default="{ row }">
            {{ row.isAutoCreated === 1 ? '自动关联' : '手动关联' }}
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

  <el-dialog v-model="submitDialogVisible" title="提交审核" width="520px">
    <el-form label-width="90px">
      <el-form-item label="审核人" required>
        <el-select v-model="submitForm.reviewerId" placeholder="请选择审核人" filterable clearable>
          <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
        </el-select>
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
      <el-button type="primary" :loading="submitLoading" @click="handleSubmitReport"
        >确认提交</el-button
      >
    </template>
  </el-dialog>

  <el-dialog
    v-model="reviewDialogVisible"
    :title="reviewAction === 'approve' ? '审核通过' : '审核驳回'"
    width="520px"
  >
    <el-form label-width="90px">
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
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.subtitle {
  margin-top: 8px;
  display: flex;
  gap: 12px;
  color: #606266;
  align-items: center;
  flex-wrap: wrap;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
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

@media (max-width: 991px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
