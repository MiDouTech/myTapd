<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { createSlaPolicy, getSlaPolicyList, updateSlaPolicy } from '@/api/sla'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { SlaPolicyCreateInput, SlaPolicyOutput, SlaPolicyUpdateInput } from '@/types/sla'
import { notifySuccess, notifyWarning } from '@/utils/feedback'
import { formatDateTime } from '@/utils/formatter'

type DialogMode = 'create' | 'edit'

const tableLoading = ref(false)
const dialogVisible = ref(false)
const dialogSubmitLoading = ref(false)
const switchLoadingId = ref<number>()
const dialogMode = ref<DialogMode>('create')
const editingPolicyId = ref<number>()

const policyList = ref<SlaPolicyOutput[]>([])

const query = reactive({
  keyword: '',
  priority: '',
  isActive: undefined as number | undefined,
  orderBy: undefined as string | undefined,
  asc: false,
  pageNum: 1,
  pageSize: 20,
})

const form = reactive({
  name: '',
  priority: 'medium',
  responseTime: 30,
  resolveTime: 240,
  warningPct: 75,
  criticalPct: 90,
  description: '',
})

const filteredPolicyList = computed(() => {
  const keyword = query.keyword.trim().toLowerCase()
  return policyList.value.filter((item) => {
    const keywordMatched =
      !keyword ||
      item.name.toLowerCase().includes(keyword) ||
      (item.description || '').toLowerCase().includes(keyword)
    const priorityMatched = !query.priority || item.priority === query.priority
    const statusMatched = query.isActive === undefined || item.isActive === query.isActive
    return keywordMatched && priorityMatched && statusMatched
  })
})

const sortedPolicyList = computed(() => {
  const sorted = [...filteredPolicyList.value]
  if (!query.orderBy) {
    return sorted
  }
  sorted.sort((a, b) => {
    const left = a[query.orderBy as keyof SlaPolicyOutput]
    const right = b[query.orderBy as keyof SlaPolicyOutput]
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
    const compared = String(left).localeCompare(String(right), 'zh-CN')
    return query.asc ? compared : -compared
  })
  return sorted
})

const total = computed(() => sortedPolicyList.value.length)

const pagedPolicyList = computed(() => {
  const start = (query.pageNum - 1) * query.pageSize
  return sortedPolicyList.value.slice(start, start + query.pageSize)
})

function normalizePage(): void {
  const maxPage = Math.max(1, Math.ceil(total.value / query.pageSize))
  if (query.pageNum > maxPage) {
    query.pageNum = maxPage
  }
}

function resetForm(): void {
  form.name = ''
  form.priority = 'medium'
  form.responseTime = 30
  form.resolveTime = 240
  form.warningPct = 75
  form.criticalPct = 90
  form.description = ''
  editingPolicyId.value = undefined
}

async function loadPolicyList(): Promise<void> {
  tableLoading.value = true
  try {
    policyList.value = await getSlaPolicyList()
    normalizePage()
  } catch {
    // 请求错误由全局拦截器统一提示，这里保留原筛选条件与旧数据
  } finally {
    tableLoading.value = false
  }
}

function handleSearch(): void {
  query.pageNum = 1
  normalizePage()
}

function handleReset(): void {
  query.keyword = ''
  query.priority = ''
  query.isActive = undefined
  query.orderBy = undefined
  query.asc = false
  query.pageNum = 1
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

function openCreateDialog(): void {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: SlaPolicyOutput): void {
  dialogMode.value = 'edit'
  editingPolicyId.value = row.id
  form.name = row.name
  form.priority = row.priority
  form.responseTime = row.responseTime
  form.resolveTime = row.resolveTime
  form.warningPct = row.warningPct
  form.criticalPct = row.criticalPct
  form.description = row.description || ''
  dialogVisible.value = true
}

function validateForm(): boolean {
  if (!form.name.trim()) {
    notifyWarning('请输入策略名称')
    return false
  }
  if (!form.priority) {
    notifyWarning('请选择优先级')
    return false
  }
  if (form.responseTime < 1 || form.resolveTime < 1) {
    notifyWarning('响应时限和解决时限必须大于 0')
    return false
  }
  if (form.warningPct < 1 || form.warningPct > 100) {
    notifyWarning('预警阈值必须在 1-100 之间')
    return false
  }
  if (form.criticalPct < 1 || form.criticalPct > 100) {
    notifyWarning('超时阈值必须在 1-100 之间')
    return false
  }
  return true
}

async function handleSubmit(): Promise<void> {
  if (!validateForm()) {
    return
  }

  dialogSubmitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      const payload: SlaPolicyCreateInput = {
        name: form.name.trim(),
        priority: form.priority,
        responseTime: form.responseTime,
        resolveTime: form.resolveTime,
        warningPct: form.warningPct,
        criticalPct: form.criticalPct,
        description: form.description.trim() || undefined,
      }
      await createSlaPolicy(payload)
      notifySuccess('SLA 策略创建成功')
    } else {
      if (!editingPolicyId.value) {
        notifyWarning('当前编辑策略缺少ID')
        return
      }
      const payload: SlaPolicyUpdateInput = {
        id: editingPolicyId.value,
        name: form.name.trim(),
        priority: form.priority,
        responseTime: form.responseTime,
        resolveTime: form.resolveTime,
        warningPct: form.warningPct,
        criticalPct: form.criticalPct,
        description: form.description.trim() || undefined,
      }
      await updateSlaPolicy(payload)
      notifySuccess('SLA 策略更新成功')
    }
    dialogVisible.value = false
    await loadPolicyList()
  } catch {
    // 失败时不关闭弹窗，保留已填内容
  } finally {
    dialogSubmitLoading.value = false
  }
}

