<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getNotificationPreferences, updateNotificationPreferences } from '@/api/notification'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { NotificationPreferenceOutput } from '@/types/notification'
import { notifySuccess, notifyWarning } from '@/utils/feedback'

const route = useRoute()
const router = useRouter()

const tabValues = ['basic', 'notification', 'integration'] as const
const sectionValues = ['webhook', 'wecom'] as const

type SettingsTab = (typeof tabValues)[number]
type IntegrationSection = (typeof sectionValues)[number]

const activeTab = ref<SettingsTab>('basic')
const activeSection = ref<IntegrationSection>('webhook')

const basicForm = reactive({
  systemName: '米多工单系统',
  timezone: 'Asia/Shanghai',
  workTimeRange: '09:00 - 18:00',
  defaultPageSize: 20,
})

const preferenceLoading = ref(false)
const preferenceSubmitLoading = ref(false)
const preferenceList = ref<NotificationPreferenceOutput[]>([])

const preferenceQuery = reactive({
  pageNum: 1,
  pageSize: 20,
})

const preferenceTotal = computed(() => preferenceList.value.length)

const pagedPreferenceList = computed(() => {
  const start = (preferenceQuery.pageNum - 1) * preferenceQuery.pageSize
  return preferenceList.value.slice(start, start + preferenceQuery.pageSize)
})

function normalizeTab(tab: unknown): SettingsTab {
  if (typeof tab === 'string' && (tabValues as readonly string[]).includes(tab)) {
    return tab as SettingsTab
  }
  return 'basic'
}

function normalizeSection(section: unknown): IntegrationSection {
  if (typeof section === 'string' && (sectionValues as readonly string[]).includes(section)) {
    return section as IntegrationSection
  }
  return 'webhook'
}

function normalizePreferencePage(): void {
  const maxPage = Math.max(1, Math.ceil(preferenceTotal.value / preferenceQuery.pageSize))
  if (preferenceQuery.pageNum > maxPage) {
    preferenceQuery.pageNum = maxPage
  }
}

function syncRouteState(): void {
  const section = activeTab.value === 'integration' ? activeSection.value : undefined
  if (route.query.tab === activeTab.value && route.query.section === section) {
    return
  }
  void router.replace({
    query: {
      ...route.query,
      tab: activeTab.value,
      section,
    },
  })
}

watch(
  () => route.query.tab,
  (value) => {
    const next = normalizeTab(value)
    if (activeTab.value !== next) {
      activeTab.value = next
    }
  },
  { immediate: true },
)

watch(
  () => route.query.section,
  (value) => {
    const next = normalizeSection(value)
    if (activeSection.value !== next) {
      activeSection.value = next
    }
  },
  { immediate: true },
)

watch(activeTab, () => {
  if (activeTab.value !== 'integration') {
    activeSection.value = 'webhook'
  }
  syncRouteState()
})

watch(activeSection, () => {
  if (activeTab.value === 'integration') {
    syncRouteState()
  }
})

async function loadPreferences(): Promise<void> {
  preferenceLoading.value = true
  try {
    preferenceList.value = await getNotificationPreferences()
    normalizePreferencePage()
  } catch {
    // 请求错误由全局拦截器统一提示，这里保留原筛选与旧数据
  } finally {
    preferenceLoading.value = false
  }
}

async function handleSavePreferences(): Promise<void> {
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
    // 提交失败时保留页面上的修改内容，便于用户继续调整
  } finally {
    preferenceSubmitLoading.value = false
  }
}

function handlePreferencePaginationChange(payload: { pageNum: number; pageSize: number }): void {
  preferenceQuery.pageNum = payload.pageNum
  preferenceQuery.pageSize = payload.pageSize
  normalizePreferencePage()
}

function openIntegrationSection(section: IntegrationSection): void {
  activeTab.value = 'integration'
  activeSection.value = section
}

function handleSaveBasicSettings(): void {
  notifyWarning('基础参数配置将在 Task020 继续深化，本阶段仅完成配置框架落地')
}

