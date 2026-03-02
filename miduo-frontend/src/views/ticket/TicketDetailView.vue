<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import {
  assignTicket,
  closeTicket,
  followTicket,
  getTicketDetail,
  getTicketNodeDuration,
  getTicketTimeTrack,
  processTicket,
  trackTicketRead,
  unfollowTicket,
  updateBugCustomerInfo,
  updateBugDevInfo,
  updateBugTestInfo,
} from '@/api/ticket'
import { getUserList } from '@/api/user'
import EmptyState from '@/components/common/EmptyState.vue'
import { useAuthStore } from '@/stores/auth'
import type {
  TicketBugCustomerInfoInput,
  TicketBugDevInfoInput,
  TicketBugTestInfoInput,
  TicketDetailOutput,
  TicketNodeDurationItem,
  TicketTimeTrackItem,
} from '@/types/ticket'
import type { UserListOutput } from '@/types/user'
import { notifySuccess } from '@/utils/feedback'
import { formatDateTime, formatFileSize } from '@/utils/formatter'

const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const detail = ref<TicketDetailOutput>()
const users = ref<UserListOutput[]>([])

const activeBugTab = ref('customer')
const timeTrackItems = ref<TicketTimeTrackItem[]>([])
const nodeDurationItems = ref<TicketNodeDurationItem[]>([])
const bugSubmitLoading = ref(false)

const assignDialogVisible = ref(false)
const processDialogVisible = ref(false)
const closeDialogVisible = ref(false)
const submitLoading = ref(false)

const assignForm = reactive({
  assigneeId: undefined as number | undefined,
  remark: '',
})

const processForm = reactive({
  targetStatus: 'processing',
  targetUserId: undefined as number | undefined,
  remark: '',
})

const closeForm = reactive({
  remark: '',
})

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
  reproduceScreenshot: '',
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
const currentStatus = computed(() => String(detail.value?.status || '').toUpperCase())

const customFieldEntries = computed(() => {
  if (!detail.value?.customFields) {
    return []
  }
  return Object.entries(detail.value.customFields).map(([key, value]) => ({ key, value }))
})

const canEditCustomerInfo = computed(() => {
  if (hasRole('ADMIN', 'TICKET_ADMIN')) {
    return true
  }
  if (!['PENDING_DISPATCH', 'PENDING_TEST', 'PENDING_TEST_ACCEPT', 'PENDING_CS_CONFIRM'].includes(currentStatus.value)) {
    return false
  }
  if (hasRole('CUSTOMER_SERVICE', 'SUBMITTER')) {
    return true
  }
  return Boolean(currentUserId.value && currentUserId.value === detail.value?.creatorId)
})

const canEditTestInfo = computed(() => {
  if (hasRole('ADMIN', 'TICKET_ADMIN')) {
    return true
  }
  if (!['PENDING_TEST', 'PENDING_TEST_ACCEPT', 'TESTING', 'PENDING_VERIFY'].includes(currentStatus.value)) {
    return false
  }
  if (hasRole('TESTER')) {
    return true
  }
  if (hasRole('HANDLER')) {
    return true
  }
  return Boolean(currentUserId.value && currentUserId.value === detail.value?.assigneeId)
})

const canEditDevInfo = computed(() => {
  if (hasRole('ADMIN', 'TICKET_ADMIN')) {
    return true
  }
  if (!['PENDING_DEV', 'PENDING_DEV_ACCEPT', 'DEVELOPING', 'PENDING_VERIFY'].includes(currentStatus.value)) {
    return false
  }
  if (hasRole('DEVELOPER')) {
    return true
  }
  if (hasRole('HANDLER')) {
    return true
  }
  return Boolean(currentUserId.value && currentUserId.value === detail.value?.assigneeId)
})

