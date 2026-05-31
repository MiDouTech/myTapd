<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'

import { getCategoryTree } from '@/api/category'
import {
  getWeeklyInvalidReportConfig,
  previewWeeklyInvalidReport,
  pushWeeklyInvalidReport,
  updateWeeklyInvalidReportConfig,
} from '@/api/weeklyInvalidReport'
import type { CategoryTreeOutput } from '@/types/category'
import type {
  WeeklyInvalidReportConfigOutput,
  WeeklyInvalidReportConfigUpdateInput,
  WeeklyInvalidReportOutput,
} from '@/types/weeklyInvalidReport'
import { notifyError, notifySuccess, notifyWarning } from '@/utils/feedback'
import { formatDateTime } from '@/utils/formatter'

interface CategoryOption {
  id: number
  label: string
}

const configLoading = ref(false)
const previewLoading = ref(false)
const pushLoading = ref(false)
const saveLoading = ref(false)
const categoryLoading = ref(false)

const activeTab = ref('config')
const cronInput = ref('')
const webhookInput = ref('')

const categoryOptions = ref<CategoryOption[]>([])
const previewData = ref<WeeklyInvalidReportOutput | null>(null)

const configForm = reactive<WeeklyInvalidReportConfigUpdateInput>({
  enabled: false,
  cronList: ['0 0 18 ? * FRI'],
  webhookUrls: [],
  statCategoryIds: [],
  maxDetailCount: 30,
  timezone: 'Asia/Shanghai',
})

const cronPresets = [
  { label: '每周五 18:00', value: '0 0 18 ? * FRI' },
  { label: '每周五 17:30', value: '0 30 17 ? * FRI' },
  { label: '每周五 19:00', value: '0 0 19 ? * FRI' },
]

function flattenCategoryTree(tree: CategoryTreeOutput[], prefix = ''): CategoryOption[] {
  const options: CategoryOption[] = []
  for (const node of tree) {
    const label = prefix ? `${prefix} / ${node.name}` : node.name
    options.push({
      id: node.id,
      label,
    })
    if (node.children && node.children.length > 0) {
      options.push(...flattenCategoryTree(node.children, label))
    }
  }
  return options
}

async function loadCategoryOptions() {
  categoryLoading.value = true
  try {
    const tree = await getCategoryTree()
    categoryOptions.value = flattenCategoryTree(tree)
  } catch {
    notifyError('加载分类列表失败')
  } finally {
    categoryLoading.value = false
  }
}

async function loadConfig() {
  configLoading.value = true
  try {
    const data: WeeklyInvalidReportConfigOutput = await getWeeklyInvalidReportConfig()
    configForm.enabled = data.enabled
    configForm.cronList = data.cronList?.length ? [...data.cronList] : ['0 0 18 ? * FRI']
    configForm.webhookUrls = data.webhookUrls || []
    configForm.statCategoryIds = data.statCategoryIds || []
    configForm.maxDetailCount = data.maxDetailCount || 30
    configForm.timezone = data.timezone || 'Asia/Shanghai'
  } catch {
    notifyError('加载无效反馈周报配置失败')
  } finally {
    configLoading.value = false
  }
}

