<script setup lang="ts">
import { computed } from 'vue'
import { Grid } from '@element-plus/icons-vue'
import VueDraggable from 'vuedraggable'

import EmptyState from '@/components/common/EmptyState.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import type {
  DashboardEfficiencyOutput,
  DashboardSlaAchievementOutput,
  DashboardWorkloadOutput,
} from '@/types/dashboard'

type EfficiencyWorkloadWidgetKey = 'efficiency' | 'workload'

const props = defineProps<{
  efficiency: DashboardEfficiencyOutput
  slaAchievement: DashboardSlaAchievementOutput
  workload: DashboardWorkloadOutput[]
  cardOrder?: EfficiencyWorkloadWidgetKey[]
  editable?: boolean
}>()

const emit = defineEmits<{
  'update:cardOrder': [value: EfficiencyWorkloadWidgetKey[]]
}>()

const defaultOrder: EfficiencyWorkloadWidgetKey[] = ['efficiency', 'workload']

const normalizedOrder = computed<EfficiencyWorkloadWidgetKey[]>(() => {
  const source = Array.isArray(props.cardOrder) ? props.cardOrder : defaultOrder
  const valid = source.filter((item) => defaultOrder.includes(item))
  const merged = [...valid]
  defaultOrder.forEach((item) => {
    if (!merged.includes(item)) {
      merged.push(item)
    }
  })
  return merged
})

const draggableCards = computed({
  get: (): Array<{ key: EfficiencyWorkloadWidgetKey }> =>
    normalizedOrder.value.map((key) => ({
      key,
    })),
  set: (value: Array<{ key: EfficiencyWorkloadWidgetKey }>) => {
    emit(
      'update:cardOrder',
      value.map((item) => item.key),
    )
  },
})

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
  <VueDraggable
    v-if="editable"
    v-model="draggableCards"
    item-key="key"
    handle=".card-drag-handle"
    :animation="200"
    class="widget-grid edit-mode"
    ghost-class="widget-drag-ghost"
  >
    <template #item="{ element }">
      <div :class="['widget-item', element.key === 'efficiency' ? 'span-medium' : 'span-large']">
        <el-card shadow="never">
          <template #header>
            <div class="header-with-handle">
              <el-icon class="card-drag-handle"><Grid /></el-icon>
              <div class="section-title">
                {{ element.key === 'efficiency' ? '处理效率与SLA' : '人员工作量 TOP10' }}
              </div>
            </div>
          </template>
          <template v-if="element.key === 'efficiency'">
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
          </template>
          <template v-else>
            <EmptyState v-if="workload.length === 0" description="暂无人员工作量数据" />
            <BaseTable v-else :data="(workload as unknown as Record<string, unknown>[])">
              <el-table-column prop="assigneeName" label="处理人" min-width="140" />
              <el-table-column prop="totalCount" label="处理总量" min-width="120" />
              <el-table-column prop="processingCount" label="处理中" min-width="120" />
              <el-table-column prop="completedCount" label="已完成" min-width="120" />
            </BaseTable>
          </template>
        </el-card>
      </div>
    </template>
  </VueDraggable>
  <div v-else class="widget-grid">
    <div
      v-for="key in normalizedOrder"
      :key="key"
      :class="['widget-item', key === 'efficiency' ? 'span-medium' : 'span-large']"
    >
      <el-card shadow="never">
        <template #header>
          <div class="section-title">{{ key === 'efficiency' ? '处理效率与SLA' : '人员工作量 TOP10' }}</div>
        </template>
        <template v-if="key === 'efficiency'">
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
        </template>
        <template v-else>
          <EmptyState v-if="workload.length === 0" description="暂无人员工作量数据" />
          <BaseTable v-else :data="(workload as unknown as Record<string, unknown>[])">
            <el-table-column prop="assigneeName" label="处理人" min-width="140" />
            <el-table-column prop="totalCount" label="处理总量" min-width="120" />
            <el-table-column prop="processingCount" label="处理中" min-width="120" />
            <el-table-column prop="completedCount" label="已完成" min-width="120" />
          </BaseTable>
        </template>
      </el-card>
    </div>
  </div>
</template>

<style scoped lang="scss">
.widget-grid {
  display: grid;
  grid-template-columns: repeat(24, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.widget-item {
  min-width: 0;
}

.span-medium {
  grid-column: span 10;
}

.span-large {
  grid-column: span 14;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

.header-with-handle {
  display: flex;
  align-items: center;
  gap: 6px;
}

.card-drag-handle {
  cursor: grab;
  color: #909399;

  &:hover {
    color: #1675d1;
  }
}

.edit-mode .widget-item :deep(.el-card) {
  border: 1px dashed #bfdcff;
}

.widget-drag-ghost {
  opacity: 0.45;
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
  .widget-grid {
    grid-template-columns: 1fr;
  }

  .span-medium,
  .span-large {
    grid-column: auto;
  }
}
</style>
