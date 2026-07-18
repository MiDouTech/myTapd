<template>
  <span class="bug-status-badge" :style="badgeStyle">{{ label }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { getTicketStatusLabel } from '@/utils/ticket-status'

const props = defineProps<{
  status: string
  statusLabel?: string
}>()

const STATUS_COLOR_MAP: Record<string, string> = {
  pending_assign: '#909399',
  pending_accept: '#909399',
  pending_review: '#E6A23C',
  pending_planning: '#909399',
  pending_research: '#8B5CF6',
  in_design: '#1675d1',
  alert_triggered: '#E6A23C',
  alert_acknowledged: '#1675d1',
  alert_stable: '#8B5CF6',
  alert_resolved: '#67C23A',
  alert_suppressed: '#909399',
  processing: '#1675d1',
  pending_cs_accept: '#F59E0B',
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
  no_action: '#909399',
  invalid: '#F56C6C',
}

const color = computed(() => STATUS_COLOR_MAP[props.status?.toLowerCase()] ?? '#909399')

const label = computed(
  () => props.statusLabel || getTicketStatusLabel(props.status),
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
