<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import { getWebhookDispatchLogPage } from '@/api/webhook'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { WebhookDispatchLogOutput, WebhookDispatchLogPageInput } from '@/types/webhook'
import { formatDateTime } from '@/utils/formatter'

const WEBHOOK_EVENT_OPTIONS = [
  { label: '工单创建', value: 'TICKET_CREATED' },
  { label: '工单状态变更', value: 'TICKET_STATUS_CHANGED' },
  { label: '工单分派', value: 'TICKET_ASSIGNED' },
  { label: '工单完结', value: 'TICKET_COMPLETED' },
  { label: '工单关闭', value: 'TICKET_CLOSED' },
] as const

const DISPATCH_STATUS_OPTIONS = [
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAIL' },
  { label: '已跳过', value: 'SKIPPED' },
] as const

const tableLoading = ref(false)
const tableData = ref<WebhookDispatchLogOutput[]>([])
const total = ref(0)

const query = reactive<
  WebhookDispatchLogPageInput & {
    ticketIdInput: string
    webhookConfigIdInput: string
  }
>({
  ticketIdInput: '',
  webhookConfigIdInput: '',
  eventType: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

function getEventTypeLabel(eventType: string): string {
  return WEBHOOK_EVENT_OPTIONS.find((item) => item.value === eventType)?.label || eventType
}

function getStatusLabel(status: string): string {
  return DISPATCH_STATUS_OPTIONS.find((item) => item.value === status)?.label || status
}

function getStatusTagType(status: string): 'success' | 'danger' | 'info' {
  if (status === 'SUCCESS') {
    return 'success'
  }
  if (status === 'FAIL') {
    return 'danger'
  }
  return 'info'
}

function parseOptionalLong(value: string): number | undefined {
  const normalized = value.trim()
  if (!normalized) {
    return undefined
  }
  const parsed = Number(normalized)
  if (!Number.isInteger(parsed) || parsed <= 0) {
    return undefined
  }
  return parsed
}

function buildQueryParams(): WebhookDispatchLogPageInput {
  return {
    ticketId: parseOptionalLong(query.ticketIdInput),
    webhookConfigId: parseOptionalLong(query.webhookConfigIdInput),
    eventType: query.eventType?.trim() || undefined,
    status: query.status?.trim() || undefined,
    pageNum: query.pageNum,
    pageSize: query.pageSize,
  }
}

async function loadDispatchLogs(): Promise<void> {
  tableLoading.value = true
  try {
    const pageResult = await getWebhookDispatchLogPage(buildQueryParams())
    tableData.value = pageResult.records
    total.value = pageResult.total
  } catch {
    // 错误提示由全局拦截器处理，这里保留当前列表状态
  } finally {
    tableLoading.value = false
  }
}

function handleSearch(): void {
  query.pageNum = 1
  void loadDispatchLogs()
}

function handleReset(): void {
  query.ticketIdInput = ''
  query.webhookConfigIdInput = ''
  query.eventType = ''
  query.status = ''
  query.pageNum = 1
  query.pageSize = 20
  void loadDispatchLogs()
}

function handlePaginationChange(payload: { pageNum: number; pageSize: number }): void {
  query.pageNum = payload.pageNum
  query.pageSize = payload.pageSize
  void loadDispatchLogs()
}

onMounted(async () => {
  await loadDispatchLogs()
})
</script>

<template>
  <el-space direction="vertical" fill :size="12">
    <el-alert
      title="排障提示：可按工单ID/配置ID查看每次推送尝试，包含状态码、失败原因与耗时。"
      type="info"
      :closable="false"
      show-icon
    />

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">Webhook 推送日志</span>
          <el-button @click="loadDispatchLogs">刷新</el-button>
        </div>
      </template>

      <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
        <el-form-item label="工单ID">
          <el-input
            v-model="query.ticketIdInput"
            clearable
            placeholder="请输入工单ID"
          />
        </el-form-item>
        <el-form-item label="配置ID">
          <el-input
            v-model="query.webhookConfigIdInput"
            clearable
            placeholder="请输入配置ID"
          />
        </el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="query.eventType" clearable placeholder="请选择内容">
            <el-option
              v-for="option in WEBHOOK_EVENT_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="请选择内容">
            <el-option
              v-for="option in DISPATCH_STATUS_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-space>
            <el-button type="primary" native-type="submit" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-space>
        </el-form-item>
      </el-form>

      <EmptyState v-if="!tableLoading && total === 0" description="暂无Webhook推送日志" />
      <template v-else>
        <BaseTable :data="tableData" :loading="tableLoading">
          <el-table-column prop="dispatchTime" label="分发时间" width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.dispatchTime || row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="eventType" label="事件类型" width="140">
            <template #default="{ row }">
              {{ getEventTypeLabel(row.eventType) }}
            </template>
          </el-table-column>
          <el-table-column prop="ticketId" label="工单ID" width="100" />
          <el-table-column prop="webhookConfigId" label="配置ID" width="100">
            <template #default="{ row }">
              {{ row.webhookConfigId ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column label="尝试次数" width="120">
            <template #default="{ row }">
              {{ row.attemptNo }}/{{ row.maxAttempts }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)">
                {{ getStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="responseCode" label="响应码" width="100">
            <template #default="{ row }">
              {{ row.responseCode ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="durationMs" label="耗时(ms)" width="100">
            <template #default="{ row }">
              {{ row.durationMs ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="requestUrl" label="请求地址" min-width="220">
            <template #default="{ row }">
              {{ row.requestUrl || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="失败原因" min-width="220">
            <template #default="{ row }">
              <el-tooltip v-if="row.failReason" :content="row.failReason" placement="top">
                <span class="fail-reason">{{ row.failReason }}</span>
              </el-tooltip>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </BaseTable>

        <BasePagination
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          @update="handlePaginationChange"
        />
      </template>
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
  margin-bottom: 8px;
}

.fail-reason {
  display: inline-block;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
