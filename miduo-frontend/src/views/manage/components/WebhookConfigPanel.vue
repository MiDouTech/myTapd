<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  createWebhookConfig,
  deleteWebhookConfig,
  getWebhookConfigDetail,
  getWebhookConfigPage,
  updateWebhookConfig,
} from '@/api/webhook'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { WebhookConfigCreateInput, WebhookConfigOutput, WebhookConfigPageInput } from '@/types/webhook'
import { confirmAction, notifySuccess } from '@/utils/feedback'
import { formatDateTime } from '@/utils/formatter'

type DialogMode = 'create' | 'edit'
type SortOrder = 'ascending' | 'descending' | null

const WEBHOOK_EVENT_OPTIONS = [
  { label: '工单创建', value: 'TICKET_CREATED' },
  { label: '工单状态变更', value: 'TICKET_STATUS_CHANGED' },
  { label: '工单分派', value: 'TICKET_ASSIGNED' },
  { label: '工单完结', value: 'TICKET_COMPLETED' },
  { label: '工单关闭', value: 'TICKET_CLOSED' },
] as const

const tableLoading = ref(false)
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const submitLoading = ref(false)
const deletingId = ref<number>()
const latestChangedId = ref<number>()

const dialogMode = ref<DialogMode>('create')
const editingId = ref<number>()
const formRef = ref<FormInstance>()

const query = reactive<WebhookConfigPageInput>({
  keyword: '',
  eventType: '',
  isActive: undefined,
  pageNum: 1,
  pageSize: 20,
})

const sortState = reactive<{
  prop: keyof WebhookConfigOutput | ''
  order: SortOrder
}>({
  prop: '',
  order: null,
})

const tableData = ref<WebhookConfigOutput[]>([])
const total = ref(0)

const form = reactive<WebhookConfigCreateInput>({
  name: '',
  url: '',
  secret: '',
  eventTypes: [],
  isActive: 1,
  timeoutMs: 5000,
  maxRetryTimes: 3,
  description: '',
})

const formRules: FormRules<WebhookConfigCreateInput> = {
  name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  url: [
    { required: true, message: '请输入Webhook地址', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        try {
          const parsedUrl = new URL(value)
          if (parsedUrl.protocol !== 'http:' && parsedUrl.protocol !== 'https:') {
            callback(new Error('Webhook地址必须为http或https协议'))
            return
          }
          callback()
        } catch {
          callback(new Error('请输入有效的Webhook地址'))
        }
      },
      trigger: 'blur',
    },
  ],
  eventTypes: [{ type: 'array', required: true, min: 1, message: '请至少选择一个事件类型', trigger: 'change' }],
  timeoutMs: [
    { required: true, message: '请输入超时时间', trigger: 'change' },
    { type: 'number', min: 1000, max: 60000, message: '超时时间需在1000~60000毫秒', trigger: 'change' },
  ],
  maxRetryTimes: [
    { required: true, message: '请输入重试次数', trigger: 'change' },
    { type: 'number', min: 1, max: 5, message: '重试次数需在1~5次', trigger: 'change' },
  ],
}

const sortedTableData = computed(() => {
  const result = [...tableData.value]
  if (!sortState.prop || !sortState.order) {
    return result
  }
  const direction = sortState.order === 'ascending' ? 1 : -1
  const sortProp = sortState.prop as keyof WebhookConfigOutput
  result.sort((left, right) => direction * compareByProp(left, right, sortProp))
  return result
})

const latestChangedRecord = computed(
  () =>
    sortedTableData.value.find((item) => item.id === latestChangedId.value) ??
    sortedTableData.value[0] ??
    undefined,
)

const latestChangeTime = computed(() =>
  formatDateTime(latestChangedRecord.value?.updateTime || latestChangedRecord.value?.createTime),
)

const latestChangeBy = computed(
  () => latestChangedRecord.value?.updateBy || latestChangedRecord.value?.createBy || '系统',
)

function compareByProp(
  left: WebhookConfigOutput,
  right: WebhookConfigOutput,
  prop: keyof WebhookConfigOutput,
): number {
  const leftValue = left[prop]
  const rightValue = right[prop]

  if (leftValue === rightValue) {
    return 0
  }
  if (leftValue === undefined || leftValue === null) {
    return 1
  }
  if (rightValue === undefined || rightValue === null) {
    return -1
  }

  if (typeof leftValue === 'number' && typeof rightValue === 'number') {
    return leftValue - rightValue
  }

  if (prop.endsWith('Time')) {
    const leftTime = new Date(String(leftValue)).getTime()
    const rightTime = new Date(String(rightValue)).getTime()
    if (!Number.isNaN(leftTime) && !Number.isNaN(rightTime)) {
      return leftTime - rightTime
    }
  }

  return String(leftValue).localeCompare(String(rightValue), 'zh-CN')
}

