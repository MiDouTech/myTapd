<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import {
  getNotificationPage,
  getNotificationPreferences,
  updateNotificationPreferences,
} from '@/api/notification'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useNotificationStore } from '@/stores/notification'
import type {
  NotificationOutput,
  NotificationPageInput,
  NotificationPreferenceOutput,
} from '@/types/notification'
import { notifySuccess, notifyWarning } from '@/utils/feedback'
import { formatDateTime } from '@/utils/formatter'

const router = useRouter()
const notificationStore = useNotificationStore()
const { lastRealtimeAt } = storeToRefs(notificationStore)

const tableLoading = ref(false)
const preferenceLoading = ref(false)
const preferenceSubmitLoading = ref(false)

const notificationList = ref<NotificationOutput[]>([])
const preferenceList = ref<NotificationPreferenceOutput[]>([])
const total = ref(0)
const timeRange = ref<string[]>([])

const query = reactive<NotificationPageInput>({
  pageNum: 1,
  pageSize: 20,
  type: undefined,
  isRead: undefined,
})

const fallbackTypeOptions = [
  { label: '工单创建', value: 'TICKET_CREATED' },
  { label: '状态变更', value: 'STATUS_CHANGED' },
  { label: '工单分派', value: 'ASSIGNED' },
  { label: 'SLA预警', value: 'SLA_WARNING' },
  { label: 'SLA超时', value: 'SLA_BREACHED' },
  { label: '工单评论', value: 'COMMENT' },
  { label: '评论@提醒', value: 'COMMENT_MENTION' },
  { label: '催办', value: 'URGE' },
  { label: '简报提审', value: 'REPORT_SUBMITTED' },
  { label: '简报审核通过', value: 'REPORT_APPROVED' },
  { label: '简报审核驳回', value: 'REPORT_REJECTED' },
  { label: '简报提醒', value: 'REPORT_REMIND' },
]

const typeOptions = computed(() => {
  const options = preferenceList.value
    .filter((item) => item.eventType)
    .map((item) => ({
      label: item.eventTypeLabel || item.eventType,
      value: item.eventType,
    }))
  return options.length > 0 ? options : fallbackTypeOptions
})

const connectionTagType = computed(() => {
  if (notificationStore.realtimeConnected) {
    return 'success'
  }
  return notificationStore.pollingFallbackActive ? 'warning' : 'info'
})

const connectionText = computed(() => {
  if (notificationStore.realtimeConnected) {
    return '实时推送中'
  }
  if (notificationStore.pollingFallbackActive) {
    return '轮询兜底中'
  }
  return '连接中'
})

async function loadNotifications(): Promise<void> {
  tableLoading.value = true
  try {
    const params: NotificationPageInput = {
      ...query,
      type: query.type || undefined,
      createTimeStart: timeRange.value[0],
      createTimeEnd: timeRange.value[1],
    }
    const result = await getNotificationPage(params)
    notificationList.value = result.records || []
    total.value = result.total || 0
  } catch {
    // 请求错误由全局拦截器统一提示，这里保留筛选条件和旧数据
  } finally {
    tableLoading.value = false
  }
}

async function loadPreferences(): Promise<void> {
  preferenceLoading.value = true
  try {
    preferenceList.value = await getNotificationPreferences()
  } catch {
    // 请求错误由全局拦截器统一提示，保留旧数据
  } finally {
    preferenceLoading.value = false
  }
}

function handleSearch(): void {
  query.pageNum = 1
  void loadNotifications()
}

function handleReset(): void {
  query.type = undefined
  query.isRead = undefined
  timeRange.value = []
  query.pageNum = 1
  void loadNotifications()
}

function handlePaginationChange(payload: { pageNum: number; pageSize: number }): void {
  query.pageNum = payload.pageNum
  query.pageSize = payload.pageSize
  void loadNotifications()
}

async function handleMarkAsRead(row: NotificationOutput, silent = false): Promise<void> {
  if (!row.id || row.isRead === 1) {
    return
  }
  await notificationStore.markAsRead(row.id)
  row.isRead = 1
  row.readAt = row.readAt || new Date().toISOString()
  if (!silent) {
    notifySuccess('通知已标记为已读')
  }
}

