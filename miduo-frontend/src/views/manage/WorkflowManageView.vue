<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { getUserList } from '@/api/user'
import {
  createHandlerGroup,
  getHandlerGroupList,
  getWorkflowDetail,
  getWorkflowList,
  updateHandlerGroup,
  updateWorkflow,
} from '@/api/workflow'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { UserListOutput } from '@/types/user'
import type {
  HandlerGroupCreateInput,
  HandlerGroupListOutput,
  WorkflowDetailOutput,
  WorkflowListOutput,
  WorkflowUpdateInput,
} from '@/types/workflow'
import { notifySuccess, notifyWarning } from '@/utils/feedback'
import { formatDateTime } from '@/utils/formatter'

interface ListQuery {
  keyword: string
  orderBy?: string
  asc: boolean
  pageNum: number
  pageSize: number
}

const workflowLoading = ref(false)
const workflowDetailLoading = ref(false)
const handlerGroupLoading = ref(false)
const handlerGroupSubmitLoading = ref(false)
const workflowDetailVisible = ref(false)
const workflowEditVisible = ref(false)
const workflowEditLoading = ref(false)
const workflowEditSubmitLoading = ref(false)
const workflowEditingId = ref<number | null>(null)
const createHandlerGroupVisible = ref(false)
const editingHandlerGroupId = ref<number | null>(null)

const workflowList = ref<WorkflowListOutput[]>([])
const workflowDetail = ref<WorkflowDetailOutput>()
const handlerGroupList = ref<HandlerGroupListOutput[]>([])
const userOptions = ref<UserListOutput[]>([])

const workflowQuery = reactive<
  ListQuery & {
    mode: string
    isActive?: number
  }
>({
  keyword: '',
  mode: '',
  isActive: undefined,
  orderBy: undefined,
  asc: false,
  pageNum: 1,
  pageSize: 20,
})

const handlerGroupQuery = reactive<ListQuery>({
  keyword: '',
  orderBy: undefined,
  asc: false,
  pageNum: 1,
  pageSize: 20,
})

const createHandlerGroupForm = reactive<HandlerGroupCreateInput>({
  name: '',
  description: '',
  skillTags: '',
  leaderId: undefined,
  memberIds: [],
})

const workflowEditForm = reactive<WorkflowUpdateInput>({
  name: '',
  mode: 'SIMPLE',
  description: '',
  isActive: 1,
  states: [],
  transitions: [],
})

const WORKFLOW_ROLE_OPTIONS = [
  { label: 'SUBMITTER（提交人）', value: 'SUBMITTER' },
  { label: 'HANDLER（处理人）', value: 'HANDLER' },
  { label: 'ADMIN（系统管理员）', value: 'ADMIN' },
  { label: 'TICKET_ADMIN（工单管理员）', value: 'TICKET_ADMIN' },
  { label: 'OBSERVER（观察者）', value: 'OBSERVER' },
  { label: 'CUSTOMER_SERVICE（客服）', value: 'CUSTOMER_SERVICE' },
  { label: 'TESTER（测试）', value: 'TESTER' },
  { label: 'DEVELOPER（开发）', value: 'DEVELOPER' },
] as const

const WORKFLOW_ROLE_LABEL_MAP: Record<string, string> = {
  SUBMITTER: '提交人',
  HANDLER: '处理人',
  ADMIN: '系统管理员',
  TICKET_ADMIN: '工单管理员',
  OBSERVER: '观察者',
  CUSTOMER_SERVICE: '客服',
  TESTER: '测试',
  DEVELOPER: '开发',
}

const SLA_ACTION_LABEL_MAP: Record<string, string> = {
  START_RESPONSE: '开始响应计时',
  START_RESOLVE: '开始解决计时',
  PAUSE: '暂停计时',
  STOP: '停止计时',
}

