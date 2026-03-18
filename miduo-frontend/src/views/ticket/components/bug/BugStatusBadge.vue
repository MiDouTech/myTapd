<template>
  <span class="bug-status-badge" :style="badgeStyle">{{ label }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: string
  statusLabel?: string
}>()

const STATUS_COLOR_MAP: Record<string, string> = {
  pending_assign: '#909399',
  pending_accept: '#909399',
  processing: '#1675d1',
  pending_test_accept: '#E6A23C',
  testing: '#E6A23C',
  investigating: '#3B82F6',
  pending_dev_accept: '#6B7280',
  developing: '#3B82F6',
  temp_resolved: '#E6A23C',
  pending_cs_confirm: '#F59E0B',
  pending_verify: '#8B5CF6',
  completed: '#67C23A',
  closed: '#67C23A',
  suspended: '#F56C6C',
}

const STATUS_LABEL_MAP: Record<string, string> = {
  pending_assign: '待分派',
  pending_accept: '待受理',
  processing: '处理中',
  pending_test_accept: '待测试受理',
  testing: '测试中',
  investigating: '排查中',
  pending_dev_accept: '待开发受理',
  developing: '开发中',
  temp_resolved: '临时解决',
  pending_cs_confirm: '待客服确认',
  pending_verify: '待验收',
  completed: '已完成',
  closed: '已关闭',
  suspended: '已挂起',
}

const color = computed(() => STATUS_COLOR_MAP[props.status?.toLowerCase()] ?? '#909399')

const label = computed(
  () => props.statusLabel || STATUS_LABEL_MAP[props.status?.toLowerCase()] || props.status,
)

const badgeStyle = computed(() => ({
  backgroundColor: color.value + '1a',
  color: color.value,
  borderColor: color.value + '66',
}))
</script>

<style scoped>
.bug-status-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid;
  white-space: nowrap;
  cursor: default;
}
</style>
