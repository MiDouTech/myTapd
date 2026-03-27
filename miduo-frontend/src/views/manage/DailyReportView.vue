<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import {
  getDailyReportConfig,
  previewDailyReport,
  pushDailyReport,
  updateDailyReportConfig,
} from '@/api/dailyReport'
import type {
  DailyReportConfigOutput,
  DailyReportConfigUpdateInput,
  DailyReportOutput,
} from '@/types/dailyReport'
import { notifyError, notifySuccess, notifyWarning } from '@/utils/feedback'

const configLoading = ref(false)
const previewLoading = ref(false)
const pushLoading = ref(false)
const saveLoading = ref(false)

const configForm = reactive<DailyReportConfigUpdateInput>({
  enabled: false,
  cron: '0 0 18 * * ?',
  webhookUrls: [],
  includeDefectDetail: true,
  includeSuspended: true,
})

const webhookInput = ref('')
const previewData = ref<DailyReportOutput | null>(null)
const activeTab = ref('config')

async function loadConfig() {
  configLoading.value = true
  try {
    const data: DailyReportConfigOutput = await getDailyReportConfig()
    configForm.enabled = data.enabled
    configForm.cron = data.cron
    configForm.webhookUrls = data.webhookUrls || []
    configForm.includeDefectDetail = data.includeDefectDetail
    configForm.includeSuspended = data.includeSuspended
  } catch {
    notifyError('加载日报配置失败')
  } finally {
    configLoading.value = false
  }
}

async function saveConfig() {
  saveLoading.value = true
  try {
    await updateDailyReportConfig(configForm)
    notifySuccess('日报配置已保存')
  } catch {
    notifyError('保存日报配置失败')
  } finally {
    saveLoading.value = false
  }
}

function addWebhook() {
  const url = webhookInput.value.trim()
  if (!url) {
    notifyWarning('请输入 Webhook 地址')
    return
  }
  if (!url.startsWith('http')) {
    notifyWarning('Webhook 地址必须以 http 开头')
    return
  }
  if (configForm.webhookUrls?.includes(url)) {
    notifyWarning('该 Webhook 地址已存在')
    return
  }
  if (!configForm.webhookUrls) {
    configForm.webhookUrls = []
  }
  configForm.webhookUrls.push(url)
  webhookInput.value = ''
}

function removeWebhook(index: number) {
  configForm.webhookUrls?.splice(index, 1)
}

async function handlePreview() {
  previewLoading.value = true
  try {
    previewData.value = await previewDailyReport()
  } catch {
    notifyError('预览日报失败')
  } finally {
    previewLoading.value = false
  }
}

async function handlePush() {
  if (!configForm.webhookUrls || configForm.webhookUrls.length === 0) {
    notifyWarning('请先配置 Webhook 地址')
    return
  }
  pushLoading.value = true
  try {
    await pushDailyReport()
    notifySuccess('日报已推送到企微群')
  } catch {
    notifyError('推送日报失败')
  } finally {
    pushLoading.value = false
  }
}

const cronPresets = [
  { label: '每天 09:00', value: '0 0 9 * * ?' },
  { label: '每天 12:00', value: '0 0 12 * * ?' },
  { label: '每天 17:00', value: '0 0 17 * * ?' },
  { label: '每天 18:00', value: '0 0 18 * * ?' },
  { label: '每天 20:00', value: '0 0 20 * * ?' },
  { label: '工作日 18:00', value: '0 0 18 * * MON-FRI' },
]

onMounted(() => {
  loadConfig()
})
</script>

