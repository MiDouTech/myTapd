<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  createBugReport,
  getBugReportDetail,
  submitBugReport,
  updateBugReport,
} from '@/api/bugreport'
import { getTicketPage } from '@/api/ticket'
import { getUserList } from '@/api/user'
import type {
  BugReportCreateInput,
  BugReportDetailOutput,
  BugReportRelatedTicketOutput,
  BugReportUpdateInput,
  DefectCategoryOutput,
  LogicCauseTreeOutput,
} from '@/types/bugreport'
import type { TicketListOutput } from '@/types/ticket'
import type { UserListOutput } from '@/types/user'
import { notifySuccess, notifyWarning } from '@/utils/feedback'
import { getCachedDefectCategoryDict, getCachedLogicCauseDict } from '@/utils/bugreport-dict-cache'
import { getBugReportStatusLabel, getBugReportStatusTagType, isBugReportEditable } from '@/utils/bugreport'

interface TicketOption {
  value: number
  label: string
}

interface LogicCauseOption {
  value: string
  label: string
  children?: LogicCauseOption[]
}

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitLoading = ref(false)
const users = ref<UserListOutput[]>([])
const defectCategories = ref<DefectCategoryOutput[]>([])
const logicCauseTree = ref<LogicCauseTreeOutput[]>([])
const ticketOptions = ref<TicketOption[]>([])
const ticketLoading = ref(false)
const currentStatus = ref('DRAFT')

let ticketSearchTimer: ReturnType<typeof setTimeout> | null = null
let ticketSearchToken = 0

const form = reactive({
  problemDesc: '',
  logicCausePath: [] as string[],
  logicCauseDetail: '',
  defectCategory: '',
  introducedProject: '',
  dateRange: [] as string[],
  solution: '',
  impactScope: '',
  severityLevel: 'P2',
  reporterId: undefined as number | undefined,
  reviewerId: undefined as number | undefined,
  remark: '',
  ticketIds: [] as number[],
  responsibleUserIds: [] as number[],
  autoPrefill: true,
})

const reportId = computed(() => {
  const value = Number(route.params.id)
  return Number.isFinite(value) && value > 0 ? value : 0
})

const isEditMode = computed(() => Boolean(reportId.value))
const canEdit = computed(() => !isEditMode.value || isBugReportEditable(currentStatus.value))

const pageTitle = computed(() => (isEditMode.value ? '编辑Bug简报' : '新建Bug简报'))

const logicCauseOptions = computed<LogicCauseOption[]>(() => {
  const convert = (nodes: LogicCauseTreeOutput[]): LogicCauseOption[] =>
    nodes.map((item) => ({
      value: item.name,
      label: item.name,
      children: item.children?.length ? convert(item.children) : undefined,
    }))
  return convert(logicCauseTree.value || [])
})

function buildTicketLabel(ticketNo?: string, title?: string): string {
  if (ticketNo && title) {
    return `${ticketNo} - ${title}`
  }
  if (ticketNo) {
    return ticketNo
  }
  if (title) {
    return title
  }
  return '-'
}

function mergeTicketOptionsByDetail(items?: BugReportRelatedTicketOutput[]): void {
  if (!items?.length) {
    return
  }
  const optionMap = new Map<number, TicketOption>()
  ticketOptions.value.forEach((item) => optionMap.set(item.value, item))
  items.forEach((item) => {
    if (!item.ticketId) {
      return
    }
    optionMap.set(item.ticketId, {
      value: item.ticketId,
      label: buildTicketLabel(item.ticketNo, item.title),
    })
  })
  ticketOptions.value = Array.from(optionMap.values())
}

function mergeTicketOptionsBySearch(items?: TicketListOutput[]): void {
  if (!items?.length) {
    return
  }
  const optionMap = new Map<number, TicketOption>()
  ticketOptions.value.forEach((item) => optionMap.set(item.value, item))
  items.forEach((item) => {
    optionMap.set(item.id, {
      value: item.id,
      label: buildTicketLabel(item.ticketNo, item.title),
    })
  })
  ticketOptions.value = Array.from(optionMap.values())
}

