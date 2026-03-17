<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import {
  getOperationLogDetail,
  getOperationLogModuleList,
  getOperationLogPage,
  getOperationLogStatistics,
} from '@/api/operationLog'
import type {
  OperationLogDetailOutput,
  OperationLogListOutput,
  OperationLogPageInput,
  OperationLogStatisticsOutput,
} from '@/types/operationLog'

// ─────────────────────────────────────────────
// 统计概览
// ─────────────────────────────────────────────
const statistics = ref<OperationLogStatisticsOutput>({
  todayTotalCount: 0,
  todayFailureCount: 0,
  todayActiveUserCount: 0,
  todaySecurityAlertCount: 0,
})

async function loadStatistics() {
  try {
    statistics.value = await getOperationLogStatistics()
  } catch {
    // 静默失败，不影响主列表
  }
}

// ─────────────────────────────────────────────
// 枚举数据
// ─────────────────────────────────────────────
const moduleList = ref<string[]>([])

async function loadEnums() {
  moduleList.value = await getOperationLogModuleList().catch(() => [] as string[])
}

// ─────────────────────────────────────────────
// 搜索表单
// ─────────────────────────────────────────────
const defaultSearchForm = (): OperationLogPageInput => ({
  pageNum: 1,
  pageSize: 20,
  startTime: getDefaultStartTime(),
  endTime: undefined,
  accountId: undefined,
  operatorName: undefined,
  operatorIp: undefined,
  logLevel: undefined,
  moduleName: undefined,
  operationItem: undefined,
  operationDetail: undefined,
  executeResult: undefined,
  sortField: 'operateTime',
  sortOrder: 'desc',
})

function getDefaultStartTime(): string {
  const d = new Date()
  d.setDate(d.getDate() - 7)
  return formatDateTime(d)
}

function formatDateTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} 00:00:00`
}

const searchForm = reactive<OperationLogPageInput>(defaultSearchForm())
const dateRange = ref<[string, string] | null>(null)

function onDateRangeChange(val: [string, string] | null) {
  if (val && val.length === 2) {
    searchForm.startTime = val[0] + ' 00:00:00'
    searchForm.endTime = val[1] + ' 23:59:59'
  } else {
    searchForm.startTime = undefined
    searchForm.endTime = undefined
  }
}

// ─────────────────────────────────────────────
// 列表数据
// ─────────────────────────────────────────────
const tableData = ref<OperationLogListOutput[]>([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const result = await getOperationLogPage(searchForm)
    tableData.value = result.records
    total.value = result.total
  } catch {
    ElMessage.error('查询失败，请重试')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  searchForm.pageNum = 1
  loadList()
}

function handleReset() {
  const defaults = defaultSearchForm()
  Object.assign(searchForm, defaults)
  dateRange.value = null
  loadList()
}

function handleSizeChange(size: number) {
  searchForm.pageSize = size
  searchForm.pageNum = 1
  loadList()
}

function handleCurrentChange(page: number) {
  searchForm.pageNum = page
  loadList()
}

function handleSortChange({ prop, order }: { prop: string; order: string | null }) {
  if (prop === 'operateTime') {
    searchForm.sortField = 'operateTime'
    searchForm.sortOrder = order === 'ascending' ? 'asc' : 'desc'
    loadList()
  }
}

// ─────────────────────────────────────────────
// 详情抽屉
// ─────────────────────────────────────────────
const drawerVisible = ref(false)
const detailLoading = ref(false)
const currentDetail = ref<OperationLogDetailOutput | null>(null)
const errorStackExpanded = ref(false)

async function openDetail(row: OperationLogListOutput) {
  drawerVisible.value = true
  detailLoading.value = true
  errorStackExpanded.value = false
  try {
    currentDetail.value = await getOperationLogDetail(row.id)
  } catch {
    ElMessage.error('获取详情失败')
    drawerVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

// ─────────────────────────────────────────────
// 样式辅助
// ─────────────────────────────────────────────
interface TagStyle {
  bg: string
  color: string
  border: string
}

const logLevelStyleMap: Record<string, TagStyle> = {
  SYSTEM: { bg: '#ecf5ff', color: '#409eff', border: '#b3d8ff' },
  BUSINESS: { bg: '#f0f9eb', color: '#67c23a', border: '#c2e7b0' },
  SECURITY: { bg: '#fdf6ec', color: '#e6a23c', border: '#f5dab1' },
  ERROR: { bg: '#fef0f0', color: '#f56c6c', border: '#fbc4c4' },
}

const executeResultStyleMap: Record<string, TagStyle> = {
  SUCCESS: { bg: '#f0f9eb', color: '#67c23a', border: '#c2e7b0' },
  FAILURE: { bg: '#fef0f0', color: '#f56c6c', border: '#fbc4c4' },
}

const defaultTagStyle: TagStyle = { bg: '#f0f9eb', color: '#67c23a', border: '#c2e7b0' }

function getLogLevelStyle(level: string): TagStyle {
  return logLevelStyleMap[level] ?? defaultTagStyle
}

function getExecuteResultStyle(result: string): TagStyle {
  return executeResultStyleMap[result] ?? defaultTagStyle
}

function formatJsonString(json: string | null | undefined): string {
  if (!json) return ''
  try {
    return JSON.stringify(JSON.parse(json), null, 2)
  } catch {
    return json
  }
}

// ─────────────────────────────────────────────
// 初始化
// ─────────────────────────────────────────────
onMounted(() => {
  loadStatistics()
  loadEnums()
  loadList()
})
</script>

<template>
  <div class="operation-log-view">
    <div class="page-header">
      <h2 class="page-title">工单日志</h2>
    </div>

    <!-- 统计概览卡片 -->
    <div class="statistics-row">
      <div class="stat-card">
        <div class="stat-value">{{ statistics.todayTotalCount }}</div>
        <div class="stat-label">今日操作总数</div>
      </div>
      <div class="stat-card stat-card--danger">
        <div class="stat-value">{{ statistics.todayFailureCount }}</div>
        <div class="stat-label">今日失败操作</div>
      </div>
      <div class="stat-card stat-card--info">
        <div class="stat-value">{{ statistics.todayActiveUserCount }}</div>
        <div class="stat-label">活跃操作人数</div>
      </div>
      <div class="stat-card stat-card--warning">
        <div class="stat-value">{{ statistics.todaySecurityAlertCount }}</div>
        <div class="stat-label">安全告警数</div>
      </div>
    </div>

    <!-- 搜索筛选区 -->
    <div class="search-panel">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="操作时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
            @change="onDateRangeChange"
          />
        </el-form-item>

        <el-form-item label="操作账号ID">
          <el-input
            v-model.number="searchForm.accountId"
            placeholder="精确匹配"
            clearable
            style="width: 140px"
          />
        </el-form-item>

        <el-form-item label="操作人">
          <el-input
            v-model="searchForm.operatorName"
            placeholder="模糊匹配"
            clearable
            style="width: 140px"
          />
        </el-form-item>

        <el-form-item label="操作人IP">
          <el-input
            v-model="searchForm.operatorIp"
            placeholder="支持前缀匹配"
            clearable
            style="width: 150px"
          />
        </el-form-item>

        <el-form-item label="日志级别">
          <el-select
            v-model="searchForm.logLevel"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option label="系统级" value="SYSTEM" />
            <el-option label="业务级" value="BUSINESS" />
            <el-option label="安全级" value="SECURITY" />
            <el-option label="错误级" value="ERROR" />
          </el-select>
        </el-form-item>

        <el-form-item label="操作模块">
          <el-input
            v-model="searchForm.moduleName"
            placeholder="模糊匹配"
            clearable
            style="width: 140px"
          />
        </el-form-item>

        <el-form-item label="操作项">
          <el-input
            v-model="searchForm.operationItem"
            placeholder="模糊匹配"
            clearable
            style="width: 160px"
          />
        </el-form-item>

        <el-form-item label="操作详情">
          <el-input
            v-model="searchForm.operationDetail"
            placeholder="关键词搜索"
            clearable
            style="width: 160px"
          />
        </el-form-item>

        <el-form-item label="执行结果">
          <el-select
            v-model="searchForm.executeResult"
            placeholder="全部"
            clearable
            style="width: 110px"
          >
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILURE" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 数据表格 -->
    <div class="table-panel">
      <el-table
        v-loading="loading"
        :data="tableData"
        :border="false"
        :stripe="true"
        :header-cell-style="{ backgroundColor: '#f5f7fa', textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
        style="width: 100%"
        @sort-change="handleSortChange"
      >
        <el-table-column
          prop="operateTime"
          label="操作时间"
          width="168"
          sortable="custom"
          fixed="left"
        >
          <template #default="{ row }">
            {{ row.operateTime ? row.operateTime.replace('T', ' ').slice(0, 19) : '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="accountId" label="账号ID" width="100" />

        <el-table-column prop="operatorName" label="操作人" width="90" />

        <el-table-column prop="operatorIp" label="操作人IP" width="140" />

        <el-table-column prop="logLevel" label="日志级别" width="100">
          <template #default="{ row }">
            <span
              class="level-tag"
              :style="{
                background: getLogLevelStyle(row.logLevel).bg,
                color: getLogLevelStyle(row.logLevel).color,
                border: `1px solid ${getLogLevelStyle(row.logLevel).border}`,
              }"
            >{{ row.logLevelDesc }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="moduleName" label="操作模块" min-width="120" show-overflow-tooltip />

        <el-table-column
          prop="requestPath"
          label="具体路径"
          min-width="180"
          show-overflow-tooltip
        />

        <el-table-column
          prop="operationItem"
          label="操作项"
          min-width="160"
          show-overflow-tooltip
        />

        <el-table-column prop="executeResult" label="执行结果" width="100">
          <template #default="{ row }">
            <span
              class="level-tag"
              :style="{
                background: getExecuteResultStyle(row.executeResult).bg,
                color: getExecuteResultStyle(row.executeResult).color,
                border: `1px solid ${getExecuteResultStyle(row.executeResult).border}`,
              }"
            >{{ row.executeResultDesc }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          :current-page="searchForm.pageNum"
          :page-size="searchForm.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="操作日志详情"
      direction="rtl"
      size="640px"
      :destroy-on-close="true"
    >
      <div v-if="detailLoading" class="detail-loading">
        <el-skeleton :rows="8" animated />
      </div>

      <div v-else-if="currentDetail" class="detail-content">
        <!-- 基础信息 -->
        <div class="detail-section">
          <div class="detail-section__title">基础信息</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="日志ID">{{ currentDetail.id }}</el-descriptions-item>
            <el-descriptions-item label="操作时间">
              {{ currentDetail.operateTime?.replace('T', ' ').slice(0, 23) }}
            </el-descriptions-item>
            <el-descriptions-item label="操作账号ID">{{
              currentDetail.accountId
            }}</el-descriptions-item>
            <el-descriptions-item label="操作人">{{
              currentDetail.operatorName
            }}</el-descriptions-item>
            <el-descriptions-item label="操作人IP">{{
              currentDetail.operatorIp
            }}</el-descriptions-item>
            <el-descriptions-item label="耗时">
              {{ currentDetail.costMillis }} ms
            </el-descriptions-item>
            <el-descriptions-item label="日志级别">
              <span
                class="level-tag"
                :style="{
                  background: getLogLevelStyle(currentDetail.logLevel).bg,
                  color: getLogLevelStyle(currentDetail.logLevel).color,
                  border: `1px solid ${getLogLevelStyle(currentDetail.logLevel).border}`,
                }"
              >{{ currentDetail.logLevelDesc }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="执行结果">
              <span
                class="level-tag"
                :style="{
                  background: getExecuteResultStyle(currentDetail.executeResult).bg,
                  color: getExecuteResultStyle(currentDetail.executeResult).color,
                  border: `1px solid ${getExecuteResultStyle(currentDetail.executeResult).border}`,
                }"
              >{{ currentDetail.executeResultDesc }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="操作模块">{{
              currentDetail.moduleName
            }}</el-descriptions-item>
            <el-descriptions-item label="具体路径" :span="2">
              <span class="mono-text"
                >{{ currentDetail.requestMethod }} {{ currentDetail.requestPath }}</span
              >
            </el-descriptions-item>
            <el-descriptions-item label="操作项" :span="2">{{
              currentDetail.operationItem
            }}</el-descriptions-item>
            <el-descriptions-item v-if="currentDetail.userAgent" label="User-Agent" :span="2">
              <span class="overflow-text">{{ currentDetail.userAgent }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 请求参数 -->
        <div v-if="currentDetail.requestParams" class="detail-section">
          <div class="detail-section__title">请求参数</div>
          <pre class="code-block">{{ formatJsonString(currentDetail.requestParams) }}</pre>
        </div>

        <!-- 变更记录 -->
        <div
          v-if="currentDetail.changeRecords && currentDetail.changeRecords.length > 0"
          class="detail-section"
        >
          <div class="detail-section__title">变更记录</div>
          <el-table :data="currentDetail.changeRecords" border size="small">
            <el-table-column prop="fieldName" label="字段名" width="140" />
            <el-table-column label="变更前">
              <template #default="{ row }">
                <span class="change-before">{{ row.beforeValue || '（空）' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="变更后">
              <template #default="{ row }">
                <span class="change-after">{{ row.afterValue || '（空）' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 错误信息（仅执行失败时展示） -->
        <div v-if="currentDetail.executeResult === 'FAILURE'" class="detail-section">
          <div class="detail-section__title">错误信息</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item v-if="currentDetail.errorCode" label="错误码">
              {{ currentDetail.errorCode }}
            </el-descriptions-item>
            <el-descriptions-item v-if="currentDetail.errorMessage" label="错误信息">
              {{ currentDetail.errorMessage }}
            </el-descriptions-item>
          </el-descriptions>
          <div v-if="currentDetail.errorStack" class="error-stack-wrapper">
            <div class="error-stack-header" @click="errorStackExpanded = !errorStackExpanded">
              <span>异常堆栈</span>
              <el-icon>
                <component :is="errorStackExpanded ? 'ArrowUp' : 'ArrowDown'" />
              </el-icon>
            </div>
            <pre v-if="errorStackExpanded" class="code-block code-block--error">{{
              currentDetail.errorStack
            }}</pre>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.operation-log-view {
  padding: 20px;
}

.page-header {
  margin-bottom: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1d2129;
  margin: 0;
}

// ─── 统计卡片 ──────────────────────────────
.statistics-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border-left: 4px solid #1675d1;

  &--danger {
    border-left-color: #f56c6c;
  }

  &--info {
    border-left-color: #409eff;
  }

  &--warning {
    border-left-color: #e6a23c;
  }
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1d2129;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #86909c;
  margin-top: 4px;
}

// ─── 搜索区 ───────────────────────────────
.search-panel {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px 4px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.search-form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
    margin-right: 16px;
  }
}

// ─── 表格区 ───────────────────────────────
.table-panel {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.level-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 20px;
  white-space: nowrap;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

// ─── 详情抽屉 ─────────────────────────────
.detail-loading {
  padding: 16px;
}

.detail-content {
  padding: 0 4px;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section__title {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid #1675d1;
}

.mono-text {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #4e5969;
}

.overflow-text {
  word-break: break-all;
  font-size: 12px;
  color: #86909c;
}

.code-block {
  background: #f5f7fa;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  padding: 12px 16px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
  color: #1d2129;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 300px;
  overflow-y: auto;

  &--error {
    background: #fff5f5;
    border-color: #fbc4c4;
    color: #f56c6c;
  }
}

.change-before {
  background: #fff5f5;
  color: #f56c6c;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 13px;
}

.change-after {
  background: #f0f9eb;
  color: #67c23a;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 13px;
}

.error-stack-wrapper {
  margin-top: 12px;
}

.error-stack-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #fef0f0;
  border: 1px solid #fbc4c4;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #f56c6c;
  user-select: none;

  &:hover {
    background: #fde2e2;
  }
}
</style>
