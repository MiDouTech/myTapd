<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  ArrowRight,
  Bell,
  BellFilled,
  ChatDotSquare,
  Document as DocumentOutlined,
  Plus,
} from '@element-plus/icons-vue'
import {
  addTicketComment,
  assignTicket,
  closeTicket,
  createTicketModule,
  deleteTicketAttachment,
  followTicket,
  getTicketDetail,
  getTicketModuleList,
  getTicketNodeDuration,
  getTicketTimeTrack,
  parseWecomCustomerInfo,
  trackTicketRead,
  unfollowTicket,
  updateBugCustomerInfo,
  updateBugDevInfo,
  updateBugTestInfo,
  uploadTicketImage,
} from '@/api/ticket'
import {
  getAvailableActions,
  transitTicket,
  transferTicket,
  getFlowHistory,
} from '@/api/workflow'
import { getUserList } from '@/api/user'
import EmptyState from '@/components/common/EmptyState.vue'
import RichTextEditor from '@/components/common/RichTextEditor.vue'
import { useAuthStore } from '@/stores/auth'
import type {
  BugChangeHistoryOutput,
  TicketBugCustomerInfoInput,
  TicketBugDevInfoInput,
  TicketBugTestInfoInput,
  TicketDetailOutput,
  TicketModuleOutput,
  TicketNodeDurationItem,
  TicketTimeTrackItem,
  WecomMessageParseOutput,
} from '@/types/ticket'
import type {
  AvailableActionOutput,
  TicketActionItem,
  TicketFlowRecordOutput,
} from '@/types/workflow'
import type { UserListOutput } from '@/types/user'
import { notifySuccess, notifyError } from '@/utils/feedback'
import { formatDateTime, formatDurationSec, formatFileSize } from '@/utils/formatter'

import BugChangeHistory from './components/bug/BugChangeHistory.vue'
import BugDetailInfoPanel from './components/bug/BugDetailInfoPanel.vue'
import BugStatusBadge from './components/bug/BugStatusBadge.vue'
import TicketTimeTrackPanel from './components/TicketTimeTrackPanel.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const detail = ref<TicketDetailOutput>()
const users = ref<UserListOutput[]>([])

const activeBugTab = ref('customer')
const timeTrackItems = ref<TicketTimeTrackItem[]>([])
const timeTrackStandalone = ref<BugChangeHistoryOutput[]>([])
const nodeDurationItems = ref<TicketNodeDurationItem[]>([])
const bugSubmitLoading = ref(false)

const activeMainTab = ref('detail')
const changeHistoryCount = ref(0)

const assignDialogVisible = ref(false)
const pendingPoolTransferVisible = ref(false)
const processDialogVisible = ref(false)
const closeDialogVisible = ref(false)
const submitLoading = ref(false)

// ---- 动态工作流操作 ----
const availableActions = ref<AvailableActionOutput | null>(null)
const selectedAction = ref<TicketActionItem | null>(null)
const flowHistory = ref<TicketFlowRecordOutput[]>([])
const flowHistoryLoading = ref(false)

const transitForm = reactive({
  transitionId: '',
  targetStatus: '',
  remark: '',
  newAssigneeId: undefined as number | undefined,
  newAssigneeIds: [] as number[],
})

const imageUploadLoading = ref(false)
const imageUploadRef = ref()
const customerScreenshotUploadLoading = ref(false)
const customerScreenshotUploadRef = ref()
const customerProblemScreenshots = ref<string[]>([])

const ticketModules = ref<TicketModuleOutput[]>([])

const assignForm = reactive({
  assigneeIds: [] as number[],
  remark: '',
})

const pendingPoolTransferForm = reactive({
  targetUserId: undefined as number | undefined,
  reason: '',
})

const closeForm = reactive({
  remark: '',
})

// ---- 评论输入 ----
const commentInput = ref('')
const commentSubmitLoading = ref(false)

function uniqStringList(values: string[]): string[] {
  return Array.from(new Set(values.filter((item) => Boolean(item && item.trim()))))
}

function parseScreenshotList(raw?: string): string[] {
  if (!raw) {
    return []
  }
  return uniqStringList(
    raw
      .split(/[,，;\n]/)
      .map((item) => item.trim())
      .filter(Boolean),
  )
}

function mergeProblemScreenshots(urls: string[]): void {
  customerProblemScreenshots.value = uniqStringList([
    ...customerProblemScreenshots.value,
    ...urls.map((item) => item.trim()),
  ])
}

function removeProblemScreenshot(url: string): void {
  customerProblemScreenshots.value = customerProblemScreenshots.value.filter((item) => item !== url)
}

// ---- 企微消息解析 ----
const wecomParseDialogVisible = ref(false)
const wecomParseMessage = ref('')
const wecomParseLoading = ref(false)
const wecomParseResult = ref<WecomMessageParseOutput | null>(null)

async function handleWecomParse() {
  if (!wecomParseMessage.value.trim()) {
    notifyError('请粘贴企微消息内容')
    return
  }
  wecomParseLoading.value = true
  wecomParseResult.value = null
  try {
    const result = await parseWecomCustomerInfo({ message: wecomParseMessage.value })
    wecomParseResult.value = result
    if (!result.matchedFields || result.matchedFields.length === 0) {
      notifyError('未能从消息中识别出有效字段，请检查消息格式')
    }
  } catch {
    notifyError('解析失败，请稍后重试')
  } finally {
    wecomParseLoading.value = false
  }
}

function applyWecomParseResult() {
  const result = wecomParseResult.value
  if (!result) {
    return
  }
  if (result.merchantNo) {
    customerInfoForm.merchantNo = result.merchantNo
  }
  if (result.companyName) {
    customerInfoForm.companyName = result.companyName
  }
  if (result.merchantAccount) {
    customerInfoForm.merchantAccount = result.merchantAccount
  }
  if (result.sceneCode) {
    customerInfoForm.sceneCode = result.sceneCode
  }
  if (result.problemDesc) {
    customerInfoForm.problemDesc = result.problemDesc
  }
  if (result.expectedResult) {
    customerInfoForm.expectedResult = result.expectedResult
  }
  if (result.problemScreenshot) {
    mergeProblemScreenshots(parseScreenshotList(result.problemScreenshot))
  }
  wecomParseDialogVisible.value = false
  wecomParseMessage.value = ''
  wecomParseResult.value = null
  notifySuccess('已将解析结果填入表单，请确认后保存')
}

function openWecomParseDialog() {
  wecomParseMessage.value = ''
  wecomParseResult.value = null
  wecomParseDialogVisible.value = true
}

const customerInfoForm = reactive<TicketBugCustomerInfoInput>({
  merchantNo: '',
  companyName: '',
  merchantAccount: '',
  problemDesc: '',
  expectedResult: '',
  sceneCode: '',
  problemScreenshot: '',
})

const testInfoForm = reactive<TicketBugTestInfoInput>({
  reproduceEnv: '',
  reproduceSteps: '',
  actualResult: '',
  impactScope: '',
  severityLevel: '',
  moduleName: '',
  testRemark: '',
})

const devInfoForm = reactive<TicketBugDevInfoInput>({
  rootCause: '',
  fixSolution: '',
  gitBranch: '',
  impactAssessment: '',
  devRemark: '',
})

