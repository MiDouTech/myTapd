<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import {
  getLatestOrganizationSyncStatus,
  getOrganizationDepartmentTree,
  getOrganizationEmployeeDetail,
  getOrganizationEmployeePage,
  getOrganizationSyncLogPage,
  manualSyncOrganization,
} from '@/api/organization'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type {
  DepartmentTreeNode,
  EmployeeDetailOutput,
  EmployeePageRecord,
  SyncStatusOutput,
} from '@/types/organization'
import { notifySuccess } from '@/utils/feedback'
import { formatDateTime } from '@/utils/formatter'

const treeProps = {
  label: 'name',
  children: 'children',
}

const departmentLoading = ref(false)
const tableLoading = ref(false)
const detailLoading = ref(false)
const syncLoading = ref(false)
const syncStatusLoading = ref(false)
const syncLogLoading = ref(false)

const departmentTree = ref<DepartmentTreeNode[]>([])
const selectedDepartmentId = ref<number>()
const selectedDepartment = ref<DepartmentTreeNode>()
const activeTab = ref<'members' | 'department'>('members')

const employees = ref<EmployeePageRecord[]>([])
const total = ref(0)

const detailVisible = ref(false)
const detailUser = ref<EmployeeDetailOutput>()

const latestSyncStatus = ref<SyncStatusOutput | null>(null)

const query = reactive({
  keyword: '',
  accountStatus: undefined as number | undefined,
  gender: undefined as number | undefined,
  syncStatus: undefined as number | undefined,
  pageNum: 1,
  pageSize: 20,
  orderBy: undefined as string | undefined,
  asc: false,
})

const syncLogDialogVisible = ref(false)
const syncLogList = ref<SyncStatusOutput[]>([])
const syncLogTotal = ref(0)
const syncLogQuery = reactive({
  syncMode: undefined as string | undefined,
  syncStatus: undefined as string | undefined,
  pageNum: 1,
  pageSize: 20,
})

const totalOrganizationUsers = computed(() =>
  departmentTree.value.reduce((sum, node) => sum + (node.totalUserCount || 0), 0),
)

const selectedDepartmentName = computed(() => selectedDepartment.value?.name || '全部部门')

const selectedDepartmentTotalUsers = computed(
  () => selectedDepartment.value?.totalUserCount ?? totalOrganizationUsers.value,
)

function findDepartmentNode(nodes: DepartmentTreeNode[], targetId: number): DepartmentTreeNode | undefined {
  for (const node of nodes) {
    if (node.id === targetId) {
      return node
    }
    if (node.children?.length) {
      const childResult = findDepartmentNode(node.children, targetId)
      if (childResult) {
        return childResult
      }
    }
  }
  return undefined
}

function getAccountStatusType(status?: number): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 1) {
    return 'success'
  }
  if (status === 2) {
    return 'warning'
  }
  if (status === 4) {
    return 'danger'
  }
  return 'info'
}

function getAccountStatusLabel(status?: number): string {
  const map: Record<number, string> = {
    1: '在职',
    2: '停用',
    4: '离职',
  }
  return status === undefined ? '未知' : map[status] || '未知'
}

function getUserSyncStatusLabel(syncStatus?: number): string {
  const map: Record<number, string> = {
    0: '未同步',
    1: '成功',
    2: '失败/失效',
  }
  return syncStatus === undefined ? '未知' : map[syncStatus] || '未知'
}

function getUserSyncStatusType(syncStatus?: number): 'success' | 'warning' | 'danger' | 'info' {
  if (syncStatus === 1) {
    return 'success'
  }
  if (syncStatus === 2) {
    return 'danger'
  }
  if (syncStatus === 0) {
    return 'warning'
  }
  return 'info'
}

function getGenderLabel(gender?: number): string {
  const map: Record<number, string> = {
    0: '未知',
    1: '男',
    2: '女',
  }
  return gender === undefined ? '未知' : map[gender] || '未知'
}

function getDepartmentStatusLabel(status?: number): string {
  return status === 0 ? '停用' : '启用'
}

function getDepartmentStatusType(status?: number): 'success' | 'info' {
  return status === 0 ? 'info' : 'success'
}

function getSyncStatusLabel(status?: string): string {
  const map: Record<string, string> = {
    SUCCESS: '成功',
    PARTIAL: '部分成功',
    FAILED: '失败',
  }
  if (!status) {
    return '未同步'
  }
  return map[status] || status
}