async function saveConfig() {
  if (!configForm.cronList?.length) {
    notifyWarning('请至少配置一个推送时间')
    return
  }
  if (!configForm.maxDetailCount || configForm.maxDetailCount <= 0) {
    notifyWarning('明细上限必须大于0')
    return
  }
  saveLoading.value = true
  try {
    await updateWeeklyInvalidReportConfig(configForm)
    notifySuccess('无效反馈周报配置已保存')
  } catch {
    notifyError('保存无效反馈周报配置失败')
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
  if (!configForm.webhookUrls) {
    configForm.webhookUrls = []
  }
  if (configForm.webhookUrls.includes(url)) {
    notifyWarning('该 Webhook 地址已存在')
    return
  }
  configForm.webhookUrls.push(url)
  webhookInput.value = ''
}

function removeWebhook(index: number) {
  configForm.webhookUrls?.splice(index, 1)
}

function addCron() {
  const cron = cronInput.value.trim()
  if (!cron) {
    notifyWarning('请输入 Cron 表达式')
    return
  }
  if (!configForm.cronList) {
    configForm.cronList = []
  }
  if (configForm.cronList.includes(cron)) {
    notifyWarning('该 Cron 表达式已存在')
    return
  }
  configForm.cronList.push(cron)
  cronInput.value = ''
}

function removeCron(index: number) {
  configForm.cronList?.splice(index, 1)
}

function isCronSelected(cronValue: string): boolean {
  return configForm.cronList?.includes(cronValue) ?? false
}

function toggleCronPreset(cronValue: string) {
  if (!configForm.cronList) {
    configForm.cronList = []
  }
  const index = configForm.cronList.indexOf(cronValue)
  if (index >= 0) {
    configForm.cronList.splice(index, 1)
    return
  }
  configForm.cronList.push(cronValue)
}

function getCronLabel(cron: string): string {
  const preset = cronPresets.find((item) => item.value === cron)
  return preset ? preset.label : cron
}

async function handlePreview() {
  previewLoading.value = true
  try {
    previewData.value = await previewWeeklyInvalidReport()
  } catch {
    notifyError('预览无效反馈周报失败')
  } finally {
    previewLoading.value = false
  }
}

async function handlePush() {
  if (!configForm.webhookUrls || configForm.webhookUrls.length === 0) {
    notifyWarning('请先配置至少一个 Webhook 地址')
    return
  }
  pushLoading.value = true
  try {
    await pushWeeklyInvalidReport()
    notifySuccess('无效反馈周报已推送到企微群')
  } catch {
    // 请求拦截器会直接展示后端可读错误文案
  } finally {
    pushLoading.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'preview' && !previewData.value) {
    void handlePreview()
  }
})

onMounted(() => {
  void loadConfig()
  void loadCategoryOptions()
})
</script>

