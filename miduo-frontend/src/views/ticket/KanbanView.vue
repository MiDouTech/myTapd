<script setup lang="ts">
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="title">工单看板</div>
    </template>
    <EmptyState description="看板能力将在后续任务中完善。" />
  </el-card>
</template>

<style scoped lang="scss">
.title {
  font-size: 16px;
  font-weight: 600;
}
</style>
