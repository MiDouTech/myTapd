<script setup lang="ts">
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="title">报表中心</div>
    </template>
    <EmptyState description="报表能力将在 Task010 中完善。" />
  </el-card>
</template>

<style scoped lang="scss">
.title {
  font-size: 16px;
  font-weight: 600;
}
</style>
