<script setup lang="ts">
import { computed } from 'vue'

import { notificationDisplayBody } from '@/utils/notificationDisplay'

const props = defineProps<{
  content?: string | null
}>()

const body = computed(() => notificationDisplayBody(props.content))
</script>

<template>
  <div v-if="body.mode === 'kv'" class="notification-content-body notification-content-body--kv">
    <div v-for="(line, idx) in body.lines" :key="`${idx}-${line.label}`" class="notification-kv-row">
      <span class="notification-kv-label">{{ line.label }}：</span>
      <span class="notification-kv-value">{{ line.value }}</span>
    </div>
  </div>
  <div v-else class="notification-content-body notification-content-body--plain">
    {{ body.text }}
  </div>
</template>

<style scoped lang="scss">
.notification-content-body {
  font-size: 13px;
  line-height: 20px;
  color: #606266;
}

.notification-content-body--kv {
  margin: 0;
}

.notification-kv-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 6px;
  margin-bottom: 4px;

  &:last-child {
    margin-bottom: 0;
  }
}

.notification-kv-label {
  flex: 0 0 auto;
  color: #909399;
  font-weight: 500;
}

.notification-kv-value {
  flex: 1 1 160px;
  min-width: 0;
  word-break: break-word;
}
</style>
