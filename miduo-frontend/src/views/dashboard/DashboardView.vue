<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

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
import { notifyError } from '@/utils/feedback'

const loading = ref(false)

const overview = ref<DashboardOverviewOutput>({
  pendingAcceptCount: 0,
  processingCount: 0,
  suspendedCount: 0,
  completedCount: 0,
  slaBreachedCount: 0,
  totalCount: 0,
})

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
const workload = ref<DashboardWorkloadOutput[]>([])

const topCards = computed(() => [
  { label: '待受理', value: overview.value.pendingAcceptCount },
  { label: '处理中', value: overview.value.processingCount },
  { label: '已挂起', value: overview.value.suspendedCount },
  { label: '已完成', value: overview.value.completedCount },
  { label: 'SLA超时', value: overview.value.slaBreachedCount },
  { label: '工单总量', value: overview.value.totalCount },
])

const trendMax = computed(() => {
  if (trend.value.length === 0) {
    return 1
  }
  return Math.max(
    ...trend.value.map((item) => Math.max(item.createdCount || 0, item.closedCount || 0, item.backlogCount || 0)),
    1,
  )
})

function toPercent(value: number, max: number): number {
  if (max <= 0) {
    return 0
  }
  return Math.min(Math.round((value / max) * 100), 100)
}

function toProgress(value: number): number {
  if (value < 0) {
    return 0
  }
  if (value > 100) {
    return 100
  }
  return Number(value.toFixed(2))
}

