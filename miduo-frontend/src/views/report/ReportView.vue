<script setup lang="ts">
import { onMounted, ref } from 'vue'

import {
  getDashboardCategoryDistribution,
  getDashboardEfficiency,
  getDashboardOverview,
  getDashboardSlaAchievement,
  getDashboardTrend,
  getDashboardWorkload,
} from '@/api/dashboard'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type {
  DashboardCategoryDistributionOutput,
  DashboardEfficiencyOutput,
  DashboardOverviewOutput,
  DashboardSlaAchievementOutput,
  DashboardTrendPointOutput,
  DashboardWorkloadOutput,
} from '@/types/dashboard'
import { notifyError, notifySuccess } from '@/utils/feedback'

type ReportTab = 'trend' | 'category' | 'efficiency' | 'sla' | 'workload' | 'overtime'

const loading = ref(false)
const activeTab = ref<ReportTab>('trend')
const rangeDays = ref(30)

const trend = ref<DashboardTrendPointOutput[]>([])
const categories = ref<DashboardCategoryDistributionOutput[]>([])
const efficiency = ref<DashboardEfficiencyOutput>({
  avgResponseMinutes: 0,
  avgResolveMinutes: 0,
  completedCount: 0,
  totalCount: 0,
  completionRate: 0,
})
const slaAchievement = ref<DashboardSlaAchievementOutput>({
  totalCount: 0,
  achievedCount: 0,
  breachedCount: 0,
  achievementRate: 0,
})
const overview = ref<DashboardOverviewOutput>({
  pendingAcceptCount: 0,
  processingCount: 0,
  suspendedCount: 0,
  completedCount: 0,
  slaBreachedCount: 0,
  totalCount: 0,
})
const workload = ref<DashboardWorkloadOutput[]>([])

async function loadReports(): Promise<void> {
  loading.value = true
  try {
    const [trendData, categoryData, efficiencyData, slaData, workloadData, overviewData] = await Promise.all([
      getDashboardTrend({ days: rangeDays.value }),
      getDashboardCategoryDistribution(),
      getDashboardEfficiency(),
      getDashboardSlaAchievement(),
      getDashboardWorkload({ limit: 10 }),
      getDashboardOverview(),
    ])
    trend.value = trendData || []
    categories.value = categoryData || []
    efficiency.value = efficiencyData
    slaAchievement.value = slaData
    workload.value = workloadData || []
    overview.value = overviewData
  } catch (error) {
    notifyError((error as Error).message || '加载报表失败')
  } finally {
    loading.value = false
  }
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
  const date = new Date().toISOString().slice(0, 10)
  if (activeTab.value === 'trend') {
    downloadCsv(
      `工单趋势报表-${date}.csv`,
      [['日期', '新建工单', '关闭工单', '积压工单'], ...trend.value.map((item) => [item.day, item.createdCount, item.closedCount, item.backlogCount])],
    )
  } else if (activeTab.value === 'category') {
    downloadCsv(
      `分类分布报表-${date}.csv`,
      [['分类', '工单数量', '占比(%)'], ...categories.value.map((item) => [item.categoryName, item.ticketCount, item.percentage])],
    )
  } else if (activeTab.value === 'workload') {
    downloadCsv(
      `人员工作量报表-${date}.csv`,
      [['处理人', '处理总量', '处理中', '已完成'], ...workload.value.map((item) => [item.assigneeName || '-', item.totalCount, item.processingCount, item.completedCount])],
    )
  } else if (activeTab.value === 'efficiency') {
    downloadCsv(`处理效率报表-${date}.csv`, [
      ['指标', '值'],
      ['平均响应时长(分钟)', efficiency.value.avgResponseMinutes],
      ['平均解决时长(分钟)', efficiency.value.avgResolveMinutes],
      ['处理完成率(%)', efficiency.value.completionRate],
      ['已完成工单', efficiency.value.completedCount],
      ['工单总量', efficiency.value.totalCount],
    ])
  } else if (activeTab.value === 'sla') {
    downloadCsv(`SLA达成报表-${date}.csv`, [
      ['指标', '值'],
      ['总计', slaAchievement.value.totalCount],
      ['达成', slaAchievement.value.achievedCount],
      ['超时', slaAchievement.value.breachedCount],
      ['达成率(%)', slaAchievement.value.achievementRate],
    ])
  } else {
    downloadCsv(`超时工单报表-${date}.csv`, [
      ['指标', '值'],
      ['工单总量', overview.value.totalCount],
      ['SLA超时工单', overview.value.slaBreachedCount],
      ['超时占比(%)', overview.value.totalCount > 0 ? ((overview.value.slaBreachedCount / overview.value.totalCount) * 100).toFixed(2) : 0],
    ])
  }
  notifySuccess('报表导出成功')
}

onMounted(() => {
  loadReports()
})
</script>