const ticketId = computed(() => Number(route.params.id))
const roleCodes = computed(() =>
  (authStore.userInfo?.roleCodes ?? []).map((item) => String(item).toUpperCase()),
)
const currentUserId = computed(() => authStore.userInfo?.id)
const currentStatus = computed(() => normalizeStatus(detail.value?.status))

const customFieldEntries = computed(() => {
  if (!detail.value?.customFields) {
    return []
  }
  return Object.entries(detail.value.customFields).map(([key, value]) => ({ key, value }))
})

const canEditCustomerInfo = computed(() => true)

const canEditTestInfo = computed(() => true)

const canEditDevInfo = computed(() => true)

function hasRole(...targets: string[]): boolean {
  return targets.some((target) => roleCodes.value.includes(target))
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

function fillBugForms(ticketDetail: TicketDetailOutput): void {
  Object.assign(customerInfoForm, {
    merchantNo: ticketDetail.bugCustomerInfo?.merchantNo || '',
    companyName: ticketDetail.bugCustomerInfo?.companyName || '',
    merchantAccount: ticketDetail.bugCustomerInfo?.merchantAccount || '',
    problemDesc: ticketDetail.bugCustomerInfo?.problemDesc || '',
    expectedResult: ticketDetail.bugCustomerInfo?.expectedResult || '',
    sceneCode: ticketDetail.bugCustomerInfo?.sceneCode || '',
    problemScreenshot: ticketDetail.bugCustomerInfo?.problemScreenshot || '',
  })
  customerProblemScreenshots.value = parseScreenshotList(ticketDetail.bugCustomerInfo?.problemScreenshot)

  // Automatically include WeChat bot images — no manual selection needed
  const wecomImageUrls = (ticketDetail.attachments || [])
    .filter((a) => a.source === 'WECOM_BOT' && Boolean(a.filePath) && isImageFile(a.fileType))
    .map((a) => a.filePath as string)
  mergeProblemScreenshots(wecomImageUrls)

  Object.assign(testInfoForm, {
    reproduceEnv: ticketDetail.bugTestInfo?.reproduceEnv || '',
    reproduceSteps: ticketDetail.bugTestInfo?.reproduceSteps || '',
    actualResult: ticketDetail.bugTestInfo?.actualResult || '',
    impactScope: ticketDetail.bugTestInfo?.impactScope || '',
    severityLevel: ticketDetail.bugTestInfo?.severityLevel || '',
    moduleName: ticketDetail.bugTestInfo?.moduleName || '',
    testRemark: ticketDetail.bugTestInfo?.testRemark || '',
  })

  Object.assign(devInfoForm, {
    rootCause: ticketDetail.bugDevInfo?.rootCause || '',
    fixSolution: ticketDetail.bugDevInfo?.fixSolution || '',
    gitBranch: ticketDetail.bugDevInfo?.gitBranch || '',
    impactAssessment: ticketDetail.bugDevInfo?.impactAssessment || '',
    devRemark: ticketDetail.bugDevInfo?.devRemark || '',
  })
}

watch(
  customerProblemScreenshots,
  (screenshots) => {
    customerInfoForm.problemScreenshot = uniqStringList(screenshots).join(',')
  },
  { deep: true },
)

async function loadModules(): Promise<void> {
  try {
    ticketModules.value = await getTicketModuleList()
  } catch {
    ticketModules.value = []
  }
}

async function loadAll(): Promise<void> {
  if (!ticketId.value) {
    return
  }
  loading.value = true
  try {
    const [ticketDetail, userList, trackOutput, nodeOutput] = await Promise.all([
      getTicketDetail(ticketId.value),
      getUserList({}),
      getTicketTimeTrack(ticketId.value),
      getTicketNodeDuration(ticketId.value),
    ])
    detail.value = ticketDetail
    users.value = userList
    timeTrackItems.value = trackOutput.tracks || []
    timeTrackStandalone.value = trackOutput.standaloneFieldChanges || []
    nodeDurationItems.value = nodeOutput.nodes || []
    fillBugForms(ticketDetail)

    try {
      availableActions.value = await getAvailableActions(ticketId.value)
    } catch {
      availableActions.value = null
    }
  } finally {
    loading.value = false
  }
}

async function loadFlowHistory(): Promise<void> {
  if (!ticketId.value) return
  flowHistoryLoading.value = true
  try {
    flowHistory.value = await getFlowHistory(ticketId.value)
  } catch {
    flowHistory.value = []
  } finally {
    flowHistoryLoading.value = false
  }
}

watch(activeMainTab, (tab) => {
  if (tab === 'flow-history' && flowHistory.value.length === 0) {
    loadFlowHistory()
  }
})

async function trackReadSilently(): Promise<void> {
  if (!ticketId.value) {
    return
  }
  try {
    await trackTicketRead(ticketId.value)
  } catch {
    // 阅读埋点失败不影响详情展示
  }
}

function openAssignDialog(): void {
  assignForm.assigneeIds = []
  assignForm.remark = ''
  assignDialogVisible.value = true
}

async function handleAssign(): Promise<void> {
  if (!assignForm.assigneeIds.length) {
    notifyError('请至少选择一名处理人')
    return
  }
  submitLoading.value = true
  try {
    await assignTicket(ticketId.value, {
      assigneeIds: [...assignForm.assigneeIds],
      remark: assignForm.remark,
    })
    notifySuccess('工单分派成功')
    assignDialogVisible.value = false
    await loadAll()
  } finally {
    submitLoading.value = false
  }
}

async function claimPendingTicket(): Promise<void> {
  const uid = currentUserId.value
  if (!uid) {
    notifyError('请先登录')
    return
  }
  submitLoading.value = true
  try {
    await assignTicket(ticketId.value, {
      assigneeIds: [uid],
      remark: '测试认领',
    })
    notifySuccess('认领成功')
    await loadAll()
  } finally {
    submitLoading.value = false
  }
}

async function handlePendingPoolTransfer(): Promise<void> {
  if (!pendingPoolTransferForm.targetUserId) {
    notifyError('请选择对接人')
    return
  }
  submitLoading.value = true
  try {
    await transferTicket(ticketId.value, {
      targetUserId: pendingPoolTransferForm.targetUserId,
      reason: pendingPoolTransferForm.reason?.trim() || '待分派对接转派',
    })
    notifySuccess('对接人已更新')
    pendingPoolTransferVisible.value = false
    pendingPoolTransferForm.targetUserId = undefined
    pendingPoolTransferForm.reason = ''
    await loadAll()
  } finally {
    submitLoading.value = false
  }
}

function openTransitDialog(action: TicketActionItem): void {
  selectedAction.value = action
  transitForm.transitionId = action.transitionId
  transitForm.targetStatus = action.targetStatus
  transitForm.remark = ''
  transitForm.newAssigneeId = undefined
  transitForm.newAssigneeIds = []
  processDialogVisible.value = true
}

async function handleProcess(): Promise<void> {
  submitLoading.value = true
  try {
    if (!selectedAction.value) {
      notifyError('请选择操作')
      return
    }
    if (selectedAction.value.requireRemark && !transitForm.remark?.trim()) {
      notifyError('该操作需要填写备注')
      return
    }
    const transitPayload: {
      transitionId: string
      targetStatus: string
      remark: string
      newAssigneeId?: number
      newAssigneeIds?: number[]
    } = {
      transitionId: transitForm.transitionId,
      targetStatus: transitForm.targetStatus,
      remark: transitForm.remark,
    }
    if (transitForm.newAssigneeIds.length > 0) {
      transitPayload.newAssigneeIds = [...transitForm.newAssigneeIds]
    } else if (transitForm.newAssigneeId != null) {
      transitPayload.newAssigneeId = transitForm.newAssigneeId
    }
    await transitTicket(ticketId.value, transitPayload)
    notifySuccess('操作成功')
    processDialogVisible.value = false
    selectedAction.value = null
    await loadAll()
  } finally {
    submitLoading.value = false
  }
}

async function handleCloseTicket(): Promise<void> {
  submitLoading.value = true
  try {
    await closeTicket(ticketId.value, closeForm)
    notifySuccess('工单关闭成功')
    closeDialogVisible.value = false
    await loadAll()
  } finally {
    submitLoading.value = false
  }
}

async function toggleFollow(): Promise<void> {
  if (!detail.value) {
    return
  }
  if (detail.value.isFollowed) {
    await unfollowTicket(ticketId.value)
    notifySuccess('已取消关注')
  } else {
    await followTicket(ticketId.value)
    notifySuccess('关注成功')
  }
  await loadAll()
}

function openBugReportDetail(reportId?: number): void {
  if (!reportId) {
    return
  }
  router.push(`/bug-report/detail/${reportId}`)
}

async function saveCustomerInfo(): Promise<void> {
  bugSubmitLoading.value = true
  try {
    customerInfoForm.problemScreenshot = uniqStringList(customerProblemScreenshots.value).join(',')
    await updateBugCustomerInfo(ticketId.value, { ...customerInfoForm })
    notifySuccess('客服信息保存成功')
    await loadAll()
  } finally {
    bugSubmitLoading.value = false
  }
}

async function saveTestInfo(): Promise<void> {
  bugSubmitLoading.value = true
  try {
    const moduleName = testInfoForm.moduleName?.trim()
    if (moduleName && !ticketModules.value.some((m) => m.name === moduleName)) {
      await createTicketModule({ name: moduleName })
      await loadModules()
    }
    await updateBugTestInfo(ticketId.value, { ...testInfoForm })
    notifySuccess('测试信息保存成功')
    await loadAll()
  } finally {
    bugSubmitLoading.value = false
  }
}

async function saveDevInfo(): Promise<void> {
  bugSubmitLoading.value = true
  try {
    await updateBugDevInfo(ticketId.value, { ...devInfoForm })
    notifySuccess('开发信息保存成功')
    await loadAll()
  } finally {
    bugSubmitLoading.value = false
  }
}

function hasRichTextContent(content?: string): boolean {
  if (!content) {
    return false
  }
  const html = content.trim()
  if (!html) {
    return false
  }
  const plainText = html
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/gi, '')
    .replace(/\s+/g, '')
  const hasStructureContent = /<(img|table|ul|ol|li|blockquote|pre)\b/i.test(html)
  return plainText.length > 0 || hasStructureContent
}