onMounted(async () => {
  await loadPreferences()
  syncRouteState()
})
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="title">系统设置</div>
    </template>

    <el-tabs v-model="activeTab" class="settings-tabs">
      <el-tab-pane label="基础参数" name="basic">
        <el-form label-width="120px" class="basic-form">
          <el-form-item label="系统名称">
            <el-input v-model="basicForm.systemName" maxlength="50" />
          </el-form-item>
          <el-form-item label="默认时区">
            <el-input v-model="basicForm.timezone" />
          </el-form-item>
          <el-form-item label="工作时间">
            <el-input v-model="basicForm.workTimeRange" />
          </el-form-item>
          <el-form-item label="默认分页条数">
            <el-select v-model="basicForm.defaultPageSize" placeholder="请选择默认分页条数">
              <el-option :value="10" label="10" />
              <el-option :value="20" label="20" />
              <el-option :value="50" label="50" />
              <el-option :value="100" label="100" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSaveBasicSettings">保存基础参数</el-button>
          </el-form-item>
        </el-form>

        <el-alert
          title="基础参数能力已完成框架落地，深度配置与持久化将在 Task020 完成。"
          type="info"
          :closable="false"
          show-icon
        />
      </el-tab-pane>

      <el-tab-pane label="通知偏好" name="notification">
        <div class="action-row">
          <el-space>
            <el-button type="primary" :loading="preferenceSubmitLoading" @click="handleSavePreferences">
              保存偏好
            </el-button>
            <el-button @click="loadPreferences">重新加载</el-button>
          </el-space>
        </div>

        <EmptyState v-if="!preferenceLoading && preferenceTotal === 0" description="暂无通知偏好数据" />
        <template v-else>
          <BaseTable :data="pagedPreferenceList" :loading="preferenceLoading">
            <el-table-column prop="eventTypeLabel" label="事件类型" min-width="180">
              <template #default="{ row }">
                {{ row.eventTypeLabel || row.eventType }}
              </template>
            </el-table-column>
            <el-table-column label="站内信" width="120">
              <template #default="{ row }">
                <el-switch v-model="row.siteEnabled" :active-value="1" :inactive-value="0" />
              </template>
            </el-table-column>
            <el-table-column label="企业微信" width="120">
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
          <BasePagination
            :current-page="preferenceQuery.pageNum"
            :page-size="preferenceQuery.pageSize"
            :total="preferenceTotal"
            @update="handlePreferencePaginationChange"
          />
        </template>
      </el-tab-pane>

      <el-tab-pane label="集成设置" name="integration">
        <el-tabs v-model="activeSection" class="integration-tabs">
          <el-tab-pane label="Webhook配置" name="webhook">
            <el-card shadow="never" class="integration-card">
              <template #header>
                <div class="integration-header">
                  <span>Webhook配置（Task020 预留）</span>
                  <el-button type="primary" link @click="openIntegrationSection('webhook')">
                    保持当前路由状态
                  </el-button>
                </div>
              </template>
              <p class="integration-text">
                已预留 Webhook 配置容器与路由状态（tab=integration&amp;section=webhook），高级能力在
                Task020 完成闭环。
              </p>
              <EmptyState description="Webhook 规则管理将在 Task020 完成。" />
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="企微群绑定" name="wecom">
            <el-card shadow="never" class="integration-card">
              <template #header>
                <div class="integration-header">
                  <span>企微群绑定（Task020 预留）</span>
                  <el-button type="primary" link @click="openIntegrationSection('wecom')">
                    保持当前路由状态
                  </el-button>
                </div>
              </template>
              <p class="integration-text">
                已预留企微群绑定容器与路由状态（tab=integration&amp;section=wecom），后续在 Task020
                打通绑定与验证流程。
              </p>
              <EmptyState description="企微群绑定能力将在 Task020 完成。" />
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<style scoped lang="scss">
.title {
  font-size: 16px;
  font-weight: 600;
}

.settings-tabs {
  margin-top: 4px;
}

.basic-form {
  max-width: 640px;
  margin-bottom: 12px;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.integration-tabs {
  margin-top: 4px;
}

.integration-card {
  min-height: 240px;
}

.integration-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.integration-text {
  margin: 0 0 12px;
  color: #606266;
  font-size: 13px;
  line-height: 20px;
}
</style>