async function handleMarkAllAsRead(): Promise<void> {
  if (total.value === 0) {
    notifyWarning('暂无通知可操作')
    return
  }
  await notificationStore.markAllAsRead()
  notificationList.value = notificationList.value.map((item) => ({
    ...item,
    isRead: 1,
    readAt: item.readAt || new Date().toISOString(),
  }))
  notifySuccess('已全部标记为已读')
}

function resolveTarget(row: NotificationOutput): { path: string; query?: Record<string, string> } | null {
  if (row.ticketId) {
    return {
      path: `/ticket/detail/${row.ticketId}`,
    }
  }
  if (row.reportId) {
    return {
      path: `/bug-report/detail/${row.reportId}`,
    }
  }
  return null
}

async function handleOpenNotification(row: NotificationOutput): Promise<void> {
  if (row.isRead !== 1) {
    await handleMarkAsRead(row, true)
  }
  const target = resolveTarget(row)
  if (!target) {
    notifyWarning('该通知暂无可跳转目标')
    return
  }
  await router.push(target)
}

function getReadTagType(readStatus?: number): 'success' | 'info' {
  return readStatus === 1 ? 'success' : 'info'
}

function getReadText(readStatus?: number): string {
  return readStatus === 1 ? '已读' : '未读'
}

function validatePreferences(): boolean {
  const invalid = preferenceList.value.find(
    (item) => item.siteEnabled !== 1 && item.wecomEnabled !== 1 && item.emailEnabled !== 1,
  )
  if (!invalid) {
    return true
  }
  const label = invalid.eventTypeLabel || invalid.eventType
  notifyWarning(`通知类型【${label}】至少保留一个通知渠道`)
  return false
}

async function handleSavePreferences(): Promise<void> {
  if (!validatePreferences()) {
    return
  }
  preferenceSubmitLoading.value = true
  try {
    await updateNotificationPreferences({
      items: preferenceList.value.map((item) => ({
        eventType: item.eventType,
        siteEnabled: item.siteEnabled,
        wecomEnabled: item.wecomEnabled,
        emailEnabled: item.emailEnabled,
      })),
    })
    notifySuccess('通知偏好保存成功')
    await loadPreferences()
  } catch {
    // 提交失败时保留当前编辑内容
  } finally {
    preferenceSubmitLoading.value = false
  }
}

function handleRestorePreferenceDefaults(): void {
  if (preferenceList.value.length === 0) {
    notifyWarning('暂无可恢复的偏好项')
    return
  }
  preferenceList.value = preferenceList.value.map((item) => ({
    ...item,
    siteEnabled: 1,
    wecomEnabled: 1,
    emailEnabled: 0,
  }))
  notifySuccess('已恢复默认偏好，请点击“保存偏好”生效')
}

watch(lastRealtimeAt, (value, oldValue) => {
  if (value && value !== oldValue) {
    void loadNotifications()
  }
})

onMounted(async () => {
  await Promise.all([loadNotifications(), loadPreferences(), notificationStore.refreshOverview()])
})
</script>