async function submitComment(): Promise<void> {
  const commentContent = commentInput.value || ''
  if (!hasRichTextContent(commentContent)) {
    notifyError('请输入评论内容')
    return
  }
  commentSubmitLoading.value = true
  try {
    await addTicketComment(ticketId.value, commentContent)
    notifySuccess('评论发表成功')
    commentInput.value = ''
    await loadAll()
  } finally {
    commentSubmitLoading.value = false
  }
}

async function refreshTicketAttachmentsOnly(): Promise<void> {
  if (!ticketId.value) {
    return
  }
  try {
    detail.value = await getTicketDetail(ticketId.value)
  } catch {
    // 附件刷新失败不影响当前页面操作
  }
}

async function handleImageUpload(uploadFile: { raw: File }): Promise<void> {
  if (!uploadFile?.raw) {
    return
  }
  imageUploadLoading.value = true
  try {
    await uploadTicketImage(ticketId.value, uploadFile.raw)
    notifySuccess('图片上传成功')
    await refreshTicketAttachmentsOnly()
  } finally {
    imageUploadLoading.value = false
    if (imageUploadRef.value) {
      ;(imageUploadRef.value as { clearFiles: () => void }).clearFiles()
    }
  }
}

async function handleCustomerScreenshotUpload(uploadFile: { raw: File }): Promise<void> {
  if (!uploadFile?.raw) {
    return
  }
  customerScreenshotUploadLoading.value = true
  try {
    const result = await uploadTicketImage(ticketId.value, uploadFile.raw)
    if (result?.url) {
      mergeProblemScreenshots([result.url])
    }
    notifySuccess('问题截图上传成功')
    await refreshTicketAttachmentsOnly()
  } finally {
    customerScreenshotUploadLoading.value = false
    if (customerScreenshotUploadRef.value) {
      ;(customerScreenshotUploadRef.value as { clearFiles: () => void }).clearFiles()
    }
  }
}

async function handleDeleteAttachment(attachmentId: number): Promise<void> {
  const target = detail.value?.attachments?.find((item) => item.id === attachmentId)
  try {
    await deleteTicketAttachment(attachmentId)
    if (target?.filePath) {
      removeProblemScreenshot(target.filePath)
    }
    notifySuccess('附件删除成功')
    await refreshTicketAttachmentsOnly()
  } catch {
    // 删除失败由全局异常处理
  }
}

function isImageFile(fileType?: string): boolean {
  if (!fileType) return false
  return fileType.startsWith('image/')
}

const attachmentImageUrls = computed(() => {
  return (detail.value?.attachments ?? [])
    .filter((a) => isImageFile(a.fileType))
    .map((a) => a.filePath || '')
})

function getAttachmentImageIndex(filePath?: string): number {
  if (!filePath) return 0
  const idx = attachmentImageUrls.value.indexOf(filePath)
  return idx >= 0 ? idx : 0
}

const STATUS_LABEL_MAP: Record<string, string> = {
  pending: '待处理',
  pending_assign: '待分派',
  pending_accept: '待受理',
  processing: '处理中',
  suspended: '已挂起',
  pending_verify: '待验收',
  completed: '已完成',
  closed: '已关闭',
  pending_test_accept: '待测试受理',
  testing: '测试中',
  investigating: '排查中',
  pending_dev_accept: '待开发受理',
  developing: '开发中',
  temp_resolved: '临时解决',
  pending_cs_confirm: '待客服确认',
  submitted: '已提交',
  dept_approval: '部门审批',
  executing: '执行中',
  rejected: '已驳回',
}

function getStatusLabel(status?: string): string {
  if (!status) return '-'
  const normalized = normalizeStatus(status)
  return STATUS_LABEL_MAP[normalized] || status
}

// 优先级标签与样式
const PRIORITY_LABEL_MAP: Record<string, string> = {
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低',
  URGENT: '紧急',
}
const PRIORITY_TYPE_MAP: Record<string, 'danger' | 'warning' | 'info' | ''> = {
  HIGH: 'danger',
  MEDIUM: 'warning',
  LOW: 'info',
  URGENT: 'danger',
}

function getPriorityLabel(priority?: string): string {
  if (!priority) return '-'
  return PRIORITY_LABEL_MAP[priority.toUpperCase()] || priority
}

