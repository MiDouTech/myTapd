<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { getWecomConfigDetail, saveWecomConfig, testWecomConnection } from '@/api/wecom'
import type { WecomConfigUpdateInput } from '@/types/wecom'
import { notifySuccess, notifyWarning } from '@/utils/feedback'
import { formatDateTime } from '@/utils/formatter'

const emit = defineEmits<{
  (e: 'open-nlp-keyword'): void
  (e: 'open-nlp-log'): void
}>()

const loading = ref(false)
const submitLoading = ref(false)
const testLoading = ref(false)
const hasPersistedConfig = ref(false)
const corpSecretMasked = ref('')
const callbackAesKeyMasked = ref('')
const latestUpdateTime = ref('')
const formRef = ref<FormInstance>()

const nlpEnabled = ref(false)

const defaultForm: WecomConfigUpdateInput = {
  corpId: '',
  agentId: '',
  corpSecret: '',
  apiBaseUrl: 'https://qyapi.weixin.qq.com',
  connectTimeoutMs: 10000,
  readTimeoutMs: 30000,
  scheduleEnabled: false,
  scheduleCron: '',
  retryCount: 0,
  batchSize: 100,
  enabled: true,
  callbackToken: '',
  callbackAesKey: '',
}

const form = reactive<WecomConfigUpdateInput>({
  ...defaultForm,
})

