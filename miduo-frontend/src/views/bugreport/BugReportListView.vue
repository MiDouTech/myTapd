<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getBugReportPage, submitBugReport, voidBugReport } from '@/api/bugreport'
import { getUserList } from '@/api/user'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { BugReportPageInput, BugReportPageOutput, DefectCategoryOutput } from '@/types/bugreport'
import type { UserListOutput } from '@/types/user'
import { confirmAction, notifySuccess } from '@/utils/feedback'
import { formatDateTime } from '@/utils/formatter'
import { getCachedDefectCategoryDict } from '@/utils/bugreport-dict-cache'
import {
  BUG_REPORT_STATUS_OPTIONS,
  canSubmitBugReport,
  canVoidBugReport,
  getBugReportStatusLabel,
  getBugReportStatusTagType,
  isBugReportEditable,
} from '@/utils/bugreport'

const router = useRouter()

const loading = ref(false)
const isMobile = ref(false)
const MOBILE_BREAKPOINT = 768
const tableData = ref<BugReportPageOutput[]>([])
const total = ref(0)
const users = ref<UserListOutput[]>([])
const defectCategories = ref<DefectCategoryOutput[]>([])
const timeRange = ref<string[]>([])

const query = reactive<BugReportPageInput>({
  pageNum: 1,
  pageSize: 20,
  reportNo: '',
  status: '',
  defectCategory: '',
  reviewerId: undefined,
  responsibleUserId: undefined,
  createTimeStart: '',
  createTimeEnd: '',
  orderBy: 'update_time',
  asc: false,
})

const userOptions = computed(() =>
  users.value.map((item) => ({
    label: item.name,
    value: item.id,
  })),
)

async function loadBaseData(): Promise<void> {
  const [userList, defectCategoryList] = await Promise.all([
    getUserList({}),
    getCachedDefectCategoryDict(),
  ])
  users.value = userList || []
  defectCategories.value = defectCategoryList || []
}

async function loadList(): Promise<void> {
  loading.value = true
  try {
    const result = await getBugReportPage({
      ...query,
      reportNo: query.reportNo?.trim() || undefined,
      status: query.status || undefined,
      defectCategory: query.defectCategory || undefined,
      createTimeStart: timeRange.value[0],
      createTimeEnd: timeRange.value[1],
    })
    tableData.value = result.records || []
    total.value = result.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  query.pageNum = 1
  void loadList()
}

function handleReset(): void {
  query.reportNo = ''
  query.status = ''
  query.defectCategory = ''
  query.reviewerId = undefined
  query.responsibleUserId = undefined
  timeRange.value = []
  query.pageNum = 1
  query.orderBy = 'update_time'
  query.asc = false
  void loadList()
}

function handlePaginationChange(payload: { pageNum: number; pageSize: number }): void {
  query.pageNum = payload.pageNum
  query.pageSize = payload.pageSize
  void loadList()
}

function handleSortChange(payload: {
  prop: string
  order: 'ascending' | 'descending' | null
}): void {
  const sortMap: Record<string, string> = {
    createTime: 'create_time',
    updateTime: 'update_time',
    submittedAt: 'submitted_at',
  }
  query.orderBy = payload.order && payload.prop ? sortMap[payload.prop] || payload.prop : undefined
  query.asc = payload.order === 'ascending'
  void loadList()
}

function openDetail(row: BugReportPageOutput): void {
  router.push(`/bug-report/detail/${row.id}`)
}

function openEdit(row: BugReportPageOutput): void {
  router.push(`/bug-report/edit/${row.id}`)
}

function updateViewportState(): void {
  isMobile.value = window.innerWidth <= MOBILE_BREAKPOINT
}

async function handleSubmit(row: BugReportPageOutput): Promise<void> {
  await confirmAction(`确认提交简报【${row.reportNo}】进入审核吗？`)
  await submitBugReport(row.id)
  notifySuccess('提交审核成功')
  await loadList()
}

async function handleVoid(row: BugReportPageOutput): Promise<void> {
  await confirmAction(`确认作废简报【${row.reportNo}】吗？`)
  await voidBugReport(row.id)
  notifySuccess('简报已作废')
  await loadList()
}

onMounted(async () => {
  updateViewportState()
  window.addEventListener('resize', updateViewportState)
  await loadBaseData()
  await loadList()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewportState)
})
</script>

