<script setup lang="ts">
interface Props {
  data: Record<string, unknown>[]
  loading?: boolean
  showSelection?: boolean
  rowKey?: string
  rowClassName?:
    | string
    | ((payload: { row: Record<string, unknown>; rowIndex: number }) => string)
}

defineProps<Props>()

const emit = defineEmits<{
  selectionChange: [rows: unknown[]]
  sortChange: [
    payload: {
      prop: string
      order: 'ascending' | 'descending' | null
    },
  ]
}>()
</script>

<template>
  <div class="base-table-wrapper">
    <el-table
      :data="data"
      :border="false"
      :stripe="true"
      :header-cell-style="{ backgroundColor: '#f5f7fa' }"
      :row-key="rowKey || 'id'"
      :row-class-name="rowClassName"
      v-loading="loading"
      class="base-table"
      @selection-change="(rows: unknown[]) => emit('selectionChange', rows)"
      @sort-change="
        (payload: { prop: string; order: 'ascending' | 'descending' | null }) =>
          emit('sortChange', payload)
      "
    >
      <el-table-column v-if="showSelection" type="selection" width="55" align="center" />
      <slot />
    </el-table>
  </div>
</template>

<style scoped lang="scss">
.base-table-wrapper {
  width: 100%;
  overflow-x: auto;
  // Ensure the element's scrollbar is visible and not clipped by parents
  -webkit-overflow-scrolling: touch;
}

.base-table {
  width: 100%;
  font-size: 14px;

  :deep(.el-table__header-wrapper th.el-table__cell) {
    background-color: #f5f7fa;
    color: #303133;
    font-weight: 500;
    text-align: center;
  }

  :deep(.el-table__body-wrapper td.el-table__cell) {
    text-align: center;
    font-weight: 400;
  }

  :deep(.el-table__body tr:hover > td.el-table__cell) {
    background-color: #f0f9ff;
  }

  // Ensure internal scrollbar wrapper is visible
  :deep(.el-scrollbar__bar.is-horizontal) {
    display: block;
  }
}
</style>