function hasRole(...targets: string[]): boolean {
  return targets.some((target) => roleCodes.value.includes(target))
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

  Object.assign(testInfoForm, {
    reproduceEnv: ticketDetail.bugTestInfo?.reproduceEnv || '',
    reproduceSteps: ticketDetail.bugTestInfo?.reproduceSteps || '',
    actualResult: ticketDetail.bugTestInfo?.actualResult || '',
    impactScope: ticketDetail.bugTestInfo?.impactScope || '',
    severityLevel: ticketDetail.bugTestInfo?.severityLevel || '',
    moduleName: ticketDetail.bugTestInfo?.moduleName || '',
    reproduceScreenshot: ticketDetail.bugTestInfo?.reproduceScreenshot || '',
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
    nodeDurationItems.value = nodeOutput.nodes || []
    fillBugForms(ticketDetail)
  } finally {
    loading.value = false
  }
}

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

async function handleAssign(): Promise<void> {
  if (!assignForm.assigneeId) {
    return
  }
  submitLoading.value = true
  try {
    await assignTicket(ticketId.value, {
      assigneeId: assignForm.assigneeId,
      remark: assignForm.remark,
    })
    notifySuccess('工单分派成功')
    assignDialogVisible.value = false
    await loadAll()
  } finally {
    submitLoading.value = false
  }
}

async function handleProcess(): Promise<void> {
  submitLoading.value = true
  try {
    await processTicket(ticketId.value, processForm)
    notifySuccess('工单处理成功')
    processDialogVisible.value = false
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

async function saveCustomerInfo(): Promise<void> {
  bugSubmitLoading.value = true
  try {
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

function formatDuration(seconds?: number): string {
  if (seconds === undefined || seconds === null) {
    return '-'
  }
  const total = Math.max(0, Math.floor(seconds))
  const hour = Math.floor(total / 3600)
  const minute = Math.floor((total % 3600) / 60)
  const second = total % 60
  if (hour > 0) {
    return `${hour}h ${minute}m ${second}s`
  }
  if (minute > 0) {
    return `${minute}m ${second}s`
  }
  return `${second}s`
}

onMounted(async () => {
  await Promise.all([loadAll(), trackReadSilently()])
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
  <el-space direction="vertical" fill :size="16">
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="detail-header">
          <div>
            <div class="ticket-title">{{ detail?.title || '工单详情' }}</div>
            <div class="ticket-no">工单编号：{{ detail?.ticketNo || '-' }}</div>
          </div>
          <el-space>
            <el-button type="primary" @click="processDialogVisible = true">处理</el-button>
            <el-button type="warning" @click="assignDialogVisible = true">转派</el-button>
            <el-button type="danger" @click="closeDialogVisible = true">关闭</el-button>
            <el-button @click="toggleFollow">{{
              detail?.isFollowed ? '取消关注' : '关注工单'
            }}</el-button>
          </el-space>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="分类">{{ detail?.categoryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{
          detail?.statusLabel || detail?.status || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="优先级">
          {{ detail?.priorityLabel || detail?.priority || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="处理人">{{
          detail?.assigneeName || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ detail?.creatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{
          detail?.sourceLabel || detail?.source || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{
          formatDateTime(detail?.createTime)
        }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{
          formatDateTime(detail?.updateTime)
        }}</el-descriptions-item>
        <el-descriptions-item label="期望完成时间">
          {{ formatDateTime(detail?.expectedTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="模板">{{ detail?.templateName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{
          detail?.description || '-'
        }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="activeBugTab">
        <el-tab-pane label="客服信息" name="customer">
          <el-form label-width="120px">
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
              <el-input
                v-model="customerInfoForm.problemScreenshot"
                placeholder="填写截图URL，多个可用逗号分隔"
                :disabled="!canEditCustomerInfo"
              />
            </el-form-item>
          </el-form>
          <div class="tab-actions">
            <el-button
              type="primary"
              :disabled="!canEditCustomerInfo"
              :loading="bugSubmitLoading"
              @click="saveCustomerInfo"
              >保存客服信息</el-button
            >
          </div>
        </el-tab-pane>

        <el-tab-pane label="测试信息" name="test">
          <el-form label-width="120px">
            <el-form-item label="复现环境">
              <el-select v-model="testInfoForm.reproduceEnv" :disabled="!canEditTestInfo">
                <el-option label="生产环境" value="PRODUCTION" />
                <el-option label="测试环境" value="TEST" />
                <el-option label="均可复现" value="BOTH" />
              </el-select>
            </el-form-item>
            <el-form-item label="复现步骤">
              <el-input
                v-model="testInfoForm.reproduceSteps"
                type="textarea"
                :rows="3"
                :disabled="!canEditTestInfo"
              />
            </el-form-item>
            <el-form-item label="实际结果">
              <el-input
                v-model="testInfoForm.actualResult"
                type="textarea"
                :rows="3"
                :disabled="!canEditTestInfo"
              />
            </el-form-item>
            <el-form-item label="影响范围">
              <el-select v-model="testInfoForm.impactScope" :disabled="!canEditTestInfo">
                <el-option label="单一商户" value="SINGLE" />
                <el-option label="部分商户" value="PARTIAL" />
                <el-option label="全部商户" value="ALL" />
              </el-select>
            </el-form-item>
            <el-form-item label="缺陷等级">
              <el-select v-model="testInfoForm.severityLevel" :disabled="!canEditTestInfo">
                <el-option label="致命" value="FATAL" />
                <el-option label="严重" value="CRITICAL" />
                <el-option label="一般" value="NORMAL" />
                <el-option label="轻微" value="MINOR" />
              </el-select>
            </el-form-item>
            <el-form-item label="所属模块">
              <el-input v-model="testInfoForm.moduleName" :disabled="!canEditTestInfo" />
            </el-form-item>
            <el-form-item label="复现截图">
              <el-input
                v-model="testInfoForm.reproduceScreenshot"
                placeholder="填写截图URL，多个可用逗号分隔"
                :disabled="!canEditTestInfo"
              />
            </el-form-item>
            <el-form-item label="测试备注">
              <el-input
                v-model="testInfoForm.testRemark"
                type="textarea"
                :rows="3"
                :disabled="!canEditTestInfo"
              />
            </el-form-item>
          </el-form>
          <div class="tab-actions">
            <el-button
              type="primary"
              :disabled="!canEditTestInfo"
              :loading="bugSubmitLoading"
              @click="saveTestInfo"
              >保存测试信息</el-button
            >
          </div>
        </el-tab-pane>

        <el-tab-pane label="开发信息" name="dev">
          <el-form label-width="120px">
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
              <el-input
                v-model="devInfoForm.devRemark"
                type="textarea"
                :rows="3"
                :disabled="!canEditDevInfo"
              />
            </el-form-item>
          </el-form>
          <div class="tab-actions">
            <el-button
              type="primary"
              :disabled="!canEditDevInfo"
              :loading="bugSubmitLoading"
              @click="saveDevInfo"
              >保存开发信息</el-button
            >
          </div>
        </el-tab-pane>

        <el-tab-pane label="时间追踪" name="track">
          <div class="track-block">
            <div class="section-title">时间链</div>
            <EmptyState v-if="!timeTrackItems.length" description="暂无时间追踪记录" />
            <el-timeline v-else>
              <el-timeline-item
                v-for="item in timeTrackItems"
                :key="item.id"
                :timestamp="formatDateTime(item.timestamp)"
              >
                <div class="track-item">
                  <div class="track-title">
                    {{ item.actionLabel || item.action || '-' }}
                    <span class="track-user">（{{ item.userName || '-' }}）</span>
                  </div>
                  <div class="track-meta">
                    状态：{{ item.fromStatus || '-' }} → {{ item.toStatus || '-' }}
                  </div>
                  <div class="track-meta" v-if="item.fromUserName || item.toUserName">
                    处理人：{{ item.fromUserName || '-' }} → {{ item.toUserName || '-' }}
                  </div>
                  <div class="track-meta" v-if="item.isFirstRead">首次阅读：是</div>
                  <div class="track-meta" v-if="item.remark">备注：{{ item.remark }}</div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>

          <div class="track-block">
            <div class="section-title">节点耗时统计</div>
            <EmptyState v-if="!nodeDurationItems.length" description="暂无节点耗时数据" />
            <el-table
              v-else
              :data="nodeDurationItems"
              :border="false"
              :stripe="true"
              :header-cell-style="{ backgroundColor: '#f5f7fa' }"
            >
              <el-table-column prop="nodeName" label="节点" align="center" min-width="130" />
              <el-table-column prop="assigneeName" label="处理人" align="center" min-width="120" />
              <el-table-column prop="assigneeRole" label="角色" align="center" min-width="120" />
              <el-table-column label="到达时间" align="center" min-width="170">
                <template #default="{ row }">
                  {{ formatDateTime(row.arriveAt) }}
                </template>
              </el-table-column>
              <el-table-column label="首次阅读" align="center" min-width="170">
                <template #default="{ row }">
                  {{ formatDateTime(row.firstReadAt) }}
                </template>
              </el-table-column>
              <el-table-column label="开始处理" align="center" min-width="170">
                <template #default="{ row }">
                  {{ formatDateTime(row.startProcessAt) }}
                </template>
              </el-table-column>
              <el-table-column label="离开时间" align="center" min-width="170">
                <template #default="{ row }">
                  {{ formatDateTime(row.leaveAt) }}
                </template>
              </el-table-column>
              <el-table-column label="等待耗时" align="center" min-width="110">
                <template #default="{ row }">
                  {{ formatDuration(row.waitDurationSec) }}
                </template>
              </el-table-column>
              <el-table-column label="处理耗时" align="center" min-width="110">
                <template #default="{ row }">
                  {{ formatDuration(row.processDurationSec) }}
                </template>
              </el-table-column>
              <el-table-column label="总耗时" align="center" min-width="110">
                <template #default="{ row }">
                  {{ formatDuration(row.totalDurationSec) }}
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="section-title">自定义字段</div>
      </template>
      <EmptyState v-if="customFieldEntries.length === 0" description="暂无自定义字段" />
      <el-table
        v-else
        :data="customFieldEntries"
        :border="false"
        :stripe="true"
        :header-cell-style="{ backgroundColor: '#f5f7fa' }"
      >
        <el-table-column prop="key" label="字段名" align="center" />
        <el-table-column prop="value" label="字段值" align="center" />
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
        <el-table-column prop="fileName" label="文件名" align="center" min-width="220" />
        <el-table-column prop="fileType" label="类型" align="center" width="120" />
        <el-table-column label="大小" align="center" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="uploadedByName" label="上传人" align="center" width="120" />
        <el-table-column label="上传时间" align="center" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="section-title">评论与处理记录</div>
      </template>
      <EmptyState v-if="!detail?.comments?.length" description="暂无评论记录" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="comment in detail.comments"
          :key="comment.id"
          :timestamp="formatDateTime(comment.createTime)"
        >
          <div class="comment-item">
            <div class="comment-title">{{ comment.userName || '-' }}</div>
            <div class="comment-content">{{ comment.content || '-' }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </el-space>

  <el-dialog v-model="assignDialogVisible" title="分派工单" width="480px">
    <el-form label-width="90px">
      <el-form-item label="处理人" required>
        <el-select v-model="assignForm.assigneeId" placeholder="请选择处理人">
          <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="assignForm.remark" type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="assignDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleAssign">确认分派</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="processDialogVisible" title="处理工单" width="520px">
    <el-form label-width="90px">
      <el-form-item label="目标状态" required>
        <el-select v-model="processForm.targetStatus">
          <el-option label="处理中" value="processing" />
          <el-option label="待验收" value="pending_accept" />
          <el-option label="已完成" value="resolved" />
          <el-option label="挂起" value="suspended" />
        </el-select>
      </el-form-item>
      <el-form-item label="目标处理人">
        <el-select v-model="processForm.targetUserId" clearable placeholder="可选">
          <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="processForm.remark" type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="processDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleProcess">确认处理</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="closeDialogVisible" title="关闭工单" width="480px">
    <el-form label-width="90px">
      <el-form-item label="关闭原因">
        <el-input v-model="closeForm.remark" type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleCloseTicket"
        >确认关闭</el-button
      >
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.ticket-title {
  font-size: 18px;
  font-weight: 600;
}

.ticket-no {
  color: #909399;
  margin-top: 6px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

.tab-actions {
  display: flex;
  justify-content: flex-end;
}

.track-block + .track-block {
  margin-top: 16px;
}

.track-item {
  background: #f8fafc;
  border-radius: 6px;
  padding: 8px 12px;
}

.track-title {
  font-weight: 600;
}

.track-user {
  font-weight: 400;
  color: #606266;
}

.track-meta {
  margin-top: 4px;
  color: #606266;
}

.comment-item {
  background: #f8fafc;
  border-radius: 6px;
  padding: 8px 12px;
}

.comment-title {
  font-weight: 500;
}

.comment-content {
  margin-top: 4px;
  color: #606266;
}
</style>