function toDateOnlyText(input?: string): string {
  if (!input) {
    return ''
  }
  return input.length >= 10 ? input.slice(0, 10) : input
}

function fillFormByDetail(detail: BugReportDetailOutput): void {
  form.problemDesc = detail.problemDesc || ''
  form.logicCausePath = [detail.logicCauseLevel1, detail.logicCauseLevel2].filter(Boolean) as string[]
  form.logicCauseDetail = detail.logicCauseDetail || ''
  form.defectCategory = detail.defectCategory || ''
  form.introducedProject = detail.introducedProject || ''
  const startDate = toDateOnlyText(detail.startDate)
  const resolveDate = toDateOnlyText(detail.resolveDate)
  form.dateRange = startDate && resolveDate ? [startDate, resolveDate] : []
  form.solution = detail.solution || ''
  form.impactScope = detail.impactScope || ''
  form.severityLevel = detail.severityLevel || 'P2'
  form.reporterId = detail.reporterId
  form.reviewerId = detail.reviewerId
  form.remark = detail.remark || ''
  form.ticketIds = detail.tickets?.map((item) => item.ticketId) || []
  form.responsibleUserIds = detail.responsibleUsers?.map((item) => item.userId) || []
  form.autoPrefill = false
  currentStatus.value = detail.status || 'DRAFT'
  mergeTicketOptionsByDetail(detail.tickets)
}

async function loadBaseData(): Promise<void> {
  const [userList, causeTree, categoryList] = await Promise.all([
    getUserList({}),
    getCachedLogicCauseDict(),
    getCachedDefectCategoryDict(),
  ])
  users.value = userList || []
  logicCauseTree.value = causeTree || []
  defectCategories.value = categoryList || []
}

async function loadDetail(): Promise<void> {
  if (!reportId.value) {
    return
  }
  loading.value = true
  try {
    const detail = await getBugReportDetail(reportId.value)
    fillFormByDetail(detail)
    if (!isBugReportEditable(detail.status)) {
      notifyWarning('当前简报状态不可编辑，仅可查看详情')
    }
  } finally {
    loading.value = false
  }
}

async function searchTickets(keyword: string): Promise<void> {
  const currentToken = ++ticketSearchToken
  ticketLoading.value = true
  try {
    const normalized = keyword.trim()
    const result = await getTicketPage({
      pageNum: 1,
      pageSize: 20,
      view: 'all',
      ticketNo: normalized || undefined,
      title: normalized || undefined,
    })
    if (currentToken !== ticketSearchToken) {
      return
    }
    mergeTicketOptionsBySearch(result.records || [])
  } finally {
    if (currentToken === ticketSearchToken) {
      ticketLoading.value = false
    }
  }
}

function handleTicketRemoteSearch(keyword: string): void {
  if (ticketSearchTimer) {
    clearTimeout(ticketSearchTimer)
  }
  ticketSearchTimer = setTimeout(() => {
    void searchTickets(keyword)
  }, 260)
}

function validateBeforeSave(): boolean {
  if (!form.problemDesc.trim()) {
    notifyWarning('请填写问题描述')
    return false
  }
  if (!form.defectCategory) {
    notifyWarning('请选择缺陷分类')
    return false
  }
  if (form.ticketIds.length === 0) {
    notifyWarning('请至少关联一个工单')
    return false
  }
  const [startDate, resolveDate] = form.dateRange
  if (startDate && resolveDate && startDate > resolveDate) {
    notifyWarning('开始日期不能晚于解决日期')
    return false
  }
  return true
}

