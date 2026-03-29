<template>
  <div class="ticket-time-track-panel">
    <div class="track-block">
      <div class="block-label">时间链</div>
      <EmptyState v-if="!mergedEntries.length" description="暂无时间追踪记录" />
      <el-timeline v-else class="track-timeline">
        <el-timeline-item
          v-for="(entry, idx) in mergedEntries"
          :key="entryKey(entry, idx)"
          :timestamp="formatDateTime(entry.timestampStr)"
          placement="top"
        >
          <div v-if="entry.kind === 'track'" class="track-item">
            <div class="track-title">
              {{ entry.track.actionLabel || entry.track.action || '-' }}
              <span class="track-user">（{{ entry.track.userName || '-' }}）</span>
            </div>
            <div class="track-meta">
              状态：{{ statusLabelFn(entry.track.fromStatus) }} → {{ statusLabelFn(entry.track.toStatus) }}
            </div>
            <div v-if="entry.track.fromUserName || entry.track.toUserName" class="track-meta">
              处理人：{{ entry.track.fromUserName || '-' }} → {{ entry.track.toUserName || '-' }}
            </div>
            <div v-if="entry.track.isFirstRead" class="track-meta">首次阅读：是</div>
            <div v-if="entry.track.remark" class="track-meta">备注：{{ entry.track.remark }}</div>
            <FieldChangeBlock v-if="entry.track.fieldChanges?.length" :fields="entry.track.fieldChanges" />
          </div>
          <div v-else class="track-item track-item-standalone">
            <div class="track-title">
              {{ entry.change.changeTypeLabel || '信息变更' }}
              <span class="track-user">（{{ entry.change.changeByUserName || '-' }}）</span>
            </div>
            <FieldChangeBlock v-if="entry.change.fields?.length" :fields="entry.change.fields" />
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>

    <div class="track-block">
      <div class="block-label">节点耗时统计</div>
      <EmptyState v-if="!nodeDurationItems.length" description="暂无节点耗时数据" />
      <div v-else-if="compact" class="mobile-node-list">
        <div v-for="(row, index) in nodeDurationItems" :key="`node-${index}`" class="mobile-node-card">
          <div class="mobile-node-head">
            <div class="mobile-node-title">{{ statusLabelFn(row.nodeName) }}</div>
            <el-tag size="small" type="primary" effect="plain">{{ row.assigneeName || '-' }}</el-tag>
          </div>
          <div class="mobile-node-grid">
            <div class="mobile-node-cell">
              <span class="mobile-node-label">角色</span>
              <span class="mobile-node-value">{{ roleLabelFn(row.assigneeRole) }}</span>
            </div>
            <div class="mobile-node-cell">
              <span class="mobile-node-label">到达时间</span>
              <span class="mobile-node-value">{{ formatDateTime(row.arriveAt) }}</span>
            </div>
            <div class="mobile-node-cell">
              <span class="mobile-node-label">首次阅读</span>
              <span class="mobile-node-value">{{ formatDateTime(row.firstReadAt) }}</span>
            </div>
            <div class="mobile-node-cell">
              <span class="mobile-node-label">开始处理</span>
              <span class="mobile-node-value">{{ formatDateTime(row.startProcessAt) }}</span>
            </div>
            <div class="mobile-node-cell">
              <span class="mobile-node-label">离开时间</span>
              <span class="mobile-node-value">{{ formatDateTime(row.leaveAt) }}</span>
            </div>
            <div class="mobile-node-cell">
              <span class="mobile-node-label">等待耗时</span>
              <span class="mobile-node-value">{{ formatDuration(row.waitDurationSec) }}</span>
            </div>
            <div class="mobile-node-cell">
              <span class="mobile-node-label">处理耗时</span>
              <span class="mobile-node-value">{{ formatDuration(row.processDurationSec) }}</span>
            </div>
            <div class="mobile-node-cell">
              <span class="mobile-node-label">总耗时</span>
              <span class="mobile-node-value">{{ formatDuration(row.totalDurationSec) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="node-duration-table-wrap">
        <el-table
          :data="nodeDurationItems"
          :border="false"
          :stripe="true"
          :header-cell-style="{ backgroundColor: '#f5f7fa' }"
          max-height="420"
        >
        <el-table-column label="节点" align="center" min-width="130">
          <template #default="{ row }">
            {{ statusLabelFn(row.nodeName) }}
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="处理人" align="center" min-width="120" />
        <el-table-column label="角色" align="center" min-width="120">
          <template #default="{ row }">
            {{ roleLabelFn(row.assigneeRole) }}
          </template>
        </el-table-column>
        <el-table-column label="到达时间" align="center" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.arriveAt) }}
          </template>
        </el-table-column>
        <el-table-column label="首次阅读" align="center" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.firstReadAt) }}
          </template>
        </el-table-column>
        <el-table-column label="开始处理" align="center" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.startProcessAt) }}
          </template>
        </el-table-column>
        <el-table-column label="离开时间" align="center" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.leaveAt) }}
          </template>
        </el-table-column>
        <el-table-column label="等待耗时" align="center" min-width="110">
          <template #default="{ row }">
            {{ formatDuration(row.waitDurationSec) }}
          </template>
        </el-table-column>
        <el-table-column label="处理耗时" align="center" min-width="110">
          <template #default="{ row }">
            {{ formatDuration(row.processDurationSec) }}
          </template>
        </el-table-column>
        <el-table-column label="总耗时" align="center" min-width="110">
          <template #default="{ row }">
            {{ formatDuration(row.totalDurationSec) }}
          </template>
        </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import EmptyState from '@/components/common/EmptyState.vue'
