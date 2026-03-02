<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getCategoryTree } from '@/api/category'
import { getTicketPage } from '@/api/ticket'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { CategoryTreeOutput } from '@/types/category'
import type { TicketListOutput, TicketPageInput, TicketView } from '@/types/ticket'
import { formatDateTime } from '@/utils/formatter'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const categoryTree = ref<CategoryTreeOutput[]>([])
const tableData = ref<TicketListOutput[]>([])
const total = ref(0)

const query = reactive<TicketPageInput>({
  pageNum: 1,
  pageSize: 20,
  view: 'my_created',
  ticketNo: '',
  title: '',
  categoryId: undefined,
  status: '',
  priority: '',
  creatorId: undefined,
  assigneeId: undefined,
  orderBy: undefined,
  asc: false,
})

const timeRange = ref<string[]>([])

const viewTabs: Array<{ label: string; value: TicketView }> = [
  { label: '我创建的', value: 'my_created' },
  { label: '我待办的', value: 'my_todo' },
  { label: '我参与的', value: 'my_participated' },
  { label: '我关注的', value: 'my_followed' },
  { label: '所有工单', value: 'all' },
]

const categoryOptions = computed(() => {
  const options: Array<{ label: string; value: number }> = []
  const walk = (nodes: CategoryTreeOutput[], prefix = ''): void => {
    nodes.forEach((node) => {
      const label = prefix ? `${prefix} / ${node.name}` : node.name
      options.push({ label, value: node.id })
      if (node.children?.length) {
        walk(node.children, label)
      }
    })
  }
  walk(categoryTree.value)
  return options
})

function normalizeViewFromRoute(): TicketView {
  if (route.path === '/ticket/all') {
    return 'all'
  }
  const routeView = route.query.view
  const values = viewTabs.map((item) => item.value)
  if (typeof routeView === 'string' && values.includes(routeView as TicketView)) {
    return routeView as TicketView
  }
  return 'my_created'
}

async function loadCategoryTree(): Promise<void> {
  categoryTree.value = await getCategoryTree()
}

async function loadTickets(): Promise<void> {
  loading.value = true
  try {
    const response = await getTicketPage({
      ...query,
      createTimeStart: timeRange.value[0],
      createTimeEnd: timeRange.value[1],
    })
    tableData.value = response.records
    total.value = response.total
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  query.pageNum = 1
  loadTickets()
}

function handleReset(): void {
  query.ticketNo = ''
  query.title = ''
  query.categoryId = undefined
  query.status = ''
  query.priority = ''
  query.creatorId = undefined
  query.assigneeId = undefined
  timeRange.value = []
  query.pageNum = 1
  loadTickets()
}

function handleTabChange(value: string | number): void {
  const view = value as TicketView
  if (view === 'all') {
    router.push('/ticket/all')
    return
  }
  router.push({
    path: '/ticket/mine',
    query: {
      view,
    },
  })
}

function handleSortChange(payload: {
  prop: string
  order: 'ascending' | 'descending' | null
}): void {
  const sortMap: Record<string, string> = {
    createTime: 'create_time',
    updateTime: 'update_time',
    priority: 'priority',
  }
  query.orderBy = payload.order && payload.prop ? sortMap[payload.prop] || payload.prop : undefined
  query.asc = payload.order === 'ascending'
  loadTickets()
}

function handlePaginationChange(payload: { pageNum: number; pageSize: number }): void {
  query.pageNum = payload.pageNum
  query.pageSize = payload.pageSize
  loadTickets()
}

function openDetail(row: TicketListOutput): void {
  router.push(`/ticket/detail/${row.id}`)
}

function getStatusType(status?: string): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  if (status === 'resolved' || status === 'closed') {
    return 'success'
  }
  if (status === 'pending_accept' || status === 'pending') {
    return 'warning'
  }
  if (status === 'processing') {
    return 'primary'
  }
  return 'info'
}

function getPriorityType(priority?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (priority === 'urgent') {
    return 'danger'
  }
  if (priority === 'high') {
    return 'warning'
  }
  if (priority === 'low') {
    return 'success'
  }
  return 'info'
}

watch(
  () => route.fullPath,
  () => {
    query.view = normalizeViewFromRoute()
    query.pageNum = 1
    loadTickets()
  },
  { immediate: true },
)

onMounted(() => {
  loadCategoryTree()
})
</script>

<template>
  <el-space direction="vertical" fill :size="16">
    <el-card shadow="never">
      <el-tabs :model-value="query.view" @tab-change="handleTabChange">
        <el-tab-pane
          v-for="tab in viewTabs"
          :key="tab.value"
          :label="tab.label"
          :name="tab.value"
        />
      </el-tabs>
      <el-form :inline="true" label-width="72px">
        <el-form-item label="工单编号">
          <el-input v-model="query.ticketNo" placeholder="请输入编号" clearable />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="query.title" placeholder="请输入标题" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryId" placeholder="请选择分类" clearable filterable>
            <el-option
              v-for="option in categoryOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="请选择状态" clearable>
            <el-option label="待受理" value="pending" />
            <el-option label="处理中" value="processing" />
            <el-option label="待验收" value="pending_accept" />
            <el-option label="已完成" value="resolved" />
            <el-option label="已关闭" value="closed" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="query.priority" placeholder="请选择优先级" clearable>
            <el-option label="紧急" value="urgent" />
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="timeRange"
            type="daterange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item>
          <el-space>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-space>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <EmptyState v-if="!loading && tableData.length === 0" description="暂无工单数据" />
      <template v-else>
        <BaseTable :data="tableData" :loading="loading" @sort-change="handleSortChange">
          <el-table-column prop="ticketNo" label="工单编号" width="160" sortable="custom" />
          <el-table-column
            prop="title"
            label="标题"
            min-width="220"
            :show-overflow-tooltip="true"
          />
          <el-table-column prop="categoryName" label="分类" min-width="140" />
          <el-table-column label="优先级" width="100" sortable="custom" prop="priority">
            <template #default="{ row }">
              <el-tag :type="getPriorityType(row.priority)">{{
                row.priorityLabel || row.priority
              }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">{{ row.statusLabel || row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="creatorName" label="创建人" width="120" />
          <el-table-column prop="assigneeName" label="处理人" width="120" />
          <el-table-column prop="createTime" label="创建时间" width="180" sortable="custom">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="180" sortable="custom">
            <template #default="{ row }">
              {{ formatDateTime(row.updateTime) }}
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
  </el-space>
</template>
