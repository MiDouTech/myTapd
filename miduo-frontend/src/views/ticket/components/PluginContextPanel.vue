<script setup lang="ts">
import { DocumentCopy } from '@element-plus/icons-vue'
import { computed } from 'vue'

import { notifyError, notifySuccess } from '@/utils/feedback'

const props = defineProps<{
  pluginContext?: Record<string, unknown> | null
  externalUserId?: string | null
  externalTicketRef?: string | null
  integrationAppId?: number | null
}>()

type ContextField = {
  key: string
  label: string
  link?: boolean
  copy?: boolean
}

const USER_FIELDS: ContextField[] = [
  { key: 'userId', label: '用户 ID' },
  { key: 'userName', label: '用户姓名' },
  { key: 'dept', label: '部门' },
  { key: 'role', label: '角色' },
  { key: 'mobile', label: '手机号' },
] 

const SYSTEM_FIELDS: ContextField[] = [
  { key: 'system', label: '系统标识' },
  { key: 'module', label: '模块' },
  { key: 'page', label: '页面' },
  { key: 'pageUrl', label: '页面 URL', link: true },
  { key: 'pageTitle', label: '页面标题' },
]

const BUSINESS_FIELDS: ContextField[] = [
  { key: 'bizId', label: '业务 ID', copy: true },
  { key: 'bizType', label: '业务类型' },
  { key: 'bizNo', label: '业务编号' },
]

const ENV_FIELDS: ContextField[] = [
  { key: 'browser', label: '浏览器' },
  { key: 'os', label: '操作系统' },
  { key: 'screen', label: '屏幕分辨率' },
  { key: 'userAgent', label: 'User-Agent' },
  { key: 'network', label: '网络类型' },
]

const EXTRA_FIELDS: ContextField[] = [
  { key: 'screenshot', label: '截图' },
  { key: 'attachments', label: '附件' },
  { key: 'clientTime', label: '客户端时间' },
]

const KNOWN_KEYS = new Set([
  ...USER_FIELDS.map((item) => item.key),
  ...SYSTEM_FIELDS.map((item) => item.key),
  ...BUSINESS_FIELDS.map((item) => item.key),
  ...ENV_FIELDS.map((item) => item.key),
  ...EXTRA_FIELDS.map((item) => item.key),
  'extra',
])

const context = computed(() => props.pluginContext ?? {})

const extraObject = computed(() => {
  const value = context.value.extra
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    return value as Record<string, unknown>
  }
  return null
})

const extraEntries = computed(() => {
  const entries: Array<{ key: string; label: string; value: unknown }> = []
  for (const field of EXTRA_FIELDS) {
    const value = context.value[field.key]
    if (hasValue(value)) {
      entries.push({ key: field.key, label: field.label, value })
    }
  }
  if (extraObject.value) {
    for (const [key, value] of Object.entries(extraObject.value)) {
      if (hasValue(value)) {
        entries.push({ key: `extra.${key}`, label: key, value })
      }
    }
  } else if (hasValue(context.value.extra) && typeof context.value.extra !== 'object') {
    entries.push({ key: 'extra', label: '扩展信息', value: context.value.extra })
  }
  for (const [key, value] of Object.entries(context.value)) {
    if (!KNOWN_KEYS.has(key) && hasValue(value)) {
      entries.push({ key, label: key, value })
    }
  }
  return entries
})

const hasAnyContent = computed(() => {
  return (
    hasGroupValues(USER_FIELDS) ||
    hasGroupValues(SYSTEM_FIELDS) ||
    hasGroupValues(BUSINESS_FIELDS) ||
    hasGroupValues(ENV_FIELDS) ||
    extraEntries.value.length > 0 ||
    hasValue(props.externalUserId) ||
    hasValue(props.externalTicketRef) ||
    hasValue(props.integrationAppId)
  )
})

function hasValue(value: unknown): boolean {
  if (value === null || value === undefined) {
    return false
  }
  if (typeof value === 'string') {
    return value.trim().length > 0
  }
  if (Array.isArray(value)) {
    return value.length > 0
  }
  return true
}

function hasGroupValues(fields: ReadonlyArray<{ key: string }>): boolean {
  return fields.some((field) => hasValue(context.value[field.key]))
}

function formatValue(value: unknown): string {
  if (value === null || value === undefined) {
    return '-'
  }
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value, null, 2)
    } catch {
      return String(value)
    }
  }
  return String(value)
}