function resetForm(): void {
  form.name = ''
  form.url = ''
  form.secret = ''
  form.eventTypes = []
  form.isActive = 1
  form.timeoutMs = 5000
  form.maxRetryTimes = 3
  form.description = ''
  editingId.value = undefined
}

function getEventTypeLabel(eventType: string): string {
  return WEBHOOK_EVENT_OPTIONS.find((item) => item.value === eventType)?.label || eventType
}

function getStatusTagType(isActive: number): 'success' | 'info' {
  return isActive === 1 ? 'success' : 'info'
}

function resolveRowClassName(payload: { row: Record<string, unknown> }): string {
  if (Number(payload.row.id) === latestChangedId.value) {
    return 'recently-changed-row'
  }
  return ''
}

function handleSortChange(payload: { prop: string; order: SortOrder }): void {
  sortState.prop = (payload.order ? payload.prop : '') as keyof WebhookConfigOutput | ''
  sortState.order = payload.order
}

function handlePaginationChange(payload: { pageNum: number; pageSize: number }): void {
  query.pageNum = payload.pageNum
  query.pageSize = payload.pageSize
  void loadWebhookConfigs()
}

function handleSearch(): void {
  query.pageNum = 1
  void loadWebhookConfigs()
}

function handleReset(): void {
  query.keyword = ''
  query.eventType = ''
  query.isActive = undefined
  query.pageNum = 1
  query.pageSize = 20
  sortState.prop = ''
  sortState.order = null
  void loadWebhookConfigs()
}

async function loadWebhookConfigs(): Promise<void> {
  tableLoading.value = true
  try {
    const pageResult = await getWebhookConfigPage({
      ...query,
      keyword: query.keyword?.trim() || undefined,
      eventType: query.eventType?.trim() || undefined,
    })
    tableData.value = pageResult.records
    total.value = pageResult.total
  } catch {
    // 统一错误提示由请求拦截器处理，这里保留当前筛选与列表状态
  } finally {
    tableLoading.value = false
  }
}

