<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { getDepartmentTree } from '@/api/department'
import { getUserList } from '@/api/user'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { DepartmentTreeOutput } from '@/types/department'
import type { UserListInput, UserListOutput } from '@/types/user'
import { formatDateTime } from '@/utils/formatter'

const departmentLoading = ref(false)
const tableLoading = ref(false)
const detailVisible = ref(false)

const departmentTree = ref<DepartmentTreeOutput[]>([])
const selectedDepartmentId = ref<number>()
const selectedDepartmentName = ref('全部部门')
const userList = ref<UserListOutput[]>([])
const detailUser = ref<UserListOutput>()

const query = reactive({
  keyword: '',
  accountStatus: undefined as number | undefined,
  orderBy: undefined as string | undefined,
  asc: false,
  pageNum: 1,
  pageSize: 20,
})

const treeProps = {
  label: 'name',
  children: 'children',
}

const sortedUserList = computed(() => {
  const sorted = [...userList.value]
  if (!query.orderBy) {
    return sorted
  }
  sorted.sort((a, b) => {
    const left = a[query.orderBy as keyof UserListOutput]
    const right = b[query.orderBy as keyof UserListOutput]
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
      return query.asc ? left - right : right - left
    }
    const result = String(left).localeCompare(String(right), 'zh-CN')
    return query.asc ? result : -result
  })
  return sorted
})

const total = computed(() => sortedUserList.value.length)

const pagedUserList = computed(() => {
  const start = (query.pageNum - 1) * query.pageSize
  return sortedUserList.value.slice(start, start + query.pageSize)
})

function normalizePage(): void {
  const maxPage = Math.max(1, Math.ceil(total.value / query.pageSize))
  if (query.pageNum > maxPage) {
    query.pageNum = maxPage
  }
}

function getStatusLabel(status?: number): string {
  const map: Record<number, string> = {
    1: '已激活',
    2: '已禁用',
    4: '未激活',
  }
  if (status === undefined) {
    return '-'
  }
  return map[status] || String(status)
}

function getStatusTagType(status?: number): 'success' | 'danger' | 'warning' | 'info' {
  if (status === 1) {
    return 'success'
  }
  if (status === 2) {
    return 'danger'
  }
  if (status === 4) {
    return 'warning'
  }
  return 'info'
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
    departmentTree.value = await getDepartmentTree()
  } catch {
    // 请求错误由全局拦截器统一提示，这里保留已加载数据
  } finally {
    departmentLoading.value = false
  }
}

async function loadUsers(): Promise<void> {
  tableLoading.value = true
  try {
    const params: UserListInput = {
      departmentId: selectedDepartmentId.value,
      keyword: query.keyword.trim() || undefined,
      accountStatus: query.accountStatus,
    }
    userList.value = await getUserList(params)
    normalizePage()
  } catch {
    // 请求错误由全局拦截器统一提示，这里保留筛选条件和旧数据
  } finally {
    tableLoading.value = false
  }
}

function handleSelectAllDepartment(): void {
  selectedDepartmentId.value = undefined
  selectedDepartmentName.value = '全部部门'
  query.pageNum = 1
  void loadUsers()
}

function handleDepartmentClick(node: DepartmentTreeOutput): void {
  selectedDepartmentId.value = node.id
  selectedDepartmentName.value = node.name
  query.pageNum = 1
  void loadUsers()
}

function handleSearch(): void {
  query.pageNum = 1
  void loadUsers()
}

function handleReset(): void {
  query.keyword = ''
  query.accountStatus = undefined
  query.orderBy = undefined
  query.asc = false
  query.pageNum = 1
  void loadUsers()
}

function handlePaginationChange(payload: { pageNum: number; pageSize: number }): void {
  query.pageNum = payload.pageNum
  query.pageSize = payload.pageSize
  normalizePage()
}