const WORKFLOW_STATE_LABEL_MAP: Record<string, string> = {
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

const STATE_TYPE_OPTIONS = [
  { label: '初始 INITIAL', value: 'INITIAL' },
  { label: '中间 INTERMEDIATE', value: 'INTERMEDIATE' },
  { label: '终态 TERMINAL', value: 'TERMINAL' },
] as const

const workflowStateCodeOptions = computed(() => {
  return workflowEditForm.states
    .filter((s) => s.code.trim().length > 0)
    .map((s) => ({
      label: `${s.name || s.code}（${s.code}）`,
      value: s.code.trim(),
    }))
})

const filteredWorkflowList = computed(() => {
  const keyword = workflowQuery.keyword.trim().toLowerCase()
  return workflowList.value.filter((item) => {
    const keywordMatched =
      !keyword ||
      item.name.toLowerCase().includes(keyword) ||
      (item.description || '').toLowerCase().includes(keyword)
    const modeMatched = !workflowQuery.mode || item.mode === workflowQuery.mode
    const statusMatched =
      workflowQuery.isActive === undefined || item.isActive === workflowQuery.isActive
    return keywordMatched && modeMatched && statusMatched
  })
})

function sortRows<T extends Record<string, unknown>>(
  rows: T[],
  orderBy: string | undefined,
  asc: boolean,
): T[] {
  const result = [...rows]
  if (!orderBy) {
    return result
  }
  result.sort((a, b) => {
    const left = a[orderBy]
    const right = b[orderBy]
    if (left === right) {
      return 0
    }
    if (left === undefined || left === null) {
      return 1
    }
    if (right === undefined || right === null) {
      return -1
    }
    if (typeof left === 'number' && typeof right === 'number') {
      return asc ? left - right : right - left
    }
    const compared = String(left).localeCompare(String(right), 'zh-CN')
    return asc ? compared : -compared
  })
  return result
}

const sortedWorkflowList = computed(() => {
  return sortRows(filteredWorkflowList.value, workflowQuery.orderBy, workflowQuery.asc)
})

const workflowTotal = computed(() => filteredWorkflowList.value.length)

const pagedWorkflowList = computed(() => {
  const start = (workflowQuery.pageNum - 1) * workflowQuery.pageSize
  return sortedWorkflowList.value.slice(start, start + workflowQuery.pageSize)
})

const filteredHandlerGroupList = computed(() => {
  const keyword = handlerGroupQuery.keyword.trim().toLowerCase()
  return handlerGroupList.value.filter((item) => {
    return (
      !keyword ||
      item.name.toLowerCase().includes(keyword) ||
      (item.description || '').toLowerCase().includes(keyword) ||
      (item.skillTags || '').toLowerCase().includes(keyword)
    )
  })
})

const sortedHandlerGroupList = computed(() => {
  return sortRows(filteredHandlerGroupList.value, handlerGroupQuery.orderBy, handlerGroupQuery.asc)
})

const handlerGroupTotal = computed(() => filteredHandlerGroupList.value.length)

const pagedHandlerGroupList = computed(() => {
  const start = (handlerGroupQuery.pageNum - 1) * handlerGroupQuery.pageSize
  return sortedHandlerGroupList.value.slice(start, start + handlerGroupQuery.pageSize)
})

const detailStateRows = computed<Record<string, unknown>[]>(() => {
  return (workflowDetail.value?.states || []).map((item) => ({ ...item }))
})

const detailTransitionRows = computed<Record<string, unknown>[]>(() => {
  return (workflowDetail.value?.transitions || []).map((item) => ({ ...item }))
})

const userSelectOptions = computed(() => {
  return userOptions.value.map((item) => ({
    label: `${item.name}${item.employeeNo ? `（${item.employeeNo}）` : ''}`,
    value: item.id,
  }))
})

const handlerGroupDialogTitle = computed(() =>
  editingHandlerGroupId.value === null ? '新建处理组' : '编辑处理组',
)

function resetCreateHandlerGroupForm(): void {
  createHandlerGroupForm.name = ''
  createHandlerGroupForm.description = ''
  createHandlerGroupForm.skillTags = ''
  createHandlerGroupForm.leaderId = undefined
  createHandlerGroupForm.memberIds = []
}

function normalizeWorkflowPage(): void {
  const maxPage = Math.max(1, Math.ceil(workflowTotal.value / workflowQuery.pageSize))
  if (workflowQuery.pageNum > maxPage) {
    workflowQuery.pageNum = maxPage
  }
}

function normalizeHandlerGroupPage(): void {
  const maxPage = Math.max(1, Math.ceil(handlerGroupTotal.value / handlerGroupQuery.pageSize))
  if (handlerGroupQuery.pageNum > maxPage) {
    handlerGroupQuery.pageNum = maxPage
  }
}

async function loadWorkflows(): Promise<void> {
  workflowLoading.value = true
  try {
    workflowList.value = await getWorkflowList()
    normalizeWorkflowPage()
  } catch {
    // 请求错误由全局拦截器统一提示，这里保留原筛选条件与旧数据
  } finally {
    workflowLoading.value = false
  }
}

async function loadHandlerGroups(): Promise<void> {
  handlerGroupLoading.value = true
  try {
    handlerGroupList.value = await getHandlerGroupList()
    normalizeHandlerGroupPage()
  } catch {
    // 请求错误由全局拦截器统一提示，这里保留原筛选条件与旧数据
  } finally {
    handlerGroupLoading.value = false
  }
}

async function loadUserOptions(): Promise<void> {
  try {
    userOptions.value = await getUserList()
  } catch {
    // 请求错误由全局拦截器统一提示，这里保留原有数据
  }
}

function handleWorkflowSearch(): void {
  workflowQuery.pageNum = 1
  normalizeWorkflowPage()
}

function handleWorkflowReset(): void {
  workflowQuery.keyword = ''
  workflowQuery.mode = ''
  workflowQuery.isActive = undefined
  workflowQuery.orderBy = undefined
  workflowQuery.asc = false
  workflowQuery.pageNum = 1
}

function handleWorkflowPaginationChange(payload: { pageNum: number; pageSize: number }): void {
  workflowQuery.pageNum = payload.pageNum
  workflowQuery.pageSize = payload.pageSize
  normalizeWorkflowPage()
}

function handleHandlerGroupSearch(): void {
  handlerGroupQuery.pageNum = 1
  normalizeHandlerGroupPage()
}

function handleHandlerGroupReset(): void {
  handlerGroupQuery.keyword = ''
  handlerGroupQuery.orderBy = undefined
  handlerGroupQuery.asc = false
  handlerGroupQuery.pageNum = 1
}

function handleHandlerGroupPaginationChange(payload: { pageNum: number; pageSize: number }): void {
  handlerGroupQuery.pageNum = payload.pageNum
  handlerGroupQuery.pageSize = payload.pageSize
  normalizeHandlerGroupPage()
}

function handleWorkflowSortChange(payload: {
  prop: string
  order: 'ascending' | 'descending' | null
}): void {
  workflowQuery.orderBy = payload.order && payload.prop ? payload.prop : undefined
  workflowQuery.asc = payload.order === 'ascending'
}

function handleHandlerGroupSortChange(payload: {
  prop: string
  order: 'ascending' | 'descending' | null
}): void {
  handlerGroupQuery.orderBy = payload.order && payload.prop ? payload.prop : undefined
  handlerGroupQuery.asc = payload.order === 'ascending'
}

function handleLeaderChange(): void {
  if (
    typeof createHandlerGroupForm.leaderId === 'number' &&
    !createHandlerGroupForm.memberIds.includes(createHandlerGroupForm.leaderId)
  ) {
    createHandlerGroupForm.memberIds = [
      ...createHandlerGroupForm.memberIds,
      createHandlerGroupForm.leaderId,
    ]
  }
}

function openCreateHandlerGroupDialog(): void {
  editingHandlerGroupId.value = null
  resetCreateHandlerGroupForm()
  createHandlerGroupVisible.value = true
}

function openEditHandlerGroupDialog(row: HandlerGroupListOutput): void {
  editingHandlerGroupId.value = row.id
  createHandlerGroupForm.name = row.name
  createHandlerGroupForm.description = row.description || ''
  createHandlerGroupForm.skillTags = row.skillTags || ''
  createHandlerGroupForm.leaderId = row.leaderId
  createHandlerGroupForm.memberIds = (row.members || []).map((m) => m.userId)
  createHandlerGroupVisible.value = true
}

async function openWorkflowDetail(row: WorkflowListOutput): Promise<void> {
  workflowDetailVisible.value = true
  workflowDetailLoading.value = true
  try {
    workflowDetail.value = await getWorkflowDetail(row.id)
  } catch {
    workflowDetail.value = undefined
  } finally {
    workflowDetailLoading.value = false
  }
}

function resetWorkflowEditForm(): void {
  workflowEditingId.value = null
  workflowEditForm.name = ''
  workflowEditForm.mode = 'SIMPLE'
  workflowEditForm.description = ''
  workflowEditForm.isActive = 1
  workflowEditForm.states = []
  workflowEditForm.transitions = []
}

async function openWorkflowEdit(row: WorkflowListOutput): Promise<void> {
  if (row.isBuiltin === 1) {
    notifyWarning('内置工作流不可编辑')
    return
  }
  workflowEditVisible.value = true
  workflowEditLoading.value = true
  workflowEditingId.value = row.id
  try {
    const detail = await getWorkflowDetail(row.id)
    workflowEditForm.name = detail.name
    workflowEditForm.mode = detail.mode
    workflowEditForm.description = detail.description || ''
    workflowEditForm.isActive = detail.isActive
    workflowEditForm.states = (detail.states || []).map((s, idx) => ({
      code: s.code,
      name: s.name,
      type: s.type || 'INTERMEDIATE',
      slaAction: s.slaAction || '',
      order: s.order ?? idx,
    }))
    workflowEditForm.transitions = (detail.transitions || []).map((t) => ({
      id: t.id || '',
      from: t.from,
      to: t.to,
      name: t.name || '',
      allowedRoles: t.allowedRoles ? [...t.allowedRoles] : [],
      requireRemark: Boolean(t.requireRemark),
      allowTransfer: Boolean(t.allowTransfer),
      isReturn: Boolean(t.isReturn),
    }))
  } catch {
    resetWorkflowEditForm()
    workflowEditVisible.value = false
  } finally {
    workflowEditLoading.value = false
  }
}

function addWorkflowStateRow(): void {
  workflowEditForm.states.push({
    code: '',
    name: '',
    type: 'INTERMEDIATE',
    slaAction: '',
    order: workflowEditForm.states.length,
  })
}

function removeWorkflowStateRow(index: number): void {
  workflowEditForm.states.splice(index, 1)
}

function addWorkflowTransitionRow(): void {
  workflowEditForm.transitions.push({
    id: '',
    from: '',
    to: '',
    name: '',
    allowedRoles: [],
    requireRemark: false,
    allowTransfer: false,
    isReturn: false,
  })
}

function removeWorkflowTransitionRow(index: number): void {
  workflowEditForm.transitions.splice(index, 1)
}

async function handleWorkflowEditSubmit(): Promise<void> {
  const name = workflowEditForm.name.trim()
  if (!name) {
    notifyWarning('请输入工作流名称')
    return
  }
  if (workflowEditForm.states.length === 0) {
    notifyWarning('请至少配置一条状态')
    return
  }
  if (workflowEditForm.transitions.length === 0) {
    notifyWarning('请至少配置一条流转规则')
    return
  }
  const wid = workflowEditingId.value
  if (wid === null) {
    return
  }

  const payload: WorkflowUpdateInput = {
    name,
    mode: workflowEditForm.mode,
    description: workflowEditForm.description?.trim() || undefined,
    isActive: workflowEditForm.isActive,
    states: workflowEditForm.states.map((s, i) => ({
      code: s.code.trim(),
      name: s.name.trim(),
      type: s.type.trim(),
      slaAction: s.slaAction?.trim() || undefined,
      order: s.order ?? i,
    })),
    transitions: workflowEditForm.transitions.map((t) => ({
      id: t.id?.trim() || undefined,
      from: t.from.trim(),
      to: t.to.trim(),
      name: t.name.trim(),
      allowedRoles: t.allowedRoles && t.allowedRoles.length > 0 ? [...t.allowedRoles] : [],
      requireRemark: t.requireRemark || undefined,
      allowTransfer: t.allowTransfer || undefined,
      isReturn: t.isReturn || undefined,
    })),
  }

  workflowEditSubmitLoading.value = true
  try {
    await updateWorkflow(wid, payload)
    notifySuccess('工作流已保存')
    workflowEditVisible.value = false
    resetWorkflowEditForm()
    await loadWorkflows()
  } catch {
    // 保留表单
  } finally {
    workflowEditSubmitLoading.value = false
  }
}

async function handleCreateHandlerGroup(): Promise<void> {
  const name = createHandlerGroupForm.name.trim()
  if (!name) {
    notifyWarning('请输入处理组名称')
    return
  }
  if (createHandlerGroupForm.memberIds.length === 0) {
    notifyWarning('请至少选择一位处理组成员')
    return
  }

  const payload: HandlerGroupCreateInput = {
    name,
    description: createHandlerGroupForm.description?.trim() || undefined,
    skillTags: createHandlerGroupForm.skillTags?.trim() || undefined,
    leaderId: createHandlerGroupForm.leaderId,
    memberIds: [...createHandlerGroupForm.memberIds],
  }

  if (typeof payload.leaderId === 'number' && !payload.memberIds.includes(payload.leaderId)) {
    payload.memberIds.push(payload.leaderId)
  }

  handlerGroupSubmitLoading.value = true
  try {
    if (editingHandlerGroupId.value === null) {
      await createHandlerGroup(payload)
      notifySuccess('处理组创建成功')
    } else {
      await updateHandlerGroup(editingHandlerGroupId.value, payload)
      notifySuccess('处理组已保存')
    }
    createHandlerGroupVisible.value = false
    editingHandlerGroupId.value = null
    await loadHandlerGroups()
  } catch {
    // 失败时不关闭弹窗，保留用户输入
  } finally {
    handlerGroupSubmitLoading.value = false
  }
}

function getStatusTagType(status?: number): 'success' | 'danger' {
  return status === 1 ? 'success' : 'danger'
}

function getModeTagType(mode?: string): 'success' | 'warning' | 'info' {
  if (mode === 'SIMPLE') {
    return 'success'
  }
  if (mode === 'ADVANCED') {
    return 'warning'
  }
  return 'info'
}

function getStateTypeLabel(type?: string): string {
  if (!type) {
    return '-'
  }
  const map: Record<string, string> = {
    START: '开始',
    PROCESS: '处理中',
    END: '结束',
    NORMAL: '普通',
    INITIAL: '初始',
    INTERMEDIATE: '中间',
    TERMINAL: '终态',
  }
  return map[type] || type
}

function formatMemberNames(row: HandlerGroupListOutput): string {
  if (!row.members || row.members.length === 0) {
    return '-'
  }
  const names = row.members.map((item) => item.userName).filter(Boolean)
  return names.length > 0 ? names.join('、') : '-'
}

function formatAllowedRoles(roles?: string[]): string {
  if (!roles || roles.length === 0) {
    return '-'
  }
  return roles.map((role) => WORKFLOW_ROLE_LABEL_MAP[role] || role).join('、')
}

function getSlaActionLabel(action?: string): string {
  if (!action) {
    return '-'
  }
  return SLA_ACTION_LABEL_MAP[action] || action
}

function getWorkflowStateLabel(state?: string): string {
  if (!state) {
    return '-'
  }
  // 详情里有些工作流状态是英文状态码，这里统一翻译成中文，便于业务人员理解。
  const normalized = state.trim().toLowerCase()
  return WORKFLOW_STATE_LABEL_MAP[normalized] || state
}

onMounted(async () => {
  await Promise.all([loadWorkflows(), loadHandlerGroups(), loadUserOptions()])
})
</script>

<template>
  <div class="workflow-manage-page">
    <el-card shadow="never" class="page-card">
      <div class="toolbar">
        <div class="title">工作流管理</div>
        <el-button type="primary" link @click="loadWorkflows">刷新列表</el-button>
      </div>

      <el-form :inline="true" label-width="72px" class="query-form">
        <el-form-item label="关键字" class="query-form-item">
          <el-input
            v-model="workflowQuery.keyword"
            class="query-input"
            placeholder="请输入名称或描述"
            clearable
            @keyup.enter="handleWorkflowSearch"
          />
        </el-form-item>
        <el-form-item label="模式" class="query-form-item">
          <el-select v-model="workflowQuery.mode" class="query-input" clearable placeholder="请选择内容">
            <el-option label="简单模式" value="SIMPLE" />
            <el-option label="高级模式" value="ADVANCED" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" class="query-form-item">
          <el-select v-model="workflowQuery.isActive" class="query-input" clearable placeholder="请选择内容">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item class="query-form-item query-form-actions">
          <el-space class="query-action-buttons">
            <el-button type="primary" @click="handleWorkflowSearch">查询</el-button>
            <el-button @click="handleWorkflowReset">重置</el-button>
          </el-space>
        </el-form-item>
      </el-form>

      <EmptyState v-if="!workflowLoading && workflowTotal === 0" description="暂无工作流数据" />
      <template v-else>
        <BaseTable
          :data="pagedWorkflowList"
          :loading="workflowLoading"
          @sort-change="handleWorkflowSortChange"
        >
          <el-table-column prop="name" label="工作流名称" min-width="160" sortable="custom" />
          <el-table-column label="模式" width="120">
            <template #default="{ row }">
              <el-tag :type="getModeTagType(row.mode)">{{ row.modeLabel || row.mode }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="stateCount" label="状态数" width="100" sortable="custom" />
          <el-table-column prop="transitionCount" label="流转数" width="100" sortable="custom" />
          <el-table-column label="启用状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.isActive)">{{ row.isActive === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="180" sortable="custom">
            <template #default="{ row }">
              {{ formatDateTime(row.updateTime || row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openWorkflowDetail(row)">详情</el-button>
              <el-button
                v-if="row.isBuiltin !== 1"
                type="primary"
                link
                @click="openWorkflowEdit(row)"
              >
                编辑
              </el-button>
            </template>
          </el-table-column>
        </BaseTable>
        <BasePagination
          :current-page="workflowQuery.pageNum"
          :page-size="workflowQuery.pageSize"
          :total="workflowTotal"
          @update="handleWorkflowPaginationChange"
        />
      </template>
    </el-card>

    <el-card shadow="never" class="page-card">
      <div class="section-toolbar">
        <div class="title">处理组管理</div>
        <el-button type="primary" @click="openCreateHandlerGroupDialog">新建处理组</el-button>
      </div>

      <el-form :inline="true" label-width="72px" class="query-form query-form--compact">
        <el-form-item label="关键字" class="query-form-item">
          <el-input
            v-model="handlerGroupQuery.keyword"
            class="query-input"
            placeholder="请输入名称/描述/技能标签"
            clearable
            @keyup.enter="handleHandlerGroupSearch"
          />
        </el-form-item>
        <el-form-item class="query-form-item query-form-actions">
          <el-space wrap class="handler-query-actions">
            <el-button type="primary" @click="handleHandlerGroupSearch">查询</el-button>
            <el-button @click="handleHandlerGroupReset">重置</el-button>
            <el-button link @click="loadHandlerGroups">刷新</el-button>
          </el-space>
        </el-form-item>
      </el-form>

      <EmptyState v-if="!handlerGroupLoading && handlerGroupTotal === 0" description="暂无处理组数据" />
      <template v-else>
        <BaseTable
          :data="pagedHandlerGroupList"
          :loading="handlerGroupLoading"
          @sort-change="handleHandlerGroupSortChange"
        >
          <el-table-column prop="name" label="处理组名称" min-width="140" sortable="custom" />
          <el-table-column
            prop="description"
            label="说明"
            min-width="180"
            :show-overflow-tooltip="true"
          />
          <el-table-column
            prop="skillTags"
            label="技能标签"
            min-width="180"
            :show-overflow-tooltip="true"
          />
          <el-table-column prop="leaderName" label="组长" width="120" />
          <el-table-column prop="memberCount" label="成员数" width="90" sortable="custom" />
          <el-table-column label="成员名单" min-width="200" :show-overflow-tooltip="true">
            <template #default="{ row }">
              {{ formatMemberNames(row) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.isActive)">{{ row.isActive === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="180" sortable="custom">
            <template #default="{ row }">
              {{ formatDateTime(row.updateTime || row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openEditHandlerGroupDialog(row)">编辑</el-button>
            </template>
          </el-table-column>
        </BaseTable>
        <BasePagination
          :current-page="handlerGroupQuery.pageNum"
          :page-size="handlerGroupQuery.pageSize"
          :total="handlerGroupTotal"
          @update="handleHandlerGroupPaginationChange"
        />
      </template>
    </el-card>
  </div>

  <el-drawer v-model="workflowDetailVisible" title="工作流详情" size="56%">
    <div v-loading="workflowDetailLoading" class="detail-wrapper">
      <template v-if="workflowDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工作流名称">
            {{ workflowDetail.name }}
          </el-descriptions-item>
          <el-descriptions-item label="模式">
            {{ workflowDetail.modeLabel || workflowDetail.mode }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(workflowDetail.isActive)">
              {{ workflowDetail.isActive === 1 ? '启用' : '停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDateTime(workflowDetail.updateTime || workflowDetail.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">
            {{ workflowDetail.description || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-card shadow="never" class="detail-section">
          <template #header>
            <div class="section-title">状态图配置</div>
          </template>
          <BaseTable :data="detailStateRows">
            <el-table-column prop="code" label="状态编码" width="140" />
            <el-table-column prop="name" label="状态名称" min-width="140" />
            <el-table-column label="状态类型" min-width="120">
              <template #default="{ row }">
                {{ getStateTypeLabel(row.type) }}
              </template>
            </el-table-column>
            <el-table-column label="SLA动作" min-width="140">
              <template #default="{ row }">
                {{ getSlaActionLabel(row.slaAction) }}
              </template>
            </el-table-column>
          </BaseTable>
        </el-card>

        <el-card shadow="never" class="detail-section">
          <template #header>
            <div class="section-title">流转规则与角色可见条件</div>
          </template>
          <BaseTable :data="detailTransitionRows">
            <el-table-column label="起始状态" min-width="140">
              <template #default="{ row }">
                {{ getWorkflowStateLabel(row.fromName || row.from) }}
              </template>
            </el-table-column>
            <el-table-column label="目标状态" min-width="140">
              <template #default="{ row }">
                {{ getWorkflowStateLabel(row.toName || row.to) }}
              </template>
            </el-table-column>
            <el-table-column prop="name" label="流转名称" min-width="140" />
            <el-table-column label="角色可见条件" min-width="220" :show-overflow-tooltip="true">
              <template #default="{ row }">
                {{ formatAllowedRoles(row.allowedRoles) }}
              </template>
            </el-table-column>
          </BaseTable>
        </el-card>
      </template>
      <EmptyState v-else description="未获取到工作流详情数据" />
    </div>
  </el-drawer>

  <el-drawer
    v-model="workflowEditVisible"
    title="编辑工作流"
    size="90%"
    destroy-on-close
    @closed="resetWorkflowEditForm"
  >
    <div v-loading="workflowEditLoading" class="workflow-edit-body">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="workflow-edit-tip"
        title="修改状态与流转将影响后续工单操作，请谨慎保存。流转中的角色须使用大写枚举：SUBMITTER、HANDLER、ADMIN、TICKET_ADMIN 等。"
      />
      <el-form label-width="100px" class="workflow-edit-form">
        <el-form-item label="名称" required>
          <el-input v-model="workflowEditForm.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="模式" required>
          <el-select v-model="workflowEditForm.mode" style="width: 200px">
            <el-option label="简单模式" value="SIMPLE" />
            <el-option label="高级模式" value="ADVANCED" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-radio-group v-model="workflowEditForm.isActive">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="workflowEditForm.description"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <el-card shadow="never" class="edit-section-card">
        <template #header>
          <div class="section-header-row">
            <span class="section-title">状态定义</span>
            <el-button type="primary" link @click="addWorkflowStateRow">添加状态</el-button>
          </div>
        </template>
        <el-table
          :data="workflowEditForm.states"
          :border="false"
          :stripe="true"
          :header-cell-style="{ backgroundColor: '#f5f7fa' }"
        >
          <el-table-column label="编码" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.code" placeholder="如 PENDING" />
            </template>
          </el-table-column>
          <el-table-column label="名称" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.name" />
            </template>
          </el-table-column>
          <el-table-column label="类型" width="200">
            <template #default="{ row }">
              <el-select v-model="row.type" style="width: 100%">
                <el-option
                  v-for="opt in STATE_TYPE_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="SLA动作" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.slaAction" placeholder="如 START_RESPONSE" />
            </template>
          </el-table-column>
          <el-table-column label="排序" width="90">
            <template #default="{ row }">
              <el-input-number v-model="row.order" :min="0" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button type="danger" link @click="removeWorkflowStateRow($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="never" class="edit-section-card">
        <template #header>
          <div class="section-header-row">
            <span class="section-title">流转规则</span>
            <el-button type="primary" link @click="addWorkflowTransitionRow">添加流转</el-button>
          </div>
        </template>
        <el-table
          :data="workflowEditForm.transitions"
          :border="false"
          :stripe="true"
          :header-cell-style="{ backgroundColor: '#f5f7fa' }"
        >
          <el-table-column label="ID" width="100">
            <template #default="{ row }">
              <el-input v-model="row.id" placeholder="可空自动生成" />
            </template>
          </el-table-column>
          <el-table-column label="起始" min-width="160">
            <template #default="{ row }">
              <el-select v-model="row.from" filterable allow-create style="width: 100%">
                <el-option
                  v-for="opt in workflowStateCodeOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="目标" min-width="160">
            <template #default="{ row }">
              <el-select v-model="row.to" filterable allow-create style="width: 100%">
                <el-option
                  v-for="opt in workflowStateCodeOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="名称" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.name" />
            </template>
          </el-table-column>
          <el-table-column label="角色" min-width="200">
            <template #default="{ row }">
              <el-select v-model="row.allowedRoles" multiple collapse-tags style="width: 100%">
                <el-option
                  v-for="opt in WORKFLOW_ROLE_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="备注必填" width="90" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.requireRemark" />
            </template>
          </el-table-column>
          <el-table-column label="可转派" width="90" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.allowTransfer" />
            </template>
          </el-table-column>
          <el-table-column label="退回" width="70" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.isReturn" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button type="danger" link @click="removeWorkflowTransitionRow($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
    <template #footer>
      <el-button @click="workflowEditVisible = false">取消</el-button>
      <el-button type="primary" :loading="workflowEditSubmitLoading" @click="handleWorkflowEditSubmit">
        保存
      </el-button>
    </template>
  </el-drawer>

  <el-dialog
    v-model="createHandlerGroupVisible"
    :title="handlerGroupDialogTitle"
    width="560px"
    @closed="editingHandlerGroupId = null"
  >
    <el-form label-width="100px">
      <el-form-item label="处理组名称" required>
        <el-input v-model="createHandlerGroupForm.name" maxlength="50" show-word-limit />
      </el-form-item>
      <el-form-item label="处理组说明">
        <el-input
          v-model="createHandlerGroupForm.description"
          type="textarea"
          :rows="2"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="技能标签">
        <el-input
          v-model="createHandlerGroupForm.skillTags"
          placeholder="多个标签可用逗号分隔"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="组长">
        <el-select
          v-model="createHandlerGroupForm.leaderId"
          filterable
          clearable
          placeholder="请选择组长"
          @change="handleLeaderChange"
        >
          <el-option
            v-for="option in userSelectOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="组成员" required>
        <el-select
          v-model="createHandlerGroupForm.memberIds"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          placeholder="请选择处理组成员"
        >
          <el-option
            v-for="option in userSelectOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createHandlerGroupVisible = false">取消</el-button>
      <el-button type="primary" :loading="handlerGroupSubmitLoading" @click="handleCreateHandlerGroup">
        {{ editingHandlerGroupId === null ? '确认创建' : '保存修改' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.workflow-manage-page {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-card {
  width: 100%;
}

.title {
  font-size: 17px;
  font-weight: 600;
  color: #1d2129;
  line-height: 24px;
  white-space: nowrap;
  flex-shrink: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.section-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
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

.query-form--compact {
  margin-bottom: 12px;
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

.handler-query-actions {
  width: auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-wrapper {
  min-height: 260px;
}

.detail-section {
  margin-top: 18px;
  border-radius: 8px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
}

.workflow-edit-body {
  min-height: 200px;
  padding-bottom: 16px;
}

.workflow-edit-tip {
  margin-bottom: 16px;
  border-radius: 8px;
}

.workflow-edit-form {
  max-width: 720px;

  :deep(.el-form-item) {
    margin-bottom: 18px;
  }
}

.edit-section-card {
  margin-top: 18px;
  border-radius: 8px;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

@media (max-width: 991px) {
  .query-form {
    padding: 10px 12px;
  }
}

@media (max-width: 768px) {
  .toolbar,
  .section-toolbar {
    margin-bottom: 12px;
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

  .query-form-actions {
    margin-left: 0;
  }

  .query-action-buttons :deep(.el-space__item) {
    width: calc(50% - 4px);
  }

  .query-action-buttons :deep(.el-button) {
    width: 100%;
  }
}
</style>