function openCreateDialog(): void {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEditDialog(row: WebhookConfigOutput): Promise<void> {
  dialogMode.value = 'edit'
  editingId.value = row.id
  dialogVisible.value = true
  dialogLoading.value = true
  try {
    const detail = await getWebhookConfigDetail(row.id)
    form.name = detail.name || ''
    form.url = detail.url
    form.secret = detail.secret || ''
    form.eventTypes = [...detail.eventTypes]
    form.isActive = detail.isActive
    form.timeoutMs = detail.timeoutMs
    form.maxRetryTimes = detail.maxRetryTimes
    form.description = detail.description || ''
  } catch {
    dialogVisible.value = false
  } finally {
    dialogLoading.value = false
  }
}

async function handleDelete(row: WebhookConfigOutput): Promise<void> {
  try {
    await confirmAction(`确认删除Webhook配置【${row.name || row.url}】吗？删除后不可恢复。`)
  } catch {
    return
  }

  deletingId.value = row.id
  try {
    await deleteWebhookConfig(row.id)
    notifySuccess('Webhook配置删除成功')
    if (tableData.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    await loadWebhookConfigs()
  } catch {
    // 失败时不调整当前筛选与页码
  } finally {
    deletingId.value = undefined
  }
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    const payload: WebhookConfigCreateInput = {
      name: form.name.trim(),
      url: form.url.trim(),
      secret: form.secret?.trim() || undefined,
      eventTypes: [...form.eventTypes],
      isActive: form.isActive,
      timeoutMs: form.timeoutMs,
      maxRetryTimes: form.maxRetryTimes,
      description: form.description?.trim() || undefined,
    }

    if (dialogMode.value === 'create') {
      const id = await createWebhookConfig(payload)
      latestChangedId.value = id
      notifySuccess('Webhook配置创建成功')
    } else if (editingId.value) {
      await updateWebhookConfig(editingId.value, payload)
      latestChangedId.value = editingId.value
      notifySuccess('Webhook配置更新成功')
    }

    dialogVisible.value = false
    await loadWebhookConfigs()
  } catch {
    // 提交失败保留表单内容，便于修正后重试
  } finally {
    submitLoading.value = false
  }
}

onMounted(async () => {
  await loadWebhookConfigs()
})
</script>

<template>
  <el-space direction="vertical" fill :size="12">
    <el-alert
      title="风险提示：Webhook地址需为可访问的http/https地址，签名密钥请直接拼接在URL参数中，请避免在公开渠道泄露。"
      type="warning"
      :closable="false"
      show-icon
    />

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">Webhook 配置管理</span>
          <el-space>
            <el-button type="primary" @click="openCreateDialog">新增配置</el-button>
            <el-button @click="loadWebhookConfigs">刷新</el-button>
          </el-space>
        </div>
      </template>

      <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="请输入名称、URL或描述"
            clearable
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
          <el-select v-model="query.isActive" clearable placeholder="请选择内容">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-space>
            <el-button type="primary" native-type="submit" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-space>
        </el-form-item>
      </el-form>

      <div class="change-metadata">
        <span>最近变更时间：{{ latestChangeTime }}</span>
        <span>变更人：{{ latestChangeBy }}</span>
      </div>

      <EmptyState v-if="!tableLoading && total === 0" description="暂无Webhook配置" />
      <template v-else>
        <BaseTable
          :data="sortedTableData"
          :loading="tableLoading"
          :row-class-name="resolveRowClassName"
          @sort-change="handleSortChange"
        >
          <el-table-column prop="name" label="配置名称" min-width="120" sortable="custom" show-overflow-tooltip />
          <el-table-column prop="url" label="Webhook URL" min-width="200" sortable="custom" show-overflow-tooltip>
            <template #default="{ row }">
              <el-link type="primary" :href="row.url" target="_blank" :underline="false">
                {{ row.url }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column label="事件类型" min-width="160">
            <template #default="{ row }">
              <div class="event-tags">
                <el-tag v-for="eventType in row.eventTypes" :key="eventType" type="info" size="small">
                  {{ getEventTypeLabel(eventType) }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="超时/重试" width="100" align="center">
            <template #default="{ row }">
              {{ row.timeoutMs }}ms / {{ row.maxRetryTimes }}次
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.isActive)" size="small">
                {{ row.isActive === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最近成功" width="160" sortable="custom" prop="lastSuccessTime">
            <template #default="{ row }">
              {{ formatDateTime(row.lastSuccessTime) }}
            </template>
          </el-table-column>
          <el-table-column label="最近失败" width="160" sortable="custom" prop="lastFailTime">
            <template #default="{ row }">
              <el-tooltip v-if="row.lastFailReason" :content="row.lastFailReason" placement="top">
                <span class="fail-reason">{{ formatDateTime(row.lastFailTime) }}</span>
              </el-tooltip>
              <template v-else>{{ formatDateTime(row.lastFailTime) }}</template>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="160" sortable="custom" prop="updateTime">
            <template #default="{ row }">
              {{ formatDateTime(row.updateTime || row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button type="danger" link size="small" :loading="deletingId === row.id" @click="handleDelete(row)">
                删除
              </el-button>
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

  <el-dialog
    v-model="dialogVisible"
    :title="dialogMode === 'create' ? '新增Webhook配置' : '编辑Webhook配置'"
    width="700px"
  >
    <div v-loading="dialogLoading">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
        <el-form-item label="配置名称" prop="name" required>
          <el-input v-model="form.name" placeholder="请输入配置名称，便于区分推送目标" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="Webhook URL" prop="url" required>
          <el-input v-model="form.url" placeholder="请输入 http(s):// 开头的回调地址，签名密钥可拼接在URL参数中" />
        </el-form-item>
        <el-form-item label="事件类型" prop="eventTypes" required>
          <el-checkbox-group v-model="form.eventTypes">
            <el-checkbox
              v-for="option in WEBHOOK_EVENT_OPTIONS"
              :key="option.value"
              :value="option.value"
              >{{ option.label }}</el-checkbox
            >
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="超时时间(ms)" prop="timeoutMs" required>
          <el-input-number v-model="form.timeoutMs" :min="1000" :max="60000" controls-position="right" />
        </el-form-item>
        <el-form-item label="重试次数" prop="maxRetryTimes" required>
          <el-input-number v-model="form.maxRetryTimes" :min="1" :max="5" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="isActive" required>
          <el-radio-group v-model="form.isActive">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请输入Webhook用途说明"
          />
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
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

.change-metadata {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}

.event-tags {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px;
}

.fail-reason {
  color: #f56c6c;
  cursor: pointer;
  border-bottom: 1px dashed #f56c6c;
}

:deep(.recently-changed-row > td.el-table__cell) {
  background-color: #f0f9ff !important;
}
</style>
