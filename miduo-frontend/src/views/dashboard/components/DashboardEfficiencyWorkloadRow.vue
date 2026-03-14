<script setup lang="ts">
import EmptyState from '@/components/common/EmptyState.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import type {
  DashboardEfficiencyOutput,
  DashboardSlaAchievementOutput,
  DashboardWorkloadOutput,
} from '@/types/dashboard'

defineProps<{
  efficiency: DashboardEfficiencyOutput
  slaAchievement: DashboardSlaAchievementOutput
  workload: DashboardWorkloadOutput[]
}>()

function toProgress(value: number): number {
  if (value < 0) {
    return 0
  }
  if (value > 100) {
    return 100
  }
  return Number(value.toFixed(2))
}
</script>

<template>
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
        <BaseTable v-else :data="(workload as unknown as Record<string, unknown>[])">
          <el-table-column prop="assigneeName" label="处理人" min-width="140" />
          <el-table-column prop="totalCount" label="处理总量" min-width="120" />
          <el-table-column prop="processingCount" label="处理中" min-width="120" />
          <el-table-column prop="completedCount" label="已完成" min-width="120" />
        </BaseTable>
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped lang="scss">
.section-row {
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
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
</style>
