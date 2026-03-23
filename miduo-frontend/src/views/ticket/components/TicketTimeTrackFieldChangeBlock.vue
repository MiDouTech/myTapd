<template>
  <div class="field-change-block">
    <div class="field-change-title">本环节填写 / 变更</div>
    <div v-for="(field, fIdx) in fields" :key="fIdx" class="field-row">
      <span class="field-label">{{ field.fieldLabel }}</span>
      <div class="field-body">
        <div class="field-text-line">
          <template v-if="!field.oldValue && field.newValue">
            <span class="new-value">{{ displayText(field.newLabel || field.newValue) }}</span>
          </template>
          <template v-else-if="field.oldValue && !field.newValue">
            <span class="old-value deleted">{{ displayText(field.oldLabel || field.oldValue) }}</span>
            <span class="deleted-hint">（已删除）</span>
          </template>
          <template v-else-if="field.oldValue && field.newValue">
            <span class="old-value">{{ displayText(field.oldLabel || field.oldValue) }}</span>
            <span class="arrow">→</span>
            <span class="new-value">{{ displayText(field.newLabel || field.newValue) }}</span>
          </template>
          <template v-else>
            <span class="empty-change">-</span>
          </template>
        </div>
        <div v-if="imagesForField(field).length" class="field-images">
          <el-image
            v-for="(src, i) in imagesForField(field)"
            :key="src + i"
            class="thumb"
            :src="src"
            :preview-src-list="imagesForField(field)"
            :initial-index="i"
            fit="cover"
            preview-teleported
            hide-on-click-modal
          >
            <template #error>
              <div class="thumb-error">图</div>
            </template>
          </el-image>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { BugFieldChangeItem } from '@/types/ticket'
import { extractImageUrlsFromText } from '@/utils/trackTimelineImages'

defineProps<{
  fields: BugFieldChangeItem[]
}>()

function displayText(val: string | null | undefined, max = 200): string {
  if (!val) return ''
  const plain = val.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
  return plain.length > max ? plain.substring(0, max) + '…' : plain
}

function imagesForField(field: BugFieldChangeItem): string[] {
  const fromNew = extractImageUrlsFromText(field.newValue || field.newLabel)
  const fromOld = extractImageUrlsFromText(field.oldValue || field.oldLabel)
  return [...new Set([...fromNew, ...fromOld])]
}
</script>

<style scoped lang="scss">
.field-change-block {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #e4e7ed;
}

.field-change-title {
  font-size: 12px;
  font-weight: 600;
  color: #1675d1;
  margin-bottom: 8px;
}

.field-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
}

.field-label {
  width: 84px;
  flex-shrink: 0;
  color: #606266;
  font-weight: 500;
}

.field-body {
  flex: 1;
  min-width: 0;
}

.field-text-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  line-height: 1.5;
  word-break: break-word;
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
}

.empty-change {
  color: #c0c4cc;
}

.field-images {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.thumb {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #ebeef5;
  overflow: hidden;
  flex-shrink: 0;
}

.thumb-error {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #909399;
  background: #f5f7fa;
}
</style>
