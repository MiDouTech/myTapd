<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { Edit } from '@element-plus/icons-vue'

import { getCategoryTree } from '@/api/category'
import { getTicketDetail, getTicketPage } from '@/api/ticket'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { CategoryTreeOutput } from '@/types/category'
import type { TicketDetailOutput, TicketListOutput, TicketPageInput, TicketView } from '@/types/ticket'
import { formatDateTime } from '@/utils/formatter'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const categoryTree = ref<CategoryTreeOutput[]>([])
const tableData = ref<TicketListOutput[]>([])
const total = ref(0)

const previewDrawerVisible = ref(false)
const previewLoading = ref(false)
const previewDetail = ref<TicketDetailOutput | null>(null)
const previewTicketId = ref<number | null>(null)

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

function reproduceEnvLabel(code?: string): string {
  if (code === 'PRODUCTION') return '生产环境'
  if (code === 'TEST') return '测试环境'
  if (code === 'BOTH') return '均可复现'
  return code || '-'
}

async function openTitlePreview(row: TicketListOutput): Promise<void> {
  previewTicketId.value = row.id
  previewDetail.value = null
  previewDrawerVisible.value = true
  previewLoading.value = true
  try {
    previewDetail.value = await getTicketDetail(row.id)
  } finally {
    previewLoading.value = false
  }
}

function goPreviewEdit(): void {
  const id = previewDetail.value?.id ?? previewTicketId.value
  if (id == null) {
    return
  }
  previewDrawerVisible.value = false
  router.push(`/ticket/detail/${id}`)
}