function buildPayload():
  | BugReportCreateInput
  | BugReportUpdateInput {
  return {
    problemDesc: form.problemDesc.trim(),
    logicCauseLevel1: form.logicCausePath[0] || undefined,
    logicCauseLevel2: form.logicCausePath[1] || undefined,
    logicCauseDetail: form.logicCauseDetail.trim() || undefined,
    defectCategory: form.defectCategory || undefined,
    introducedProject: form.introducedProject.trim() || undefined,
    startDate: form.dateRange[0] || undefined,
    resolveDate: form.dateRange[1] || undefined,
    solution: form.solution.trim() || undefined,
    impactScope: form.impactScope.trim() || undefined,
    severityLevel: form.severityLevel || undefined,
    reporterId: form.reporterId,
    reviewerId: form.reviewerId,
    remark: form.remark.trim() || undefined,
    ticketIds: [...form.ticketIds],
    responsibleUserIds: [...form.responsibleUserIds],
    autoPrefill: form.autoPrefill,
  }
}

async function handleSaveDraft(): Promise<void> {
  if (!canEdit.value) {
    notifyWarning('当前状态不可编辑')
    return
  }
  const valid = validateBeforeSave()
  if (!valid) {
    return
  }
  submitLoading.value = true
  try {
    const payload = buildPayload()
    if (reportId.value) {
      await updateBugReport(reportId.value, payload as BugReportUpdateInput)
      notifySuccess('简报草稿保存成功')
    } else {
      const newId = await createBugReport(payload as BugReportCreateInput)
      notifySuccess('简报草稿创建成功')
      await router.replace(`/bug-report/edit/${newId}`)
    }
  } finally {
    submitLoading.value = false
  }
}

async function handleSaveAndSubmit(): Promise<void> {
  if (!canEdit.value) {
    notifyWarning('当前状态不可编辑')
    return
  }
  const valid = validateBeforeSave()
  if (!valid) {
    return
  }
  if (!form.reviewerId) {
    notifyWarning('提交审核前请先选择审核人')
    return
  }
  submitLoading.value = true
  try {
    const payload = buildPayload()
    let targetId = reportId.value
    if (targetId) {
      await updateBugReport(targetId, payload as BugReportUpdateInput)
    } else {
      targetId = await createBugReport(payload as BugReportCreateInput)
    }
    await submitBugReport(targetId, {
      reviewerId: form.reviewerId,
    })
    notifySuccess('简报提交审核成功')
    await router.push(`/bug-report/detail/${targetId}`)
  } finally {
    submitLoading.value = false
  }
}

function handleCancel(): void {
  if (reportId.value) {
    router.push(`/bug-report/detail/${reportId.value}`)
    return
  }
  router.push('/bug-report')
}