function getSyncStatusType(status?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'SUCCESS') {
    return 'success'
  }
  if (status === 'PARTIAL') {
    return 'warning'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  return 'info'
}

function getSyncModeLabel(syncMode?: string): string {
  if (syncMode === 'MANUAL') {
    return '手动'
  }
  if (syncMode === 'SCHEDULE') {
    return '定时'
  }
  return syncMode || '-'
}

function formatDuration(durationMs?: number): string {
  if (!durationMs || durationMs <= 0) {
    return '-'
  }
  if (durationMs < 1000) {
    return `${durationMs} ms`
  }
  return `${(durationMs / 1000).toFixed(2)} s`
}

function formatRoleCodes(roleCodes?: string[]): string {
  if (!roleCodes || roleCodes.length === 0) {
    return '-'
  }
  return roleCodes.join('、')
}

async function loadDepartmentTree(): Promise<void> {
  departmentLoading.value = true
  try {
    const tree = await getOrganizationDepartmentTree()
    departmentTree.value = tree
    if (selectedDepartmentId.value !== undefined) {
      selectedDepartment.value = findDepartmentNode(tree, selectedDepartmentId.value)
      if (!selectedDepartment.value) {
        selectedDepartmentId.value = undefined
      }
    }
    if (selectedDepartmentId.value === undefined) {
      selectedDepartment.value = undefined
    }
  } catch {
    // 请求失败时保留当前数据，避免影响正在进行的筛选操作
  } finally {
    departmentLoading.value = false
  }
}

async function loadEmployees(): Promise<void> {
  tableLoading.value = true
  try {
    const page = await getOrganizationEmployeePage({
      departmentId: selectedDepartmentId.value,
      keyword: query.keyword.trim() || undefined,
      accountStatus: query.accountStatus,
      gender: query.gender,
      syncStatus: query.syncStatus,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      orderBy: query.orderBy,
      asc: query.asc,
    })
    employees.value = page.records || []
    total.value = page.total || 0
    query.pageNum = page.pageNum || query.pageNum
    query.pageSize = page.pageSize || query.pageSize
  } catch {
    // 统一错误提示由拦截器处理，页面保留现有数据
  } finally {
    tableLoading.value = false
  }
}

async function loadLatestSyncStatus(): Promise<void> {
  syncStatusLoading.value = true
  try {
    latestSyncStatus.value = await getLatestOrganizationSyncStatus()
  } catch {
    // 同步状态读取失败时，保留上一次成功读取的数据
  } finally {
    syncStatusLoading.value = false
  }
}

async function loadSyncLogs(): Promise<void> {
  syncLogLoading.value = true
  try {
    const page = await getOrganizationSyncLogPage({
      syncMode: syncLogQuery.syncMode,
      syncStatus: syncLogQuery.syncStatus,
      pageNum: syncLogQuery.pageNum,
      pageSize: syncLogQuery.pageSize,
    })
    syncLogList.value = page.records || []
    syncLogTotal.value = page.total || 0
    syncLogQuery.pageNum = page.pageNum || syncLogQuery.pageNum
    syncLogQuery.pageSize = page.pageSize || syncLogQuery.pageSize
  } catch {
    // 同步日志读取失败时保留当前列表，避免中断排障过程
  } finally {
    syncLogLoading.value = false
  }
}

function handleSelectAllDepartment(): void {
  selectedDepartmentId.value = undefined
  selectedDepartment.value = undefined
  query.pageNum = 1
  void loadEmployees()
}

function handleDepartmentClick(node: DepartmentTreeNode): void {
  selectedDepartmentId.value = node.id
  selectedDepartment.value = node
  query.pageNum = 1
  void loadEmployees()
}

function handleSearch(): void {
  query.pageNum = 1
  void loadEmployees()
}

function handleReset(): void {
  query.keyword = ''
  query.accountStatus = undefined
  query.gender = undefined
  query.syncStatus = undefined
  query.pageNum = 1
  query.pageSize = 20
  query.orderBy = undefined
  query.asc = false
  void loadEmployees()
}

function handlePaginationChange(payload: { pageNum: number; pageSize: number }): void {
  query.pageNum = payload.pageNum
  query.pageSize = payload.pageSize
  void loadEmployees()
}

