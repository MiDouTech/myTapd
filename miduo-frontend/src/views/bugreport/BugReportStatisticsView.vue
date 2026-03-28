<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getBugReportStatistics } from '@/api/bugreport'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { BugReportStatisticsOutput } from '@/types/bugreport'
import { notifySuccess } from '@/utils/feedback'

const router = useRouter()
const loading = ref(false)
const timeRange = ref<string[]>([])

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
  const end = new Date()
  const start = new Date(end)
  start.setDate(start.getDate() - (days - 1))
  start.setHours(0, 0, 0, 0)
  end.setHours(23, 59, 59, 0)
  timeRange.value = [formatDateTimeText(start), formatDateTimeText(end)]
  void loadStatistics()
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
  void loadStatistics()
}

function handleReset(): void {
  timeRange.value = []
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
    <el-card shadow="never">
      <div class="toolbar">
        <el-space>
          <el-button @click="router.push('/bug-report')">返回列表</el-button>
          <el-button @click="loadStatistics">刷新</el-button>
          <el-button type="primary" @click="handleExport">导出CSV</el-button>
        </el-space>
      </div>

      <el-form :inline="true" label-width="84px" class="query-form">
        <el-form-item label="统计周期">
          <el-date-picker
            v-model="timeRange"
            type="daterange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
          />
        </el-form-item>
        <el-form-item>
          <el-space>
            <el-button @click="applyQuickRange(7)">近7天</el-button>
            <el-button @click="applyQuickRange(30)">近30天</el-button>
            <el-button @click="applyQuickRange(90)">近90天</el-button>
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

.toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.query-form {
  margin-top: 14px;
  padding: 14px 16px;
  background: var(--md-bg-panel, #f9fafb);
  border-radius: 8px;
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
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .metric-wrapper {
    grid-template-columns: 1fr;
  }

  .board-grid {
    grid-template-columns: 1fr;
    grid-auto-rows: minmax(280px, auto);
  }
}
</style>