<template>
  <div class="bug-report-list-page">
    <el-card shadow="never" class="list-card">
      <div class="toolbar">
        <div class="title">Bug简报列表</div>
        <el-space class="toolbar-actions" wrap>
          <el-button @click="router.push('/bug-report/statistics')">查看统计</el-button>
          <el-button type="primary" @click="router.push('/bug-report/edit')">新建简报</el-button>
        </el-space>
      </div>

      <el-form :inline="true" label-width="72px" class="query-form" @submit.prevent="handleSearch">
        <el-form-item label="简报编号" class="query-form-item">
          <el-input
            v-model="query.reportNo"
            class="query-input"
            placeholder="请输入简报编号"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态" class="query-form-item">
          <el-select v-model="query.status" class="query-input" placeholder="请选择内容" clearable>
            <el-option
              v-for="option in BUG_REPORT_STATUS_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="缺陷分类" class="query-form-item">
          <el-select
            v-model="query.defectCategory"
            class="query-input"
            placeholder="请选择内容"
            clearable
            filterable
          >
            <el-option
              v-for="item in defectCategories"
              :key="item.id"
              :label="item.name"
              :value="item.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="审核人" class="query-form-item">
          <el-select
            v-model="query.reviewerId"
            class="query-input"
            placeholder="请选择内容"
            clearable
            filterable
          >
            <el-option
              v-for="option in userOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="责任人" class="query-form-item">
          <el-select
            v-model="query.responsibleUserId"
            class="query-input"
            placeholder="请选择内容"
            clearable
            filterable
          >
            <el-option
              v-for="option in userOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间" class="query-form-item">
          <el-date-picker
            v-model="timeRange"
            class="query-input"
            type="daterange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
          />
        </el-form-item>
        <el-form-item class="query-form-item query-form-actions">
          <el-space class="query-action-buttons">
            <el-button type="primary" native-type="submit" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-space>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="list-card">
      <EmptyState v-if="!loading && tableData.length === 0" description="暂无Bug简报数据" />
      <template v-else>
        <BaseTable :data="tableData" :loading="loading" @sort-change="handleSortChange">
          <el-table-column prop="reportNo" label="简报编号" min-width="180" sortable="custom">
            <template #default="{ row }">
              <el-button type="primary" link class="cell-link" @click="openDetail(row)">
                {{ row.reportNo }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column
            prop="problemDesc"
            label="标题"
            min-width="220"
            align="center"
            :show-overflow-tooltip="true"
          />
          <el-table-column label="状态" min-width="120">
            <template #default="{ row }">
              <el-tag :type="getBugReportStatusTagType(row.status)">
                {{ getBugReportStatusLabel(row.status, row.statusLabel) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="defectCategory" label="缺陷分类" min-width="150" />
          <el-table-column prop="severityLevel" label="严重级别" width="110" />
          <el-table-column prop="reviewerName" label="审核人" width="130" />
          <el-table-column prop="submittedAt" label="提交时间" width="180" sortable="custom">
            <template #default="{ row }">
              {{ formatDateTime(row.submittedAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="180" sortable="custom">
            <template #default="{ row }">
              {{ formatDateTime(row.updateTime) }}
            </template>
          </el-table-column>
          <el-table-column
            label="操作"
            :width="isMobile ? 90 : 260"
            align="center"
            :fixed="isMobile ? undefined : 'right'"
          >
            <template #default="{ row }">
              <template v-if="isMobile">
                <el-dropdown trigger="click">
                  <el-button type="primary" link class="mobile-operation-trigger">操作</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="openDetail(row)">详情</el-dropdown-item>
                      <el-dropdown-item v-if="isBugReportEditable(row.status)" @click="openEdit(row)">
                        编辑
                      </el-dropdown-item>
                      <el-dropdown-item v-if="canSubmitBugReport(row.status)" @click="handleSubmit(row)">
                        提交审核
                      </el-dropdown-item>
                      <el-dropdown-item v-if="canVoidBugReport(row.status)" @click="handleVoid(row)">
                        作废
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
              <el-space v-else :size="4">
                <el-button type="primary" link @click="openDetail(row)">详情</el-button>
                <el-button v-if="isBugReportEditable(row.status)" type="primary" link @click="openEdit(row)">
                  编辑
                </el-button>
                <el-button v-if="canSubmitBugReport(row.status)" type="primary" link @click="handleSubmit(row)">
                  提交审核
                </el-button>
                <el-button v-if="canVoidBugReport(row.status)" type="danger" link @click="handleVoid(row)">
                  作废
                </el-button>
              </el-space>
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
  </div>
</template>

<style scoped lang="scss">
.bug-report-list-page {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.list-card {
  width: 100%;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.title {
  font-size: 17px;
  font-weight: 600;
  color: #1d2129;
  line-height: 24px;
  white-space: nowrap;
  flex-shrink: 0;
}

.toolbar-actions {
  width: auto;
  flex: 0 0 auto;
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

.cell-link {
  padding: 0;
  font-weight: 500;

  &:hover {
    text-decoration: underline;
  }
}

.mobile-operation-trigger {
  min-height: 44px;
}

@media (max-width: 991px) {
  .query-form {
    padding: 10px 12px;
  }
}

@media (max-width: 768px) {
  .toolbar {
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

  .query-action-buttons :deep(.el-space__item) {
    width: calc(50% - 4px);
  }

  .query-action-buttons :deep(.el-button) {
    width: 100%;
  }
}
</style>
