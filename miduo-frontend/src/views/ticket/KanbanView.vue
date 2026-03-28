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

onMounted(() => {
  loadKanban()
})
</script>

<template>
  <el-space direction="vertical" fill :size="16" v-loading="loading">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="title">工单看板</div>
        <el-button @click="loadKanban">刷新</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <EmptyState v-if="columns.length === 0" description="暂无看板数据" />
      <div v-else class="kanban-container">
        <div
          v-for="column in columns"
          :key="column.status"
          class="kanban-column"
          @dragover.prevent
          @drop.prevent="handleDrop(column.status)"
        >
          <div class="column-header">
            <div class="name">{{ column.statusLabel || column.status }}</div>
            <el-tag size="small">{{ column.tickets.length }}</el-tag>
          </div>
          <div class="column-body">
            <div
              v-for="ticket in column.tickets"
              :key="ticket.id"
              class="ticket-card"
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
  </el-space>
</template>

<style scoped lang="scss">
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.kanban-container {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 6px;
  align-items: flex-start;
}

.kanban-column {
  min-width: 280px;
  max-width: 300px;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 200px);
}

.column-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  flex-shrink: 0;

  .name {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }
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
  padding: 10px;
  cursor: grab;
}

.ticket-no {
  color: #1675d1;
  font-size: 12px;
  cursor: pointer;
  outline: none;

  &:focus-visible {
    box-shadow: 0 0 0 2px #1675d1;
    border-radius: 2px;
  }
}

.ticket-title {
  margin-top: 6px;
  font-size: 14px;
  color: #303133;
  line-height: 20px;
}

.ticket-meta {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;

  .assignee {
    color: #909399;
    font-size: 12px;
  }
}

.ticket-foot {
  margin-top: 10px;
  color: #909399;
  font-size: 12px;
  display: flex;
  justify-content: space-between;
  gap: 8px;
}
</style>
