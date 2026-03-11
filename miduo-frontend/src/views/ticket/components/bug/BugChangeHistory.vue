<template>
  <div class="bug-change-history">
    <BugChangeHistoryFilter @change="handleFilterChange" />

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="4" animated />
    </div>

    <div v-else-if="filteredList.length === 0" class="empty-state">
      <el-empty description="暂无变更历史" :image-size="60" />
    </div>

    <div v-else class="history-list">
      <!-- 表头 -->
      <div class="list-header">
        <span class="col-seq">序号({{ filteredList.length }})</span>
        <span class="col-time">变更时间</span>
        <span class="col-user">变更人</span>
        <span class="col-type">变更类型</span>
        <span class="col-fields">变更字段</span>
      </div>
      <BugChangeHistoryItem v-for="item in filteredList" :key="item.id" :item="item" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { getTicketChangeHistory } from '@/api/ticket'
import type { BugChangeHistoryOutput } from '@/types/ticket'

import BugChangeHistoryFilter from './BugChangeHistoryFilter.vue'
import BugChangeHistoryItem from './BugChangeHistoryItem.vue'

const props = defineProps<{
  ticketId: number
}>()

const emit = defineEmits<{
  countUpdate: [count: number]
}>()

const loading = ref(false)
const allList = ref<BugChangeHistoryOutput[]>([])
const filterChangeType = ref('')
const filterFieldName = ref('')

const filteredList = computed(() => {
  let list = allList.value
  if (filterChangeType.value) {
    list = list.filter((item) => item.changeType === filterChangeType.value)
  }
  if (filterFieldName.value) {
    list = list.filter((item) =>
      item.fields.some((f) => f.fieldName === filterFieldName.value),
    )
  }
  return list
})

function handleFilterChange(changeType: string, fieldName: string): void {
  filterChangeType.value = changeType
  filterFieldName.value = fieldName
}

async function loadHistory(): Promise<void> {
  if (!props.ticketId) return
  loading.value = true
  try {
    const data = await getTicketChangeHistory(props.ticketId)
    allList.value = data
    emit('countUpdate', data.length)
  } catch {
    allList.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.bug-change-history {
  padding: 0 4px;
}

.loading-state {
  padding: 16px 0;
}

.empty-state {
  padding: 24px 0;
  display: flex;
  justify-content: center;
}

.history-list {
  display: flex;
  flex-direction: column;
}

.list-header {
  display: grid;
  grid-template-columns: 60px 1fr;
  padding: 6px 0;
  border-bottom: 2px solid #e4e7ed;
  font-size: 12px;
  color: #909399;
  font-weight: 500;
  gap: 8px;
}

.col-seq {
  text-align: center;
  white-space: nowrap;
}

.col-time,
.col-user,
.col-type,
.col-fields {
  flex: 1;
}
</style>