function handleSortChange(payload: {
  prop: string
  order: 'ascending' | 'descending' | null
}): void {
  query.orderBy = payload.order && payload.prop ? payload.prop : undefined
  query.asc = payload.order === 'ascending'
}

function openDetail(row: UserListOutput): void {
  detailUser.value = row
  detailVisible.value = true
}

onMounted(async () => {
  await Promise.all([loadDepartmentTree(), loadUsers()])
})
</script>

<template>
  <el-row :gutter="16">
    <el-col :xs="24" :md="7" :lg="6">
      <el-card shadow="never" class="tree-card" v-loading="departmentLoading">
        <template #header>
          <div class="card-header">
            <span class="title">组织架构</span>
            <el-button type="primary" link @click="handleSelectAllDepartment">全部部门</el-button>
          </div>
        </template>
        <div class="selected-tip">当前筛选：{{ selectedDepartmentName }}</div>
        <EmptyState v-if="!departmentLoading && departmentTree.length === 0" description="暂无组织架构数据" />
        <el-tree
          v-else
          :data="departmentTree"
          node-key="id"
          highlight-current
          default-expand-all
          :props="treeProps"
          @node-click="handleDepartmentClick"
        />
      </el-card>
    </el-col>

    <el-col :xs="24" :md="17" :lg="18">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <span class="title">用户列表</span>
            <el-button type="primary" link @click="loadUsers">刷新</el-button>
          </div>
        </template>

        <el-form :inline="true" label-width="72px">
          <el-form-item label="关键字">
            <el-input
              v-model="query.keyword"
              placeholder="姓名/工号"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="账号状态">
            <el-select v-model="query.accountStatus" clearable placeholder="全部状态">
              <el-option label="已激活" :value="1" />
              <el-option label="已禁用" :value="2" />
              <el-option label="未激活" :value="4" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-space>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-space>
          </el-form-item>
        </el-form>

        <EmptyState v-if="!tableLoading && total === 0" description="暂无用户数据" />
        <template v-else>
          <BaseTable :data="pagedUserList" :loading="tableLoading" @sort-change="handleSortChange">
            <el-table-column prop="name" label="姓名" min-width="120" sortable="custom" />
            <el-table-column prop="employeeNo" label="工号" min-width="120" sortable="custom" />
            <el-table-column prop="departmentName" label="部门" min-width="120" sortable="custom" />
            <el-table-column label="账号状态" width="110">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.accountStatus)">
                  {{ getStatusLabel(row.accountStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="角色" min-width="180" :show-overflow-tooltip="true">
              <template #default="{ row }">
                {{ formatRoleCodes(row.roleCodes) }}
              </template>
            </el-table-column>
            <el-table-column prop="email" label="邮箱" min-width="180" :show-overflow-tooltip="true" />
            <el-table-column prop="phone" label="手机号" width="140" />
            <el-table-column prop="position" label="职位" min-width="120" />
            <el-table-column prop="createTime" label="创建时间" width="180" sortable="custom">
              <template #default="{ row }">
                {{ formatDateTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center" fixed="right">
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
    </el-col>
  </el-row>

  <el-drawer v-model="detailVisible" title="用户详情" size="520px">
    <template v-if="detailUser">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="姓名">{{ detailUser.name }}</el-descriptions-item>
        <el-descriptions-item label="工号">{{ detailUser.employeeNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ detailUser.departmentName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="getStatusTagType(detailUser.accountStatus)">
            {{ getStatusLabel(detailUser.accountStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          {{ formatRoleCodes(detailUser.roleCodes) }}
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detailUser.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detailUser.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="职位">{{ detailUser.position || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDateTime(detailUser.createTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </template>
    <EmptyState v-else description="暂无用户详情" />
  </el-drawer>
</template>

<style scoped lang="scss">
.title {
  font-size: 16px;
  font-weight: 600;
}

.tree-card {
  min-height: 640px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.selected-tip {
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}
</style>
