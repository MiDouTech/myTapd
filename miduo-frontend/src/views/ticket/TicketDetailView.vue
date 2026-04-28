<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
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
  urgeTicket,
} from '@/api/ticket'
import {
  getAvailableActions,
  transitTicket,
  transferTicket,
  returnTicket,
  getFlowHistory,
} from '@/api/workflow'
import { getUserList } from '@/api/user'
import EmptyState from '@/components/common/EmptyState.vue'
import RichTextEditor from '@/components/common/RichTextEditor.vue'
import type { MentionPanelAnchor } from '@/components/common/RichTextEditor.vue'
import { useAuthStore } from '@/stores/auth'
import type {
  BugChangeHistoryOutput,
  TicketBugCustomerInfoInput,
  TicketBugDevInfoInput,
  TicketBugReportOutput,
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
import { confirmAction, notifySuccess, notifyError } from '@/utils/feedback'
import { formatDateTime, formatDurationSec, formatFileSize, formatRoleLabel } from '@/utils/formatter'
import { formatTicketDescriptionForDisplay } from '@/utils/ticket-description-display'
import { getTicketStatusLabel, normalizeTicketStatusCode } from '@/utils/ticket-status'

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

const descriptionDisplayHtml = computed(() => formatTicketDescriptionForDisplay(detail.value?.description))

const activeBugTab = ref('customer')
const timeTrackItems = ref<TicketTimeTrackItem[]>([])
const timeTrackStandalone = ref<BugChangeHistoryOutput[]>([])
const nodeDurationItems = ref<TicketNodeDurationItem[]>([])
const bugSubmitLoading = ref(false)
const attachmentPreviewVisible = ref(false)
const attachmentPreviewUrl = ref('')

const MOBILE_BREAKPOINT = 768
const viewportWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1024)

const activeMainTab = ref('detail')
const changeHistoryCount = ref(0)

const assignDialogVisible = ref(false)
const pendingPoolTransferVisible = ref(false)
const processDialogVisible = ref(false)
const closeDialogVisible = ref(false)
const submitLoading = ref(false)
const urgeDialogVisible = ref(false)
const urgeExtraNotifyUserIds = ref<number[]>([])

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
  reproduceEnv: '' as string,
  plannedFullResolveAt: '' as string,
})

const attachmentUploadLoading = ref(false)
const attachmentUploadRef = ref()
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
const commentEditorRef = ref<InstanceType<typeof RichTextEditor> | null>(null)
const commentMentionUserIds = ref<number[]>([])
const mentionPopoverVisible = ref(false)
const mentionKeyword = ref('')
const mentionCandidates = ref<UserListOutput[]>([])
const mentionLoading = ref(false)
const mentionPanelOpen = ref(false)
const mentionPanelKeyword = ref('')
const mentionPanelAnchor = ref<MentionPanelAnchor | null>(null)
/** 为 true 表示浮层由编辑器内 @ 触发，选人后需删掉 @关键词 */
const mentionFromInlineEditor = ref(false)
const mentionTab = ref<'people' | 'content'>('people')
/** 编辑器内 @ 浮层与「@同事」弹层共用候选列表时的高亮下标（默认第一条） */
const mentionActiveIndex = ref(0)
let mentionSearchTimer: ReturnType<typeof setTimeout> | null = null

watch(mentionCandidates, () => {
  mentionActiveIndex.value = 0
})

watch(mentionTab, () => {
  if (mentionPanelOpen.value && mentionTab.value === 'people') {
    mentionActiveIndex.value = 0
  }
})

function closeMentionFloatingPanel(): void {
  mentionPanelOpen.value = false
  mentionPanelAnchor.value = null
  mentionFromInlineEditor.value = false
  mentionActiveIndex.value = 0
}

function onEditorMentionPanel(
  payload:
    | { open: false }
    | { open: true; keyword: string; anchor: MentionPanelAnchor },
): void {
  if (!payload.open) {
    if (mentionFromInlineEditor.value) {
      closeMentionFloatingPanel()
    }
    return
  }
  mentionFromInlineEditor.value = true
  mentionTab.value = 'people'
  mentionPanelKeyword.value = payload.keyword
  mentionPanelAnchor.value = payload.anchor
  mentionPanelOpen.value = true
  scheduleMentionSearch()
}

watch(mentionPopoverVisible, (open) => {
  if (open) {
    closeMentionFloatingPanel()
    mentionKeyword.value = ''
    mentionFromInlineEditor.value = false
    mentionActiveIndex.value = 0
    void loadMentionUsers()
  }
})

watch(mentionPanelKeyword, () => {
  if (mentionPanelOpen.value) {
    scheduleMentionSearch()
  }
})

function scheduleMentionSearch() {
  if (mentionSearchTimer) {
    clearTimeout(mentionSearchTimer)
  }
  mentionSearchTimer = setTimeout(() => {
    void loadMentionUsers()
  }, 280)
}

async function loadMentionUsers() {
  mentionLoading.value = true
  try {
    const kw = mentionPanelOpen.value
      ? mentionPanelKeyword.value.trim()
      : mentionKeyword.value.trim()
    const list = await getUserList({
      accountStatus: 1,
      ...(kw ? { keyword: kw } : {}),
    })
    mentionCandidates.value = list
  } catch {
    mentionCandidates.value = []
  } finally {
    mentionLoading.value = false
  }
}

function mentionDisplaySub(u: UserListOutput): string {
  const no = u.employeeNo?.trim()
  if (no) {
    return no
  }
  return String(u.id)
}