function getPriorityType(priority?: string): 'danger' | 'warning' | 'info' | '' {
  if (!priority) return 'info'
  return PRIORITY_TYPE_MAP[priority.toUpperCase()] || 'info'
}

// 评论类型图标
function isOperationLog(type?: string): boolean {
  return type === 'OPERATION'
}

onMounted(async () => {
  await Promise.all([loadAll(), trackReadSilently(), loadModules()])
})

watch(
  () => ticketId.value,
  async (newTicketId, oldTicketId) => {
    if (!newTicketId || newTicketId === oldTicketId) {
      return
    }
    await Promise.all([loadAll(), trackReadSilently()])
  },
)
</script>

<template>
  <div class="ticket-detail-page">
    <!-- 顶部信息卡片 -->
    <el-card shadow="never" class="header-card" v-loading="loading">
      <template #header>
        <div class="detail-header">
          <!-- 左侧：工单号 + 优先级 + 状态 -->
          <div class="header-meta">
            <span class="ticket-no-text">{{ detail?.ticketNo || '-' }}</span>
            <el-tag
              v-if="detail?.priority"
              :type="getPriorityType(detail.priority)"
              size="small"
              effect="light"
              class="priority-tag"
            >
              {{ getPriorityLabel(detail.priority) }}
            </el-tag>
            <BugStatusBadge
              v-if="detail?.status"
              :status="detail.status"
              :status-label="detail?.statusLabel"
            />
          </div>
          <!-- 中间：工单标题 -->
          <div class="ticket-title-row">
            <h2 class="ticket-title">{{ detail?.title || '工单详情' }}</h2>
            <div v-if="detail?.categoryFullPath" class="ticket-category">
              {{ detail.categoryFullPath }}
            </div>
          </div>
        </div>
      </template>

      <!-- 操作栏（sticky） -->
      <div class="action-bar">
        <div class="action-bar-left">
          <template v-if="availableActions && availableActions.actions.length > 0">
            <el-button
              v-for="action in availableActions.actions"
              :key="action.transitionId"
              :type="action.isReturn ? 'warning' : action.targetStatus === 'closed' ? 'danger' : 'primary'"
              size="small"
              @click="openTransitDialog(action)"
            >
              {{ action.actionName }}
            </el-button>
          </template>
          <template v-if="currentStatus === 'pending_assign'">
            <el-button size="small" type="primary" plain @click="openAssignDialog">
              分派处理人
            </el-button>
            <el-button
              v-if="hasRole('TESTER', 'ADMIN', 'TICKET_ADMIN')"
              size="small"
              type="success"
              plain
              @click="claimPendingTicket"
            >
              认领
            </el-button>
            <el-button size="small" type="warning" plain @click="pendingPoolTransferVisible = true">
              对接转派
            </el-button>
          </template>
          <el-button v-else size="small" type="warning" plain @click="openAssignDialog">
            转派
          </el-button>
        </div>
        <div class="action-bar-right">
          <el-button
            size="small"
            :type="detail?.isFollowed ? 'primary' : 'default'"
            plain
            @click="toggleFollow"
          >
            <el-icon style="margin-right: 4px">
              <BellFilled v-if="detail?.isFollowed" />
              <Bell v-else />
            </el-icon>
            {{ detail?.isFollowed ? '已关注' : '关注' }}
          </el-button>
        </div>
      </div>

      <!-- TAPD 三区域布局：左侧主区 + 右侧信息面板 -->
      <div class="detail-layout">
        <!-- 左侧主区 -->
        <div class="detail-main">
          <!-- 工单描述 -->
          <div v-if="detail?.description" class="description-block">
            <div class="block-label">描述</div>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <div class="description-content" v-html="detail.description" />
          </div>

          <!-- 主 Tab -->
          <el-tabs v-model="activeMainTab" class="main-tabs">
            <el-tab-pane label="详细信息" name="detail">
              <el-tabs v-model="activeBugTab" class="inner-tabs">
                <el-tab-pane label="客服信息" name="customer">
                  <!-- 企微消息一键解析入口 -->
                  <div v-if="canEditCustomerInfo" class="wecom-parse-bar">
                    <el-button
                      type="primary"
                      plain
                      size="small"
                      class="wecom-parse-btn"
                      @click="openWecomParseDialog"
                    >
                      <el-icon style="margin-right: 4px"><ChatDotSquare /></el-icon>
                      企微消息一键解析
                    </el-button>
                    <span class="wecom-parse-hint">粘贴企微收到的客服消息，自动识别并填入下方字段</span>
                  </div>
                  <el-form label-width="120px" class="info-form">
                    <el-form-item label="商户编号">
                      <el-input v-model="customerInfoForm.merchantNo" :disabled="!canEditCustomerInfo" />
                    </el-form-item>
                    <el-form-item label="公司名称">
                      <el-input v-model="customerInfoForm.companyName" :disabled="!canEditCustomerInfo" />
                    </el-form-item>
                    <el-form-item label="商户账号">
                      <el-input v-model="customerInfoForm.merchantAccount" :disabled="!canEditCustomerInfo" />
                    </el-form-item>
                    <el-form-item label="场景码">
                      <el-input v-model="customerInfoForm.sceneCode" :disabled="!canEditCustomerInfo" />
                    </el-form-item>
                    <el-form-item label="问题描述">
                      <el-input
                        v-model="customerInfoForm.problemDesc"
                        type="textarea"
                        :rows="3"
                        :disabled="!canEditCustomerInfo"
                      />
                    </el-form-item>
                    <el-form-item label="预期结果">
                      <el-input
                        v-model="customerInfoForm.expectedResult"
                        type="textarea"
                        :rows="3"
                        :disabled="!canEditCustomerInfo"
                      />
                    </el-form-item>
                    <el-form-item label="问题截图">
                      <div class="problem-screenshot-field">
                        <el-upload
                          v-if="canEditCustomerInfo"
                          ref="customerScreenshotUploadRef"
                          class="screenshot-upload"
                          drag
                          :show-file-list="false"
                          accept="image/jpeg,image/jpg,image/png,image/gif,image/webp,image/bmp"
                          :on-change="handleCustomerScreenshotUpload"
                          :auto-upload="false"
                        >
                          <div class="upload-drag-content">
                            <el-icon class="upload-drag-icon"><Plus /></el-icon>
                            <div class="upload-drag-text">将文件拖到此处触发上传</div>
                            <div class="upload-drag-subtext">或点击按钮上传问题截图（支持多张）</div>
                            <el-button type="primary" size="small" plain :loading="customerScreenshotUploadLoading">
                              点击上传
                            </el-button>
                          </div>
                        </el-upload>

                        <div class="screenshot-tip">
                          问题截图支持多选，可来源于企微附图，也可在此处手动上传补充。
                        </div>

                        <div v-if="customerProblemScreenshots.length" class="selected-screenshot-wrap">
                          <div class="selected-screenshot-title">
                            已选问题截图（{{ customerProblemScreenshots.length }}）
                          </div>
                          <div class="selected-screenshot-grid">
                            <div
                              v-for="(url, urlIndex) in customerProblemScreenshots"
                              :key="url"
                              class="selected-screenshot-item"
                            >
                              <el-image
                                :src="url"
                                :preview-src-list="customerProblemScreenshots"
                                :initial-index="urlIndex"
                                fit="cover"
                                class="selected-screenshot-image"
                                preview-teleported
                              />
                              <el-button
                                type="danger"
                                link
                                size="small"
                                :disabled="!canEditCustomerInfo"
                                @click="removeProblemScreenshot(url)"
                              >
                                移除
                              </el-button>
                            </div>
                          </div>
                        </div>
                      </div>
                    </el-form-item>
                  </el-form>
                  <div class="tab-actions">
                    <el-button
                      type="primary"
                      :disabled="!canEditCustomerInfo"
                      :loading="bugSubmitLoading"
                      @click="saveCustomerInfo"
                    >保存客服信息</el-button>
                  </div>
                </el-tab-pane>

                <el-tab-pane label="测试信息" name="test">
                  <el-form label-width="120px" class="info-form">
                    <el-form-item label="复现环境">
                      <el-select v-model="testInfoForm.reproduceEnv" :disabled="!canEditTestInfo" placeholder="请选择">
                        <el-option label="生产环境" value="PRODUCTION" />
                        <el-option label="测试环境" value="TEST" />
                        <el-option label="均可复现" value="BOTH" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="复现步骤">
                      <RichTextEditor
                        v-model="testInfoForm.reproduceSteps"
                        :disabled="!canEditTestInfo"
                        :ticket-id="ticketId"
                        placeholder="请填写复现步骤，支持粘贴截图、插入图片和表格..."
                        :height="220"
                      />
                    </el-form-item>
                    <el-form-item label="实际结果">
                      <RichTextEditor
                        v-model="testInfoForm.actualResult"
                        :disabled="!canEditTestInfo"
                        :ticket-id="ticketId"
                        placeholder="请填写实际结果，支持粘贴截图、插入图片和表格..."
                        :height="180"
                      />
                    </el-form-item>
                    <el-form-item label="影响范围">
                      <el-select v-model="testInfoForm.impactScope" :disabled="!canEditTestInfo" placeholder="请选择">
                        <el-option label="单一商户" value="SINGLE" />
                        <el-option label="部分商户" value="PARTIAL" />
                        <el-option label="全部商户" value="ALL" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="缺陷等级">
                      <el-select v-model="testInfoForm.severityLevel" :disabled="!canEditTestInfo" placeholder="请选择">
                        <el-option label="P0（致命）" value="P0" />
                        <el-option label="P1（严重）" value="P1" />
                        <el-option label="P2（一般）" value="P2" />
                        <el-option label="P3（轻微）" value="P3" />
                        <el-option label="P4（建议）" value="P4" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="所属模块">
                      <el-select
                        v-model="testInfoForm.moduleName"
                        :disabled="!canEditTestInfo"
                        filterable
                        allow-create
                        clearable
                        placeholder="请选择或输入模块名称"
                        style="width: 100%"
                      >
                        <el-option
                          v-for="mod in ticketModules"
                          :key="mod.id"
                          :label="mod.name"
                          :value="mod.name"
                        />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="测试备注">
                      <RichTextEditor
                        v-model="testInfoForm.testRemark"
                        :disabled="!canEditTestInfo"
                        :ticket-id="ticketId"
                        placeholder="请填写测试备注，支持粘贴图片、插入表格等富文本内容..."
                        :height="180"
                      />
                    </el-form-item>
                  </el-form>
                  <div class="tab-actions">
                    <el-button
                      type="primary"
                      :disabled="!canEditTestInfo"
                      :loading="bugSubmitLoading"
                      @click="saveTestInfo"
                    >保存测试信息</el-button>
                  </div>
                </el-tab-pane>

                <el-tab-pane label="开发信息" name="dev">
                  <el-form label-width="120px" class="info-form">
                    <el-form-item label="缺陷原因">
                      <el-input
                        v-model="devInfoForm.rootCause"
                        type="textarea"
                        :rows="3"
                        :disabled="!canEditDevInfo"
                      />
                    </el-form-item>
                    <el-form-item label="修复方案">
                      <el-input
                        v-model="devInfoForm.fixSolution"
                        type="textarea"
                        :rows="3"
                        :disabled="!canEditDevInfo"
                      />
                    </el-form-item>
                    <el-form-item label="关联分支">
                      <el-input v-model="devInfoForm.gitBranch" :disabled="!canEditDevInfo" />
                    </el-form-item>
                    <el-form-item label="影响评估">
                      <el-input
                        v-model="devInfoForm.impactAssessment"
                        type="textarea"
                        :rows="3"
                        :disabled="!canEditDevInfo"
                      />
                    </el-form-item>
                    <el-form-item label="开发备注">
                      <RichTextEditor
                        v-model="devInfoForm.devRemark"
                        :disabled="!canEditDevInfo"
                        :ticket-id="ticketId"
                        placeholder="请填写开发备注，支持粘贴图片、插入表格等富文本内容..."
                        :height="180"
                      />
                    </el-form-item>
                  </el-form>
                  <div class="tab-actions">
                    <el-button
                      type="primary"
                      :disabled="!canEditDevInfo"
                      :loading="bugSubmitLoading"
                      @click="saveDevInfo"
                    >保存开发信息</el-button>
                  </div>
                </el-tab-pane>

                <el-tab-pane label="时间追踪" name="track">
                  <TicketTimeTrackPanel
                    :tracks="timeTrackItems"
                    :standalone-field-changes="timeTrackStandalone"
                    :node-duration-items="nodeDurationItems"
                    :status-label-fn="getStatusLabel"
                    :format-duration="formatDurationSec"
                  />
                </el-tab-pane>
              </el-tabs>
            </el-tab-pane>

            <!-- 变更历史 Tab -->
            <el-tab-pane :label="`变更历史(${changeHistoryCount})`" name="history">
              <BugChangeHistory
                v-if="detail?.id"
                :ticket-id="detail.id"
                @count-update="changeHistoryCount = $event"
              />
            </el-tab-pane>

            <!-- 流转历史 Tab -->
            <el-tab-pane label="流转历史" name="flow-history">
              <div v-loading="flowHistoryLoading" class="flow-history-container">
                <el-empty v-if="!flowHistory.length && !flowHistoryLoading" description="暂无流转记录" />
                <el-timeline v-else>
                  <el-timeline-item
                    v-for="record in flowHistory"
                    :key="record.id"
                    :timestamp="record.createTime"
                    placement="top"
                  >
                    <div class="flow-record-card">
                      <div class="flow-record-main">
                        <el-tag size="small" :type="record.flowType === 'RETURN' ? 'warning' : 'primary'">
                          {{ record.flowTypeLabel || record.flowType }}
                        </el-tag>
                        <span v-if="record.fromStatusName" class="flow-status-text">
                          {{ record.fromStatusName }}
                        </span>
                        <el-icon v-if="record.toStatus !== record.fromStatus" class="flow-arrow">
                          <ArrowRight />
                        </el-icon>
                        <span v-if="record.toStatus !== record.fromStatus" class="flow-status-text flow-status-to">
                          {{ record.toStatusName }}
                        </span>
                        <el-divider direction="vertical" />
                        <span class="flow-operator">
                          操作人：{{ record.operatorName || record.operatorId }}
                          （{{ record.operatorRole }}）
                        </span>
                        <template v-if="record.fromAssigneeName !== record.toAssigneeName && record.toAssigneeName">
                          <el-divider direction="vertical" />
                          <span class="flow-operator">
                            处理人：{{ record.fromAssigneeName || '-' }} → {{ record.toAssigneeName }}
                          </span>
                        </template>
                      </div>
                      <div v-if="record.remark" class="flow-record-remark">
                        {{ record.remark }}
                      </div>
                    </div>
                  </el-timeline-item>
                </el-timeline>
              </div>
            </el-tab-pane>

            <!-- 更多（预留） -->
            <el-tab-pane label="更多" name="more" disabled />
          </el-tabs>
        </div>

        <!-- 右侧信息面板（sticky） -->
        <div class="detail-sidebar">
          <BugDetailInfoPanel
            v-if="detail"
            :detail="detail"
            :ticket-id="ticketId"
            @refresh="loadAll"
          />
        </div>
      </div>
    </el-card>

    <!-- 自定义字段 -->
    <el-card v-if="customFieldEntries.length > 0" shadow="never" class="section-card">
      <template #header>
        <div class="section-header">
          <span class="section-title">自定义字段</span>
        </div>
      </template>
      <el-table
        :data="customFieldEntries"
        :border="false"
        :stripe="true"
        :header-cell-style="{ backgroundColor: '#f5f7fa' }"
      >
        <el-table-column prop="key" label="字段名" align="center" />
        <el-table-column prop="value" label="字段值" align="center" />
      </el-table>
    </el-card>

    <!-- 附件 -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="section-header">
          <span class="section-title">附件</span>
        </div>
      </template>
      <el-upload
        ref="imageUploadRef"
        class="attachment-upload-dropzone"
        drag
        :show-file-list="false"
        accept="image/jpeg,image/jpg,image/png,image/gif,image/webp,image/bmp"
        :on-change="handleImageUpload"
        :auto-upload="false"
      >
        <div class="upload-drag-content">
          <el-icon class="upload-drag-icon"><Plus /></el-icon>
          <div class="upload-drag-text">将文件拖到此处触发上传</div>
          <div class="upload-drag-subtext">或点击按钮上传附件图片</div>
          <el-button type="primary" size="small" plain :loading="imageUploadLoading">
            点击上传
          </el-button>
        </div>
      </el-upload>
      <EmptyState v-if="!detail?.attachments?.length" description="暂无附件，可拖拽或点击上方区域上传图片" />
      <div v-else class="attachment-list">
        <div
          v-for="attachment in detail.attachments"
          :key="attachment.id"
          class="attachment-item"
        >
          <div class="attachment-preview">
            <el-image
              v-if="isImageFile(attachment.fileType)"
              :src="attachment.filePath"
              :preview-src-list="attachmentImageUrls"
              :initial-index="getAttachmentImageIndex(attachment.filePath)"
              fit="cover"
              class="attachment-thumbnail"
              preview-teleported
              lazy
            />
            <el-icon v-else class="attachment-icon"><DocumentOutlined /></el-icon>
          </div>
          <div class="attachment-info">
            <div class="attachment-name" :title="attachment.fileName">{{ attachment.fileName }}</div>
            <div class="attachment-meta">
              <el-tag
                v-if="attachment.source === 'WECOM_BOT'"
                type="success"
                size="small"
                effect="plain"
              >企微</el-tag>
              <el-tag
                v-else
                type="info"
                size="small"
                effect="plain"
              >Web</el-tag>
              <span>{{ formatFileSize(attachment.fileSize) }}</span>
              <span class="meta-divider">·</span>
              <span>{{ attachment.uploadedByName || '-' }}</span>
              <span class="meta-divider">·</span>
              <span>{{ formatDateTime(attachment.createTime) }}</span>
            </div>
          </div>
          <div class="attachment-actions">
            <el-button
              v-if="isImageFile(attachment.fileType) && attachment.filePath"
              type="primary"
              link
              size="small"
              :href="attachment.filePath"
              target="_blank"
            >查看</el-button>
            <el-popconfirm
              title="确认删除此附件？"
              @confirm="handleDeleteAttachment(attachment.id)"
            >
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 关联Bug简报 -->
    <el-card v-if="detail?.bugReports?.length" shadow="never" class="section-card">
      <template #header>
        <div class="section-header">
          <span class="section-title">关联Bug简报</span>
        </div>
      </template>
      <el-table
        :data="detail.bugReports"
        :border="false"
        :stripe="true"
        :header-cell-style="{ backgroundColor: '#f5f7fa' }"
      >
        <el-table-column prop="reportNo" label="简报编号" min-width="180" align="center" />
        <el-table-column label="状态" width="140" align="center">
          <template #default="{ row }">
            {{ row.statusLabel || row.status || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="关联方式" width="120" align="center">
          <template #default="{ row }">
            {{ row.isAutoCreated === 1 ? '自动' : '手动' }}
          </template>
        </el-table-column>
        <el-table-column label="关联时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openBugReportDetail(row.id)">查看简报</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 评论与处理记录 -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="section-header">
          <span class="section-title">评论与处理记录</span>
          <span class="section-count">{{ detail?.comments?.length || 0 }} 条</span>
        </div>
      </template>

      <!-- 发表评论输入框 -->
      <div class="comment-input-area">
        <div class="comment-input-avatar">
          <el-avatar :size="32" class="current-user-avatar">
            {{ authStore.userInfo?.name?.charAt(0) || '我' }}
          </el-avatar>
        </div>
        <div class="comment-input-body">
          <RichTextEditor
            v-model="commentInput"
            :ticket-id="ticketId"
            placeholder="发表评论（支持粘贴图片、表格等富文本内容）..."
            :height="180"
            class="comment-editor"
          />
          <div class="comment-submit-row">
            <span class="comment-hint">支持粘贴图片、表格等内容</span>
            <el-button
              type="primary"
              size="small"
              :loading="commentSubmitLoading"
              :disabled="!hasRichTextContent(commentInput)"
              @click="submitComment"
            >发表评论</el-button>
          </div>
        </div>
      </div>

      <!-- 评论列表 -->
      <div class="comment-divider" v-if="detail?.comments?.length" />
      <EmptyState v-if="!detail?.comments?.length" description="暂无评论，来发表第一条评论吧" />
      <div v-else class="comment-list">
        <div
          v-for="comment in detail.comments"
          :key="comment.id"
          class="comment-item"
          :class="{ 'comment-operation': isOperationLog(comment.type) }"
        >
          <div class="comment-avatar">
            <el-avatar :size="32" class="user-avatar">
              {{ comment.userName?.charAt(0) || '?' }}
            </el-avatar>
          </div>
          <div class="comment-body">
            <div class="comment-header">
              <span class="comment-username">{{ comment.userName || '-' }}</span>
              <el-tag v-if="isOperationLog(comment.type)" type="info" size="small" effect="plain">操作记录</el-tag>
              <span class="comment-time">{{ formatDateTime(comment.createTime) }}</span>
            </div>
            <div v-if="isOperationLog(comment.type)" class="comment-content comment-content-plain">
              {{ comment.content || '-' }}
            </div>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <div v-else class="comment-content" v-html="comment.content || '-'" />
          </div>
        </div>
      </div>
    </el-card>
  </div>

  <!-- 企微消息解析弹窗 -->
  <el-dialog
    v-model="wecomParseDialogVisible"
    title="企微消息一键解析"
    width="640px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div class="wecom-dialog-body">
      <p class="wecom-dialog-tip">
        将企微机器人收到的客服消息粘贴到下方，系统将通过自然语言智能匹配，自动识别商户编号、公司名称、问题描述等字段并填入表单。支持结构化格式与自由文本两种输入方式。
      </p>
      <el-collapse class="parse-example-collapse">
        <el-collapse-item title="查看支持的消息格式示例" name="example">
          <div class="parse-example-content">
            <div class="parse-example-section">
              <div class="parse-example-label">✅ 结构化格式（识别率最高）</div>
              <pre class="parse-example-code">商户编号：10004557
公司名称：山东英贝健生物技术有限公司
商户账号：ybj0101
问题描述：用户反馈支付页面无法跳转，点击支付按钮后页面白屏
预期结果：点击支付后应正常跳转到支付页面</pre>
            </div>
            <div class="parse-example-section">
              <div class="parse-example-label">✅ 自然语言格式（也可识别）</div>
              <pre class="parse-example-code">商户10004557，山东英贝健生物技术有限公司反馈，用户无法完成支付，点击支付按钮后页面出现白屏，账号ybj0101，预期点击后正常跳转到支付页面</pre>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
      <el-input
        v-model="wecomParseMessage"
        type="textarea"
        :rows="8"
        placeholder="请粘贴企微消息内容（支持自由文本格式，系统将自动识别字段）..."
        class="wecom-parse-textarea"
      />
      <div v-if="wecomParseResult" class="wecom-parse-preview">
        <div class="preview-header">
          <span class="preview-title">解析结果预览</span>
          <el-tag
            :type="(wecomParseResult.confidence ?? 0) >= 60 ? 'success' : 'warning'"
            size="small"
          >
            置信度 {{ wecomParseResult.confidence ?? 0 }}%
          </el-tag>
        </div>
        <el-descriptions :column="1" border size="small" class="preview-desc">
          <el-descriptions-item v-if="wecomParseResult.merchantNo" label="商户编号">
            <el-tag type="success" size="small" class="matched-tag">已识别</el-tag>
            {{ wecomParseResult.merchantNo }}
          </el-descriptions-item>
          <el-descriptions-item v-if="wecomParseResult.companyName" label="公司名称">
            <el-tag type="success" size="small" class="matched-tag">已识别</el-tag>
            {{ wecomParseResult.companyName }}
          </el-descriptions-item>
          <el-descriptions-item v-if="wecomParseResult.merchantAccount" label="商户账号">
            <el-tag type="success" size="small" class="matched-tag">已识别</el-tag>
            {{ wecomParseResult.merchantAccount }}
          </el-descriptions-item>
          <el-descriptions-item v-if="wecomParseResult.sceneCode" label="场景码">
            <el-tag type="success" size="small" class="matched-tag">已识别</el-tag>
            {{ wecomParseResult.sceneCode }}
          </el-descriptions-item>
          <el-descriptions-item v-if="wecomParseResult.problemDesc" label="问题描述">
            <el-tag type="success" size="small" class="matched-tag">已识别</el-tag>
            {{ wecomParseResult.problemDesc }}
          </el-descriptions-item>
          <el-descriptions-item v-if="wecomParseResult.expectedResult" label="预期结果">
            <el-tag type="success" size="small" class="matched-tag">已识别</el-tag>
            {{ wecomParseResult.expectedResult }}
          </el-descriptions-item>
          <el-descriptions-item v-if="wecomParseResult.problemScreenshot" label="问题截图">
            <el-tag type="success" size="small" class="matched-tag">已识别</el-tag>
            {{ wecomParseResult.problemScreenshot }}
          </el-descriptions-item>
        </el-descriptions>
        <p
          v-if="!wecomParseResult.matchedFields || wecomParseResult.matchedFields.length === 0"
          class="no-match-tip"
        >
          未能识别到任何字段，请尝试使用"字段名：字段值"的格式发送消息
        </p>
      </div>
    </div>
    <template #footer>
      <el-button @click="wecomParseDialogVisible = false">取消</el-button>
      <el-button
        type="primary"
        plain
        :loading="wecomParseLoading"
        :disabled="!wecomParseMessage.trim()"
        @click="handleWecomParse"
      >
        {{ wecomParseResult ? '重新解析' : '开始解析' }}
      </el-button>
      <el-button
        v-if="wecomParseResult && wecomParseResult.matchedFields && wecomParseResult.matchedFields.length > 0"
        type="primary"
        @click="applyWecomParseResult"
      >
        填入表单
      </el-button>
    </template>
  </el-dialog>

  <!-- 分派工单弹窗（支持多人，首位为主处理人） -->
  <el-dialog
    v-model="assignDialogVisible"
    :title="currentStatus === 'pending_assign' ? '分派 / 认领（进入下一环节）' : '转派工单'"
    width="520px"
  >
    <el-form label-width="100px">
      <el-form-item label="处理人" required>
        <el-select
          v-model="assignForm.assigneeIds"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          placeholder="可选择多名处理人，第一位为主负责人"
          style="width: 100%"
        >
          <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="assignForm.remark" type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="assignDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleAssign">确认</el-button>
    </template>
  </el-dialog>

  <!-- 待分派：仅更换对接人（不改变流程状态） -->
  <el-dialog v-model="pendingPoolTransferVisible" title="待分派 · 对接转派" width="480px">
    <el-form label-width="90px">
      <el-form-item label="对接人" required>
        <el-select v-model="pendingPoolTransferForm.targetUserId" filterable placeholder="选择新的测试对接人" style="width: 100%">
          <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="pendingPoolTransferForm.reason" type="textarea" placeholder="可选" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pendingPoolTransferVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handlePendingPoolTransfer">确认</el-button>
    </template>
  </el-dialog>

  <!-- 工单操作弹窗 -->
  <el-dialog
    v-model="processDialogVisible"
    :title="selectedAction ? selectedAction.actionName : '工单操作'"
    width="520px"
  >
    <el-form label-width="100px">
      <el-form-item label="目标状态">
        <el-tag type="primary">
          {{ selectedAction?.targetStatusName || transitForm.targetStatus }}
        </el-tag>
        <el-tag v-if="selectedAction?.isReturn" type="warning" style="margin-left:8px;">退回</el-tag>
      </el-form-item>
      <el-form-item v-if="selectedAction?.allowTransfer" label="指定处理人">
        <el-select
          v-model="transitForm.newAssigneeIds"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          clearable
          placeholder="可选多名，第一位为主处理人；不选则保持当前"
          style="width: 100%"
        >
          <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注" :required="selectedAction?.requireRemark">
        <el-input
          v-model="transitForm.remark"
          type="textarea"
          :rows="3"
          :placeholder="selectedAction?.requireRemark ? '必填：请说明操作原因' : '可选'"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="processDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleProcess">
        确认{{ selectedAction?.actionName || '操作' }}
      </el-button>
    </template>
  </el-dialog>

  <!-- 关闭工单弹窗 -->
  <el-dialog v-model="closeDialogVisible" title="关闭工单" width="480px">
    <el-form label-width="90px">
      <el-form-item label="关闭原因">
        <el-input v-model="closeForm.remark" type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleCloseTicket">确认关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.ticket-detail-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 24px;
}

// ===== 头部卡片 =====
.header-card {
  :deep(.el-card__header) {
    padding: 18px 24px 14px;
    border-bottom: 1px solid #f0f0f0;
  }

  :deep(.el-card__body) {
    padding: 16px 24px 24px;
  }
}

.detail-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.ticket-no-text {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
  font-family: monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
}

.priority-tag {
  font-weight: 500;
}

.ticket-title-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ticket-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1d2129;
  line-height: 1.4;
}

