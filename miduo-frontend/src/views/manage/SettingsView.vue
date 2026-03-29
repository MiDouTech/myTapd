<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getNotificationPreferences, updateNotificationPreferences } from '@/api/notification'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { NotificationPreferenceOutput } from '@/types/notification'
import WebhookConfigPanel from '@/views/manage/components/WebhookConfigPanel.vue'
import WebhookDispatchLogPanel from '@/views/manage/components/WebhookDispatchLogPanel.vue'
import WecomConfigPanel from '@/views/manage/components/WecomConfigPanel.vue'
import WecomGroupBindingPanel from '@/views/manage/components/WecomGroupBindingPanel.vue'
import WecomNlpKeywordPanel from '@/views/manage/components/WecomNlpKeywordPanel.vue'
import WecomNlpLogPanel from '@/views/manage/components/WecomNlpLogPanel.vue'
import { notifySuccess } from '@/utils/feedback'

const route = useRoute()
const router = useRouter()

const tabValues = ['basic', 'notification', 'integration'] as const
const sectionValues = ['wecomConfig', 'webhook', 'webhookLog', 'wecom', 'nlpKeyword', 'nlpLog'] as const

type SettingsTab = (typeof tabValues)[number]
type IntegrationSection = (typeof sectionValues)[number]

const activeTab = ref<SettingsTab>('notification')
const activeSection = ref<IntegrationSection>('wecomConfig')

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
  if (typeof tab === 'string' && tab !== 'basic' && (tabValues as readonly string[]).includes(tab)) {
    return tab as SettingsTab
  }
  return 'notification'
}

function normalizeSection(section: unknown): IntegrationSection {
  if (typeof section === 'string' && (sectionValues as readonly string[]).includes(section)) {
    return section as IntegrationSection
  }
  return 'wecomConfig'
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
    activeSection.value = 'wecomConfig'
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

onMounted(async () => {
  await loadPreferences()
  syncRouteState()
})
</script>

<template>
  <el-card shadow="never">
    <el-tabs v-model="activeTab" class="settings-tabs">
      <!-- 基础参数模块暂未启用，隐藏待后续开放 -->

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
          <el-tab-pane label="企微连接配置" name="wecomConfig">
            <WecomConfigPanel
              @open-nlp-keyword="activeSection = 'nlpKeyword'"
              @open-nlp-log="activeSection = 'nlpLog'"
            />
          </el-tab-pane>

          <el-tab-pane label="Webhook配置" name="webhook">
            <WebhookConfigPanel />
          </el-tab-pane>

          <el-tab-pane label="Webhook推送日志" name="webhookLog">
            <WebhookDispatchLogPanel />
          </el-tab-pane>

          <el-tab-pane label="企微群绑定" name="wecom">
            <WecomGroupBindingPanel />
          </el-tab-pane>

          <el-tab-pane label="NLP关键词配置" name="nlpKeyword">
            <WecomNlpKeywordPanel />
          </el-tab-pane>

          <el-tab-pane label="NLP解析日志" name="nlpLog">
            <WecomNlpLogPanel />
          </el-tab-pane>
        </el-tabs>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<style scoped lang="scss">
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
</style>
