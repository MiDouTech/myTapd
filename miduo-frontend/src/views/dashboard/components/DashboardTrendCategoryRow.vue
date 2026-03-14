<script setup lang="ts">
import { computed } from 'vue'

import EmptyState from '@/components/common/EmptyState.vue'
import type { DashboardCategoryDistributionOutput, DashboardTrendPointOutput } from '@/types/dashboard'

const props = defineProps<{
  trend: DashboardTrendPointOutput[]
  categories: DashboardCategoryDistributionOutput[]
}>()

const trendMax = computed(() => {
  if (props.trend.length === 0) {
    return 1
  }
  return Math.max(
    ...props.trend.map((item) =>
      Math.max(item.createdCount || 0, item.closedCount || 0, item.backlogCount || 0),
    ),
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
</script>

<template>
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
          <div
            v-for="item in categories"
            :key="`${item.categoryId}-${item.categoryName}`"
            class="category-item"
          >
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
</template>

<style scoped lang="scss">
.section-row {
  margin-bottom: 16px;
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

@media (max-width: 991px) {
  .trend-item {
    grid-template-columns: 1fr;
  }
}
</style>
