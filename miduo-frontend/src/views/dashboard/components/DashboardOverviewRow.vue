<script setup lang="ts">
import { computed } from 'vue'

import type { DashboardOverviewOutput } from '@/types/dashboard'

const props = defineProps<{
  data: DashboardOverviewOutput
}>()

const topCards = computed(() => [
  { label: '待受理', value: props.data.pendingAcceptCount },
  { label: '处理中', value: props.data.processingCount },
  { label: '已挂起', value: props.data.suspendedCount },
  { label: '已完成', value: props.data.completedCount },
  { label: 'SLA超时', value: props.data.slaBreachedCount },
  { label: '工单总量', value: props.data.totalCount },
])
</script>

<template>
  <el-row :gutter="16" class="stat-row">
    <el-col v-for="card in topCards" :key="card.label" :xs="12" :sm="8" :md="4">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-title">{{ card.label }}</div>
        <div class="stat-value">{{ card.value }}</div>
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped lang="scss">
.stat-row {
  margin-bottom: 16px;
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
</style>