function pickMentionUser(u: UserListOutput) {
  const editor = commentEditorRef.value
  if (!editor || u.id == null) {
    return
  }
  if (mentionFromInlineEditor.value) {
    const kw = mentionPanelKeyword.value
    editor.deleteBackwardChars(1 + kw.length)
    closeMentionFloatingPanel()
  }
  const safeName = (u.name || '用户').replace(/</g, '').replace(/>/g, '')
  const sub = mentionDisplaySub(u)
  const html = `<span data-comment-mention="1" data-user-id="${u.id}" class="ticket-comment-mention">@${safeName}(${sub})</span>`
  editor.insertHtml(html)
  if (!commentMentionUserIds.value.includes(u.id)) {
    commentMentionUserIds.value.push(u.id)
  }
  mentionPopoverVisible.value = false
}

/** 与后端一致：从评论 HTML 中解析 @ 占位上的 data-user-id，避免富文本与 ref 不同步时漏发通知 */
function extractMentionUserIdsFromCommentHtml(html: string): number[] {
  const ids = new Set<number>()
  const re = /data-user-id\s*=\s*["']?(\d+)["']?/gi
  let m: RegExpExecArray | null
  while ((m = re.exec(html)) !== null) {
    const n = Number(m[1])
    if (Number.isFinite(n) && n > 0) {
      ids.add(n)
    }
  }
  const reParen = /@([^(<\s]+)\((\d{1,19})\)/g
  while ((m = reParen.exec(html)) !== null) {
    const n = Number(m[2])
    if (Number.isFinite(n) && n > 0) {
      ids.add(n)
    }
  }
  return Array.from(ids)
}

const mentionFloatPanelStyle = computed(() => {
  const a = mentionPanelAnchor.value
  if (!a || !mentionPanelOpen.value) {
    return {}
  }
  const panelW = 320
  const margin = 8
  const left = Math.min(window.innerWidth - panelW - margin, Math.max(margin, a.left))
  const spaceAbove = a.top
  const preferAbove = spaceAbove > 120
  if (preferAbove) {
    return {
      position: 'fixed' as const,
      left: `${left}px`,
      top: `${a.top - margin}px`,
      width: `${panelW}px`,
      transform: 'translateY(-100%)',
      zIndex: 3000,
    }
  }
  return {
    position: 'fixed' as const,
    left: `${left}px`,
    top: `${a.top + a.height + margin}px`,
    width: `${panelW}px`,
    zIndex: 3000,
  }
})

function onDocumentPointerDownMention(ev: MouseEvent): void {
  if (!mentionPanelOpen.value) {
    return
  }
  const t = ev.target as Node | null
  const floatEl = document.querySelector('.comment-mention-float')
  if (t && floatEl?.contains(t)) {
    return
  }
  closeMentionFloatingPanel()
}

function onKeydownMention(ev: KeyboardEvent): void {
  if (ev.key === 'Escape' && mentionPanelOpen.value) {
    closeMentionFloatingPanel()
    return
  }

  const list = mentionCandidates.value
  const inlinePeoplePanel =
    mentionPanelOpen.value && mentionTab.value === 'people' && mentionFromInlineEditor.value
  const popoverPeopleList = mentionPopoverVisible.value && !mentionPanelOpen.value
  const popoverPick = popoverPeopleList

  if (mentionLoading.value || !list.length) {
    return
  }

  const canArrowNav = inlinePeoplePanel || popoverPeopleList
  if (canArrowNav) {
    if (ev.key === 'ArrowDown') {
      ev.preventDefault()
      mentionActiveIndex.value = Math.min(list.length - 1, mentionActiveIndex.value + 1)
      return
    }
    if (ev.key === 'ArrowUp') {
      ev.preventDefault()
      mentionActiveIndex.value = Math.max(0, mentionActiveIndex.value - 1)
      return
    }
  }

  if (ev.key !== 'Enter') {
    return
  }
  if (!inlinePeoplePanel && !popoverPick) {
    return
  }

  ev.preventDefault()
  ev.stopPropagation()
  const idx = Math.min(Math.max(0, mentionActiveIndex.value), list.length - 1)
  const u = list[idx]
  if (u) {
    pickMentionUser(u)
  }
}

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
  plannedFullResolveAt: '',
})

const ticketId = computed(() => Number(route.params.id))
const roleCodes = computed(() =>
  (authStore.userInfo?.roleCodes ?? []).map((item) => String(item).toUpperCase()),
)
const currentUserId = computed(() => authStore.userInfo?.id)
const currentStatus = computed(() => normalizeTicketStatusCode(detail.value?.status))
/** 内置缺陷工作流 ID 与后端 Flyway 一致 */
const isDefectWorkflow = computed(() => detail.value?.workflowId === 3)

const urgeDefaultNotifyUserIds = computed(() => {
  const d = detail.value
  if (!d) {
    return [] as number[]
  }
  if (d.urgeDefaultNotifyUserIds?.length) {
    return d.urgeDefaultNotifyUserIds
  }
  if (d.assigneeIds?.length) {
    return d.assigneeIds
  }
  if (d.assigneeId != null) {
    return [d.assigneeId]
  }
  return [] as number[]
})

const canShowUrgeTicket = computed(() => {
  if (!detail.value) {
    return false
  }
  const s = currentStatus.value
  if (s === 'pending_assign' || s === 'alert_triggered') {
    return false
  }
  if (
    s === 'completed' ||
    s === 'closed' ||
    s === 'rejected' ||
    s === 'alert_resolved' ||
    s === 'alert_suppressed'
  ) {
    return false
  }
  return urgeDefaultNotifyUserIds.value.length > 0
})

const canShowRollbackLastStep = computed(() => {
  if (!detail.value) {
    return false
  }
  if (currentStatus.value === 'closed') {
    return true
  }
  if (currentStatus.value !== 'completed') {
    return false
  }
  // 已完成仅在存在未归档简报时允许回退（与后端兜底校验保持一致）
  const reports = detail.value.bugReports ?? []
  return reports.some((report) => (report.status || '').trim().toUpperCase() !== 'ARCHIVED')
})