import type { BugChangeHistoryOutput, TicketNodeDurationItem, TicketTimeTrackItem } from '@/types/ticket'
import { formatDateTime } from '@/utils/formatter'

import FieldChangeBlock from './TicketTimeTrackFieldChangeBlock.vue'

const props = defineProps<{
  tracks: TicketTimeTrackItem[]
  standaloneFieldChanges: BugChangeHistoryOutput[]
  nodeDurationItems: TicketNodeDurationItem[]
  compact?: boolean
  statusLabelFn: (status?: string) => string
  roleLabelFn: (role?: string) => string
  formatDuration: (sec?: number) => string
}>()

type MergedEntry =
  | { kind: 'track'; at: number; timestampStr: string | undefined; track: TicketTimeTrackItem }
  | { kind: 'standalone'; at: number; timestampStr: string; change: BugChangeHistoryOutput }

function parseToMs(isoOrSql?: string | null): number {
  if (!isoOrSql) return 0
  const t = Date.parse(isoOrSql.replace(' ', 'T'))
  return Number.isNaN(t) ? 0 : t
}

const mergedEntries = computed<MergedEntry[]>(() => {
  const list: MergedEntry[] = []
  for (const track of props.tracks || []) {
    list.push({
      kind: 'track',
      at: parseToMs(track.timestamp),
      timestampStr: track.timestamp,
      track,
    })
  }
  for (const change of props.standaloneFieldChanges || []) {
    list.push({
      kind: 'standalone',
      at: parseToMs(change.changeTime),
      timestampStr: change.changeTime,
      change,
    })
  }
  list.sort((a, b) => {
    if (a.at !== b.at) return a.at - b.at
    return 0
  })
  return list
})

function entryKey(entry: MergedEntry, idx: number): string {
  if (entry.kind === 'track') {
    return `t-${entry.track.id}-${idx}`
  }
  return `s-${entry.change.id}-${idx}`
}
</script>

<style scoped lang="scss">
.ticket-time-track-panel {
  .track-block + .track-block {
    margin-top: 20px;
  }

  .block-label {
    font-weight: 600;
    font-size: 14px;
    margin-bottom: 10px;
    color: #303133;
  }

  .track-item {
    background: #f8fafc;
    border-radius: 6px;
    padding: 10px 12px;
    width: 100%;
    box-sizing: border-box;
    border: 1px solid #eef2f7;
  }

  .track-item-standalone {
    border-left: 3px solid #1675d1;
  }

  .track-title {
    font-weight: 600;
    font-size: 13px;
  }

  .track-user {
    font-weight: 400;
    color: #606266;
  }

  .track-meta {
    margin-top: 4px;
    color: #606266;
    font-size: 12px;
    line-height: 1.5;
    word-break: break-word;
  }
}

.track-timeline {
  :deep(.el-timeline-item__wrapper) {
    padding-left: 18px;
  }

  :deep(.el-timeline-item__content) {
    width: 100%;
  }
}

.mobile-node-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mobile-node-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  padding: 10px 12px;
}

.mobile-node-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.mobile-node-title {
  font-weight: 600;
  color: #1d2129;
}

.mobile-node-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 6px;
}

.mobile-node-cell {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 4px 0;
  border-bottom: 1px dashed #eef2f7;
}

.mobile-node-cell:last-child {
  border-bottom: none;
}

.mobile-node-label {
  color: #86909c;
  font-size: 12px;
  flex-shrink: 0;
}

.mobile-node-value {
  color: #303133;
  font-size: 12px;
  text-align: right;
  word-break: break-word;
}

.node-duration-table-wrap {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;

  :deep(.el-table) {
    min-width: 1360px;
  }
}

@media (max-width: 768px) {
  .track-timeline {
    :deep(.el-timeline-item__wrapper) {
      padding-left: 14px;
    }
  }
}
</style>
