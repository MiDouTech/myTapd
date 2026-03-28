<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getKanbanData, moveKanbanTicket } from '@/api/kanban'
import EmptyState from '@/components/common/EmptyState.vue'
import type { KanbanColumnOutput } from '@/types/kanban'
import { notifyError, notifySuccess } from '@/utils/feedback'
import { formatDateTime } from '@/utils/formatter'

const router = useRouter()
const loading = ref(false)
const columns = ref<KanbanColumnOutput[]>([])
const draggingTicketId = ref<number>()
const draggingFromStatus = ref('')

function openTicketDetailInNewTab(id: number): void {
  const { href } = router.resolve({
    name: 'ticketDetail',
    params: { id: String(id) },
  })
  const opened = window.open(href, '_blank', 'noopener,noreferrer')
  if (!opened) {
    notifyError('无法打开新标签页，请检查浏览器是否拦截了弹窗')
  }
}

async function loadKanban(): Promise<void> {
  loading.value = true
  try {
    columns.value = (await getKanbanData({ limit: 300 })) || []
  } catch (error) {
    notifyError((error as Error).message || '加载工单看板失败')
  } finally {
    loading.value = false
  }
}

function handleDragStart(ticketId: number, fromStatus: string): void {
  draggingTicketId.value = ticketId
  draggingFromStatus.value = fromStatus
}

async function handleDrop(targetStatus: string): Promise<void> {
  if (!draggingTicketId.value) {
    return
  }
  if (draggingFromStatus.value === targetStatus) {
    return
  }
  try {
    await moveKanbanTicket({
      ticketId: draggingTicketId.value,
      targetStatus,
      remark: '看板拖拽状态变更',
    })
    notifySuccess('状态更新成功')
    await loadKanban()
  } catch (error) {
    notifyError((error as Error).message || '状态更新失败')
  } finally {
    draggingTicketId.value = undefined
    draggingFromStatus.value = ''
  }
}

function getPriorityType(priority?: string): 'success' | 'warning' | 'danger' | 'info' {
  const code = String(priority || '').toLowerCase()
  if (code === 'urgent') {
    return 'danger'
  }
  if (code === 'high') {
    return 'warning'
  }
  if (code === 'low') {
    return 'success'
  }
  return 'info'
}

function getPriorityBarClass(priority?: string): string {
  const code = String(priority || '').toLowerCase()
  if (code === 'urgent') return 'priority-bar--urgent'
  if (code === 'high') return 'priority-bar--high'
  if (code === 'low') return 'priority-bar--low'
  return 'priority-bar--medium'
}

function getColumnStatusClass(status?: string): string {
  if (!status) return ''
  const s = status.toLowerCase()
  if (['completed', 'closed'].includes(s)) return 'column--success'
  if (['pending_assign', 'pending_accept', 'pending_test_accept', 'pending_dev_accept', 'pending_verify', 'pending_cs_confirm'].includes(s)) return 'column--warning'
  if (['processing', 'testing', 'developing', 'executing'].includes(s)) return 'column--primary'
  if (s === 'suspended') return 'column--danger'
  return ''
}

const dragOverStatus = ref('')

function handleDragEnter(status: string): void {
  dragOverStatus.value = status
}

function handleDragLeave(): void {
  dragOverStatus.value = ''
}

onMounted(() => {
  loadKanban()
})
</script>