function urgeNotifyUserName(userId: number): string {
  return users.value.find((u) => u.id === userId)?.name ?? `用户#${userId}`
}

const urgeDefaultNotifyNames = computed(() =>
  urgeDefaultNotifyUserIds.value.map(urgeNotifyUserName).join('、'),
)

const extraNotifyCandidates = computed(() => {
  const ids = new Set(urgeDefaultNotifyUserIds.value)
  return users.value.filter((u) => !ids.has(u.id))
})

const assignDialogTitle = computed(() => {
  if (currentStatus.value === 'pending_assign' || currentStatus.value === 'alert_triggered') {
    return '分派 / 认领（进入下一环节）'
  }
  if (isDefectWorkflow.value && currentStatus.value === 'testing') {
    return '追加协同处理人'
  }
  return '转派工单'
})
const isCompactLayout = computed(() => viewportWidth.value <= MOBILE_BREAKPOINT)

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

function updateViewportWidth(): void {
  if (typeof window === 'undefined') {
    return
  }
  viewportWidth.value = window.innerWidth
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
    plannedFullResolveAt: ticketDetail.bugDevInfo?.plannedFullResolveAt
      ? String(ticketDetail.bugDevInfo.plannedFullResolveAt).replace(' ', 'T').slice(0, 19)
      : '',
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
      mergeAssignees:
        isDefectWorkflow.value && currentStatus.value === 'testing' ? true : undefined,
    })
    notifySuccess(
      isDefectWorkflow.value && currentStatus.value === 'testing'
        ? '已追加协同处理人'
        : '工单分派成功',
    )
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
      remark: currentStatus.value === 'alert_triggered' ? '告警认领' : '测试认领',
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
  transitForm.reproduceEnv =
    isDefectWorkflow.value &&
    currentStatus.value === 'testing' &&
    action.targetStatus === 'pending_dev_accept'
      ? testInfoForm.reproduceEnv || ''
      : ''
  transitForm.plannedFullResolveAt =
    isDefectWorkflow.value &&
    action.targetStatus === 'temp_resolved' &&
    (action.actionName || '').includes('临时解决')
      ? devInfoForm.plannedFullResolveAt || ''
      : ''
  processDialogVisible.value = true
}

