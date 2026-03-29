<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import {
  CircleCheck,
  DataAnalysis,
  DataLine,
  Download,
  Histogram,
  Refresh,
  Timer,
  User,
  Warning,
} from '@element-plus/icons-vue'

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

const overtimeRate = computed(() => {
  if (overview.value.totalCount === 0) return 0
  return ((overview.value.slaBreachedCount / overview.value.totalCount) * 100).toFixed(2)
})

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
      ['超时占比(%)', overtimeRate.value],
    ])
  }
  notifySuccess('报表导出成功')
}

onMounted(() => {
  loadReports()
})
</script>

<template>
  <div class="report-center" v-loading="loading">
    <div class="page-actions">
      <el-radio-group v-model="rangeDays" @change="loadReports" class="range-group">
        <el-radio-button :value="1">今天</el-radio-button>
        <el-radio-button :value="7">近7天</el-radio-button>
        <el-radio-button :value="14">近14天</el-radio-button>
        <el-radio-button :value="30">近30天</el-radio-button>
        <el-radio-button :value="60">近60天</el-radio-button>
      </el-radio-group>
      <el-button :icon="Refresh" circle @click="loadReports" title="刷新" />
      <el-button type="primary" :icon="Download" @click="handleExport">导出报表</el-button>
    </div>

    <!-- Overview Summary Cards -->
    <div class="overview-grid">
      <div class="summary-card blue">
        <div class="summary-body">
          <div class="summary-label">工单总量</div>
          <div class="summary-value">{{ overview.totalCount }}</div>
          <div class="summary-desc">统计周期内全部工单</div>
        </div>
        <div class="summary-icon">
          <el-icon><DataLine /></el-icon>
        </div>
      </div>
      <div class="summary-card orange">
        <div class="summary-body">
          <div class="summary-label">处理中</div>
          <div class="summary-value">{{ overview.processingCount }}</div>
          <div class="summary-desc">当前正在处理的工单</div>
        </div>
        <div class="summary-icon">
          <el-icon><Timer /></el-icon>
        </div>
      </div>
      <div class="summary-card green">
        <div class="summary-body">
          <div class="summary-label">已完成</div>
          <div class="summary-value">{{ overview.completedCount }}</div>
          <div class="summary-desc">已关闭解决的工单</div>
        </div>
        <div class="summary-icon">
          <el-icon><CircleCheck /></el-icon>
        </div>
      </div>
      <div class="summary-card red">
        <div class="summary-body">
          <div class="summary-label">SLA超时</div>
          <div class="summary-value">{{ overview.slaBreachedCount }}</div>
          <div class="summary-desc">超出服务时限的工单</div>
        </div>
        <div class="summary-icon">
          <el-icon><Warning /></el-icon>
        </div>
      </div>
    </div>

    <!-- Tabbed Report Content -->
    <el-card shadow="never" class="report-card">
      <el-tabs v-model="activeTab" class="report-tabs">
        <el-tab-pane name="trend">
          <template #label>
            <span class="tab-label"><el-icon><DataLine /></el-icon>工单趋势</span>
          </template>
          <div class="tab-content">
            <EmptyState v-if="trend.length === 0" description="暂无趋势数据" />
            <BaseTable v-else :data="trend as unknown as Record<string, unknown>[]">
              <el-table-column prop="day" label="日期" min-width="140" />
              <el-table-column prop="createdCount" label="新建工单" min-width="120">
                <template #default="{ row }">
                  <span class="badge badge-blue">{{ row.createdCount }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="closedCount" label="关闭工单" min-width="120">
                <template #default="{ row }">
                  <span class="badge badge-green">{{ row.closedCount }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="backlogCount" label="积压工单" min-width="120">
                <template #default="{ row }">
                  <span class="badge badge-amber">{{ row.backlogCount }}</span>
                </template>
              </el-table-column>
            </BaseTable>
          </div>
        </el-tab-pane>

        <el-tab-pane name="category">
          <template #label>
            <span class="tab-label"><el-icon><Histogram /></el-icon>分类分布</span>
          </template>
          <div class="tab-content">
            <EmptyState v-if="categories.length === 0" description="暂无分类分布数据" />
            <BaseTable v-else :data="categories as unknown as Record<string, unknown>[]">
              <el-table-column prop="categoryName" label="分类" min-width="180" />
              <el-table-column prop="ticketCount" label="工单数量" min-width="120">
                <template #default="{ row }">
                  <span class="num-primary">{{ row.ticketCount }}</span>
                </template>
              </el-table-column>
              <el-table-column label="占比" min-width="200">
                <template #default="{ row }">
                  <div class="progress-cell">
                    <el-progress
                      :percentage="Number(row.percentage) || 0"
                      :stroke-width="8"
                      color="#1675d1"
                      :show-text="false"
                      class="pct-bar"
                    />
                    <span class="pct-text">{{ row.percentage }}%</span>
                  </div>
                </template>
              </el-table-column>
            </BaseTable>
          </div>
        </el-tab-pane>

        <el-tab-pane name="efficiency">
          <template #label>
            <span class="tab-label"><el-icon><Timer /></el-icon>处理效率</span>
          </template>
          <div class="tab-content">
            <div class="metric-grid">
              <div class="metric-card">
                <div class="metric-icon blue">
                  <el-icon><Timer /></el-icon>
                </div>
                <div class="metric-body">
                  <div class="metric-value">{{ efficiency.avgResponseMinutes }}<span class="metric-unit">分钟</span></div>
                  <div class="metric-label">平均响应时长</div>
                </div>
              </div>
              <div class="metric-card">
                <div class="metric-icon purple">
                  <el-icon><DataAnalysis /></el-icon>
                </div>
                <div class="metric-body">
                  <div class="metric-value">{{ efficiency.avgResolveMinutes }}<span class="metric-unit">分钟</span></div>
                  <div class="metric-label">平均解决时长</div>
                </div>
              </div>
              <div class="metric-card">
                <div class="metric-icon green">
                  <el-icon><CircleCheck /></el-icon>
                </div>
                <div class="metric-body">
                  <div class="metric-value">{{ efficiency.completionRate }}<span class="metric-unit">%</span></div>
                  <div class="metric-label">处理完成率</div>
                  <el-progress
                    :percentage="Number(efficiency.completionRate) || 0"
                    :stroke-width="6"
                    color="#10b981"
                    :show-text="false"
                    class="metric-progress"
                  />
                </div>
              </div>
              <div class="metric-card">
                <div class="metric-icon orange">
                  <el-icon><DataLine /></el-icon>
                </div>
                <div class="metric-body">
                  <div class="metric-value">{{ efficiency.completedCount }}<span class="metric-unit"> / {{ efficiency.totalCount }}</span></div>
                  <div class="metric-label">完成 / 总计（工单数）</div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="sla">
          <template #label>
            <span class="tab-label"><el-icon><CircleCheck /></el-icon>SLA达成</span>
          </template>
          <div class="tab-content">
            <div class="sla-layout">
              <div class="sla-rate-panel">
                <div class="sla-rate-label">SLA达成率</div>
                <el-progress
                  type="circle"
                  :percentage="Number(slaAchievement.achievementRate) || 0"
                  :width="140"
                  :stroke-width="12"
                  :color="Number(slaAchievement.achievementRate) >= 80 ? '#10b981' : '#f59e0b'"
                >
                  <template #default="{ percentage }">
                    <span class="sla-circle-value">{{ percentage }}%</span>
                  </template>
                </el-progress>
                <div class="sla-rate-desc">目标达成率 ≥ 80%</div>
              </div>
              <div class="sla-metrics">
                <div class="metric-card">
                  <div class="metric-icon blue">
                    <el-icon><DataLine /></el-icon>
                  </div>
                  <div class="metric-body">
                    <div class="metric-value">{{ slaAchievement.totalCount }}</div>
                    <div class="metric-label">工单总计</div>
                  </div>
                </div>
                <div class="metric-card">
                  <div class="metric-icon green">
                    <el-icon><CircleCheck /></el-icon>
                  </div>
                  <div class="metric-body">
                    <div class="metric-value success-text">{{ slaAchievement.achievedCount }}</div>
                    <div class="metric-label">按时达成</div>
                  </div>
                </div>
                <div class="metric-card">
                  <div class="metric-icon red">
                    <el-icon><Warning /></el-icon>
                  </div>
                  <div class="metric-body">
                    <div class="metric-value danger-text">{{ slaAchievement.breachedCount }}</div>
                    <div class="metric-label">超时违约</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="workload">
          <template #label>
            <span class="tab-label"><el-icon><User /></el-icon>人员工作量</span>
          </template>
          <div class="tab-content">
            <EmptyState v-if="workload.length === 0" description="暂无人员工作量数据" />
            <BaseTable v-else :data="workload as unknown as Record<string, unknown>[]">
              <el-table-column prop="assigneeName" label="处理人" min-width="140">
                <template #default="{ row }">
                  <div class="user-cell">
                    <div class="user-avatar">{{ String(row.assigneeName || '-').charAt(0) }}</div>
                    <span>{{ row.assigneeName || '-' }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="totalCount" label="处理总量" min-width="110">
                <template #default="{ row }">
                  <span class="num-primary">{{ row.totalCount }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="processingCount" label="处理中" min-width="110">
                <template #default="{ row }">
                  <span class="badge badge-orange">{{ row.processingCount }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="completedCount" label="已完成" min-width="110">
                <template #default="{ row }">
                  <span class="badge badge-green">{{ row.completedCount }}</span>
                </template>
              </el-table-column>
              <el-table-column label="完成率" min-width="160">
                <template #default="{ row }">
                  <div class="progress-cell">
                    <el-progress
                      :percentage="row.totalCount > 0 ? Math.round((Number(row.completedCount) / Number(row.totalCount)) * 100) : 0"
                      :stroke-width="8"
                      color="#10b981"
                      :show-text="false"
                      class="pct-bar"
                    />
                    <span class="pct-text">
                      {{ row.totalCount > 0 ? Math.round((Number(row.completedCount) / Number(row.totalCount)) * 100) : 0 }}%
                    </span>
                  </div>
                </template>
              </el-table-column>
            </BaseTable>
          </div>
        </el-tab-pane>

        <el-tab-pane name="overtime">
          <template #label>
            <span class="tab-label"><el-icon><Warning /></el-icon>超时工单</span>
          </template>
          <div class="tab-content">
            <div class="overtime-layout">
              <div class="overtime-highlight">
                <div class="overtime-icon"><el-icon><Warning /></el-icon></div>
                <div class="overtime-value danger-text">{{ overtimeRate }}%</div>
                <div class="overtime-label">超时占比</div>
                <el-progress
                  :percentage="Number(overtimeRate) || 0"
                  :stroke-width="10"
                  color="#ef4444"
                  :show-text="false"
                  class="overtime-bar"
                />
              </div>
              <div class="overtime-metrics">
                <div class="metric-card">
                  <div class="metric-icon blue">
                    <el-icon><DataLine /></el-icon>
                  </div>
                  <div class="metric-body">
                    <div class="metric-value">{{ overview.totalCount }}</div>
                    <div class="metric-label">工单总量</div>
                  </div>
                </div>
                <div class="metric-card">
                  <div class="metric-icon red">
                    <el-icon><Warning /></el-icon>
                  </div>
                  <div class="metric-body">
                    <div class="metric-value danger-text">{{ overview.slaBreachedCount }}</div>
                    <div class="metric-label">SLA超时工单</div>
                  </div>
                </div>
                <div class="metric-card">
                  <div class="metric-icon orange">
                    <el-icon><Timer /></el-icon>
                  </div>
                  <div class="metric-body">
                    <div class="metric-value">{{ overview.pendingAcceptCount }}</div>
                    <div class="metric-label">待受理工单</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
$primary: #1675d1;
$success: #10b981;
$warning: #f59e0b;
$danger: #ef4444;
$purple: #8b5cf6;
$text-primary: #1e293b;
$text-secondary: #64748b;
$border: #e2e8f0;
$bg: #f8fafc;

.report-center {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ── Page Actions ── */
.page-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-bottom: 14px;
}

/* ── Overview Cards ── */
.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.summary-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 22px;
  border-radius: 10px;
  border: 1px solid $border;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
    border-radius: 10px 0 0 10px;
  }

  &.blue::before { background: $primary; }
  &.orange::before { background: $warning; }
  &.green::before { background: $success; }
  &.red::before { background: $danger; }

  .summary-body {
    padding-left: 4px;
  }

  .summary-label {
    font-size: 13px;
    color: $text-secondary;
    font-weight: 500;
  }

  .summary-value {
    font-size: 32px;
    font-weight: 700;
    line-height: 1.1;
    margin: 4px 0 4px;
    color: $text-primary;
  }

  .summary-desc {
    font-size: 12px;
    color: #94a3b8;
  }

  .summary-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    flex-shrink: 0;
  }

  &.blue .summary-icon { background: #eff6ff; color: $primary; }
  &.orange .summary-icon { background: #fffbeb; color: $warning; }
  &.green .summary-icon { background: #f0fdf4; color: $success; }
  &.red .summary-icon { background: #fef2f2; color: $danger; }
}

/* ── Report Card ── */
.report-card {
  border-radius: 10px;
  border: 1px solid $border;

  :deep(.el-card__body) {
    padding: 0 20px 20px;
  }
}

/* ── Tabs ── */
.report-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 20px;
    border-bottom: 1px solid $border;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__item) {
    height: 48px;
    line-height: 48px;
    font-size: 14px;
    color: $text-secondary;
    padding: 0 16px;

    &.is-active {
      color: $primary;
      font-weight: 600;
    }
  }

  :deep(.el-tabs__active-bar) {
    background-color: $primary;
    height: 3px;
    border-radius: 3px 3px 0 0;
  }
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 5px;
}

.tab-content {
  min-height: 200px;
}

/* ── Badges ── */
.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  min-width: 36px;
  text-align: center;
}

.badge-blue  { background: #eff6ff; color: $primary; }
.badge-green { background: #f0fdf4; color: $success; }
.badge-amber { background: #fffbeb; color: $warning; }
.badge-orange { background: #fff7ed; color: #ea580c; }

.num-primary {
  color: $primary;
  font-weight: 600;
  font-size: 15px;
}

/* ── Progress cell ── */
.progress-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .pct-bar {
    flex: 1;
  }

  .pct-text {
    font-size: 12px;
    color: $text-secondary;
    min-width: 38px;
    text-align: right;
  }
}

/* ── Metric Cards ── */
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.metric-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  background: $bg;
  border: 1px solid $border;
  border-radius: 10px;
  padding: 18px 16px;

  .metric-icon {
    width: 44px;
    height: 44px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    flex-shrink: 0;

    &.blue   { background: #eff6ff; color: $primary; }
    &.green  { background: #f0fdf4; color: $success; }
    &.orange { background: #fff7ed; color: #ea580c; }
    &.purple { background: #f5f3ff; color: $purple; }
    &.red    { background: #fef2f2; color: $danger; }
  }

  .metric-body {
    flex: 1;
    min-width: 0;
  }

  .metric-value {
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
    line-height: 1.1;
  }

  .metric-unit {
    font-size: 14px;
    font-weight: 400;
    color: $text-secondary;
    margin-left: 4px;
  }

  .metric-label {
    font-size: 13px;
    color: $text-secondary;
    margin-top: 4px;
  }

  .metric-progress {
    margin-top: 8px;
  }
}

/* ── SLA Layout ── */
.sla-layout {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.sla-rate-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  background: $bg;
  border: 1px solid $border;
  border-radius: 10px;
  padding: 28px 36px;
  flex-shrink: 0;

  .sla-rate-label {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
  }

  .sla-circle-value {
    font-size: 22px;
    font-weight: 700;
    color: $text-primary;
  }

  .sla-rate-desc {
    font-size: 12px;
    color: $text-secondary;
  }
}

.sla-metrics {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

/* ── Color helpers ── */
.success-text { color: $success; }
.danger-text  { color: $danger; }

/* ── User Cell ── */
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;

  .user-avatar {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: linear-gradient(135deg, $primary, #42a5f5);
    color: #fff;
    font-size: 12px;
    font-weight: 600;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }
}

/* ── Overtime Layout ── */
.overtime-layout {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.overtime-highlight {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  padding: 28px 36px;
  flex-shrink: 0;
  min-width: 180px;

  .overtime-icon {
    font-size: 32px;
    color: $danger;
  }

  .overtime-value {
    font-size: 36px;
    font-weight: 700;
    line-height: 1;
  }

  .overtime-label {
    font-size: 14px;
    color: $text-secondary;
  }

  .overtime-bar {
    width: 120px;
    margin-top: 4px;
  }
}

.overtime-metrics {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

/* ── Responsive ── */
@media (max-width: 1100px) {
  .overview-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .sla-metrics,
  .overtime-metrics {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-actions {
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  .overview-grid {
    grid-template-columns: 1fr 1fr;
  }

  .sla-layout,
  .overtime-layout {
    flex-direction: column;
  }

  .sla-rate-panel,
  .overtime-highlight {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
