<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { getUserList } from '@/api/user'
import {
  createHandlerGroup,
  getHandlerGroupList,
  getWorkflowDetail,
  getWorkflowList,
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
const createHandlerGroupVisible = ref(false)

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
  resetCreateHandlerGroupForm()
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
    await createHandlerGroup(payload)
    notifySuccess('处理组创建成功')
    createHandlerGroupVisible.value = false
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
  return roles.join('、')
}

onMounted(async () => {
  await Promise.all([loadWorkflows(), loadHandlerGroups(), loadUserOptions()])
})
</script>

<template>
  <el-space direction="vertical" fill :size="16">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">工作流管理</span>
          <el-button type="primary" link @click="loadWorkflows">刷新列表</el-button>
        </div>
      </template>

      <el-form :inline="true" label-width="72px">
        <el-form-item label="关键字">
          <el-input
            v-model="workflowQuery.keyword"
            placeholder="请输入名称或描述"
            clearable
            @keyup.enter="handleWorkflowSearch"
          />
        </el-form-item>
        <el-form-item label="模式">
          <el-select v-model="workflowQuery.mode" clearable placeholder="全部模式">
            <el-option label="简单模式" value="SIMPLE" />
            <el-option label="高级模式" value="ADVANCED" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="workflowQuery.isActive" clearable placeholder="全部状态">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-space>
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
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openWorkflowDetail(row)">详情</el-button>
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

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">处理组管理</span>
          <el-button type="primary" @click="openCreateHandlerGroupDialog">新建处理组</el-button>
        </div>
      </template>

      <el-form :inline="true" label-width="72px">
        <el-form-item label="关键字">
          <el-input
            v-model="handlerGroupQuery.keyword"
            placeholder="请输入名称/描述/技能标签"
            clearable
            @keyup.enter="handleHandlerGroupSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-space>
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
        </BaseTable>
        <BasePagination
          :current-page="handlerGroupQuery.pageNum"
          :page-size="handlerGroupQuery.pageSize"
          :total="handlerGroupTotal"
          @update="handleHandlerGroupPaginationChange"
        />
      </template>
    </el-card>
  </el-space>

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
            <el-table-column prop="slaAction" label="SLA动作" min-width="140" />
          </BaseTable>
        </el-card>

        <el-card shadow="never" class="detail-section">
          <template #header>
            <div class="section-title">流转规则与角色可见条件</div>
          </template>
          <BaseTable :data="detailTransitionRows">
            <el-table-column label="起始状态" min-width="140">
              <template #default="{ row }">
                {{ row.fromName || row.from }}
              </template>
            </el-table-column>
            <el-table-column label="目标状态" min-width="140">
              <template #default="{ row }">
                {{ row.toName || row.to }}
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

  <el-dialog v-model="createHandlerGroupVisible" title="新建处理组" width="560px">
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
        确认创建
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.title {
  font-size: 16px;
  font-weight: 600;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-wrapper {
  min-height: 260px;
}

.detail-section {
  margin-top: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
}
</style>
