<script setup lang="ts">
import { reactive, ref } from 'vue'

import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { confirmAction, notifySuccess } from '@/utils/feedback'

interface DemoRow {
  id: number
  name: string
  owner: string
  updateTime: string
}

const loading = ref(false)
const showEmpty = ref(false)
const tableData = ref<DemoRow[]>([
  { id: 1, name: '工单列表样式规范', owner: '张三', updateTime: '2026-03-02 10:00:00' },
  { id: 2, name: '分页组件规范', owner: '李四', updateTime: '2026-03-02 11:00:00' },
])

const page = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 2,
})

function handlePaginationChange(payload: { pageNum: number; pageSize: number }): void {
  page.pageNum = payload.pageNum
  page.pageSize = payload.pageSize
}

async function handleConfirm(): Promise<void> {
  await confirmAction('确认执行规范示例操作吗？')
  notifySuccess('操作成功')
}

function toggleLoading(): void {
  loading.value = true
  globalThis.setTimeout(() => {
    loading.value = false
  }, 1200)
}
</script>

<template>
  <el-space direction="vertical" fill :size="16">
    <el-card shadow="never">
      <el-space wrap>
        <el-button type="primary" @click="toggleLoading">演示加载态</el-button>
        <el-button @click="showEmpty = !showEmpty">切换空状态</el-button>
        <el-button type="success" @click="handleConfirm">演示反馈组件</el-button>
      </el-space>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="title">标准表格 + 标准分页</div>
      </template>
      <EmptyState v-if="showEmpty" description="当前为示例空状态" />
      <template v-else>
        <BaseTable :data="tableData" :loading="loading">
          <el-table-column prop="id" label="ID" width="80" sortable="custom" />
          <el-table-column prop="name" label="名称" :show-overflow-tooltip="true" />
          <el-table-column prop="owner" label="负责人" />
          <el-table-column prop="updateTime" label="更新时间" sortable="custom" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default>
              <el-button type="primary" link>查看</el-button>
            </template>
          </el-table-column>
        </BaseTable>
        <BasePagination
          :current-page="page.pageNum"
          :page-size="page.pageSize"
          :total="page.total"
          @update="handlePaginationChange"
        />
      </template>
    </el-card>
  </el-space>
</template>

<style scoped lang="scss">
.title {
  font-size: 16px;
  font-weight: 600;
}
</style>