function getStatusType(status?: string): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  if (!status) return 'info'
  // 终态：完成/关闭/驳回
  if (['completed', 'closed', 'rejected'].includes(status)) {
    return 'success'
  }
  // 待处理类（需要人工介入）
  if (['pending_assign', 'pending_accept', 'pending_test_accept', 'pending_dev_accept', 'pending_verify', 'pending_cs_confirm'].includes(status)) {
    return 'warning'
  }
  // 进行中类
  if (['processing', 'testing', 'developing', 'executing'].includes(status)) {
    return 'primary'
  }
  // 挂起类
  if (status === 'suspended') {
    return 'danger'
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
          <el-select
            v-model="query.categoryId"
            class="query-select"
            placeholder="请选择内容"
            clearable
            filterable
          >
            <el-option
              v-for="option in categoryOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" class="query-select" placeholder="请选择内容" clearable>
            <!-- 通用工单状态 -->
            <el-option label="待分派" value="pending_assign" />
            <el-option label="待受理" value="pending_accept" />
            <el-option label="处理中" value="processing" />
            <el-option label="已挂起" value="suspended" />
            <el-option label="待验收" value="pending_verify" />
            <el-option label="已完成" value="completed" />
            <el-option label="已关闭" value="closed" />
            <!-- 缺陷工单专属状态 -->
            <el-option label="待测试受理" value="pending_test_accept" />
            <el-option label="测试中" value="testing" />
            <el-option label="待开发受理" value="pending_dev_accept" />
            <el-option label="开发中" value="developing" />
            <el-option label="待客服确认" value="pending_cs_confirm" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="query.priority" class="query-select" placeholder="请选择内容" clearable>
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
          <el-table-column prop="ticketNo" label="工单编号" width="160" sortable="custom">
            <template #default="{ row }">
              <el-button type="primary" link class="cell-link" @click="openDetail(row)">
                {{ row.ticketNo }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column
            prop="companyName"
            label="公司名称"
            min-width="140"
            :show-overflow-tooltip="true"
          >
            <template #default="{ row }">
              {{ row.companyName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="220" :show-overflow-tooltip="true">
            <template #default="{ row }">
              <el-button type="primary" link class="cell-link title-link" @click="openTitlePreview(row)">
                {{ row.title }}
              </el-button>
            </template>
          </el-table-column>
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

    <el-drawer
      v-model="previewDrawerVisible"
      direction="rtl"
      size="min(720px, 92vw)"
      destroy-on-close
      class="ticket-preview-drawer"
      :show-close="true"
    >
      <template #header>
        <div class="preview-drawer-header">
          <div class="preview-drawer-header-main">
            <div class="preview-drawer-meta">
              <el-tag v-if="previewDetail?.status" :type="getStatusType(previewDetail.status)" size="small">
                {{ previewDetail.statusLabel || previewDetail.status }}
              </el-tag>
              <span class="preview-ticket-no">{{ previewDetail?.ticketNo || '—' }}</span>
            </div>
            <h3 class="preview-title">{{ previewDetail?.title || '加载中…' }}</h3>
          </div>
          <el-button type="primary" :disabled="!previewDetail" @click="goPreviewEdit">
            <el-icon class="preview-edit-icon"><Edit /></el-icon>
            编辑
          </el-button>
        </div>
      </template>

      <div v-loading="previewLoading" class="preview-drawer-body">
        <template v-if="previewDetail">
          <div class="preview-layout">
            <div class="preview-main">
              <div v-if="previewDetail.description" class="preview-block">
                <div class="preview-block-label">描述</div>
                <!-- eslint-disable-next-line vue/no-v-html -->
                <div class="preview-html" v-html="previewDetail.description" />
              </div>

              <el-tabs class="preview-tabs">
                <el-tab-pane label="详细信息" name="detail">
                  <el-tabs class="preview-inner-tabs">
                    <el-tab-pane label="客服信息" name="customer">
                      <el-descriptions :column="1" border size="small" class="preview-desc">
                        <el-descriptions-item label="商户编号">
                          {{ previewDetail.bugCustomerInfo?.merchantNo || '-' }}
                        </el-descriptions-item>
                        <el-descriptions-item label="公司名称">
                          {{ previewDetail.bugCustomerInfo?.companyName || '-' }}
                        </el-descriptions-item>
                        <el-descriptions-item label="商户账号">
                          {{ previewDetail.bugCustomerInfo?.merchantAccount || '-' }}
                        </el-descriptions-item>
                        <el-descriptions-item label="场景码">
                          {{ previewDetail.bugCustomerInfo?.sceneCode || '-' }}
                        </el-descriptions-item>
                        <el-descriptions-item label="问题描述">
                          <span class="preview-plain">{{ previewDetail.bugCustomerInfo?.problemDesc || '-' }}</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="预期结果">
                          <span class="preview-plain">{{ previewDetail.bugCustomerInfo?.expectedResult || '-' }}</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="问题截图">
                          <span class="preview-plain">{{ previewDetail.bugCustomerInfo?.problemScreenshot || '-' }}</span>
                        </el-descriptions-item>
                      </el-descriptions>
                    </el-tab-pane>
                    <el-tab-pane label="测试信息" name="test">
                      <!-- eslint-disable vue/no-v-html — 与工单详情页一致，展示后端富文本字段 -->
                      <el-descriptions :column="1" border size="small" class="preview-desc">
                        <el-descriptions-item label="复现环境">
                          {{ reproduceEnvLabel(previewDetail.bugTestInfo?.reproduceEnv) }}
                        </el-descriptions-item>
                        <el-descriptions-item label="复现步骤">
                          <div
                            v-if="previewDetail.bugTestInfo?.reproduceSteps"
                            class="preview-html preview-html--compact"
                            v-html="previewDetail.bugTestInfo.reproduceSteps"
                          />
                          <span v-else>-</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="实际结果">
                          <div
                            v-if="previewDetail.bugTestInfo?.actualResult"
                            class="preview-html preview-html--compact"
                            v-html="previewDetail.bugTestInfo.actualResult"
                          />
                          <span v-else>-</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="影响范围">
                          {{ previewDetail.bugTestInfo?.impactScope || '-' }}
                        </el-descriptions-item>
                        <el-descriptions-item label="严重级别">
                          {{ previewDetail.bugTestInfo?.severityLevel || '-' }}
                        </el-descriptions-item>
                        <el-descriptions-item label="模块名称">
                          {{ previewDetail.bugTestInfo?.moduleName || '-' }}
                        </el-descriptions-item>
                        <el-descriptions-item label="测试备注">
                          <span class="preview-plain">{{ previewDetail.bugTestInfo?.testRemark || '-' }}</span>
                        </el-descriptions-item>
                      </el-descriptions>
                      <!-- eslint-enable vue/no-v-html -->
                    </el-tab-pane>
                    <el-tab-pane label="开发信息" name="dev">
                      <el-descriptions :column="1" border size="small" class="preview-desc">
                        <el-descriptions-item label="根因分析">
                          <span class="preview-plain">{{ previewDetail.bugDevInfo?.rootCause || '-' }}</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="修复方案">
                          <span class="preview-plain">{{ previewDetail.bugDevInfo?.fixSolution || '-' }}</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="Git 分支">
                          {{ previewDetail.bugDevInfo?.gitBranch || '-' }}
                        </el-descriptions-item>
                        <el-descriptions-item label="影响评估">
                          <span class="preview-plain">{{ previewDetail.bugDevInfo?.impactAssessment || '-' }}</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="开发备注">
                          <span class="preview-plain">{{ previewDetail.bugDevInfo?.devRemark || '-' }}</span>
                        </el-descriptions-item>
                      </el-descriptions>
                    </el-tab-pane>
                  </el-tabs>
                </el-tab-pane>
              </el-tabs>
            </div>

            <aside class="preview-side">
              <div class="preview-side-title">基础信息</div>
              <el-descriptions :column="1" border size="small" class="preview-desc preview-side-desc">
                <el-descriptions-item label="分类">
                  {{ previewDetail.categoryFullPath || previewDetail.categoryName || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="优先级">
                  <el-tag v-if="previewDetail.priority" :type="getPriorityType(previewDetail.priority)" size="small">
                    {{ previewDetail.priorityLabel || previewDetail.priority }}
                  </el-tag>
                  <span v-else>-</span>
                </el-descriptions-item>
                <el-descriptions-item label="创建人">
                  {{ previewDetail.creatorName || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="处理人">
                  {{ previewDetail.assigneeName || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="创建时间">
                  {{ formatDateTime(previewDetail.createTime) || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="更新时间">
                  {{ formatDateTime(previewDetail.updateTime) || '-' }}
                </el-descriptions-item>
                <el-descriptions-item v-if="previewDetail.expectedTime" label="预计结束">
                  {{ formatDateTime(previewDetail.expectedTime) }}
                </el-descriptions-item>
              </el-descriptions>
            </aside>
          </div>
        </template>
      </div>
    </el-drawer>
  </el-space>
</template>

<style scoped lang="scss">
.query-select {
  width: 220px;
  max-width: 100%;
}

.cell-link {
  padding: 0;
  font-weight: 500;
}

.title-link {
  text-align: left;
  white-space: normal;
  height: auto;
  line-height: 1.4;
}

.preview-drawer-header {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
  padding-right: 8px;
}

.preview-drawer-header-main {
  flex: 1;
  min-width: 0;
}

.preview-drawer-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 6px;
}

.preview-ticket-no {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.preview-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--el-text-color-primary);
}

.preview-edit-icon {
  margin-right: 4px;
  vertical-align: middle;
}

.preview-drawer-body {
  min-height: 200px;
}

.preview-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.preview-main {
  flex: 1;
  min-width: 0;
}

.preview-side {
  flex: 0 0 240px;
  position: sticky;
  top: 0;
}

.preview-side-title {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.preview-block {
  margin-bottom: 16px;
}

.preview-block-label {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-regular);
}

.preview-html {
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.preview-html--compact {
  max-height: 280px;
  padding: 8px;
  overflow: auto;
  background: var(--el-fill-color-lighter);
  border-radius: 4px;
}

.preview-plain {
  white-space: pre-wrap;
  word-break: break-word;
}

.preview-tabs {
  margin-top: 4px;
}

.preview-inner-tabs {
  margin-top: 8px;
}

.preview-desc {
  margin-top: 8px;
}

.preview-side-desc :deep(.el-descriptions__label) {
  width: 88px;
}

@media (max-width: 900px) {
  .preview-layout {
    flex-direction: column;
  }

  .preview-side {
    flex: none;
    width: 100%;
    position: static;
  }
}
</style>
