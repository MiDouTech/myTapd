<script setup lang="ts">
import { DocumentCopy } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  createIntegrationApp,
  getIntegrationAppDetail,
  getIntegrationAppPage,
  rotateIntegrationAppSecret,
  updateIntegrationApp,
} from '@/api/integration'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type {
  IntegrationAppCreateInput,
  IntegrationAppOutput,
  IntegrationAppPageInput,
  IntegrationAppUpdateInput,
} from '@/types/integration'
import { confirmAction, notifyError, notifySuccess } from '@/utils/feedback'
import { formatDateTime } from '@/utils/formatter'

type DialogMode = 'create' | 'edit'

const PERMISSION_OPTIONS = [
  { label: '签发 LaunchToken', value: 'plugin:launch-token' },
  { label: '插件建单', value: 'plugin:ticket:create' },
  { label: '查询我的工单', value: 'plugin:ticket:read-mine' },
  { label: '附件上传', value: 'plugin:attachment:upload' },
  { label: '工单导出', value: 'open:ticket:export' },
] as const

const tableLoading = ref(false)
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const submitLoading = ref(false)
const rotateLoading = ref(false)

const dialogMode = ref<DialogMode>('create')
const editingId = ref<number>()
const formRef = ref<FormInstance>()

const credentialVisible = ref(false)
const credentialAppKey = ref('')
const credentialAppSecret = ref('')

const detailAppKey = ref('')
const detailAppSecret = ref('')

const query = reactive<IntegrationAppPageInput>({
  keyword: '',
  status: undefined,
  pageNum: 1,
  pageSize: 20,
})

const tableData = ref<IntegrationAppOutput[]>([])
const total = ref(0)

const form = reactive({
  appName: '',
  systemCode: '',
  defaultCategoryId: undefined as number | undefined,
  categoryMappingText: '',
  callbackUrl: '',
  callbackSecret: '',
  allowedOrigins: '',
  permissionCodes: [] as string[],
  status: 1,
})