<template>
  <div class="weekly-invalid-report-view">
    <el-tabs v-model="activeTab" class="report-tabs">
      <el-tab-pane label="推送配置" name="config">
        <el-card v-loading="configLoading" shadow="never" class="config-card">
          <el-form label-width="160px" label-position="right">
            <el-form-item label="自动推送">
              <el-switch v-model="configForm.enabled" active-text="开启" inactive-text="关闭" />
            </el-form-item>

            <el-form-item label="推送时间">
              <div class="cron-section">
                <div class="cron-add">
                  <el-input
                    v-model="cronInput"
                    placeholder="输入 Cron 表达式"
                    style="width: 320px"
                    @keyup.enter="addCron"
                  >
                    <template #prepend>Cron</template>
                  </el-input>
                  <el-button type="primary" @click="addCron">添加</el-button>
                </div>
                <div class="cron-presets">
                  <el-tag
                    v-for="preset in cronPresets"
                    :key="preset.value"
                    class="cron-tag"
                    :type="isCronSelected(preset.value) ? '' : 'info'"
                    :effect="isCronSelected(preset.value) ? 'dark' : 'plain'"
                    @click="toggleCronPreset(preset.value)"
                  >
                    {{ preset.label }}
                  </el-tag>
                </div>
                <div v-if="configForm.cronList?.length" class="cron-list">
                  <el-tag
                    v-for="(cron, index) in configForm.cronList"
                    :key="cron"
                    closable
                    class="cron-item-tag"
                    @close="removeCron(index)"
                  >
                    {{ getCronLabel(cron) }}
                  </el-tag>
                </div>
              </div>
            </el-form-item>

            <el-form-item label="调度时区">
              <el-input
                v-model="configForm.timezone"
                placeholder="例如 Asia/Shanghai"
                style="max-width: 360px"
              />
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
                <div v-if="configForm.webhookUrls?.length" class="webhook-list">
                  <div v-for="(url, index) in configForm.webhookUrls" :key="index" class="webhook-item">
                    <el-tag closable type="info" @close="removeWebhook(index)">
                      {{ url.length > 70 ? url.slice(0, 70) + '...' : url }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </el-form-item>

            <el-form-item label="统计范围分类">
              <el-select
                v-model="configForm.statCategoryIds"
                class="category-select"
                multiple
                filterable
                clearable
                collapse-tags
                collapse-tags-tooltip
                :loading="categoryLoading"
                placeholder="请选择纳入周报统计的分类（按分类ID）"
              >
                <el-option
                  v-for="option in categoryOptions"
                  :key="option.id"
                  :label="option.label"
                  :value="option.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="明细上限">
              <el-input-number
                v-model="configForm.maxDetailCount"
                :min="1"
                :max="200"
                controls-position="right"
              />
              <el-text type="info" size="small" style="margin-left: 12px">
                防止周报过长触发企微消息长度限制
              </el-text>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="saveLoading" @click="saveConfig">保存配置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="周报预览" name="preview">
        <div class="preview-actions">
          <el-button type="primary" :loading="previewLoading" @click="handlePreview">刷新预览</el-button>
          <el-button type="success" :loading="pushLoading" @click="handlePush">手动推送到群</el-button>
        </div>

        <div v-if="previewData" class="preview-content">
          <el-card shadow="never" class="summary-card">
            <template #header>
              <span class="card-header-title">{{ previewData.reportDate }} 无效反馈周报</span>
            </template>
            <div class="summary-range">统计区间：{{ previewData.weekRangeLabel }}</div>
            <div class="summary-stats">
              <div class="stat-item stat-total">
                <span class="stat-label">无效反馈总数</span>
                <span class="stat-value">{{ previewData.summary.invalidTotalCount }}</span>
              </div>
              <div class="stat-item stat-reporter">
                <span class="stat-label">涉及反馈人</span>
                <span class="stat-value">{{ previewData.summary.reporterCount }}</span>
              </div>
              <div class="stat-item stat-detail">
                <span class="stat-label">明细展示条数</span>
                <span class="stat-value">{{ previewData.summary.detailDisplayCount }}</span>
              </div>
            </div>
          </el-card>

          <el-card shadow="never" class="section-card">
            <template #header>
              <span class="section-title">1、按反馈人统计</span>
            </template>
            <el-table
              :data="previewData.reporterStats"
              :border="false"
              :stripe="true"
              :header-cell-style="{ backgroundColor: '#f5f7fa', textAlign: 'center' }"
              :cell-style="{ textAlign: 'center' }"
            >
              <el-table-column type="index" label="序号" width="70" />
              <el-table-column prop="reporterName" label="反馈人" min-width="160" />
              <el-table-column prop="invalidCount" label="无效反馈数量" min-width="140" />
            </el-table>
          </el-card>

          <el-card shadow="never" class="section-card">
            <template #header>
              <span class="section-title">2、无效反馈明细</span>
            </template>
            <el-table
              :data="previewData.ticketDetails"
              :border="false"
              :stripe="true"
              :header-cell-style="{ backgroundColor: '#f5f7fa', textAlign: 'center' }"
              :cell-style="{ textAlign: 'center' }"
            >
              <el-table-column type="index" label="序号" width="70" />
              <el-table-column prop="ticketNo" label="工单编号" width="180" />
              <el-table-column prop="title" label="标题" min-width="280" :show-overflow-tooltip="true" />
              <el-table-column prop="reporterName" label="反馈人" min-width="120" />
              <el-table-column label="关闭时间" min-width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.closedTime) }}
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <el-card shadow="never" class="markdown-card">
            <template #header>
              <span class="section-title">企微 Markdown 预览</span>
            </template>
            <pre class="markdown-preview">{{ previewData.markdownContent }}</pre>
          </el-card>
        </div>

        <div v-else-if="!previewLoading" class="preview-placeholder">
          <el-empty description="暂无周报数据" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped lang="scss">
.weekly-invalid-report-view {
  padding: 0;
}

.report-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 20px;
  }
}

.config-card {
  max-width: 880px;

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.cron-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.cron-add,
.webhook-add {
  display: flex;
  gap: 8px;
}

.cron-presets,
.cron-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.cron-tag {
  cursor: pointer;
}

.webhook-section,
.category-select {
  width: 100%;
}

.webhook-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
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
    font-size: 17px;
    font-weight: 600;
    color: #1d2129;
  }
}

.summary-range {
  margin-bottom: 14px;
  font-size: 14px;
  color: #606266;
}

.summary-stats {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.stat-item {
  min-width: 140px;
  padding: 12px 20px;
  border-radius: 8px;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  align-items: center;
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

.stat-total .stat-value {
  color: #f56c6c;
}

.stat-reporter .stat-value {
  color: #1675d1;
}

.stat-detail .stat-value {
  color: #67c23a;
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

.markdown-card {
  :deep(.el-card__header) {
    padding: 12px 20px;
    background: #f5f7fa;
  }
}

.markdown-preview {
  margin: 0;
  padding: 16px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  background: #fafafa;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.preview-placeholder {
  padding: 40px 0;
}
</style>