function handleSortChange(payload: { prop: string; order: 'ascending' | 'descending' | null }): void {
  query.orderBy = payload.order && payload.prop ? payload.prop : undefined
  query.asc = payload.order === 'ascending'
  query.pageNum = 1
  void loadEmployees()
}

async function openDetail(row: EmployeePageRecord): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailUser.value = await getOrganizationEmployeeDetail(row.id)
  } catch {
    detailUser.value = undefined
  } finally {
    detailLoading.value = false
  }
}

async function handleManualSync(): Promise<void> {
  syncLoading.value = true
  try {
    const result = await manualSyncOrganization()
    const summary = [
      `总数${result.totalCount}`,
      `成功${result.successCount}`,
      `失败${result.failCount}`,
      `部门+${result.departmentCreatedCount ?? 0}`,
      `部门~${result.departmentUpdatedCount ?? 0}`,
      `部门停用${result.departmentDisabledCount ?? 0}`,
      `用户+${result.userCreatedCount ?? 0}`,
      `用户~${result.userUpdatedCount ?? 0}`,
      `用户离职${result.userDisabledCount ?? 0}`,
    ]
    notifySuccess(`同步完成：${summary.join('，')}`)
    await Promise.all([loadDepartmentTree(), loadEmployees(), loadLatestSyncStatus()])
    if (syncLogDialogVisible.value) {
      await loadSyncLogs()
    }
  } catch {
    // 失败提示由拦截器统一处理
  } finally {
    syncLoading.value = false
  }
}

function openSyncLogDialog(): void {
  syncLogDialogVisible.value = true
  syncLogQuery.pageNum = 1
  void loadSyncLogs()
}

function handleSyncLogSearch(): void {
  syncLogQuery.pageNum = 1
  void loadSyncLogs()
}

function handleSyncLogReset(): void {
  syncLogQuery.syncMode = undefined
  syncLogQuery.syncStatus = undefined
  syncLogQuery.pageNum = 1
  syncLogQuery.pageSize = 20
  void loadSyncLogs()
}

function handleSyncLogPaginationChange(payload: { pageNum: number; pageSize: number }): void {
  syncLogQuery.pageNum = payload.pageNum
  syncLogQuery.pageSize = payload.pageSize
  void loadSyncLogs()
}

onMounted(async () => {
  await Promise.all([loadDepartmentTree(), loadEmployees(), loadLatestSyncStatus()])
})
</script>