const formRules: FormRules = {
  appName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
  systemCode: [{ required: true, message: '请输入系统标识', trigger: 'blur' }],
  defaultCategoryId: [{ required: true, message: '请输入默认分类 ID', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const isEditMode = computed(() => dialogMode.value === 'edit')

function resetForm(): void {
  form.appName = ''
  form.systemCode = ''
  form.defaultCategoryId = undefined
  form.categoryMappingText = ''
  form.callbackUrl = ''
  form.callbackSecret = ''
  form.allowedOrigins = ''
  form.permissionCodes = PERMISSION_OPTIONS.slice(0, 3).map((item) => item.value)
  form.status = 1
  detailAppKey.value = ''
  detailAppSecret.value = ''
  editingId.value = undefined
}

function parseCategoryMapping(text: string): Record<string, number> | undefined {
  const trimmed = text.trim()
  if (!trimmed) {
    return undefined
  }
  let parsed: unknown
  try {
    parsed = JSON.parse(trimmed)
  } catch {
    throw new Error('分类映射必须是合法的 JSON 对象')
  }
  if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
    throw new Error('分类映射必须是 JSON 对象，如 {"channel": 12}')
  }
  const result: Record<string, number> = {}
  for (const [key, value] of Object.entries(parsed as Record<string, unknown>)) {
    const categoryId = Number(value)
    if (!key.trim() || !Number.isFinite(categoryId) || categoryId <= 0) {
      throw new Error('分类映射的值必须为正整数分类 ID')
    }
    result[key.trim()] = categoryId
  }
  return Object.keys(result).length > 0 ? result : undefined
}

function formatCategoryMapping(mapping?: Record<string, number>): string {
  if (!mapping || Object.keys(mapping).length === 0) {
    return ''
  }
  return JSON.stringify(mapping, null, 2)
}

function parsePermissions(codes: string[]): string | undefined {
  const normalized = codes.map((item) => item.trim()).filter(Boolean)
  return normalized.length > 0 ? normalized.join(',') : undefined
}

function parsePermissionCodes(permissions?: string): string[] {
  if (!permissions?.trim()) {
    return PERMISSION_OPTIONS.slice(0, 3).map((item) => item.value)
  }
  return permissions
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function getStatusTagType(status: number): 'success' | 'info' {
  return status === 1 ? 'success' : 'info'
}

async function copyCredential(text: string, label: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(text)
    notifySuccess(`${label}已复制`)
  } catch {
    notifyError('复制失败，请手动选择复制')
  }
}

function openCredentialDialog(appKey: string, appSecret: string): void {
  credentialAppKey.value = appKey
  credentialAppSecret.value = appSecret
  credentialVisible.value = true
}

function handlePaginationChange(payload: { pageNum: number; pageSize: number }): void {
  query.pageNum = payload.pageNum
  query.pageSize = payload.pageSize
  void loadApps()
}

function handleSearch(): void {
  query.pageNum = 1
  void loadApps()
}

function handleReset(): void {
  query.keyword = ''
  query.status = undefined
  query.pageNum = 1
  query.pageSize = 20
  void loadApps()
}

async function loadApps(): Promise<void> {
  tableLoading.value = true
  try {
    const pageResult = await getIntegrationAppPage({
      ...query,
      keyword: query.keyword?.trim() || undefined,
    })
    tableData.value = pageResult.records
    total.value = pageResult.total
  } catch {
    // 统一错误提示由请求拦截器处理
  } finally {
    tableLoading.value = false
  }
}

function openCreateDialog(): void {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEditDialog(row: IntegrationAppOutput): Promise<void> {
  dialogMode.value = 'edit'
  editingId.value = row.id
  dialogVisible.value = true
  dialogLoading.value = true
  try {
    const detail = await getIntegrationAppDetail(row.id)
    form.appName = detail.appName || ''
    form.systemCode = detail.systemCode || ''
    form.defaultCategoryId = detail.defaultCategoryId
    form.categoryMappingText = formatCategoryMapping(detail.categoryMapping)
    form.callbackUrl = detail.callbackUrl || ''
    form.callbackSecret = ''
    form.allowedOrigins = detail.allowedOrigins || ''
    form.permissionCodes = parsePermissionCodes(detail.permissions)
    form.status = detail.status
    detailAppKey.value = detail.appKey || ''
    detailAppSecret.value = detail.appSecret || ''
  } catch {
    dialogVisible.value = false
  } finally {
    dialogLoading.value = false
  }
}

async function handleRotateSecret(): Promise<void> {
  if (!editingId.value) {
    return
  }
  try {
    await confirmAction('确认轮换 AppSecret 吗？旧密钥将立即失效，请同步更新业务系统配置。')
  } catch {
    return
  }

  rotateLoading.value = true
  try {
    const result = await rotateIntegrationAppSecret(editingId.value)
    detailAppSecret.value = result.appSecret
    detailAppKey.value = result.appKey
    openCredentialDialog(result.appKey, result.appSecret)
    notifySuccess('AppSecret 轮换成功')
    await loadApps()
  } catch {
    // 保留当前表单
  } finally {
    rotateLoading.value = false
  }
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  let categoryMapping: Record<string, number> | undefined
  try {
    categoryMapping = parseCategoryMapping(form.categoryMappingText)
  } catch (error) {
    notifyError(error instanceof Error ? error.message : '分类映射格式不正确')
    return
  }

  submitLoading.value = true
  try {
    const permissions = parsePermissions(form.permissionCodes)

    if (dialogMode.value === 'create') {
      const payload: IntegrationAppCreateInput = {
        appName: form.appName.trim(),
        systemCode: form.systemCode.trim(),
        defaultCategoryId: Number(form.defaultCategoryId),
        categoryMapping,
        callbackUrl: form.callbackUrl.trim() || undefined,
        callbackSecret: form.callbackSecret.trim() || undefined,
        allowedOrigins: form.allowedOrigins.trim() || undefined,
        permissions,
      }
      const id = await createIntegrationApp(payload)
      const detail = await getIntegrationAppDetail(id)
      if (detail.appKey && detail.appSecret) {
        openCredentialDialog(detail.appKey, detail.appSecret)
      }
      notifySuccess('接入应用创建成功，请妥善保存 AppKey / AppSecret')
    } else if (editingId.value) {
      const payload: IntegrationAppUpdateInput = {
        appName: form.appName.trim(),
        defaultCategoryId: Number(form.defaultCategoryId),
        categoryMapping,
        callbackUrl: form.callbackUrl.trim() || undefined,
        callbackSecret: form.callbackSecret.trim() || undefined,
        allowedOrigins: form.allowedOrigins.trim() || undefined,
        permissions,
        status: form.status,
      }
      await updateIntegrationApp(editingId.value, payload)
      notifySuccess('接入应用更新成功')
    }

    dialogVisible.value = false
    await loadApps()
  } catch {
    // 提交失败保留表单内容
  } finally {
    submitLoading.value = false
  }
}

onMounted(async () => {
  await loadApps()
})
</script>

<template>
  <el-space direction="vertical" fill :size="12">
    <el-alert
      title="接入应用用于业务系统通过 ticket-sdk 提交工单。AppSecret 仅用于服务端，请勿写入前端代码。"
      type="warning"
      :closable="false"
      show-icon
    />

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">接入应用管理</span>
          <el-space>
            <el-button type="primary" @click="openCreateDialog">新建应用</el-button>
            <el-button @click="loadApps">刷新</el-button>
          </el-space>
        </div>
      </template>

      <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="应用名称 / 系统标识 / AppKey" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="请选择">
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

      <EmptyState v-if="!tableLoading && total === 0" description="暂无接入应用" />
      <template v-else>
        <BaseTable :data="tableData" :loading="tableLoading">
          <el-table-column prop="appName" label="应用名称" min-width="120" align="center" show-overflow-tooltip />
          <el-table-column prop="systemCode" label="系统标识" min-width="100" align="center" show-overflow-tooltip />
          <el-table-column prop="appKey" label="AppKey" min-width="160" align="center" show-overflow-tooltip />
          <el-table-column
            prop="defaultCategoryId"
            label="默认分类 ID"
            width="120"
            align="center"
          />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)" size="small">
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="160" align="center">
            <template #default="{ row }">
              {{ formatDateTime(row.updateTime || row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
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
    :title="isEditMode ? '编辑接入应用' : '新建接入应用'"
    width="720px"
  >
    <div v-loading="dialogLoading">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="130px">
        <el-form-item label="应用名称" prop="appName" required>
          <el-input v-model="form.appName" placeholder="如：米多星球" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="系统标识" prop="systemCode" :required="!isEditMode">
          <el-input
            v-model="form.systemCode"
            :disabled="isEditMode"
            placeholder="全局唯一，如 xingqiu"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="默认分类 ID" prop="defaultCategoryId" required>
          <el-input-number
            v-model="form.defaultCategoryId"
            :min="1"
            :step="1"
            controls-position="right"
            placeholder="工单分类 ID"
          />
        </el-form-item>
        <el-form-item label="分类映射">
          <el-input
            v-model="form.categoryMappingText"
            type="textarea"
            :rows="4"
            placeholder='可选，JSON 对象，如 {"channel": 12, "order": 15}'
          />
        </el-form-item>
        <el-form-item label="回调地址">
          <el-input v-model="form.callbackUrl" placeholder="状态变更 Webhook URL（http/https）" />
        </el-form-item>
        <el-form-item label="回调密钥">
          <el-input
            v-model="form.callbackSecret"
            type="password"
            show-password
            :placeholder="isEditMode ? '留空表示不修改' : '可选，用于回调签名校验'"
          />
        </el-form-item>
        <el-form-item label="允许域名">
          <el-input
            v-model="form.allowedOrigins"
            type="textarea"
            :rows="2"
            placeholder="SDK 来源白名单，多个域名用逗号分隔"
          />
        </el-form-item>
        <el-form-item label="开放权限">
          <el-checkbox-group v-model="form.permissionCodes">
            <el-checkbox v-for="option in PERMISSION_OPTIONS" :key="option.value" :value="option.value">
              {{ option.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item v-if="isEditMode" label="状态" prop="status" required>
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="isEditMode && detailAppKey">
          <el-divider content-position="left">凭证信息</el-divider>
          <el-form-item label="AppKey">
            <el-input :model-value="detailAppKey" readonly>
              <template #append>
                <el-button @click="copyCredential(detailAppKey, 'AppKey')">
                  <el-icon><DocumentCopy /></el-icon>
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="AppSecret">
            <el-input :model-value="detailAppSecret" readonly type="password" show-password>
              <template #append>
                <el-button @click="copyCredential(detailAppSecret, 'AppSecret')">
                  <el-icon><DocumentCopy /></el-icon>
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="warning" plain :loading="rotateLoading" @click="handleRotateSecret">
              轮换 AppSecret
            </el-button>
          </el-form-item>
        </template>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="credentialVisible" title="接入凭证" width="560px" @closed="credentialAppKey = ''">
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      title="请立即复制并妥善保存 AppSecret，关闭后将无法再次完整查看（可通过轮换重新生成）。"
      class="credential-tip"
    />
    <el-descriptions :column="1" border size="small" class="credential-desc">
      <el-descriptions-item label="AppKey">
        <div class="credential-row">
          <span class="credential-text">{{ credentialAppKey }}</span>
          <el-button type="primary" link @click="copyCredential(credentialAppKey, 'AppKey')">
            <el-icon><DocumentCopy /></el-icon> 复制
          </el-button>
        </div>
      </el-descriptions-item>
      <el-descriptions-item label="AppSecret">
        <div class="credential-row">
          <span class="credential-text">{{ credentialAppSecret }}</span>
          <el-button type="primary" link @click="copyCredential(credentialAppSecret, 'AppSecret')">
            <el-icon><DocumentCopy /></el-icon> 复制
          </el-button>
        </div>
      </el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button type="primary" @click="credentialVisible = false">我已保存</el-button>
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

.credential-tip {
  margin-bottom: 12px;
}

.credential-desc {
  margin-top: 8px;
}

.credential-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.credential-text {
  word-break: break-all;
  font-family: monospace;
  font-size: 13px;
}
</style>