<template>
  <div class="daily-report-view">
    <div class="page-header">
      <h2 class="page-title">日报管理</h2>
      <p class="page-desc">配置日报自动推送规则，支持预览和手动推送到企微群</p>
    </div>

    <el-tabs v-model="activeTab" class="report-tabs">
      <el-tab-pane label="推送配置" name="config">
        <el-card v-loading="configLoading" shadow="never" class="config-card">
          <el-form label-width="140px" label-position="right">
            <el-form-item label="自动推送">
              <el-switch
                v-model="configForm.enabled"
                active-text="开启"
                inactive-text="关闭"
              />
            </el-form-item>

            <el-form-item label="推送时间">
              <div class="cron-section">
                <el-input v-model="configForm.cron" placeholder="Cron 表达式" style="width: 260px">
                  <template #prepend>Cron</template>
                </el-input>
                <div class="cron-presets">
                  <el-tag
                    v-for="preset in cronPresets"
                    :key="preset.value"
                    :type="configForm.cron === preset.value ? '' : 'info'"
                    class="cron-tag"
                    effect="plain"
                    @click="configForm.cron = preset.value"
                  >
                    {{ preset.label }}
                  </el-tag>
                </div>
              </div>
            </el-form-item>

            <el-form-item label="企微群 Webhook">
              <div class="webhook-section">
                <div class="webhook-add">
                  <el-input
                    v-model="webhookInput"
                    placeholder="输入企微群机器人 Webhook 地址"
                    clearable
                    @keyup.enter="addWebhook"
                  />
                  <el-button type="primary" @click="addWebhook">添加</el-button>
                </div>
                <div v-if="configForm.webhookUrls && configForm.webhookUrls.length > 0" class="webhook-list">
                  <div
                    v-for="(url, index) in configForm.webhookUrls"
                    :key="index"
                    class="webhook-item"
                  >
                    <el-tag closable type="info" @close="removeWebhook(index)">
                      {{ url.length > 60 ? url.substring(0, 60) + '...' : url }}
                    </el-tag>
                  </div>
                </div>
                <div v-else class="webhook-empty">
                  <el-text type="info" size="small">暂未配置 Webhook 地址</el-text>
                </div>
              </div>
            </el-form-item>

            <el-form-item label="缺陷明细分类">
              <el-switch
                v-model="configForm.includeDefectDetail"
                active-text="包含"
                inactive-text="不包含"
              />
              <el-text type="info" size="small" style="margin-left: 12px">
                已解决区域是否展示缺陷/非缺陷子分类
              </el-text>
            </el-form-item>

            <el-form-item label="挂起工单列表">
              <el-switch
                v-model="configForm.includeSuspended"
                active-text="包含"
                inactive-text="不包含"
              />
              <el-text type="info" size="small" style="margin-left: 12px">
                日报中是否展示挂起工单明细
              </el-text>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="saveLoading" @click="saveConfig">
                保存配置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="日报预览" name="preview">
        <div class="preview-actions">
          <el-button type="primary" :loading="previewLoading" @click="handlePreview">
            刷新预览
          </el-button>
          <el-button
            type="success"
            :loading="pushLoading"
            @click="handlePush"
          >
            手动推送到群
          </el-button>
        </div>

        <div v-if="previewData" class="preview-content">
          <el-card shadow="never" class="summary-card">
            <template #header>
              <span class="card-header-title">{{ previewData.reportDate }} 线上问题日报</span>
            </template>
            <div class="summary-stats">
              <div class="stat-item">
                <span class="stat-label">问题反馈总数</span>
                <span class="stat-value">{{ previewData.summary.totalFeedbackCount }}</span>
              </div>
              <div class="stat-item stat-pending">
                <span class="stat-label">待解决</span>
                <span class="stat-value">{{ previewData.summary.pendingResolveCount }}</span>
              </div>
              <div class="stat-item stat-temp">
                <span class="stat-label">临时解决</span>
                <span class="stat-value">{{ previewData.summary.tempResolvedCount }}</span>
              </div>
              <div class="stat-item stat-resolved">
                <span class="stat-label">已解决</span>
                <span class="stat-value">{{ previewData.summary.resolvedCount }}</span>
              </div>
              <div class="stat-item stat-suspended">
                <span class="stat-label">挂起</span>
                <span class="stat-value">{{ previewData.summary.suspendedCount }}</span>
              </div>
            </div>
          </el-card>

          <template v-if="previewData.pendingSection">
            <el-card shadow="never" class="section-card">
              <template #header>
                <span class="section-title">1、待解决</span>
              </template>
              <template v-for="(sub, si) in previewData.pendingSection.subSections" :key="si">
                <div class="subsection">
                  <div class="subsection-title">1.{{ si + 1 }}、{{ sub.title }}：</div>
                  <div v-if="sub.tickets && sub.tickets.length > 0" class="ticket-list">
                    <div v-for="ticket in sub.tickets" :key="ticket.id" class="ticket-item">
                      <el-tag size="small" type="info">{{ ticket.ticketNo }}</el-tag>
                      <span class="ticket-title">{{ ticket.title }}</span>
                      <span v-if="ticket.assigneeName" class="ticket-assignee">
                        —— {{ ticket.assigneeName }}
                      </span>
                      <el-tag
                        v-if="ticket.severityLevel"
                        size="small"
                        :type="getSeverityType(ticket.severityLevel)"
                      >
                        {{ ticket.severityLevel }}
                      </el-tag>
                    </div>
                  </div>
                  <div v-else class="ticket-empty">无</div>
                </div>
              </template>
            </el-card>
          </template>

          <template v-if="previewData.tempResolvedSection">
            <el-card shadow="never" class="section-card">
              <template #header>
                <span class="section-title">2、临时解决</span>
              </template>
              <template
                v-for="(sub, si) in previewData.tempResolvedSection.subSections"
                :key="si"
              >
                <div class="ticket-list">
                  <div v-for="ticket in sub.tickets" :key="ticket.id" class="ticket-item">
                    <el-tag size="small" type="info">{{ ticket.ticketNo }}</el-tag>
                    <span class="ticket-title">{{ ticket.title }}</span>
                    <span v-if="ticket.assigneeName" class="ticket-assignee">
                      —— {{ ticket.assigneeName }}
                    </span>
                  </div>
                </div>
              </template>
              <div
                v-if="previewData.tempResolvedSection.count === 0"
                class="ticket-empty"
              >
                无
              </div>
            </el-card>
          </template>

          <template v-if="previewData.resolvedSection">
            <el-card shadow="never" class="section-card">
              <template #header>
                <span class="section-title">3、已解决</span>
              </template>
              <div
                v-if="previewData.resolvedSection.subSections && previewData.resolvedSection.subSections.length > 0"
              >
                <div
                  v-for="(sub, si) in previewData.resolvedSection.subSections"
                  :key="si"
                  class="subsection-inline"
                >
                  3.{{ si + 1 }}、{{ sub.title }}：{{ sub.count }}个
                </div>
              </div>
              <div v-else class="subsection-inline">
                共{{ previewData.resolvedSection.count }}个
              </div>
            </el-card>
          </template>

          <template v-if="previewData.suspendedSection">
            <el-card shadow="never" class="section-card">
              <template #header>
                <span class="section-title">4、挂起</span>
              </template>
              <template
                v-for="(sub, si) in previewData.suspendedSection.subSections"
                :key="si"
              >
                <div class="ticket-list">
                  <div v-for="(ticket, ti) in sub.tickets" :key="ticket.id" class="ticket-item">
                    <span class="ticket-index">4.{{ ti + 1 }}</span>
                    <el-tag size="small" type="info">{{ ticket.ticketNo }}</el-tag>
                    <span class="ticket-title">{{ ticket.title }}</span>
                    <span v-if="ticket.assigneeName" class="ticket-assignee">
                      —— {{ ticket.assigneeName }}
                    </span>
                  </div>
                </div>
              </template>
              <div
                v-if="previewData.suspendedSection.count === 0"
                class="ticket-empty"
              >
                无
              </div>
            </el-card>
          </template>

          <el-card shadow="never" class="markdown-card">
            <template #header>
              <span class="section-title">企微 Markdown 预览</span>
            </template>
            <pre class="markdown-preview">{{ previewData.markdownContent }}</pre>
          </el-card>
        </div>

        <div v-else-if="!previewLoading" class="preview-placeholder">
          <el-empty description="点击「刷新预览」查看今日日报" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script lang="ts">