const rules: FormRules<WecomConfigUpdateInput> = {
  corpId: [
    { required: true, message: '请输入企业微信CorpID', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        const normalized = value?.trim() || ''
        if (!normalized.startsWith('ww')) {
          callback(new Error('CorpID通常以ww开头，请确认填写的是企微企业ID'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  agentId: [
    { required: true, message: '请输入AgentID', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        const normalized = value?.trim() || ''
        if (!/^\d+$/.test(normalized)) {
          callback(new Error('AgentID必须为数字'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  corpSecret: [{ required: true, message: '请输入应用Secret', trigger: 'blur' }],
  apiBaseUrl: [{ required: true, message: '请输入企微API基础地址', trigger: 'blur' }],
  connectTimeoutMs: [
    { required: true, message: '请输入连接超时', trigger: 'change' },
    { type: 'number', min: 1000, max: 120000, message: '连接超时范围应在1000~120000ms', trigger: 'change' },
  ],
  readTimeoutMs: [
    { required: true, message: '请输入读取超时', trigger: 'change' },
    { type: 'number', min: 1000, max: 180000, message: '读取超时范围应在1000~180000ms', trigger: 'change' },
  ],
  retryCount: [
    { required: true, message: '请输入失败重试次数', trigger: 'change' },
    { type: 'number', min: 0, max: 10, message: '失败重试次数范围应在0~10', trigger: 'change' },
  ],
  batchSize: [
    { required: true, message: '请输入同步批次大小', trigger: 'change' },
    { type: 'number', min: 1, max: 1000, message: '同步批次大小范围应在1~1000', trigger: 'change' },
  ],
  scheduleCron: [
    {
      validator: (_rule, value: string, callback) => {
        if (form.scheduleEnabled && !(value && value.trim())) {
          callback(new Error('开启定时同步后，Cron表达式不能为空'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

function resetForm(): void {
  Object.assign(form, defaultForm)
  corpSecretMasked.value = ''
  callbackAesKeyMasked.value = ''
  latestUpdateTime.value = '-'
  hasPersistedConfig.value = false
}

async function loadConfig(): Promise<void> {
  loading.value = true
  try {
    const detail = await getWecomConfigDetail()
    if (!detail) {
      resetForm()
      return
    }

    form.corpId = detail.corpId || ''
    form.agentId = detail.agentId || ''
    form.corpSecret = ''
    form.apiBaseUrl = detail.apiBaseUrl || defaultForm.apiBaseUrl
    form.connectTimeoutMs = detail.connectTimeoutMs || defaultForm.connectTimeoutMs
    form.readTimeoutMs = detail.readTimeoutMs || defaultForm.readTimeoutMs
    form.scheduleEnabled = detail.scheduleEnabled ?? defaultForm.scheduleEnabled
    form.scheduleCron = detail.scheduleCron || ''
    form.retryCount = detail.retryCount ?? defaultForm.retryCount
    form.batchSize = detail.batchSize ?? defaultForm.batchSize
    form.enabled = detail.enabled ?? defaultForm.enabled
    form.callbackToken = detail.callbackToken || ''
    form.callbackAesKey = ''

    corpSecretMasked.value = detail.corpSecretMasked || ''
    callbackAesKeyMasked.value = detail.callbackAesKeyMasked || ''
    latestUpdateTime.value = formatDateTime(detail.updateTime)
    hasPersistedConfig.value = true
  } catch {
    // 请求失败时保留当前表单，避免覆盖用户正在输入的内容
  } finally {
    loading.value = false
  }
}

async function handleSave(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    await saveWecomConfig({
      ...form,
      corpId: form.corpId.trim(),
      agentId: form.agentId.trim(),
      corpSecret: form.corpSecret.trim(),
      apiBaseUrl: form.apiBaseUrl.trim(),
      scheduleCron: form.scheduleEnabled ? form.scheduleCron?.trim() || undefined : undefined,
    })
    notifySuccess('企微连接配置保存成功')
    await loadConfig()
  } catch {
    // 提交失败时保留表单，便于用户修正
  } finally {
    submitLoading.value = false
  }
}

async function handleTestConnection(): Promise<void> {
  testLoading.value = true
  try {
    const output = await testWecomConnection()
    if (output.success) {
      notifySuccess(`连接成功，当前可访问部门数：${output.departmentCount}`)
      return
    }
    notifyWarning(output.message || '连接失败，请检查企微配置')
  } catch {
    // 统一错误提示由请求拦截器处理
  } finally {
    testLoading.value = false
  }
}

onMounted(async () => {
  await loadConfig()
})
</script>

<template>
  <el-space direction="vertical" fill :size="12">
    <el-alert
      title="填写位置：系统设置 -> 集成设置 -> 企微连接配置。保存后可直接进行连接测试并用于组织同步。"
      type="info"
      :closable="false"
      show-icon
    />
    <el-alert
      v-if="hasPersistedConfig && corpSecretMasked"
      :title="`已保存密钥：${corpSecretMasked}（安全策略：每次保存都需要重新输入完整Secret）`"
      type="warning"
      :closable="false"
      show-icon
    />

    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span class="card-title">企微连接配置</span>
          <el-space>
            <el-button type="primary" :loading="submitLoading" @click="handleSave">保存配置</el-button>
            <el-button :loading="testLoading" @click="handleTestConnection">连接测试</el-button>
            <el-button @click="loadConfig">刷新</el-button>
          </el-space>
        </div>
      </template>

      <div class="meta-row">
        <span>最近更新时间：{{ latestUpdateTime }}</span>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="150px" class="config-form">
        <el-form-item label="CorpID" prop="corpId" required>
          <el-input v-model="form.corpId" placeholder="请输入企微企业ID（通常以ww开头）" />
        </el-form-item>
        <el-form-item label="AgentID" prop="agentId" required>
          <el-input v-model="form.agentId" placeholder="请输入企微自建应用AgentID" />
        </el-form-item>
        <el-form-item label="应用Secret" prop="corpSecret" required>
          <el-input
            v-model="form.corpSecret"
            show-password
            placeholder="请输入企微应用Secret（每次保存需完整输入）"
          />
        </el-form-item>
        <el-form-item label="API基础地址" prop="apiBaseUrl" required>
          <el-input v-model="form.apiBaseUrl" placeholder="默认：https://qyapi.weixin.qq.com" />
        </el-form-item>
        <el-form-item label="连接超时(ms)" prop="connectTimeoutMs" required>
          <el-input-number v-model="form.connectTimeoutMs" :min="1000" :max="120000" controls-position="right" />
        </el-form-item>
        <el-form-item label="读取超时(ms)" prop="readTimeoutMs" required>
          <el-input-number v-model="form.readTimeoutMs" :min="1000" :max="180000" controls-position="right" />
        </el-form-item>
        <el-form-item label="失败重试次数" prop="retryCount" required>
          <el-input-number v-model="form.retryCount" :min="0" :max="10" controls-position="right" />
        </el-form-item>
        <el-form-item label="同步批次大小" prop="batchSize" required>
          <el-input-number v-model="form.batchSize" :min="1" :max="1000" controls-position="right" />
        </el-form-item>
        <el-form-item label="定时同步" prop="scheduleEnabled">
          <el-switch v-model="form.scheduleEnabled" />
        </el-form-item>
        <el-form-item v-if="form.scheduleEnabled" label="同步Cron" prop="scheduleCron">
          <el-input v-model="form.scheduleCron" placeholder="例如：0 0/30 * * * ?" />
        </el-form-item>
        <el-form-item label="配置状态" prop="enabled">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>

        <el-divider content-position="left">回调配置（企微消息推送验证）</el-divider>

        <el-alert
          v-if="hasPersistedConfig && callbackAesKeyMasked"
          :title="`已保存AESKey：${callbackAesKeyMasked}（安全策略：每次保存都需要重新输入完整AESKey）`"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 12px;"
        />

        <el-form-item label="回调Token" prop="callbackToken">
          <el-input
            v-model="form.callbackToken"
            placeholder="请输入企微回调Token（企微回调配置页面中设置的Token）"
          />
        </el-form-item>
        <el-form-item label="回调AESKey" prop="callbackAesKey">
          <el-input
            v-model="form.callbackAesKey"
            show-password
            placeholder="请输入企微回调EncodingAESKey（43位，每次保存需完整输入）"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 自然语言建单配置区 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">自然语言建单配置</span>
        </div>
      </template>

      <el-form label-width="150px" class="config-form">
        <el-form-item label="功能开关">
          <el-switch v-model="nlpEnabled" active-text="启用自然语言建单" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="关键词规则配置">
          <el-button type="primary" plain @click="emit('open-nlp-keyword')">
            管理关键词规则
          </el-button>
          <span class="field-hint">配置意图识别、分类推断和优先级识别关键词</span>
        </el-form-item>
        <el-form-item label="解析日志">
          <el-button plain @click="emit('open-nlp-log')">
            查看解析日志
          </el-button>
          <span class="field-hint">查看自然语言建单的识别效果和处理记录</span>
        </el-form-item>
      </el-form>

      <el-alert
        title="自然语言建单：用户在企微群 @工单助手 后，直接发送任意自由文本，系统自动解析意图，回复工单预览卡片，用户回复「1」确认创建。群聊超时60秒，私聊超时300秒。"
        type="info"
        :closable="false"
        show-icon
        style="margin-top: 8px;"
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

.meta-row {
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}

.config-form {
  max-width: 760px;
}

.field-hint {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