function isLinkValue(value: unknown): value is string {
  if (typeof value !== 'string') {
    return false
  }
  const trimmed = value.trim()
  return trimmed.startsWith('http://') || trimmed.startsWith('https://')
}

async function copyText(text: string, label: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(text)
    notifySuccess(`${label}已复制`)
  } catch {
    notifyError('复制失败，请手动选择复制')
  }
}
</script>

<template>
  <el-card shadow="never" class="plugin-context-panel">
    <template #header>
      <div class="panel-header">
        <span class="panel-title">插件上下文</span>
      </div>
    </template>

    <el-empty v-if="!hasAnyContent" description="暂无插件上下文信息" />

    <template v-else>
      <el-descriptions
        v-if="integrationAppId || externalUserId || externalTicketRef"
        :column="1"
        border
        size="small"
        class="context-block"
        title="接入信息"
      >
        <el-descriptions-item v-if="integrationAppId" label="接入应用 ID">
          {{ integrationAppId }}
        </el-descriptions-item>
        <el-descriptions-item v-if="externalUserId" label="外部用户 ID">
          {{ externalUserId }}
        </el-descriptions-item>
        <el-descriptions-item v-if="externalTicketRef" label="外部工单引用">
          {{ externalTicketRef }}
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions
        v-if="hasGroupValues(USER_FIELDS)"
        :column="1"
        border
        size="small"
        class="context-block"
        title="用户信息"
      >
        <el-descriptions-item
          v-for="field in USER_FIELDS"
          :key="field.key"
          v-show="hasValue(context[field.key])"
          :label="field.label"
        >
          {{ formatValue(context[field.key]) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions
        v-if="hasGroupValues(SYSTEM_FIELDS)"
        :column="1"
        border
        size="small"
        class="context-block"
        title="系统信息"
      >
        <el-descriptions-item
          v-for="field in SYSTEM_FIELDS"
          :key="field.key"
          v-show="hasValue(context[field.key])"
          :label="field.label"
        >
          <el-link
            v-if="field.link && isLinkValue(context[field.key])"
            type="primary"
            :href="String(context[field.key])"
            target="_blank"
            :underline="false"
          >
            {{ context[field.key] }}
          </el-link>
          <span v-else>{{ formatValue(context[field.key]) }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions
        v-if="hasGroupValues(BUSINESS_FIELDS)"
        :column="1"
        border
        size="small"
        class="context-block"
        title="业务信息"
      >
        <el-descriptions-item
          v-for="field in BUSINESS_FIELDS"
          :key="field.key"
          v-show="hasValue(context[field.key])"
          :label="field.label"
        >
          <div v-if="field.copy" class="copy-row">
            <span>{{ formatValue(context[field.key]) }}</span>
            <el-button
              type="primary"
              link
              @click="copyText(String(context[field.key]), field.label)"
            >
              <el-icon><DocumentCopy /></el-icon> 复制
            </el-button>
          </div>
          <span v-else>{{ formatValue(context[field.key]) }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions
        v-if="hasGroupValues(ENV_FIELDS)"
        :column="1"
        border
        size="small"
        class="context-block"
        title="环境信息"
      >
        <el-descriptions-item
          v-for="field in ENV_FIELDS"
          :key="field.key"
          v-show="hasValue(context[field.key])"
          :label="field.label"
        >
          <span class="mono-text">{{ formatValue(context[field.key]) }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions
        v-if="extraEntries.length > 0"
        :column="1"
        border
        size="small"
        class="context-block"
        title="其他信息"
      >
        <el-descriptions-item v-for="entry in extraEntries" :key="entry.key" :label="entry.label">
          <pre v-if="typeof entry.value === 'object'" class="json-block">{{ formatValue(entry.value) }}</pre>
          <span v-else>{{ formatValue(entry.value) }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </template>
  </el-card>
</template>

<style scoped lang="scss">
.plugin-context-panel {
  margin-bottom: 12px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  font-size: 14px;
  font-weight: 500;
}

.context-block + .context-block {
  margin-top: 16px;
}

.copy-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.mono-text {
  word-break: break-all;
  font-size: 13px;
}

.json-block {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  font-family: monospace;
}

:deep(.el-descriptions__title) {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

:deep(.el-descriptions__label) {
  width: 120px;
  background-color: #f5f7fa;
  font-weight: 500;
}
</style>