.ticket-category {
  font-size: 12px;
  color: #909399;
}

// ===== 操作栏 =====
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0 16px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}

.action-bar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.action-bar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

// ===== 主布局 =====
.detail-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.detail-main {
  flex: 1;
  min-width: 0;
}

.detail-sidebar {
  width: 320px;
  flex-shrink: 0;
  position: sticky;
  top: 60px;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
  border-left: 1px solid #f0f0f0;
  padding-left: 16px;
}

// ===== 描述区块 =====
.description-block {
  margin-bottom: 20px;
  padding: 16px 20px;
  background: #fafbfc;
  border-radius: 8px;
  border: 1px solid #ebedf0;
}

.block-label {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 10px;
}

.description-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.7;
  word-break: break-word;

  :deep(img) {
    max-width: 100%;
    border-radius: 4px;
  }
}

// ===== Tabs =====
.main-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 18px;
  }
}

.inner-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 14px;
  }
}

.info-form {
  :deep(.el-form-item) {
    margin-bottom: 18px;
  }

  :deep(.el-form-item__label) {
    font-size: 14px;
    color: #606266;
    font-weight: 500;
  }
}

.tab-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.upload-drag-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.upload-drag-icon {
  font-size: 24px;
  color: #1675d1;
}

.upload-drag-text {
  font-size: 14px;
  color: #303133;
}

