<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import { pageNlpLogs } from '@/api/wecom'
import type { NlpLogPageInput, NlpLogPageOutput } from '@/types/wecom'

const loading = ref(false)
const tableData = ref<NlpLogPageOutput[]>([])
const total = ref(0)

const query = reactive<NlpLogPageInput>({
  pageNum: 1,
  pageSize: 20,
  parseType: undefined,
  chatId: undefined,
  fromWecomUserid: undefined,
  minConfidence: undefined,
  startTime: undefined,
  endTime: undefined,
})

const parseTypeOptions = [
  { label: '格式模板', value: 'template' },
  { label: '自然语言', value: 'natural_language' },
]

const dateRange = ref<[string, string] | null>(null)

async function loadLogs(): Promise<void> {
  loading.value = true
  try {
    if (dateRange.value && dateRange.value.length === 2) {
      query.startTime = dateRange.value[0]
      query.endTime = dateRange.value[1]
    } else {
      query.startTime = undefined
      query.endTime = undefined
    }
    const result = await pageNlpLogs({ ...query })
    tableData.value = result.records
    total.value = result.total
  } catch {
    // 错误由拦截器统一处理
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  query.pageNum = 1
  loadLogs()
}

function handleReset(): void {
  query.parseType = undefined
  query.chatId = undefined
  query.fromWecomUserid = undefined
  query.minConfidence = undefined
  query.startTime = undefined
  query.endTime = undefined
  dateRange.value = null
  query.pageNum = 1
  loadLogs()
}

function handlePageChange(page: number): void {
  query.pageNum = page
  loadLogs()
}

function handleSizeChange(size: number): void {
  query.pageSize = size
  query.pageNum = 1
  loadLogs()
}

function getParseTypeLabel(parseType: string | undefined): string {
  if (!parseType) return '-'
  const opt = parseTypeOptions.find((o) => o.value === parseType)
  return opt ? opt.label : parseType
}

function getStatusTag(status: string | undefined): 'success' | 'danger' | 'info' | 'warning' {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAIL') return 'danger'
  if (status === 'DUPLICATE') return 'warning'
  return 'info'
}

onMounted(async () => {
  await loadLogs()
})
</script>

<template>
  <el-space direction="vertical" fill :size="12">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">NLP解析日志</span>
          <el-button @click="loadLogs">刷新</el-button>
        </div>
      </template>

      <el-form inline class="filter-form" @submit.prevent="handleSearch">
        <el-form-item label="解析类型">
          <el-select
            v-model="query.parseType"
            clearable
            placeholder="请选择内容"
            style="width: 140px"
          >
            <el-option
              v-for="opt in parseTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="群ChatID">
          <el-input v-model="query.chatId" clearable placeholder="输入群ChatID" style="width: 180px" />
        </el-form-item>
        <el-form-item label="最低置信度">
          <el-input-number
            v-model="query.minConfidence"
            :min="0"
            :max="100"
            controls-position="right"
            style="width: 100px"
            placeholder="0-100"
          />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 340px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        :data="tableData"
        :border="false"
        :stripe="true"
        v-loading="loading"
        :header-cell-style="{ backgroundColor: '#f5f7fa' }"
      >
        <el-table-column prop="chatId" label="群ChatID" width="160" align="center" show-overflow-tooltip />
        <el-table-column prop="fromWecomUserid" label="发送人" width="140" align="center" show-overflow-tooltip />
        <el-table-column prop="rawMessage" label="原始消息" min-width="200" align="center" show-overflow-tooltip />
        <el-table-column label="解析类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.parseType === 'natural_language' ? 'primary' : 'info'"
              size="small"
            >
              {{ getParseTypeLabel(row.parseType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置信度" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.nlpConfidence !== null && row.nlpConfidence !== undefined">
              {{ row.nlpConfidence }}%
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="处理状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)" size="small">
              {{ row.status || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联工单" width="100" align="center">
          <template #default="{ row }">
            {{ row.ticketId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" min-width="160" align="center" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="160" align="center" />
      </el-table>

      <el-pagination
        :current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        class="pagination"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>
  </el-space>
</template>

<style scoped lang="scss">
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 14px;
  font-weight: 500;
}

.filter-form {
  margin-bottom: 12px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
