<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getBugReportStatistics } from '@/api/bugreport'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { BugReportStatisticsOutput } from '@/types/bugreport'
import { notifySuccess } from '@/utils/feedback'

const router = useRouter()
const loading = ref(false)
const timeRange = ref<string[]>([])
/** 与当前时间范围一致的快捷预设天数；手动改日期后为 null */
const quickRangePreset = ref<number | null>(30)
const suppressPresetClear = ref(false)

const statistics = ref<BugReportStatisticsOutput>({
  logicCauseDistribution: [],
  defectCategoryDistribution: [],
  introducedProjectTop: [],
  responsibleStatistics: [],
  timelyCount: 0,
  totalCount: 0,
  timelyRate: 0,
})

function formatDateTimeText(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function applyQuickRange(days: number): void {
  quickRangePreset.value = days
  suppressPresetClear.value = true
  const end = new Date()
  const start = new Date(end)
  start.setDate(start.getDate() - (days - 1))
  start.setHours(0, 0, 0, 0)
  end.setHours(23, 59, 59, 0)
  timeRange.value = [formatDateTimeText(start), formatDateTimeText(end)]
  void nextTick(() => {
    suppressPresetClear.value = false
  })
  void loadStatistics()
}

function onDateRangeChange(): void {
  if (suppressPresetClear.value) {
    return
  }
  quickRangePreset.value = null
}

async function loadStatistics(): Promise<void> {
  loading.value = true
  try {
    const result = await getBugReportStatistics({
      createTimeStart: timeRange.value[0],
      createTimeEnd: timeRange.value[1],
    })
    statistics.value = {
      logicCauseDistribution: result.logicCauseDistribution || [],
      defectCategoryDistribution: result.defectCategoryDistribution || [],
      introducedProjectTop: result.introducedProjectTop || [],
      responsibleStatistics: result.responsibleStatistics || [],
      timelyCount: result.timelyCount || 0,
      totalCount: result.totalCount || 0,
      timelyRate: result.timelyRate || 0,
    }
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  quickRangePreset.value = null
  void loadStatistics()
}

function handleReset(): void {
  timeRange.value = []
  quickRangePreset.value = null
  void loadStatistics()
}

function csvEscape(value: string | number): string {
  const raw = String(value ?? '')
  if (raw.includes(',') || raw.includes('"') || raw.includes('\n')) {
    return `"${raw.replace(/"/g, '""')}"`
  }
  return raw
}

function downloadCsv(fileName: string, rows: Array<Array<string | number>>): void {
  const text = `\ufeff${rows.map((row) => row.map(csvEscape).join(',')).join('\n')}`
  const blob = new Blob([text], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  window.URL.revokeObjectURL(url)
}

function handleExport(): void {
  const startDate = timeRange.value[0] ? timeRange.value[0].slice(0, 10) : '全部'
  const endDate = timeRange.value[1] ? timeRange.value[1].slice(0, 10) : '全部'
  const rows: Array<Array<string | number>> = [
    ['Bug简报统计看板', `${startDate} ~ ${endDate}`],
    [],
    ['核心指标', '值'],
    ['简报总数', statistics.value.totalCount],
    ['及时提交数', statistics.value.timelyCount],
    ['及时率(%)', statistics.value.timelyRate],
    [],
    ['逻辑归因分布'],
    ['归因', '数量', '占比(%)'],
    ...statistics.value.logicCauseDistribution.map((item) => [item.name, item.count, item.rate ?? '']),
    [],
    ['缺陷分类分布'],
    ['分类', '数量', '占比(%)'],
    ...statistics.value.defectCategoryDistribution.map((item) => [item.name, item.count, item.rate ?? '']),
    [],
    ['引入项目Top'],
    ['项目', '数量', '占比(%)'],
    ...statistics.value.introducedProjectTop.map((item) => [item.name, item.count, item.rate ?? '']),
    [],
    ['责任人统计'],
    ['责任人', '数量'],
    ...statistics.value.responsibleStatistics.map((item) => [item.userName || item.userId, item.count]),
  ]
  downloadCsv(`Bug简报统计_${startDate}_${endDate}.csv`, rows)
  notifySuccess('统计数据导出成功')
}

onMounted(() => {
  applyQuickRange(30)
})
</script>

<template>
  <div class="statistics-page" v-loading="loading">
    <el-card shadow="never" class="filter-card">
      <div class="toolbar">
        <div class="title">Bug简报统计看板</div>
        <el-space class="toolbar-actions" wrap>
          <el-button @click="router.push('/bug-report')">返回列表</el-button>
          <el-button @click="loadStatistics">刷新</el-button>
          <el-button type="primary" @click="handleExport">导出CSV</el-button>
        </el-space>
      </div>

      <el-form :inline="true" label-width="72px" class="query-form">
        <el-form-item label="统计周期" class="query-form-item">
          <el-date-picker
            v-model="timeRange"
            class="query-input query-input--wide"
            type="daterange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            @change="onDateRangeChange"
          />
        </el-form-item>
        <el-form-item class="query-form-item query-form-actions">
          <el-space wrap class="query-action-row">
            <el-button :type="quickRangePreset === 1 ? 'primary' : 'default'" @click="applyQuickRange(1)">
              今天
            </el-button>
            <el-button :type="quickRangePreset === 7 ? 'primary' : 'default'" @click="applyQuickRange(7)">
              近7天
            </el-button>
            <el-button :type="quickRangePreset === 30 ? 'primary' : 'default'" @click="applyQuickRange(30)">
              近30天
            </el-button>
            <el-button :type="quickRangePreset === 90 ? 'primary' : 'default'" @click="applyQuickRange(90)">
              近90天
            </el-button>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-space>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="metrics-card">
      <div class="metric-wrapper">
        <div class="metric-item">
          <div class="label">简报总数</div>
          <div class="value">{{ statistics.totalCount }}</div>
        </div>
        <div class="metric-item">
          <div class="label">及时提交数</div>
          <div class="value success">{{ statistics.timelyCount }}</div>
        </div>
        <div class="metric-item">
          <div class="label">及时率（%）</div>
          <div class="value">{{ statistics.timelyRate }}</div>
        </div>
      </div>
    </el-card>

    <div class="board-grid">
      <el-card shadow="never" class="table-card">
        <template #header>
          <div class="section-title">逻辑归因分布</div>
        </template>
        <EmptyState
          v-if="statistics.logicCauseDistribution.length === 0"
          description="暂无逻辑归因分布数据"
        />
        <BaseTable v-else :data="statistics.logicCauseDistribution">
          <el-table-column prop="name" label="归因" min-width="220" />
          <el-table-column prop="count" label="数量" width="100" align="center" />
          <el-table-column label="占比" width="100" align="center">
            <template #default="{ row }">
              {{ row.rate != null ? row.rate + '%' : '-' }}
            </template>
          </el-table-column>
        </BaseTable>
      </el-card>

      <el-card shadow="never" class="table-card">
        <template #header>
          <div class="section-title">缺陷分类分布</div>
        </template>
        <EmptyState
          v-if="statistics.defectCategoryDistribution.length === 0"
          description="暂无缺陷分类分布数据"
        />
        <BaseTable v-else :data="statistics.defectCategoryDistribution">
          <el-table-column prop="name" label="分类" min-width="220" />
          <el-table-column prop="count" label="数量" width="100" align="center" />
          <el-table-column label="占比" width="100" align="center">
            <template #default="{ row }">
              {{ row.rate != null ? row.rate + '%' : '-' }}
            </template>
          </el-table-column>
        </BaseTable>
      </el-card>

      <el-card shadow="never" class="table-card">
        <template #header>
          <div class="section-title">引入项目 Top</div>
        </template>
        <EmptyState v-if="statistics.introducedProjectTop.length === 0" description="暂无引入项目统计数据" />
        <BaseTable v-else :data="statistics.introducedProjectTop">
          <el-table-column prop="name" label="项目" min-width="220" />
          <el-table-column prop="count" label="数量" width="100" align="center" />
          <el-table-column label="占比" width="100" align="center">
            <template #default="{ row }">
              {{ row.rate != null ? row.rate + '%' : '-' }}
            </template>
          </el-table-column>
        </BaseTable>
      </el-card>

      <el-card shadow="never" class="table-card">
        <template #header>
          <div class="section-title">责任人统计</div>
        </template>
        <EmptyState v-if="statistics.responsibleStatistics.length === 0" description="暂无责任人统计数据" />
        <BaseTable v-else :data="statistics.responsibleStatistics">
          <el-table-column prop="userName" label="责任人" min-width="220">
            <template #default="{ row }">
              {{ row.userName || row.userId }}
            </template>
          </el-table-column>
          <el-table-column prop="count" label="数量" width="120" />
        </BaseTable>
      </el-card>
    </div>
  </div>
</template>

<style scoped lang="scss">
.statistics-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: calc(100vh - 180px);
}

.filter-card {
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

.query-input--wide {
  width: 280px;
}

.query-form-actions {
  margin-left: auto;
  margin-right: 0;
}

.query-action-row {
  width: auto;
}

.metrics-card {
  :deep(.el-card__body) {
    padding: 18px 20px;
  }
}

.metric-wrapper {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.metric-item {
  background: linear-gradient(135deg, #f7faff 0%, #eef5ff 100%);
  border: 1px solid #dbe9ff;
  border-radius: 10px;
  padding: 18px 16px;

  .label {
    color: #909399;
    font-size: 13px;
  }

  .value {
    color: #1675d1;
    font-size: 30px;
    font-weight: 700;
    margin-top: 6px;
  }

  .success {
    color: #67c23a;
  }
}

.section-title {
  font-size: 17px;
  font-weight: 600;
  color: #1d2129;
}

.board-grid {
  flex: 1;
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(420px, 1fr));
  grid-auto-rows: minmax(320px, 1fr);
}

.table-card {
  height: 100%;

  :deep(.el-card__body) {
    height: calc(100% - 58px);
    display: flex;
    flex-direction: column;
  }
}

@media (max-width: 991px) {
  .query-form {
    padding: 10px 12px;
  }

  .metric-wrapper {
    grid-template-columns: 1fr;
  }

  .board-grid {
    grid-template-columns: 1fr;
    grid-auto-rows: minmax(280px, auto);
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

  .query-input,
  .query-input--wide {
    width: 100%;
  }

  .query-form-actions {
    margin-left: 0;
  }

  .query-action-row {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