<template>
  <el-space direction="vertical" fill :size="12">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="title">组织账号管理</div>
          <el-space class="card-header-right">
            <span class="sync-tip">最近同步状态：</span>
            <el-tag :type="getSyncStatusType(latestSyncStatus?.syncStatus)">
              {{ getSyncStatusLabel(latestSyncStatus?.syncStatus) }}
            </el-tag>
            <span class="sync-tip">{{ formatDateTime(latestSyncStatus?.endTime) }}</span>
            <span v-if="syncStatusLoading" class="sync-tip">加载中...</span>
          </el-space>
        </div>
      </template>

      <div class="toolbar-row">
        <el-form :inline="true">
          <el-form-item label="关键字">
            <el-input
              v-model="query.keyword"
              clearable
              placeholder="姓名/工号"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="账号状态">
            <el-select v-model="query.accountStatus" clearable placeholder="请选择">
              <el-option label="在职" :value="1" />
              <el-option label="停用" :value="2" />
              <el-option label="离职" :value="4" />
            </el-select>
          </el-form-item>
          <el-form-item label="性别">
            <el-select v-model="query.gender" clearable placeholder="请选择">
              <el-option label="男" :value="1" />
              <el-option label="女" :value="2" />
              <el-option label="未知" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="同步状态">
            <el-select v-model="query.syncStatus" clearable placeholder="请选择">
              <el-option label="未同步" :value="0" />
              <el-option label="成功" :value="1" />
              <el-option label="失败/失效" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-space>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-space>
          </el-form-item>
        </el-form>

        <el-space>
          <el-button type="primary" :loading="syncLoading" @click="handleManualSync">同步企业微信</el-button>
          <el-button type="warning" plain @click="openSyncLogDialog">查看同步日志</el-button>
        </el-space>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :md="7" :lg="6">
        <el-card shadow="never" class="tree-card" v-loading="departmentLoading">
          <template #header>
            <div class="card-header">
              <span class="subtitle">组织架构</span>
              <el-button type="primary" link @click="handleSelectAllDepartment">全部部门</el-button>
            </div>
          </template>

          <div class="selected-tip">
            当前：{{ selectedDepartmentName }}（{{ selectedDepartmentTotalUsers }}人）
          </div>

          <EmptyState v-if="!departmentLoading && departmentTree.length === 0" description="暂无组织架构数据" />
          <el-tree
            v-else
            :data="departmentTree"
            node-key="id"
            highlight-current
            default-expand-all
            :props="treeProps"
            @node-click="handleDepartmentClick"
          >
            <template #default="{ data }">
              <div class="tree-node">
                <span class="tree-node-name">{{ data.name }}（{{ data.totalUserCount || 0 }}）</span>
                <el-tag size="small" :type="getDepartmentStatusType(data.deptStatus)">
                  {{ getDepartmentStatusLabel(data.deptStatus) }}
                </el-tag>
              </div>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="17" :lg="18">
        <el-card shadow="never">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="成员列表" name="members">
              <EmptyState v-if="!tableLoading && total === 0" description="暂无员工数据" />
              <template v-else>
                <BaseTable :data="employees" :loading="tableLoading" @sort-change="handleSortChange">
                  <el-table-column prop="name" label="姓名" min-width="160" sortable="custom">
                    <template #default="{ row }">
                      <div class="user-name-cell">
                        <el-avatar :size="24" :src="row.avatarUrl">{{ row.name?.slice(0, 1) || 'U' }}</el-avatar>
                        <div class="user-name-content">
                          <div>{{ row.name }}</div>
                          <div class="sub-text">{{ row.employeeNo || '-' }}</div>
                        </div>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="position" label="职位" min-width="140" />
                  <el-table-column prop="phoneMasked" label="手机号" width="150" sortable="custom" />
                  <el-table-column prop="gender" label="性别" width="100" sortable="custom">
                    <template #default="{ row }">
                      {{ row.genderName || getGenderLabel(row.gender) }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="departmentName" label="部门" min-width="140" />
                  <el-table-column prop="accountStatus" label="状态" width="110" sortable="custom">
                    <template #default="{ row }">
                      <el-tag :type="getAccountStatusType(row.accountStatus)">
                        {{ row.accountStatusName || getAccountStatusLabel(row.accountStatus) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="syncStatus" label="同步状态" width="120" sortable="custom">
                    <template #default="{ row }">
                      <el-tag :type="getUserSyncStatusType(row.syncStatus)">
                        {{ row.syncStatusName || getUserSyncStatusLabel(row.syncStatus) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="syncTime" label="同步时间" width="180" sortable="custom">
                    <template #default="{ row }">
                      {{ formatDateTime(row.syncTime) }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="createTime" label="创建时间" width="180" sortable="custom">
                    <template #default="{ row }">
                      {{ formatDateTime(row.createTime) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="120" align="center" fixed="right">
                    <template #default="{ row }">
                      <el-button type="primary" link @click="openDetail(row)">查看详情</el-button>
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
            </el-tab-pane>

            <el-tab-pane label="部门详情" name="department">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="部门名称">
                  {{ selectedDepartment?.name || '全部部门' }}
                </el-descriptions-item>
                <el-descriptions-item label="部门状态">
                  <el-tag :type="getDepartmentStatusType(selectedDepartment?.deptStatus)">
                    {{ getDepartmentStatusLabel(selectedDepartment?.deptStatus) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="直属成员数">
                  {{ selectedDepartment?.directUserCount ?? totalOrganizationUsers }}
                </el-descriptions-item>
                <el-descriptions-item label="含子部门成员数">
                  {{ selectedDepartment?.totalUserCount ?? totalOrganizationUsers }}
                </el-descriptions-item>
                <el-descriptions-item label="企微部门ID">
                  {{ selectedDepartment?.wecomDeptId || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="最近同步时间">
                  {{ formatDateTime(selectedDepartment?.syncTime || latestSyncStatus?.endTime) }}
                </el-descriptions-item>
              </el-descriptions>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </el-space>

  <el-drawer v-model="detailVisible" title="员工详情" size="520px">
    <div v-loading="detailLoading">
      <template v-if="detailUser">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="姓名">{{ detailUser.name }}</el-descriptions-item>
        <el-descriptions-item label="工号">{{ detailUser.employeeNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ detailUser.departmentName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="职位">{{ detailUser.position || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detailUser.phoneMasked || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detailUser.emailMasked || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ detailUser.genderName || getGenderLabel(detailUser.gender) }}
        </el-descriptions-item>
        <el-descriptions-item label="企微UserId">
          {{ detailUser.wecomUseridMasked || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="getAccountStatusType(detailUser.accountStatus)">
            {{ detailUser.accountStatusName || getAccountStatusLabel(detailUser.accountStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="同步状态">
          <el-tag :type="getUserSyncStatusType(detailUser.syncStatus)">
            {{ detailUser.syncStatusName || getUserSyncStatusLabel(detailUser.syncStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="同步时间">
          {{ formatDateTime(detailUser.syncTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          {{ formatRoleCodes(detailUser.roleCodes) }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detailUser.createTime) }}</el-descriptions-item>
      </el-descriptions>
      </template>
      <EmptyState v-else description="暂无员工详情" />
    </div>
  </el-drawer>

  <el-dialog v-model="syncLogDialogVisible" title="同步日志" width="980px">
    <el-form :inline="true" class="sync-log-filter">
      <el-form-item label="触发方式">
        <el-select v-model="syncLogQuery.syncMode" clearable placeholder="请选择">
          <el-option label="手动" value="MANUAL" />
          <el-option label="定时" value="SCHEDULE" />
        </el-select>
      </el-form-item>
      <el-form-item label="同步状态">
        <el-select v-model="syncLogQuery.syncStatus" clearable placeholder="请选择">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="部分成功" value="PARTIAL" />
          <el-option label="失败" value="FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-space>
          <el-button type="primary" @click="handleSyncLogSearch">查询</el-button>
          <el-button @click="handleSyncLogReset">重置</el-button>
        </el-space>
      </el-form-item>
    </el-form>

    <EmptyState v-if="!syncLogLoading && syncLogTotal === 0" description="暂无同步日志" />
    <template v-else>
      <BaseTable :data="syncLogList" :loading="syncLogLoading">
        <el-table-column prop="syncMode" label="触发方式" width="110">
          <template #default="{ row }">
            {{ getSyncModeLabel(row.syncMode) }}
          </template>
        </el-table-column>
        <el-table-column prop="syncStatus" label="同步状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getSyncStatusType(row.syncStatus)">
              {{ getSyncStatusLabel(row.syncStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="总数" width="90" />
        <el-table-column prop="successCount" label="成功" width="90" />
        <el-table-column prop="failCount" label="失败" width="90" />
        <el-table-column prop="retryCount" label="重试次数" width="100" />
        <el-table-column prop="durationMs" label="耗时" width="110">
          <template #default="{ row }">
            {{ formatDuration(row.durationMs) }}
          </template>
        </el-table-column>
        <el-table-column prop="triggerBy" label="触发人" width="120">
          <template #default="{ row }">
            {{ row.triggerBy || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="220" :show-overflow-tooltip="true">
          <template #default="{ row }">
            {{ row.errorMessage || '-' }}
          </template>
        </el-table-column>
      </BaseTable>
      <BasePagination
        :current-page="syncLogQuery.pageNum"
        :page-size="syncLogQuery.pageSize"
        :total="syncLogTotal"
        @update="handleSyncLogPaginationChange"
      />
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.title {
  font-size: 17px;
  font-weight: 600;
  color: #1d2129;
  line-height: 24px;
  white-space: nowrap;
  flex-shrink: 0;
}

.subtitle {
  font-size: 14px;
  font-weight: 500;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.card-header-right {
  width: auto;
  flex: 0 0 auto;
}

.toolbar-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.tree-card {
  min-height: 680px;
}

.selected-tip {
  margin-bottom: 12px;
  font-size: 13px;
  color: #606266;
}

.tree-node {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.tree-node-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name-content {
  line-height: 18px;
}

.sub-text {
  color: #909399;
  font-size: 12px;
}

.sync-tip {
  color: #606266;
  font-size: 13px;
}

.sync-log-filter {
  margin-bottom: 8px;
}
</style>