<template>
  <div class="notification-center-page">
    <el-card shadow="never" class="page-card">
      <div class="toolbar">
        <div class="title">通知中心</div>
        <el-space class="toolbar-actions" wrap>
          <el-tag :type="connectionTagType">{{ connectionText }}</el-tag>
          <el-button @click="loadNotifications">刷新列表</el-button>
          <el-button type="primary" plain @click="handleMarkAllAsRead">全部标记已读</el-button>
        </el-space>
      </div>

      <el-form :inline="true" label-width="72px" class="query-form" @submit.prevent="handleSearch">
        <el-form-item label="通知类型" class="query-form-item">
          <el-select v-model="query.type" class="query-input" placeholder="请选择内容" clearable filterable>
            <el-option
              v-for="option in typeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="已读状态" class="query-form-item">
          <el-select v-model="query.isRead" class="query-input" placeholder="请选择内容" clearable>
            <el-option label="未读" :value="0" />
            <el-option label="已读" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围" class="query-form-item">
          <el-date-picker
            v-model="timeRange"
            class="query-input query-input--wide"
            type="daterange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
          />
        </el-form-item>
        <el-form-item class="query-form-item query-form-actions">
          <el-space class="query-action-buttons">
            <el-button type="primary" native-type="submit" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-space>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="page-card">
      <EmptyState v-if="!tableLoading && notificationList.length === 0" description="暂无通知数据" />
      <template v-else>
        <BaseTable :data="notificationList" :loading="tableLoading">
          <el-table-column label="通知类型" min-width="130">
            <template #default="{ row }">
              {{ row.typeLabel || row.type || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="220" :show-overflow-tooltip="true" />
          <el-table-column prop="content" label="内容" min-width="280" :show-overflow-tooltip="true" />
          <el-table-column label="渠道" min-width="130">
            <template #default="{ row }">
              {{ row.channelLabel || row.channel || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getReadTagType(row.isRead)">
                {{ getReadText(row.isRead) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="通知时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <el-space>
                <el-button v-if="row.isRead !== 1" type="primary" link @click="handleMarkAsRead(row)">
                  标记已读
                </el-button>
                <el-button type="primary" link @click="handleOpenNotification(row)">查看</el-button>
              </el-space>
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

    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span class="title">通知偏好</span>
          <el-space>
            <el-button @click="handleRestorePreferenceDefaults">恢复默认</el-button>
            <el-button @click="loadPreferences">重新加载</el-button>
            <el-button type="primary" :loading="preferenceSubmitLoading" @click="handleSavePreferences">
              保存偏好
            </el-button>
          </el-space>
        </div>
      </template>

      <el-alert
        title="每种通知类型至少开启一个渠道，避免因误操作导致消息漏收。"
        type="warning"
        :closable="false"
        show-icon
        class="tip-alert"
      />

      <EmptyState v-if="!preferenceLoading && preferenceList.length === 0" description="暂无通知偏好配置" />
      <BaseTable v-else :data="preferenceList" :loading="preferenceLoading">
        <el-table-column label="通知类型" min-width="180">
          <template #default="{ row }">
            {{ row.eventTypeLabel || row.eventType }}
          </template>
        </el-table-column>
        <el-table-column label="站内信" width="120">
          <template #default="{ row }">
            <el-switch v-model="row.siteEnabled" :active-value="1" :inactive-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="企微应用" width="120">
          <template #default="{ row }">
            <el-switch v-model="row.wecomEnabled" :active-value="1" :inactive-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="邮件" width="120">
          <template #default="{ row }">
            <el-switch v-model="row.emailEnabled" :active-value="1" :inactive-value="0" />
          </template>
        </el-table-column>
      </BaseTable>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.notification-center-page {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-card {
  width: 100%;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.title {
  font-size: 17px;
  font-weight: 600;
  color: #1d2129;
  line-height: 24px;
  white-space: nowrap;
  flex-shrink: 0;
}

.toolbar-actions {
  width: auto;
  flex: 0 0 auto;
}

.query-form {
  width: 100%;
  padding: 14px 16px;
  background: var(--md-bg-panel, #f9fafb);
  border-radius: 8px;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px 16px;
}

.query-form-item {
  margin-bottom: 0;
  margin-right: 0;
}

.query-input {
  width: 210px;
  max-width: 100%;
}

.query-input--wide {
  width: 280px;
}

.query-form-actions {
  margin-left: auto;
  margin-right: 0;
}

.query-action-buttons {
  width: auto;
}

.query-action-buttons :deep(.el-button) {
  min-width: 88px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  .title {
    font-size: 16px;
    font-weight: 600;
    color: #1d2129;
  }
  flex-wrap: wrap;
}

.tip-alert {
  margin-bottom: 14px;
  border-radius: 8px;
}

@media (max-width: 991px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .query-form {
    padding: 10px 12px;
  }
}

@media (max-width: 768px) {
  .toolbar {
    margin-bottom: 12px;
  }

  .query-form {
    gap: 0;
  }

  .query-form-item {
    width: 100%;
    margin-bottom: 12px;
    margin-right: 0;
  }

  .query-form-item :deep(.el-form-item__content) {
    width: 100%;
    margin-left: 0 !important;
  }

  .query-input,
  .query-input--wide {
    width: 100%;
  }

  .query-form-actions {
    margin-left: 0;
  }

  .query-action-buttons :deep(.el-space__item) {
    width: calc(50% - 4px);
  }

  .query-action-buttons :deep(.el-button) {
    width: 100%;
  }
}
</style>