async function handleSwitchChange(
  row: SlaPolicyOutput,
  value: string | number | boolean,
): Promise<void> {
  const nextStatus = Number(value)
  if (Number.isNaN(nextStatus) || nextStatus === row.isActive) {
    return
  }
  switchLoadingId.value = row.id
  try {
    await updateSlaPolicy({ id: row.id, isActive: nextStatus })
    row.isActive = nextStatus
    notifySuccess(nextStatus === 1 ? '策略已启用' : '策略已停用')
  } catch {
    // 失败时不更新本地状态，Switch 自动保持原状态
  } finally {
    switchLoadingId.value = undefined
  }
}

function getPriorityTagType(priority?: string): 'danger' | 'warning' | 'primary' | 'success' | 'info' {
  if (priority === 'urgent') {
    return 'danger'
  }
  if (priority === 'high') {
    return 'warning'
  }
  if (priority === 'medium') {
    return 'primary'
  }
  if (priority === 'low') {
    return 'success'
  }
  return 'info'
}

onMounted(async () => {
  await loadPolicyList()
})
</script>

<template>
  <el-space direction="vertical" fill :size="16">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">SLA 管理</span>
          <el-button type="primary" @click="openCreateDialog">新增策略</el-button>
        </div>
      </template>

      <el-form :inline="true" label-width="72px">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="请输入策略名称或描述"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="query.priority" clearable placeholder="全部优先级">
            <el-option label="紧急" value="urgent" />
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.isActive" clearable placeholder="全部状态">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-space>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button link @click="loadPolicyList">刷新</el-button>
          </el-space>
        </el-form-item>
      </el-form>

      <EmptyState v-if="!tableLoading && total === 0" description="暂无SLA策略" />
      <template v-else>
        <BaseTable :data="pagedPolicyList" :loading="tableLoading" @sort-change="handleSortChange">
          <el-table-column prop="name" label="策略名称" min-width="140" sortable="custom" />
          <el-table-column label="优先级" width="100">
            <template #default="{ row }">
              <el-tag :type="getPriorityTagType(row.priority)">
                {{ row.priorityLabel || row.priority }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="responseTime" label="响应时限(分钟)" width="130" sortable="custom" />
          <el-table-column prop="resolveTime" label="解决时限(分钟)" width="130" sortable="custom" />
          <el-table-column prop="warningPct" label="预警阈值(%)" width="120" sortable="custom" />
          <el-table-column prop="criticalPct" label="超时阈值(%)" width="120" sortable="custom" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-switch
                :model-value="row.isActive"
                :active-value="1"
                :inactive-value="0"
                :loading="switchLoadingId === row.id"
                @change="handleSwitchChange(row, $event)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="180" sortable="custom">
            <template #default="{ row }">
              {{ formatDateTime(row.updateTime || row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
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
  </el-space>

  <el-dialog
    v-model="dialogVisible"
    :title="dialogMode === 'create' ? '新增SLA策略' : '编辑SLA策略'"
    width="620px"
  >
    <el-form label-width="120px">
      <el-form-item label="策略名称" required>
        <el-input v-model="form.name" maxlength="50" show-word-limit />
      </el-form-item>
      <el-form-item label="优先级" required>
        <el-select v-model="form.priority" placeholder="请选择优先级">
          <el-option label="紧急" value="urgent" />
          <el-option label="高" value="high" />
          <el-option label="中" value="medium" />
          <el-option label="低" value="low" />
        </el-select>
      </el-form-item>
      <el-form-item label="响应时限(分钟)" required>
        <el-input-number v-model="form.responseTime" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="解决时限(分钟)" required>
        <el-input-number v-model="form.resolveTime" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="预警阈值(%)" required>
        <el-input-number v-model="form.warningPct" :min="1" :max="100" controls-position="right" />
      </el-form-item>
      <el-form-item label="超时阈值(%)" required>
        <el-input-number v-model="form.criticalPct" :min="1" :max="100" controls-position="right" />
      </el-form-item>
      <el-form-item label="策略说明">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="2"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="dialogSubmitLoading" @click="handleSubmit">保存</el-button>
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
</style>