<template>
  <el-space direction="vertical" fill :size="16" v-loading="loading">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="left">
          <span class="title">报表中心</span>
          <el-select v-model="rangeDays" class="days-select" @change="loadReports">
            <el-option :value="7" label="近7天" />
            <el-option :value="14" label="近14天" />
            <el-option :value="30" label="近30天" />
            <el-option :value="60" label="近60天" />
          </el-select>
        </div>
        <el-space>
          <el-button @click="loadReports">刷新</el-button>
          <el-button type="primary" @click="handleExport">导出报表</el-button>
        </el-space>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="工单趋势" name="trend" />
        <el-tab-pane label="分类分布" name="category" />
        <el-tab-pane label="处理效率" name="efficiency" />
        <el-tab-pane label="SLA达成" name="sla" />
        <el-tab-pane label="人员工作量" name="workload" />
        <el-tab-pane label="超时工单" name="overtime" />
      </el-tabs>

      <div v-if="activeTab === 'trend'">
        <EmptyState v-if="trend.length === 0" description="暂无趋势数据" />
        <BaseTable v-else :data="trend">
          <el-table-column prop="day" label="日期" min-width="140" />
          <el-table-column prop="createdCount" label="新建工单" min-width="120" />
          <el-table-column prop="closedCount" label="关闭工单" min-width="120" />
          <el-table-column prop="backlogCount" label="积压工单" min-width="120" />
        </BaseTable>
      </div>

      <div v-else-if="activeTab === 'category'">
        <EmptyState v-if="categories.length === 0" description="暂无分类分布数据" />
        <BaseTable v-else :data="categories">
          <el-table-column prop="categoryName" label="分类" min-width="180" />
          <el-table-column prop="ticketCount" label="工单数量" min-width="140" />
          <el-table-column prop="percentage" label="占比(%)" min-width="120" />
        </BaseTable>
      </div>

      <div v-else-if="activeTab === 'efficiency'" class="metric-wrapper">
        <div class="metric-item">
          <div class="label">平均响应时长（分钟）</div>
          <div class="value">{{ efficiency.avgResponseMinutes }}</div>
        </div>
        <div class="metric-item">
          <div class="label">平均解决时长（分钟）</div>
          <div class="value">{{ efficiency.avgResolveMinutes }}</div>
        </div>
        <div class="metric-item">
          <div class="label">处理完成率（%）</div>
          <div class="value">{{ efficiency.completionRate }}</div>
        </div>
      </div>

      <div v-else-if="activeTab === 'sla'" class="metric-wrapper">
        <div class="metric-item">
          <div class="label">总计</div>
          <div class="value">{{ slaAchievement.totalCount }}</div>
        </div>
        <div class="metric-item">
          <div class="label">达成</div>
          <div class="value success">{{ slaAchievement.achievedCount }}</div>
        </div>
        <div class="metric-item">
          <div class="label">超时</div>
          <div class="value danger">{{ slaAchievement.breachedCount }}</div>
        </div>
        <div class="metric-item">
          <div class="label">达成率（%）</div>
          <div class="value">{{ slaAchievement.achievementRate }}</div>
        </div>
      </div>

      <div v-else-if="activeTab === 'workload'">
        <EmptyState v-if="workload.length === 0" description="暂无人员工作量数据" />
        <BaseTable v-else :data="workload">
          <el-table-column prop="assigneeName" label="处理人" min-width="140" />
          <el-table-column prop="totalCount" label="处理总量" min-width="120" />
          <el-table-column prop="processingCount" label="处理中" min-width="120" />
          <el-table-column prop="completedCount" label="已完成" min-width="120" />
        </BaseTable>
      </div>

      <div v-else class="metric-wrapper">
        <div class="metric-item">
          <div class="label">工单总量</div>
          <div class="value">{{ overview.totalCount }}</div>
        </div>
        <div class="metric-item">
          <div class="label">SLA超时工单</div>
          <div class="value danger">{{ overview.slaBreachedCount }}</div>
        </div>
        <div class="metric-item">
          <div class="label">超时占比（%）</div>
          <div class="value">
            {{
              overview.totalCount > 0
                ? ((overview.slaBreachedCount / overview.totalCount) * 100).toFixed(2)
                : 0
            }}
          </div>
        </div>
      </div>
    </el-card>
  </el-space>
</template>

<style scoped lang="scss">
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.days-select {
  width: 120px;
}

.metric-wrapper {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 12px;
}

.metric-item {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 14px;

  .label {
    color: #909399;
    font-size: 13px;
  }

  .value {
    color: #1675d1;
    font-size: 24px;
    font-weight: 600;
    margin-top: 6px;
  }

  .success {
    color: #67c23a;
  }

  .danger {
    color: #f56c6c;
  }
}

@media (max-width: 991px) {
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .metric-wrapper {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }
}
</style>
