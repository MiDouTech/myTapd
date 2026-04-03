<script setup lang="ts">
import type { WorkflowObservationOutput } from '@/types/workflow'
import { formatDateTime, formatDurationSec, formatRoleLabel } from '@/utils/formatter'

defineProps<{
  loading?: boolean
  observation?: WorkflowObservationOutput
}>()
</script>

<template>
  <div v-loading="loading" class="workflow-observation-panel">
    <template v-if="observation">
      <div class="observation-summary">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">关联工单数</div>
          <div class="summary-value">{{ observation.ticketCount ?? 0 }}</div>
        </el-card>
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">最近流转记录</div>
          <div class="summary-value">{{ observation.recentFlows?.length ?? 0 }}</div>
        </el-card>
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">节点统计数</div>
          <div class="summary-value">{{ observation.nodeStats?.length ?? 0 }}</div>
        </el-card>
      </div>

      <div class="observation-sections">
        <el-card shadow="never" class="observation-card">
          <template #header>
            <span class="section-title">节点热度与耗时</span>
          </template>
          <el-empty v-if="!observation.nodeStats?.length" description="暂无节点统计数据" />
          <el-table
            v-else
            :data="observation.nodeStats"
            :border="false"
            :stripe="true"
            :header-cell-style="{ backgroundColor: '#f5f7fa' }"
          >
            <el-table-column prop="nodeName" label="节点名称" min-width="160" />
            <el-table-column prop="nodeType" label="节点类型" width="120" />
            <el-table-column prop="ticketCount" label="进入次数" width="100" />
            <el-table-column label="平均等待" width="140">
              <template #default="{ row }">
                {{ formatDurationSec(row.avgWaitDurationSec) }}
              </template>
            </el-table-column>
            <el-table-column label="平均处理" width="140">
              <template #default="{ row }">
                {{ formatDurationSec(row.avgProcessDurationSec) }}
              </template>
            </el-table-column>
            <el-table-column label="平均停留" width="140">
              <template #default="{ row }">
                {{ formatDurationSec(row.avgTotalDurationSec) }}
              </template>
            </el-table-column>
            <el-table-column label="最大停留" width="140">
              <template #default="{ row }">
                {{ formatDurationSec(row.maxTotalDurationSec) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" class="observation-card">
          <template #header>
            <span class="section-title">最近流转时间线</span>
          </template>
          <el-empty v-if="!observation.recentFlows?.length" description="暂无流转记录" />
          <el-timeline v-else class="flow-timeline">
            <el-timeline-item
              v-for="record in observation.recentFlows"
              :key="record.id"
              :timestamp="formatDateTime(record.createTime)"
              placement="top"
            >
              <div class="timeline-main">
                <div class="timeline-row">
                  <el-tag size="small" :type="record.flowType === 'RETURN' ? 'warning' : 'primary'">
                    {{ record.flowTypeLabel || record.flowType }}
                  </el-tag>
                  <span class="ticket-no">{{ record.ticketNo }}</span>
                </div>
                <div class="timeline-row">
                  <span class="status-name">{{ record.fromStatusName || record.fromStatus }}</span>
                  <span class="arrow">→</span>
                  <span class="status-name status-name--to">
                    {{ record.toStatusName || record.toStatus }}
                  </span>
                </div>
                <div class="timeline-row timeline-meta">
                  <span>操作人：{{ record.operatorName || record.operatorId }}</span>
                  <span>角色：{{ formatRoleLabel(record.operatorRole) }}</span>
                  <span v-if="record.toAssigneeName">处理人：{{ record.toAssigneeName }}</span>
                </div>
                <div v-if="record.remark" class="timeline-remark">
                  备注：{{ record.remark }}
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </div>
    </template>
    <el-empty v-else description="暂无运行观察数据" />
  </div>
</template>

<style scoped lang="scss">
.workflow-observation-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.observation-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-card {
  border-radius: 10px;
}

.summary-label {
  font-size: 13px;
  color: #86909c;
}

.summary-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
  color: #1d2129;
}

.observation-sections {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.observation-card {
  border-radius: 10px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
}

.flow-timeline {
  padding-left: 8px;
}

.timeline-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-bottom: 8px;
}

.timeline-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.ticket-no {
  font-size: 13px;
  color: #4e5969;
}

.status-name {
  font-weight: 500;
  color: #4e5969;
}

.status-name--to {
  color: #1675d1;
}

.arrow {
  color: #86909c;
}

.timeline-meta {
  font-size: 13px;
  color: #86909c;
}

.timeline-remark {
  padding: 8px 10px;
  border-radius: 8px;
  background: #f7f8fa;
  font-size: 13px;
  color: #4e5969;
}

@media (max-width: 768px) {
  .observation-summary {
    grid-template-columns: 1fr;
  }
}
</style>