<template>
  <div class="kanban-page" v-loading="loading">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="title">工单看板</div>
        <el-button @click="loadKanban">刷新</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="kanban-card">
      <EmptyState v-if="columns.length === 0" description="暂无看板数据" />
      <div v-else class="kanban-container">
        <div
          v-for="column in columns"
          :key="column.status"
          class="kanban-column"
          :class="[getColumnStatusClass(column.status), { 'drag-over': dragOverStatus === column.status }]"
          @dragover.prevent
          @dragenter.prevent="handleDragEnter(column.status)"
          @dragleave="handleDragLeave"
          @drop.prevent="handleDrop(column.status); dragOverStatus = ''"
        >
          <div class="column-header">
            <div class="name">{{ column.statusLabel || column.status }}</div>
            <el-tag size="small" round>{{ column.tickets.length }}</el-tag>
          </div>
          <div class="column-body">
            <div
              v-for="ticket in column.tickets"
              :key="ticket.id"
              class="ticket-card"
              :class="getPriorityBarClass(ticket.priority)"
              draggable="true"
              @dragstart="handleDragStart(ticket.id, column.status)"
            >
              <div
                class="ticket-no"
                role="link"
                tabindex="0"
                @mousedown.stop
                @click.stop="openTicketDetailInNewTab(ticket.id)"
                @keydown.enter.prevent="openTicketDetailInNewTab(ticket.id)"
              >
                {{ ticket.ticketNo }}
              </div>
              <div class="ticket-title">{{ ticket.title }}</div>
              <div class="ticket-meta">
                <el-tag :type="getPriorityType(ticket.priority)" size="small">
                  {{ ticket.priorityLabel || ticket.priority || '-' }}
                </el-tag>
                <span class="assignee">{{ ticket.assigneeName || '未分配' }}</span>
              </div>
              <div class="ticket-foot">
                <span>{{ ticket.categoryName || '未分类' }}</span>
                <span>{{ formatDateTime(ticket.updateTime) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.kanban-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  min-width: 0;
  overflow: hidden;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 17px;
  font-weight: 600;
  color: #1d2129;
}

.kanban-card {
  min-width: 0;
  overflow: hidden;

  :deep(.el-card__body) {
    overflow: hidden;
  }
}

.kanban-container {
  display: flex;
  gap: 14px;
  overflow-x: auto;
  padding-bottom: 8px;
  align-items: flex-start;
}

.kanban-column {
  min-width: 290px;
  max-width: 310px;
  background: #f5f7fa;
  border-radius: 10px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 200px);
  border: 1px solid #eef2f7;
  transition: background 0.15s ease;

  &:hover {
    background: #f0f4f8;
  }
}

.column-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e5e7eb;
  flex-shrink: 0;

  .name {
    font-size: 14px;
    font-weight: 600;
    color: var(--md-text-primary, #1d2129);
  }
}

.column--primary > .column-header { border-bottom-color: #1675d1; }
.column--warning > .column-header { border-bottom-color: #e6a23c; }
.column--success > .column-header { border-bottom-color: #67c23a; }
.column--danger > .column-header { border-bottom-color: #f56c6c; }

.kanban-column.drag-over {
  background: #e8f2fc;
  border-color: #1675d1;
  border-style: dashed;
}

.column-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 80px;
  overflow-y: auto;
  padding-right: 4px;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: #c0c4cc;
    border-radius: 2px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }
}

.ticket-card {
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  border-left: 3px solid #c0c4cc;
  padding: 12px 12px 12px 10px;
  cursor: grab;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 3px 10px rgba(0, 0, 0, 0.08);
  }

  &:active {
    cursor: grabbing;
    transform: translateY(0);
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
  }

  &.priority-bar--urgent { border-left-color: #f56c6c; }
  &.priority-bar--high { border-left-color: #e6a23c; }
  &.priority-bar--medium { border-left-color: #1675d1; }
  &.priority-bar--low { border-left-color: #67c23a; }
}

.ticket-no {
  color: #1675d1;
  font-size: 12px;
  font-weight: 500;
  font-family: 'SFMono-Regular', Consolas, monospace;
  cursor: pointer;
  outline: none;

  &:hover {
    text-decoration: underline;
  }

  &:focus-visible {
    box-shadow: 0 0 0 2px #1675d1;
    border-radius: 2px;
  }
}

.ticket-title {
  margin-top: 8px;
  font-size: 14px;
  color: #1d2129;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ticket-meta {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;

  .assignee {
    color: #6b7280;
    font-size: 12px;
  }
}

.ticket-foot {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f2f5;
  color: #9ca3af;
  font-size: 12px;
  display: flex;
  justify-content: space-between;
  gap: 8px;
}
</style>
