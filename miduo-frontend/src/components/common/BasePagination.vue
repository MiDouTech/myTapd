<script setup lang="ts">
interface Props {
  currentPage: number
  pageSize: number
  total: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  update: [{ pageNum: number; pageSize: number }]
}>()

function handleSizeChange(pageSize: number): void {
  emit('update', { pageNum: 1, pageSize })
}

function handleCurrentChange(pageNum: number): void {
  emit('update', { pageNum, pageSize: props.pageSize })
}
</script>

<template>
  <div class="pagination-wrapper">
    <el-pagination
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<style scoped lang="scss">
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--md-border-light, #eef2f7);
}
</style>
