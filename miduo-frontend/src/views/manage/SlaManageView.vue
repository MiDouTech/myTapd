<script setup lang="ts">
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="title">SLA 管理</div>
    </template>
    <EmptyState description="SLA 策略页面将在后续任务中完善。" />
  </el-card>
</template>

<style scoped lang="scss">
.title {
  font-size: 16px;
  font-weight: 600;
}
</style>