async function handleProcess(): Promise<void> {
  submitLoading.value = true
  const guidedTargetStatus = transitForm.targetStatus
  const guidedActionName = selectedAction.value?.actionName
  try {
    if (!selectedAction.value) {
      notifyError('请选择操作')
      return
    }
    if (selectedAction.value.requireRemark && !transitForm.remark?.trim()) {
      notifyError('该操作需要填写备注')
      return
    }
    if (
      isDefectWorkflow.value &&
      currentStatus.value === 'testing' &&
      selectedAction.value.targetStatus === 'pending_dev_accept'
    ) {
      const env = transitForm.reproduceEnv?.trim() || testInfoForm.reproduceEnv?.trim()
      if (!env) {
        notifyError('确认缺陷转开发前请选择复现环境')
        return
      }
    }
    if (
      isDefectWorkflow.value &&
      selectedAction.value.targetStatus === 'temp_resolved' &&
      selectedAction.value.actionName?.includes('临时解决')
    ) {
      const plan = transitForm.plannedFullResolveAt?.trim() || devInfoForm.plannedFullResolveAt?.trim()
      if (!plan) {
        notifyError('临时解决必须填写计划彻底解决时间')
        return
      }
    }
    const transitPayload: {
      transitionId: string
      targetStatus: string
      remark: string
      newAssigneeId?: number
      newAssigneeIds?: number[]
      reproduceEnv?: string
      plannedFullResolveAt?: string
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
    if (
      isDefectWorkflow.value &&
      currentStatus.value === 'testing' &&
      selectedAction.value.targetStatus === 'pending_dev_accept'
    ) {
      transitPayload.reproduceEnv = transitForm.reproduceEnv?.trim() || testInfoForm.reproduceEnv?.trim()
    }
    if (
      isDefectWorkflow.value &&
      selectedAction.value.targetStatus === 'temp_resolved' &&
      selectedAction.value.actionName?.includes('临时解决')
    ) {
      transitPayload.plannedFullResolveAt =
        transitForm.plannedFullResolveAt?.trim() || devInfoForm.plannedFullResolveAt?.trim()
    }
    await transitTicket(ticketId.value, transitPayload)
    processDialogVisible.value = false
    selectedAction.value = null
    // 仅在详情刷新成功后再提示成功，避免流转已成功但详情接口失败时出现「操作成功」与报错并存
    await loadAll()
    notifySuccess('操作成功')
    await redirectToBugReportEditIfNeeded(guidedTargetStatus, guidedActionName)
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

async function handleRollbackLastStep(): Promise<void> {
  await confirmAction(
    '确认回退到上一步吗？系统会同时恢复到上一步状态和上一步处理人。',
    '回退上一步',
  )
  submitLoading.value = true
  try {
    await returnTicket(ticketId.value, {
      reason: '误操作回退上一步',
    })
    await loadAll()
    notifySuccess('已回退到上一步')
  } finally {
    submitLoading.value = false
  }
}

function openUrgeDialog(): void {
  urgeExtraNotifyUserIds.value = []
  urgeDialogVisible.value = true
}

async function handleUrgeSubmit(): Promise<void> {
  const defaultSet = new Set(urgeDefaultNotifyUserIds.value)
  const extra = urgeExtraNotifyUserIds.value.filter((id) => !defaultSet.has(id))
  submitLoading.value = true
  try {
    await urgeTicket(ticketId.value, { extraNotifyUserIds: extra })
    notifySuccess('催办通知已发送')
    urgeDialogVisible.value = false
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
    const fromHtml = extractMentionUserIdsFromCommentHtml(commentContent)
    const mergedMentions = Array.from(
      new Set([...commentMentionUserIds.value, ...fromHtml]),
    )
    await addTicketComment(ticketId.value, {
      content: commentContent,
      mentionedUserIds: mergedMentions.length > 0 ? mergedMentions : undefined,
    })
    notifySuccess('评论发表成功')
    commentInput.value = ''
    commentMentionUserIds.value = []
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

const ATTACHMENT_ACCEPT =
  'image/jpeg,image/jpg,image/png,image/gif,image/webp,image/bmp,.xls,.xlsx,.txt,.csv,.pdf,.doc,.docx,.zip,.rar,.7z,.mp4,.mov,.avi,.webm,.mkv,.wmv,.m4v'

async function handleAttachmentUpload(uploadFile: { raw: File }): Promise<void> {
  if (!uploadFile?.raw) {
    return
  }
  attachmentUploadLoading.value = true
  try {
    await uploadTicketImage(ticketId.value, uploadFile.raw, 'attachment')
    notifySuccess('附件上传成功')
    await refreshTicketAttachmentsOnly()
  } finally {
    attachmentUploadLoading.value = false
    if (attachmentUploadRef.value) {
      ;(attachmentUploadRef.value as { clearFiles: () => void }).clearFiles()
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

function isVideoFile(fileType?: string): boolean {
  if (!fileType) return false
  return fileType.startsWith('video/')
}

function openVideoInNewTab(filePath?: string): void {
  if (filePath) {
    window.open(filePath, '_blank')
  }
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

function openAttachmentPreview(filePath?: string): void {
  if (!filePath) {
    return
  }
  attachmentPreviewUrl.value = filePath
  attachmentPreviewVisible.value = true
}

function handleCommentImageClick(event: MouseEvent): void {
  const target = event.target as HTMLElement
  if (target.tagName === 'IMG') {
    const src = (target as HTMLImageElement).src
    if (src) {
      openAttachmentPreview(src)
    }
  }
}

function getStatusLabel(status?: string): string {
  return getTicketStatusLabel(status)
}

function isBugReportPendingFill(row: TicketBugReportOutput): boolean {
  const code = (row.status || '').trim().toUpperCase()
  if (code === 'DRAFT') {
    return true
  }
  return (row.statusLabel || '').trim() === '待填写'
}

function shouldRedirectToBugReportEdit(targetStatusRaw?: string, actionNameRaw?: string): boolean {
  const normalized = normalizeTicketStatusCode(targetStatusRaw)
  const actionName = (actionNameRaw || '').trim()
  // 需求指定的两个动作：处理完成 / 临时解决确认（兼容历史动作名“验证完成”）。
  if (
    actionName.includes('处理完成') ||
    actionName.includes('临时解决确认') ||
    actionName.includes('验证完成')
  ) {
    return true
  }
  // 若后端未返回动作名称，降级按终态判断，避免因数据缺失导致不跳转。
  if (!actionName) {
    return normalized === 'temp_resolved' || normalized === 'completed'
  }
  return false
}

/**
 * 处理完成链路后直接进入简报编辑：
 * - 优先打开已自动生成的「待填写」简报草稿
 * - 若暂无草稿，则跳到新建页并把当前工单ID带过去
 */
async function redirectToBugReportEditIfNeeded(
  targetStatusRaw?: string,
  actionNameRaw?: string,
): Promise<void> {
  if (!isDefectWorkflow.value) {
    return
  }
  if (!shouldRedirectToBugReportEdit(targetStatusRaw, actionNameRaw)) {
    return
  }
  const pendingReportId = (detail.value?.bugReports ?? []).find(isBugReportPendingFill)?.id
  if (pendingReportId && pendingReportId > 0) {
    await router.push(`/bug-report/edit/${pendingReportId}`)
    return
  }
  await router.push({
    path: '/bug-report/edit',
    query: { ticketId: String(ticketId.value) },
  })
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

interface AvatarColor { bg: string; text: string }

const AVATAR_COLORS: AvatarColor[] = [
  { bg: '#e8f0fe', text: '#1675d1' },
  { bg: '#fef0e6', text: '#e6720a' },
  { bg: '#e6f9ee', text: '#17a34a' },
  { bg: '#fce8ec', text: '#dc2626' },
  { bg: '#f3e8ff', text: '#7c3aed' },
  { bg: '#e0f2fe', text: '#0284c7' },
]

const DEFAULT_AVATAR_COLOR: AvatarColor = { bg: '#e8f0fe', text: '#1675d1' }

function getAvatarColor(name: string): AvatarColor {
  if (!name) return DEFAULT_AVATAR_COLOR
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length] ?? DEFAULT_AVATAR_COLOR
}

onMounted(async () => {
  if (typeof window !== 'undefined') {
    updateViewportWidth()
    window.addEventListener('resize', updateViewportWidth, { passive: true })
    document.addEventListener('pointerdown', onDocumentPointerDownMention, true)
    document.addEventListener('keydown', onKeydownMention, true)
  }
  await Promise.all([loadAll(), trackReadSilently(), loadModules()])
})

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', updateViewportWidth)
    document.removeEventListener('pointerdown', onDocumentPointerDownMention, true)
    document.removeEventListener('keydown', onKeydownMention, true)
  }
  if (mentionSearchTimer) {
    clearTimeout(mentionSearchTimer)
    mentionSearchTimer = null
  }
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
          <el-button
            v-if="canShowRollbackLastStep"
            size="small"
            type="warning"
            plain
            :loading="submitLoading"
            @click="handleRollbackLastStep"
          >
            回退上一步
          </el-button>
          <template v-if="currentStatus === 'pending_assign' || currentStatus === 'alert_triggered'">
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
            {{ isDefectWorkflow && currentStatus === 'testing' ? '追加处理人' : '转派' }}
          </el-button>
          <el-button
            v-if="canShowUrgeTicket"
            size="small"
            type="danger"
            plain
            @click="openUrgeDialog"
          >
            催办
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
          <div v-if="descriptionDisplayHtml" class="description-block">
            <div class="block-label">描述</div>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <div class="description-content" v-html="descriptionDisplayHtml" />
          </div>

          <!-- 主 Tab -->
          <el-tabs v-model="activeMainTab" class="main-tabs">
            <el-tab-pane label="详细信息" name="detail">
              <el-tabs v-model="activeBugTab" class="inner-tabs bug-info-tabs">
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
                  <el-form
                    :label-width="isCompactLayout ? 'auto' : '120px'"
                    :label-position="isCompactLayout ? 'top' : 'right'"
                    class="info-form"
                  >
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
                  <el-form
                    :label-width="isCompactLayout ? 'auto' : '120px'"
                    :label-position="isCompactLayout ? 'top' : 'right'"
                    class="info-form"
                  >
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
                  <el-form
                    :label-width="isCompactLayout ? 'auto' : '120px'"
                    :label-position="isCompactLayout ? 'top' : 'right'"
                    class="info-form"
                  >
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
                    <el-form-item label="计划彻底解决">
                      <el-date-picker
                        v-model="devInfoForm.plannedFullResolveAt"
                        type="datetime"
                        value-format="YYYY-MM-DDTHH:mm:ss"
                        placeholder="临时解决时必填，可选填以提前维护"
                        :disabled="!canEditDevInfo"
                        style="width: 100%"
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
                    :compact="isCompactLayout"
                    :status-label-fn="getStatusLabel"
                    :role-label-fn="formatRoleLabel"
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
                          （{{ formatRoleLabel(record.operatorRole) }}）
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
        ref="attachmentUploadRef"
        class="attachment-upload-dropzone"
        drag
        :show-file-list="false"
        :accept="ATTACHMENT_ACCEPT"
        :on-change="handleAttachmentUpload"
        :auto-upload="false"
      >
        <div class="upload-drag-content">
          <el-icon class="upload-drag-icon"><Plus /></el-icon>
          <div class="upload-drag-text">将文件拖到此处触发上传</div>
          <div class="upload-drag-subtext">
            或点击按钮上传附件（图片、Excel、文本、PDF、Word、压缩包、视频等，单文件最大 200MB）
          </div>
          <el-button type="primary" size="small" plain :loading="attachmentUploadLoading">
            点击上传
          </el-button>
        </div>
      </el-upload>
      <EmptyState
        v-if="!detail?.attachments?.length"
        description="暂无附件，可拖拽或点击上方区域上传文件"
      />
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
            <video
              v-else-if="isVideoFile(attachment.fileType)"
              :src="attachment.filePath"
              controls
              class="attachment-video-thumbnail"
              preload="metadata"
            />
            <el-icon v-else class="attachment-icon"><DocumentOutlined /></el-icon>
          </div>
          <div class="attachment-info">
            <div class="attachment-name-row">
              <div class="attachment-name" :title="attachment.fileName">{{ attachment.fileName }}</div>
              <div class="attachment-actions attachment-actions-inline">
                <el-button
                  v-if="isImageFile(attachment.fileType) && attachment.filePath"
                  type="primary"
                  link
                  size="small"
                  @click="openAttachmentPreview(attachment.filePath)"
                >查看</el-button>
                <el-button
                  v-if="isVideoFile(attachment.fileType) && attachment.filePath"
                  type="primary"
                  link
                  size="small"
                  @click="openVideoInNewTab(attachment.filePath)"
                >播放</el-button>
                <el-button
                  v-if="
                    attachment.filePath &&
                    !isImageFile(attachment.fileType) &&
                    !isVideoFile(attachment.fileType)
                  "
                  type="primary"
                  link
                  size="small"
                  tag="a"
                  :href="attachment.filePath"
                  target="_blank"
                  rel="noopener noreferrer"
                >下载</el-button>
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
            <div class="attachment-meta attachment-meta-primary">
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
            </div>
            <div class="attachment-meta attachment-meta-secondary">
              <span class="meta-label">上传人：</span>
              <span>{{ attachment.uploadedByName || '-' }}</span>
              <span class="meta-divider">·</span>
              <span class="meta-label">上传时间：</span>
              <span>{{ formatDateTime(attachment.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 关联Bug简报 -->
    <el-card
      v-if="detail?.bugReports?.length"
      id="ticket-related-briefing"
      shadow="never"
      class="section-card"
    >
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
            ref="commentEditorRef"
            v-model="commentInput"
            :ticket-id="ticketId"
            placeholder="发表评论（支持粘贴图片、表格；输入 @ 可搜索同事）..."
            :height="180"
            :auto-grow="true"
            :max-height="480"
            :mention-trigger="true"
            class="comment-editor"
            @mention-panel="onEditorMentionPanel"
          />
          <Teleport to="body">
            <div
              v-show="mentionPanelOpen"
              class="comment-mention-float el-popper is-pure is-light"
              :style="mentionFloatPanelStyle"
              role="listbox"
              aria-label="提及同事"
            >
              <div class="comment-mention-float-inner">
                <div class="comment-mention-float-head">
                  <span class="comment-mention-float-title">提及人或内容</span>
                  <div class="comment-mention-float-tabs">
                    <button
                      type="button"
                      :class="['cmt-tab', mentionTab === 'people' ? 'is-active' : '']"
                      @mousedown.prevent
                      @click="mentionTab = 'people'"
                    >
                      人
                    </button>
                    <span class="cmt-tab-sep" />
                    <button
                      type="button"
                      :class="['cmt-tab', mentionTab === 'content' ? 'is-active' : '']"
                      @mousedown.prevent
                      @click="mentionTab = 'content'"
                    >
                      内容
                    </button>
                  </div>
                </div>
                <template v-if="mentionTab === 'people'">
                  <el-scrollbar max-height="220px" class="comment-mention-scroll">
                    <div v-if="mentionLoading" class="comment-mention-status">加载中…</div>
                    <template v-else>
                      <div
                        v-for="(u, idx) in mentionCandidates"
                        :key="u.id"
                        :class="[
                          'comment-mention-row',
                          'comment-mention-row-rich',
                          idx === mentionActiveIndex ? 'is-active' : '',
                        ]"
                        @mouseenter="mentionActiveIndex = idx"
                        @mousedown.prevent
                        @click="pickMentionUser(u)"
                      >
                        <el-avatar :size="32" :src="u.avatarUrl || undefined" class="comment-mention-av">
                          {{ u.name?.charAt(0) || '?' }}
                        </el-avatar>
                        <div class="comment-mention-row-text">
                          <div class="comment-mention-primary">
                            {{ u.name }}<span class="comment-mention-id">({{ mentionDisplaySub(u) }})</span>
                          </div>
                          <div class="comment-mention-sub">{{ mentionDisplaySub(u) }}</div>
                        </div>
                      </div>
                      <div v-if="!mentionCandidates.length" class="comment-mention-status">无匹配用户</div>
                    </template>
                  </el-scrollbar>
                </template>
                <div v-else class="comment-mention-status comment-mention-content-placeholder">
                  内容提及（文档/工单链接）暂未开放，请使用「人」选择同事。
                </div>
              </div>
            </div>
          </Teleport>
          <div class="comment-submit-row">
            <div class="comment-submit-left">
              <span class="comment-hint">
                支持粘贴图片、表格。点击「@同事」可在正文插入 @，对方将收到站内通知；若已绑定企微且开启推送，会同步收到企微消息。
              </span>
              <el-popover
                v-model:visible="mentionPopoverVisible"
                placement="top-start"
                :width="300"
                trigger="click"
              >
                <template #reference>
                  <el-button type="primary" link size="small">@同事</el-button>
                </template>
                <div class="comment-mention-popover">
                  <div class="comment-mention-float-head comment-mention-popover-head">
                    <span class="comment-mention-float-title">提及人或内容</span>
                    <div class="comment-mention-float-tabs">
                      <span class="cmt-tab is-active">人</span>
                      <span class="cmt-tab-sep" />
                      <span class="cmt-tab is-muted">内容</span>
                    </div>
                  </div>
                  <p class="comment-mention-tip">
                    选择后插入 @；也可在编辑框内直接输入 @ 搜索。发表后被 @ 同事会收到站内提醒。
                  </p>
                  <el-input
                    v-model="mentionKeyword"
                    placeholder="搜索姓名/工号"
                    size="small"
                    clearable
                    @input="scheduleMentionSearch"
                  />
                  <el-scrollbar max-height="220px" class="comment-mention-scroll">
                    <div v-if="mentionLoading" class="comment-mention-status">加载中…</div>
                    <template v-else>
                      <div
                        v-for="(u, idx) in mentionCandidates"
                        :key="u.id"
                        :class="[
                          'comment-mention-row',
                          'comment-mention-row-rich',
                          idx === mentionActiveIndex ? 'is-active' : '',
                        ]"
                        @mouseenter="mentionActiveIndex = idx"
                        @click="pickMentionUser(u)"
                      >
                        <el-avatar :size="32" :src="u.avatarUrl || undefined" class="comment-mention-av">
                          {{ u.name?.charAt(0) || '?' }}
                        </el-avatar>
                        <div class="comment-mention-row-text">
                          <div class="comment-mention-primary">
                            {{ u.name }}<span class="comment-mention-id">({{ mentionDisplaySub(u) }})</span>
                          </div>
                          <div class="comment-mention-sub">{{ mentionDisplaySub(u) }}</div>
                        </div>
                      </div>
                      <div v-if="!mentionCandidates.length" class="comment-mention-status">无匹配用户</div>
                    </template>
                  </el-scrollbar>
                </div>
              </el-popover>
            </div>
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
            <el-avatar
              :size="32"
              class="user-avatar"
              :style="{ background: getAvatarColor(comment.userName ?? '').bg, color: getAvatarColor(comment.userName ?? '').text }"
            >
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
            <div v-else class="comment-content" v-html="comment.content || '-'" @click="handleCommentImageClick" />
          </div>
        </div>
      </div>
    </el-card>
  </div>

  <!-- 企微消息解析弹窗 -->
  <el-dialog
    v-model="wecomParseDialogVisible"
    title="企微消息一键解析"
    width="min(640px, 92vw)"
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
  <el-dialog v-model="assignDialogVisible" :title="assignDialogTitle" width="min(520px, 92vw)">
    <el-form label-width="100px">
      <p
        v-if="isDefectWorkflow && currentStatus === 'testing'"
        class="assign-merge-hint"
      >
        工单仍为「测试复现中」，不会变更流程状态；所选人员将加入协同处理人（主负责人不变）。
      </p>
      <el-form-item label="处理人" required>
        <el-select
          v-model="assignForm.assigneeIds"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          :placeholder="
            isDefectWorkflow && currentStatus === 'testing'
              ? '选择要加入协同的开发等人员（可多选）'
              : '可选择多名处理人，第一位为主负责人'
          "
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

  <!-- 催办：默认通知关联处理人，可追加通知人 -->
  <el-dialog v-model="urgeDialogVisible" title="催办通知" width="min(520px, 92vw)">
    <p class="urge-dialog-hint">
      将向以下关联处理人发送催办通知：<strong>{{ urgeDefaultNotifyNames }}</strong>
    </p>
    <el-form label-width="100px">
      <el-form-item label="追加通知人">
        <el-select
          v-model="urgeExtraNotifyUserIds"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          placeholder="可选，额外通知其他同事"
          style="width: 100%"
        >
          <el-option
            v-for="user in extraNotifyCandidates"
            :key="user.id"
            :label="user.name"
            :value="user.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="urgeDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleUrgeSubmit">
        确认发送
      </el-button>
    </template>
  </el-dialog>

  <!-- 待分派：仅更换对接人（不改变流程状态） -->
  <el-dialog v-model="pendingPoolTransferVisible" title="待分派 · 对接转派" width="min(480px, 92vw)">
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
    width="min(520px, 92vw)"
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
      <el-form-item
        v-if="
          isDefectWorkflow &&
          currentStatus === 'testing' &&
          selectedAction?.targetStatus === 'pending_dev_accept'
        "
        label="复现环境"
        required
      >
        <el-select v-model="transitForm.reproduceEnv" placeholder="转开发必填" style="width: 100%">
          <el-option label="生产环境" value="PRODUCTION" />
          <el-option label="测试环境" value="TEST" />
          <el-option label="均可复现" value="BOTH" />
        </el-select>
        <div class="form-hint">将同步写入测试信息，也可事先在「测试信息」页维护</div>
      </el-form-item>
      <el-form-item
        v-if="
          isDefectWorkflow &&
          selectedAction?.targetStatus === 'temp_resolved' &&
          (selectedAction?.actionName || '').includes('临时解决')
        "
        label="计划彻底解决"
        required
      >
        <el-date-picker
          v-model="transitForm.plannedFullResolveAt"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="选择计划彻底解决时间"
          style="width: 100%"
        />
        <div class="form-hint">不得早于今天；也可事先在「开发信息」中维护</div>
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
  <el-dialog v-model="closeDialogVisible" title="关闭工单" width="min(480px, 92vw)">
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

  <el-dialog
    v-model="attachmentPreviewVisible"
    title="图片预览"
    width="min(920px, 96vw)"
    append-to-body
    class="attachment-preview-dialog"
    destroy-on-close
  >
    <div class="attachment-preview-dialog-body">
      <el-image
        v-if="attachmentPreviewUrl"
        :src="attachmentPreviewUrl"
        fit="contain"
        class="attachment-preview-image"
        preview-teleported
      />
      <EmptyState v-else description="暂无可预览图片" />
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.ticket-detail-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 24px;
}

.form-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.assign-merge-hint {
  margin: 0 0 12px;
  padding: 10px 12px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  background: #f5f7fa;
  border-radius: 6px;
}

.urge-dialog-hint {
  margin: 0 0 16px;
  padding: 10px 12px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  background: #f5f7fa;
  border-radius: 6px;
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

.action-bar-left :deep(.el-button + .el-button),
.action-bar-right :deep(.el-button + .el-button) {
  margin-left: 0;
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
  background: var(--md-bg-panel, #f9fafb);
  border: 1px solid var(--md-border-light, #eef2f7);
  border-radius: 10px;
  padding: 16px;
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

.bug-info-tabs {
  :deep(.el-tabs__item) {
    font-weight: 500;
    padding: 0 12px;
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

/* 关联 Bug 简报「查看简报」：与详情页其他 plain 主按钮一致，对比度高于默认实心 */
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

.attachment-video-thumbnail {
  width: 120px;
  height: 68px;
  object-fit: cover;
  border-radius: 4px;
  background-color: #000;
}

.attachment-icon {
  font-size: 28px;
  color: #909399;
}

.attachment-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.attachment-name-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.attachment-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  word-break: break-all;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.attachment-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  row-gap: 4px;
  column-gap: 6px;
}

.attachment-meta-secondary {
  color: #606266;
}

.meta-label {
  color: #909399;
}

.meta-divider {
  color: #d4d4d4;
}

.attachment-actions {
  flex-shrink: 0;
  display: flex;
  gap: 6px;
}

.attachment-actions-inline {
  :deep(.el-button) {
    white-space: nowrap;
  }
}

.attachment-preview-dialog-body {
  min-height: 320px;
  max-height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f8fa;
  border-radius: 8px;
  padding: 12px;
}

.attachment-preview-image {
  width: 100%;
  height: 66vh;
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

  :deep(.ticket-comment-mention) {
    display: inline-block;
    margin: 0 2px;
    padding: 0 6px;
    border-radius: 4px;
    color: #1675d1;
    font-weight: 500;
    background: #ecf5ff;
    vertical-align: baseline;
  }
}

.comment-submit-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.comment-submit-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.comment-hint {
  font-size: 12px;
  color: #c0c4cc;
}

.comment-mention-popover {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.comment-mention-popover-head {
  margin-bottom: 0;
}

.comment-mention-float {
  padding: 0;
  border-radius: 8px;
  box-shadow:
    0 6px 16px 0 rgb(0 0 0 / 8%),
    0 3px 6px -4px rgb(0 0 0 / 12%),
    0 9px 28px 8px rgb(0 0 0 / 5%);
}

.comment-mention-float-inner {
  padding: 10px 12px 12px;
  background: #fff;
  border-radius: 8px;
}

.comment-mention-float-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-mention-float-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.comment-mention-float-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.cmt-tab {
  border: none;
  background: transparent;
  padding: 2px 6px;
  border-radius: 4px;
  cursor: pointer;
  color: #909399;
  font-size: 12px;

  &.is-active {
    color: #1675d1;
    background: #ecf5ff;
    font-weight: 500;
  }

  &.is-muted {
    cursor: default;
    color: #c0c4cc;
  }
}

.cmt-tab-sep {
  width: 1px;
  height: 12px;
  background: #e4e7ed;
}

.comment-mention-content-placeholder {
  text-align: left;
  padding: 8px 4px 4px;
  line-height: 1.5;
}

.comment-mention-tip {
  margin: 0;
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
}

.comment-mention-scroll {
  margin-top: 4px;
}

.comment-mention-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 6px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;

  &:hover {
    background: #f0f9ff;
  }

  &.is-active {
    background: #e6f0fc;
  }
}

.comment-mention-row-rich {
  align-items: flex-start;
  justify-content: flex-start;
}

.comment-mention-av {
  flex-shrink: 0;
}

.comment-mention-row-text {
  flex: 1;
  min-width: 0;
}

.comment-mention-primary {
  font-size: 14px;
  font-weight: 400;
  color: #303133;
  line-height: 1.4;
}

.comment-mention-id {
  color: #606266;
  font-weight: 400;
}

.comment-mention-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.comment-mention-status {
  padding: 12px 6px;
  font-size: 12px;
  color: #909399;
  text-align: center;
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
  font-size: 13px;
  font-weight: 500;
}

.comment-body {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  background: #f5f7fa;
  border-radius: 0 8px 8px 8px;
  padding: 12px 16px;

  :deep(.ticket-comment-mention) {
    display: inline-block;
    margin: 0 2px;
    padding: 0 6px;
    border-radius: 4px;
    color: #1675d1;
    font-weight: 500;
    background: #ecf5ff;
    vertical-align: baseline;
  }
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
  overflow: hidden;
  max-width: 100%;
  box-sizing: border-box;

  :deep(p) {
    margin: 0 0 8px;
    max-width: 100%;
    overflow-wrap: break-word;
  }

  :deep(p:last-child) {
    margin-bottom: 0;
  }

  :deep(div) {
    max-width: 100%;
  }

  :deep(img) {
    display: block !important;
    max-width: 100% !important;
    width: 100% !important;
    height: auto !important;
    border-radius: 4px;
    cursor: zoom-in;
    transition: opacity 0.15s;
    object-fit: contain;

    &:hover {
      opacity: 0.85;
    }
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

@media (max-width: 1024px) {
  .detail-layout {
    gap: 16px;
  }

  .detail-sidebar {
    width: 280px;
    top: 56px;
  }
}

@media (max-width: 768px) {
  .ticket-detail-page {
    gap: 12px;
    padding-bottom: 16px;
  }

  .header-card {
    :deep(.el-card__header) {
      padding: 14px 14px 10px;
    }

    :deep(.el-card__body) {
      padding: 12px 14px 16px;
    }
  }

  .ticket-title {
    font-size: 16px;
  }

  .action-bar {
    align-items: stretch;
    padding: 10px 0 12px;
    margin-bottom: 12px;
    gap: 8px;
  }

  .action-bar-left,
  .action-bar-right {
    width: 100%;
  }

  .action-bar-left {
    gap: 6px;
  }

  .action-bar-left :deep(.el-button),
  .action-bar-right :deep(.el-button) {
    min-height: 36px;
    padding: 0 12px;
  }

  .detail-layout {
    flex-direction: column;
    gap: 12px;
  }

  .detail-sidebar {
    width: 100%;
    position: static;
    max-height: none;
    overflow: visible;
    border-left: none;
    border-top: 1px solid #f0f0f0;
    padding-left: 0;
    padding-top: 12px;
  }

  .description-block {
    margin-bottom: 12px;
    padding: 12px;
  }

  .main-tabs {
    :deep(.el-tabs__header) {
      margin-bottom: 12px;
    }
  }

  .inner-tabs {
    :deep(.el-tabs__header) {
      margin-bottom: 10px;
    }
  }

  .bug-info-tabs {
    :deep(.el-tabs__item) {
      min-height: 36px;
      padding: 0 12px;
    }
  }

  .main-tabs :deep(.el-tabs__nav-scroll),
  .inner-tabs :deep(.el-tabs__nav-scroll) {
    overflow-x: auto;
  }

  .main-tabs :deep(.el-tabs__nav),
  .inner-tabs :deep(.el-tabs__nav) {
    white-space: nowrap;
  }

  .info-form {
    :deep(.el-form-item) {
      margin-bottom: 14px;
    }

    :deep(.el-form-item__label:not(.is-top)) {
      width: 88px !important;
      font-size: 13px;
    }

    :deep(.el-form-item__content:not(.is-top)) {
      margin-left: 88px !important;
    }
  }

  .tab-actions {
    margin-top: 10px;
    padding-top: 10px;
  }

  .wecom-parse-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    padding: 10px 12px;
  }

  .wecom-parse-hint {
    line-height: 1.5;
  }

  .selected-screenshot-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .flow-record-card {
    padding: 10px 12px;
  }

  .section-card {
    :deep(.el-card__header) {
      padding: 12px 14px;
    }

    :deep(.el-card__body) {
      padding: 12px 14px;
    }
  }

  .attachment-item {
    align-items: flex-start;
    padding: 10px 12px;
    gap: 10px;
  }

  .attachment-name-row {
    flex-direction: column;
    gap: 4px;
  }

  .attachment-actions-inline {
    align-self: flex-end;
  }

  .attachment-actions {
    justify-content: flex-start;
  }

  .attachment-preview-image {
    height: 56vh;
  }

  .comment-input-area {
    gap: 10px;
    align-items: stretch;
  }

  .comment-input-avatar {
    display: none;
  }

  .comment-submit-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .comment-item {
    gap: 10px;
  }

  .comment-avatar {
    display: none;
  }

  .comment-body {
    border-radius: 8px;
    padding: 10px 12px;
  }

  .comment-time {
    margin-left: 0;
  }

  .parse-example-code {
    font-size: 11px;
  }
}
</style>
