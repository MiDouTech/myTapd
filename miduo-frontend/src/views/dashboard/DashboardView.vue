<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Grid } from '@element-plus/icons-vue'
import VueDraggable from 'vuedraggable'

import {
  getDashboardCategoryDistribution,
  getDashboardEfficiency,
  getDashboardOverview,
  getDashboardSlaAchievement,
  getDashboardTrend,
  getDashboardWorkload,
} from '@/api/dashboard'
import { useDashboardLayoutStore } from '@/stores/dashboardLayout'
import type {
  DashboardCategoryDistributionOutput,
  DashboardEfficiencyOutput,
  DashboardOverviewOutput,
  DashboardSlaAchievementOutput,
  DashboardTrendPointOutput,
  DashboardWorkloadOutput,
} from '@/types/dashboard'
import type { DashboardLayoutItem } from '@/types/dashboardLayout'
import { notifyError } from '@/utils/feedback'
import DashboardEfficiencyWorkloadRow from './components/DashboardEfficiencyWorkloadRow.vue'
import DashboardOverviewRow from './components/DashboardOverviewRow.vue'
import DashboardTrendCategoryRow from './components/DashboardTrendCategoryRow.vue'

const layoutStore = useDashboardLayoutStore()
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

/**
 * draggableRows: 双向绑定给 VueDraggable 的可拖拽行组列表
 * 修改此数组仅影响 editingLayout 中的非固定部分
 */
const draggableRows = computed({
  get: (): DashboardLayoutItem[] => layoutStore.draggableLayout,
  set: (newList: DashboardLayoutItem[]) => {
    layoutStore.updateEditingOrder(newList)
  },
})

function onDragEnd(): void {
  // draggableRows setter already handles the reorder via updateEditingOrder
}

async function handleSaveLayout(): Promise<void> {
  try {
    await layoutStore.saveLayout()
    ElMessage.success('布局已保存')
  } catch (error) {
    ElMessage.error((error as Error).message || '保存布局失败')
  }
}

async function handleResetLayout(): Promise<void> {
  try {
    await ElMessageBox.confirm(
      '确认恢复为系统默认布局？您当前的自定义布局将被清除。',
      '恢复默认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await layoutStore.resetLayout()
    ElMessage.success('已恢复默认布局')
  } catch {
    // user cancelled
  }
}

async function loadDashboard(): Promise<void> {
  loading.value = true
  try {
    const [overviewData, trendData, categoryData, efficiencyData, slaData, workloadData] =
      await Promise.all([
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
  void Promise.all([layoutStore.fetchLayout(), loadDashboard()])
})
</script>

<template>
  <div class="dashboard-page" v-loading="loading">
    <!-- 页面头部：标题 + 操作按钮 -->
    <div class="dashboard-header">
      <div class="dashboard-title">数据看板</div>
      <div class="header-actions">
        <!-- 查看模式：编辑布局按钮 -->
        <el-button
          v-if="!layoutStore.isEditMode"
          class="edit-layout-btn"
          type="primary"
          plain
          size="small"
          :icon="Edit"
          @click="layoutStore.enterEditMode()"
        >
          编辑布局
        </el-button>
        <!-- 编辑模式：恢复默认 / 取消 / 保存布局 -->
        <div v-else class="edit-mode-actions">
          <el-button type="default" link @click="handleResetLayout">恢复默认</el-button>
          <el-button size="small" @click="layoutStore.cancelEditMode()">取消</el-button>
          <el-button
            type="primary"
            size="small"
            :loading="layoutStore.saving"
            @click="handleSaveLayout"
          >
            保存布局
          </el-button>
        </div>
      </div>
    </div>

    <!-- 编辑模式提示条 -->
    <el-alert
      v-if="layoutStore.isEditMode"
      class="edit-tip"
      type="info"
      :closable="false"
      show-icon
      title="拖拽模块可调整顺序，仅对您自己生效"
    />

    <!-- 固定置顶：overview 行组（不参与拖拽） -->
    <DashboardOverviewRow :data="overview" />

    <!-- 编辑模式：vuedraggable 包裹可拖拽行组 -->
    <VueDraggable
      v-if="layoutStore.isEditMode"
      v-model="draggableRows"
      item-key="rowGroupKey"
      handle=".drag-handle"
      :animation="200"
      ghost-class="drag-ghost"
      @end="onDragEnd"
    >
      <template #item="{ element }">
        <div class="draggable-row-wrapper edit-mode">
          <div class="drag-handle">
            <el-icon><Grid /></el-icon>
          </div>
          <DashboardTrendCategoryRow
            v-if="element.rowGroupKey === 'trend_category'"
            :trend="trend"
            :categories="categories"
          />
          <DashboardEfficiencyWorkloadRow
            v-else-if="element.rowGroupKey === 'efficiency_workload'"
            :efficiency="efficiency"
            :sla-achievement="slaAchievement"
            :workload="workload"
          />
        </div>
      </template>
    </VueDraggable>

    <!-- 查看模式：普通 v-for -->
    <template v-else v-for="rowGroup in layoutStore.draggableLayout" :key="rowGroup.rowGroupKey">
      <DashboardTrendCategoryRow
        v-if="rowGroup.rowGroupKey === 'trend_category'"
        :trend="trend"
        :categories="categories"
      />
      <DashboardEfficiencyWorkloadRow
        v-else-if="rowGroup.rowGroupKey === 'efficiency_workload'"
        :efficiency="efficiency"
        :sla-achievement="slaAchievement"
        :workload="workload"
      />
    </template>
  </div>
</template>

<style scoped lang="scss">
.dashboard-page {
  // Global layout styles only
}

.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.dashboard-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.edit-mode-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.edit-tip {
  margin-bottom: 12px;
}

.draggable-row-wrapper {
  position: relative;

  &.edit-mode {
    border: 1px dashed #1675d1;
    border-radius: 8px;
    background: rgba(22, 117, 209, 0.02);
    margin-bottom: 16px;
    padding-top: 8px;
  }
}

.drag-handle {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 10;
  cursor: grab;
  color: #909399;
  font-size: 16px;
  padding: 4px;
  border-radius: 4px;
  transition: color 0.2s;

  &:hover {
    color: #1675d1;
  }

  &:active {
    cursor: grabbing;
  }
}

:global(.drag-ghost) {
  opacity: 0.4;
  background: #e8f4ff;
  border: 2px solid #1675d1 !important;
}

@media (max-width: 767px) {
  .edit-layout-btn,
  .edit-mode-actions,
  .edit-tip {
    display: none !important;
  }
}
</style>