.upload-drag-subtext {
  font-size: 12px;
  color: #909399;
}

.problem-screenshot-field {
  width: 100%;
}

.screenshot-upload {
  width: 100%;
}

.screenshot-tip {
  margin: 8px 0 10px;
  font-size: 12px;
  color: #909399;
}

.selected-screenshot-title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.selected-screenshot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(128px, 1fr));
  gap: 10px;
}

.selected-screenshot-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 8px;
  background: #fafafa;
}

.selected-screenshot-image {
  width: 100%;
  height: 96px;
  border-radius: 4px;
  display: block;
  margin-bottom: 6px;
}

// ===== 流转历史 =====
.flow-history-container {
  padding: 4px 0;
}

.flow-record-card {
  background: #fafafa;
  border: 1px solid #ebedf0;
  border-radius: 8px;
  padding: 12px 16px;
  transition: box-shadow 0.15s;

  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }
}

.flow-record-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.flow-status-text {
  font-size: 13px;
  color: #303133;
}

.flow-status-to {
  font-weight: 600;
  color: #1675d1;
}

.flow-arrow {
  color: #909399;
  font-size: 14px;
}

.flow-operator {
  color: #909399;
  font-size: 12px;
}

.flow-record-remark {
  margin-top: 6px;
  font-size: 13px;
  color: #606266;
  padding-left: 2px;
}