async function loadDashboard(): Promise<void> {
  loading.value = true
  try {
    const [overviewData, trendData, categoryData, efficiencyData, slaData, workloadData] = await Promise.all([
      getDashboardOverview(),
      getDashboardTrend({ days: 14 }),
      getDashboardCategoryDistribution(),
      getDashboardEfficiency(),
      getDashboardSlaAchievement(),
      getDashboardWorkload({ limit: 10 }),
    ])

    overview.value = {
      pendingAcceptCount: overviewData.pendingAcceptCount || 0,
      processingCount: overviewData.processingCount || 0,
      suspendedCount: overviewData.suspendedCount || 0,
      completedCount: overviewData.completedCount || 0,
      slaBreachedCount: overviewData.slaBreachedCount || 0,
      totalCount: overviewData.totalCount || 0,
    }
    trend.value = trendData || []
    categories.value = categoryData || []
    efficiency.value = {
      avgResponseMinutes: efficiencyData.avgResponseMinutes || 0,
      avgResolveMinutes: efficiencyData.avgResolveMinutes || 0,
      completedCount: efficiencyData.completedCount || 0,
      totalCount: efficiencyData.totalCount || 0,
      completionRate: efficiencyData.completionRate || 0,
    }
    slaAchievement.value = {
      totalCount: slaData.totalCount || 0,
      achievedCount: slaData.achievedCount || 0,
      breachedCount: slaData.breachedCount || 0,
      achievementRate: slaData.achievementRate || 0,
    }
    workload.value = workloadData || []
  } catch (error) {
    notifyError((error as Error).message || '加载数据看板失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboard()
})
</script>

<template>
  <div class="dashboard-page" v-loading="loading">
    <el-row :gutter="16" class="stat-row">
      <el-col v-for="card in topCards" :key="card.label" :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">{{ card.label }}</div>
          <div class="stat-value">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never">
          <template #header>
            <div class="section-title">工单趋势（近14天）</div>
          </template>
          <EmptyState v-if="trend.length === 0" description="暂无趋势数据" />
          <div v-else class="trend-list">
            <div v-for="item in trend" :key="item.day" class="trend-item">
              <div class="trend-day">{{ item.day }}</div>
              <div class="trend-bars">
                <div class="trend-bar">
                  <span class="label created">新建 {{ item.createdCount }}</span>
                  <el-progress
                    :stroke-width="8"
                    :show-text="false"
                    :percentage="toPercent(item.createdCount || 0, trendMax)"
                    color="#1675d1"
                  />
                </div>
                <div class="trend-bar">
                  <span class="label closed">关闭 {{ item.closedCount }}</span>
                  <el-progress
                    :stroke-width="8"
                    :show-text="false"
                    :percentage="toPercent(item.closedCount || 0, trendMax)"
                    color="#67c23a"
                  />
                </div>
                <div class="trend-bar">
                  <span class="label backlog">积压 {{ item.backlogCount }}</span>
                  <el-progress
                    :stroke-width="8"
                    :show-text="false"
                    :percentage="toPercent(item.backlogCount || 0, trendMax)"
                    color="#e6a23c"
                  />
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never">
          <template #header>
            <div class="section-title">分类分布</div>
          </template>
          <EmptyState v-if="categories.length === 0" description="暂无分类分布数据" />
          <div v-else class="category-list">
            <div v-for="item in categories" :key="`${item.categoryId}-${item.categoryName}`" class="category-item">
              <div class="category-head">
                <span class="name">{{ item.categoryName }}</span>
                <span class="count">{{ item.ticketCount }}（{{ item.percentage }}%）</span>
              </div>
              <el-progress :show-text="false" :stroke-width="10" :percentage="toProgress(item.percentage || 0)" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :lg="10">
        <el-card shadow="never">
          <template #header>
            <div class="section-title">处理效率与SLA</div>
          </template>
          <div class="metric-grid">
            <div class="metric-item">
              <div class="metric-label">平均响应时长（分钟）</div>
              <div class="metric-value">{{ efficiency.avgResponseMinutes }}</div>
            </div>
            <div class="metric-item">
              <div class="metric-label">平均解决时长（分钟）</div>
              <div class="metric-value">{{ efficiency.avgResolveMinutes }}</div>
            </div>
          </div>
          <div class="circle-metrics">
            <div class="circle-item">
              <div class="circle-title">处理完成率</div>
              <el-progress
                type="circle"
                :percentage="toProgress(efficiency.completionRate || 0)"
                :stroke-width="8"
                color="#1675d1"
              />
            </div>
            <div class="circle-item">
              <div class="circle-title">SLA达成率</div>
              <el-progress
                type="circle"
                :percentage="toProgress(slaAchievement.achievementRate || 0)"
                :stroke-width="8"
                color="#67c23a"
              />
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="14">
        <el-card shadow="never">
          <template #header>
            <div class="section-title">人员工作量 TOP10</div>
          </template>
          <EmptyState v-if="workload.length === 0" description="暂无人员工作量数据" />
          <BaseTable v-else :data="workload">
            <el-table-column prop="assigneeName" label="处理人" min-width="140" />
            <el-table-column prop="totalCount" label="处理总量" min-width="120" />
            <el-table-column prop="processingCount" label="处理中" min-width="120" />
            <el-table-column prop="completedCount" label="已完成" min-width="120" />
          </BaseTable>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.dashboard-page {
  .stat-row,
  .section-row {
    margin-bottom: 16px;
  }
}

.stat-card {
  margin-bottom: 12px;
}

.stat-title {
  color: #909399;
  font-size: 14px;
}

.stat-value {
  color: #1675d1;
  font-size: 30px;
  font-weight: 600;
  margin-top: 8px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

.trend-list {
  max-height: 460px;
  overflow-y: auto;
  padding-right: 4px;
}

.trend-item {
  display: grid;
  grid-template-columns: 110px 1fr;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f2f5;
}

.trend-day {
  color: #606266;
  font-size: 13px;
  line-height: 22px;
}

.trend-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.trend-bar {
  .label {
    display: inline-block;
    min-width: 90px;
    margin-bottom: 4px;
    font-size: 12px;
  }

  .created {
    color: #1675d1;
  }

  .closed {
    color: #67c23a;
  }

  .backlog {
    color: #e6a23c;
  }
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.category-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;

  .name {
    color: #303133;
    font-size: 14px;
  }

  .count {
    color: #909399;
    font-size: 13px;
  }
}

.metric-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.metric-item {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px;

  .metric-label {
    color: #909399;
    font-size: 12px;
  }

  .metric-value {
    color: #1675d1;
    font-size: 22px;
    font-weight: 600;
    margin-top: 6px;
  }
}

.circle-metrics {
  margin-top: 20px;
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.circle-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;

  .circle-title {
    color: #606266;
    font-size: 13px;
  }
}

@media (max-width: 991px) {
  .trend-item {
    grid-template-columns: 1fr;
  }
}
</style>
