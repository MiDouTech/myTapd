<template>
  <div class="bug-change-history-item">
    <!-- 序号列 -->
    <div class="item-seq">{{ item.seq }}</div>

    <!-- 时间 + 人 + 类型 -->
    <div class="item-meta">
      <span class="item-time">{{ item.changeTime }}</span>
      <span class="item-user">
        <el-avatar v-if="item.changeByAvatar" :src="item.changeByAvatar" :size="18" />
        <span class="user-name">{{ item.changeByUserName }}</span>
      </span>
      <el-tag size="small" :type="changeTypeTag(item.changeType)" effect="plain">
        {{ item.changeTypeLabel }}
      </el-tag>
    </div>

    <!-- 变更字段列表 -->
    <div class="item-fields">
      <div v-for="(field, idx) in item.fields" :key="idx" class="field-row">
        <span class="field-label">{{ field.fieldLabel }}</span>
        <div class="field-change">
          <!-- 仅有新值（创建场景） -->
          <template v-if="!field.oldValue && field.newValue">
            <span class="new-value">{{ truncate(field.newLabel || field.newValue) }}</span>
          </template>
          <!-- 仅有旧值（删除场景） -->
          <template v-else-if="field.oldValue && !field.newValue">
            <span class="old-value deleted">{{ truncate(field.oldLabel || field.oldValue) }}</span>
            <span class="deleted-hint">（已删除）</span>
          </template>
          <!-- 有旧值有新值（变更场景） -->
          <template v-else-if="field.oldValue && field.newValue">
            <span class="old-value">{{ truncate(field.oldLabel || field.oldValue) }}</span>
            <span class="arrow">→</span>
            <span class="new-value">{{ truncate(field.newLabel || field.newValue) }}</span>
          </template>
          <!-- 空 -->
          <template v-else>
            <span class="empty-change">-</span>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { BugChangeHistoryOutput } from '@/types/ticket'

defineProps<{
  item: BugChangeHistoryOutput
}>()

const CHANGE_TYPE_TAG_MAP: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
  CREATE: 'success',
  MANUAL_CHANGE: '',
  STATUS_CHANGE: 'warning',
  SYSTEM_AUTO: 'info',
  COMMENT: 'info',
  ATTACHMENT: 'info',
}

function changeTypeTag(type: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  return CHANGE_TYPE_TAG_MAP[type] ?? ''
}

function truncate(val: string | null | undefined, max = 120): string {
  if (!val) return ''
  return val.length > max ? val.substring(0, max) + '...' : val
}
</script>

<style scoped>
.bug-change-history-item {
  display: grid;
  grid-template-columns: 36px 1fr;
  gap: 0;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.bug-change-history-item:last-child {
  border-bottom: none;
}

.item-seq {
  grid-row: 1 / 3;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 2px;
  font-size: 13px;
  font-weight: 600;
  color: #1675d1;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.item-time {
  font-size: 12px;
  color: #909399;
}

.item-user {
  display: flex;
  align-items: center;
  gap: 4px;
}

.user-name {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}

.item-fields {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.field-label {
  width: 80px;
  flex-shrink: 0;
  font-size: 12px;
  color: #606266;
  font-weight: 500;
}

.field-change {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-size: 13px;
}

.old-value {
  color: #909399;
  text-decoration: line-through;
}

.old-value.deleted {
  color: #f56c6c;
}

.deleted-hint {
  color: #f56c6c;
  font-size: 12px;
}

.new-value {
  color: #303133;
}

.arrow {
  color: #c0c4cc;
  font-size: 12px;
}

.empty-change {
  color: #c0c4cc;
}
</style>