// ===== 分区卡片 =====
.section-card {
  :deep(.el-card__header) {
    padding: 14px 20px;
  }

  :deep(.el-card__body) {
    padding: 16px 20px;
  }
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
}

.section-count {
  font-size: 12px;
  color: #909399;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 10px;
}

// ===== 附件 =====
.attachment-upload-dropzone {
  margin-bottom: 12px;
  width: 100%;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
  transition: background 0.2s;

  &:hover {
    background: #f0f9ff;
  }
}

.attachment-preview {
  flex-shrink: 0;
  width: 60px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
}

.attachment-thumbnail {
  width: 60px;
  height: 60px;
  object-fit: cover;
}

.attachment-icon {
  font-size: 28px;
  color: #909399;
}

.attachment-info {
  flex: 1;
  min-width: 0;
}

.attachment-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.meta-divider {
  color: #d4d4d4;
}

.attachment-actions {
  flex-shrink: 0;
  display: flex;
  gap: 4px;
}

// ===== 评论区 =====
.comment-input-area {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  margin-bottom: 4px;
}

.comment-input-avatar {
  flex-shrink: 0;
  padding-top: 2px;
}

.current-user-avatar {
  background: #1675d1;
  color: #fff;
  font-size: 13px;
}

.comment-input-body {
  flex: 1;
  min-width: 0;
}