function getSeverityType(level: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  if (level === 'P0') return 'danger'
  if (level === 'P1') return 'warning'
  if (level === 'P2') return ''
  return 'info'
}
</script>

<style lang="scss" scoped>
.daily-report-view {
  padding: 0;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
}

.page-desc {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.report-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 20px;
  }
}

.config-card {
  max-width: 800px;

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.cron-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cron-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.cron-tag {
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    opacity: 0.8;
  }
}

.webhook-section {
  width: 100%;
}

.webhook-add {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.webhook-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.webhook-item {
  :deep(.el-tag) {
    max-width: 100%;
    height: auto;
    white-space: normal;
    line-height: 1.6;
    padding: 4px 8px;
  }
}

.webhook-empty {
  padding: 4px 0;
}

.preview-actions {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}

.preview-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-card {
  .card-header-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }
}

.summary-stats {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 20px;
  border-radius: 8px;
  background: #f5f7fa;
  min-width: 100px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.stat-pending .stat-value {
  color: #e6a23c;
}

.stat-temp .stat-value {
  color: #909399;
}

.stat-resolved .stat-value {
  color: #67c23a;
}

.stat-suspended .stat-value {
  color: #f56c6c;
}

.section-card {
  :deep(.el-card__header) {
    padding: 12px 20px;
    background: #f5f7fa;
  }
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.subsection {
  margin-bottom: 12px;
}

.subsection-title {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 6px;
}

.subsection-inline {
  font-size: 14px;
  color: #606266;
  padding: 4px 0;
}

.ticket-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-left: 8px;
}

.ticket-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
  line-height: 1.8;
}

.ticket-index {
  color: #909399;
  min-width: 24px;
}

.ticket-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticket-assignee {
  color: #909399;
  white-space: nowrap;
}

.ticket-empty {
  padding: 4px 8px;
  color: #c0c4cc;
  font-size: 13px;
}

.markdown-card {
  :deep(.el-card__header) {
    padding: 12px 20px;
    background: #f5f7fa;
  }
}

.markdown-preview {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.6;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 16px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-word;
  color: #303133;
  margin: 0;
}

.preview-placeholder {
  padding: 40px 0;
}
</style>
