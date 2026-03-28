<script setup lang="ts">
interface Props {
  description?: string
  actionText?: string
}

const props = withDefaults(defineProps<Props>(), {
  description: '暂无数据',
  actionText: '',
})

const emit = defineEmits<{
  action: []
}>()
</script>

<template>
  <div class="empty-state">
    <el-empty :description="description" :image-size="80">
      <template v-if="props.actionText" #default>
        <el-button type="primary" size="small" @click="emit('action')">
          {{ props.actionText }}
        </el-button>
      </template>
    </el-empty>
  </div>
</template>

<style scoped lang="scss">
.empty-state {
  padding: 32px 0;

  :deep(.el-empty__description p) {
    color: var(--md-text-muted, #909399);
    font-size: 14px;
  }
}
</style>