.comment-editor {
  width: 100%;

  :deep(.rich-text-editor) {
    border-radius: 6px;
  }
}

.comment-submit-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.comment-hint {
  font-size: 12px;
  color: #c0c4cc;
}

.comment-divider {
  height: 1px;
  background: #ebedf0;
  margin: 20px 0;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.comment-item {
  display: flex;
  gap: 14px;
  align-items: flex-start;

  &.comment-operation {
    .comment-body {
      background: #fafbfc;
      border: 1px solid #ebedf0;
    }
  }
}

.comment-avatar {
  flex-shrink: 0;
}

.user-avatar {
  background: #e8f0fe;
  color: #1675d1;
  font-size: 13px;
}

.comment-body {
  flex: 1;
  min-width: 0;
  background: #f5f7fa;
  border-radius: 0 8px 8px 8px;
  padding: 12px 16px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.comment-username {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
}

.comment-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-left: auto;
}

.comment-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  word-break: break-word;

  :deep(p) {
    margin: 0 0 8px;
  }

  :deep(p:last-child) {
    margin-bottom: 0;
  }

  :deep(img) {
    max-width: 100%;
    border-radius: 4px;
  }

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 8px 0;
  }

  :deep(th),
  :deep(td) {
    border: 1px solid #e5e6eb;
    padding: 6px 8px;
  }
}

.comment-content-plain {
  white-space: pre-wrap;
}

// ===== 企微解析弹窗 =====
.wecom-parse-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 14px;
  background: #f0f7ff;
  border: 1px solid #c8e0ff;
  border-radius: 6px;
}

.wecom-parse-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.wecom-parse-hint {
  font-size: 12px;
  color: #606266;
}

.wecom-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.wecom-dialog-tip {
  margin: 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.parse-example-collapse {
  border: 1px solid #e4e7ed;
  border-radius: 6px;

  :deep(.el-collapse-item__header) {
    font-size: 12px;
    color: #909399;
    padding: 0 12px;
    height: 36px;
    background: #fafafa;
    border-radius: 6px;
  }

  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }

  :deep(.el-collapse-item__content) {
    padding: 10px 12px;
  }
}

.parse-example-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.parse-example-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.parse-example-label {
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
}

.parse-example-code {
  margin: 0;
  padding: 8px 10px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  font-size: 12px;
  color: #495057;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'Courier New', Courier, monospace;
}

.wecom-parse-textarea {
  width: 100%;
}

.wecom-parse-preview {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.preview-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.preview-desc {
  width: 100%;
}

.matched-tag {
  margin-right: 8px;
}

.no-match-tip {
  margin: 12px;
  font-size: 13px;
  color: #e6a23c;
}
</style>
