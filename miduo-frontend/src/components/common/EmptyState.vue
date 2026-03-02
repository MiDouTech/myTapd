<script setup lang="ts">
interface Props {
  description?: string
}

withDefaults(defineProps<Props>(), {
  description: '暂无数据',
})
</script>

<template>
  <div class="empty-state">
    <el-empty :description="description" />
  </div>
</template>

<style scoped lang="scss">
.empty-state {
  padding: 24px 0;
}
</style>