onMounted(async () => {
  await loadBaseData()
  await searchTickets('')
  if (reportId.value) {
    await loadDetail()
  }
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
  <el-card shadow="never" v-loading="loading">
    <template #header>
      <div class="header">
        <div class="title-group">
          <div class="title">{{ pageTitle }}</div>
          <div v-if="isEditMode" class="status-line">
            <span>当前状态：</span>
            <el-tag :type="getBugReportStatusTagType(currentStatus)">
              {{ getBugReportStatusLabel(currentStatus) }}
            </el-tag>
          </div>
        </div>
        <el-space>
          <el-button @click="handleCancel">取消</el-button>
          <el-button
            v-if="canEdit"
            type="primary"
            plain
            :loading="submitLoading"
            @click="handleSaveDraft"
          >
            保存草稿
          </el-button>
          <el-button
            v-if="canEdit"
            type="primary"
            :loading="submitLoading"
            @click="handleSaveAndSubmit"
          >
            保存并提交
          </el-button>
        </el-space>
      </div>
    </template>

    <el-alert
      v-if="!canEdit"
      title="当前简报状态不可编辑，请返回详情页处理流程操作。"
      type="warning"
      :closable="false"
      show-icon
      class="status-alert"
    />

    <el-form label-width="120px" :disabled="!canEdit" class="edit-form">
      <el-form-item label="问题描述" required>
        <el-input
          v-model="form.problemDesc"
          type="textarea"
          :rows="4"
          maxlength="1000"
          show-word-limit
          placeholder="请输入问题描述"
        />
      </el-form-item>

      <el-form-item label="逻辑归因">
        <el-cascader
          v-model="form.logicCausePath"
          :options="logicCauseOptions"
          :props="{
            checkStrictly: true,
            emitPath: true,
            value: 'value',
            label: 'label',
            children: 'children',
          }"
          clearable
          filterable
          class="w-520"
          placeholder="请选择逻辑归因（可选）"
        />
      </el-form-item>

      <el-form-item label="归因明细">
        <el-input
          v-model="form.logicCauseDetail"
          type="textarea"
          :rows="2"
          maxlength="500"
          show-word-limit
          placeholder="请输入归因补充说明（可选）"
        />
      </el-form-item>

      <el-form-item label="缺陷分类" required>
        <el-select
          v-model="form.defectCategory"
          clearable
          filterable
          class="w-420"
          placeholder="请选择缺陷分类"
        >
          <el-option
            v-for="item in defectCategories"
            :key="item.id"
            :label="item.name"
            :value="item.name"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="问题周期">
        <el-date-picker
          v-model="form.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="解决日期"
        />
      </el-form-item>

      <el-form-item label="引入项目">
        <el-input
          v-model="form.introducedProject"
          maxlength="100"
          show-word-limit
          placeholder="请输入引入项目（可选）"
          class="w-420"
        />
      </el-form-item>

      <el-form-item label="严重级别">
        <el-select v-model="form.severityLevel" class="w-220">
          <el-option label="P0" value="P0" />
          <el-option label="P1" value="P1" />
          <el-option label="P2" value="P2" />
          <el-option label="P3" value="P3" />
        </el-select>
      </el-form-item>

      <el-form-item label="影响范围">
        <el-input
          v-model="form.impactScope"
          type="textarea"
          :rows="2"
          maxlength="500"
          show-word-limit
          placeholder="请输入影响范围（可选）"
        />
      </el-form-item>

      <el-form-item label="解决方案">
        <el-input
          v-model="form.solution"
          type="textarea"
          :rows="3"
          maxlength="1000"
          show-word-limit
          placeholder="请输入解决方案（可选）"
        />
      </el-form-item>

      <el-form-item label="反馈人">
        <el-select
          v-model="form.reporterId"
          clearable
          filterable
          class="w-420"
          placeholder="请选择反馈人（可选）"
        >
          <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="审核人">
        <el-select
          v-model="form.reviewerId"
          clearable
          filterable
          class="w-420"
          placeholder="请选择审核人"
        >
          <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="责任人">
        <el-select
          v-model="form.responsibleUserIds"
          multiple
          clearable
          filterable
          class="w-520"
          placeholder="请选择责任人（可多选）"
        >
          <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="关联工单" required>
        <el-select
          v-model="form.ticketIds"
          multiple
          filterable
          remote
          reserve-keyword
          collapse-tags
          collapse-tags-tooltip
          class="w-640"
          :loading="ticketLoading"
          placeholder="输入工单编号或标题搜索"
          :remote-method="handleTicketRemoteSearch"
        >
          <el-option
            v-for="option in ticketOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="自动预填">
        <el-switch v-model="form.autoPrefill" />
        <span class="form-tip">开启后将根据关联工单自动补齐部分字段（不覆盖已手工填写内容）</span>
      </el-form-item>

      <el-form-item label="备注">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请输入备注（可选）"
        />
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style scoped lang="scss">
.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.title-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.status-line {
  color: #606266;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-alert {
  margin-bottom: 16px;
}

.edit-form {
  max-width: 980px;
}

.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

.w-220 {
  width: 220px;
}

.w-420 {
  width: 420px;
}

.w-520 {
  width: 520px;
}

.w-640 {
  width: 640px;
}

@media (max-width: 991px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
  }

  .w-220,
  .w-420,
  .w-520,
  .w-640 {
    width: 100%;
  }
}
</style>
