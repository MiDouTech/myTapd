<script setup lang="ts">
import { computed } from 'vue'
import { Grid } from '@element-plus/icons-vue'
import VueDraggable from 'vuedraggable'

import type { DashboardOverviewOutput } from '@/types/dashboard'

type OverviewCardKey =
  | 'pending_accept'
  | 'processing'
  | 'suspended'
  | 'completed'
  | 'sla_breached'
  | 'total'

const props = defineProps<{
  data: DashboardOverviewOutput
  cardOrder?: OverviewCardKey[]
  editable?: boolean
}>()

const emit = defineEmits<{
  'update:cardOrder': [value: OverviewCardKey[]]
}>()

const defaultOrder: OverviewCardKey[] = [
  'pending_accept',
  'processing',
  'suspended',
  'completed',
  'sla_breached',
  'total',
]

const cardMap = computed(() =>
  ({
    pending_accept: { key: 'pending_accept', label: '待受理', value: props.data.pendingAcceptCount },
    processing: { key: 'processing', label: '处理中', value: props.data.processingCount },
    suspended: { key: 'suspended', label: '已挂起', value: props.data.suspendedCount },
    completed: { key: 'completed', label: '已完成', value: props.data.completedCount },
    sla_breached: { key: 'sla_breached', label: 'SLA超时', value: props.data.slaBreachedCount },
    total: { key: 'total', label: '工单总量', value: props.data.totalCount },
  }) as Record<OverviewCardKey, { key: OverviewCardKey; label: string; value: number }>,
)

const normalizedOrder = computed<OverviewCardKey[]>(() => {
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

const topCards = computed(() => normalizedOrder.value.map((key) => cardMap.value[key]))

const draggableCards = computed({
  get: (): Array<{ key: OverviewCardKey; label: string; value: number }> =>
    normalizedOrder.value.map((key) => ({
      key,
      label: cardMap.value[key].label,
      value: cardMap.value[key].value,
    })),
  set: (value: Array<{ key: OverviewCardKey; label: string; value: number }>) => {
    emit(
      'update:cardOrder',
      value.map((item) => item.key),
    )
  },
})
</script>

<template>
  <VueDraggable
    v-if="editable"
    v-model="draggableCards"
    item-key="key"
    handle=".card-drag-handle"
    :animation="200"
    class="stat-grid edit-mode"
    ghost-class="card-drag-ghost"
  >
    <template #item="{ element }">
      <el-card shadow="hover" class="stat-card">
        <div class="card-head">
          <el-icon class="card-drag-handle"><Grid /></el-icon>
          <span class="stat-title">{{ element.label }}</span>
        </div>
        <div class="stat-value">{{ element.value }}</div>
      </el-card>
    </template>
  </VueDraggable>
  <div v-else class="stat-grid">
    <el-card v-for="card in topCards" :key="card.key" shadow="hover" class="stat-card">
      <div class="stat-title">{{ card.label }}</div>
      <div class="stat-value">{{ card.value }}</div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  min-height: 120px;

  .card-head {
    display: flex;
    align-items: center;
    gap: 6px;
  }
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

.edit-mode .stat-card {
  border: 1px dashed #bfdcff;
}

.card-drag-handle {
  cursor: grab;
  color: #909399;

  &:hover {
    color: #1675d1;
  }
}

.card-drag-ghost {
  opacity: 0.45;
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
