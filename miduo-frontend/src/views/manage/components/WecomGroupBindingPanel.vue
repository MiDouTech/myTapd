<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import { getCategoryTree } from '@/api/category'
import { getTemplateList } from '@/api/template'
import { createWecomGroupBinding, getWecomGroupBindingList, updateWecomGroupBinding } from '@/api/wecom'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { CategoryTreeOutput } from '@/types/category'
import type { TemplateListOutput } from '@/types/template'
import type { WecomGroupBindingListOutput } from '@/types/wecom'
import { notifySuccess, notifyWarning } from '@/utils/feedback'
import { formatDateTime } from '@/utils/formatter'

type DialogMode = 'create' | 'edit'
type SortOrder = 'ascending' | 'descending' | null
type WecomSortProp = keyof WecomGroupBindingListOutput | 'templateName' | ''

interface CategoryOption {
  label: string
  value: number
  templateId?: number
}

interface BindingFormState {
  chatId: string
  chatName: string
  defaultCategoryId?: number
  templateId?: number
  webhookUrl: string
  isActive: number
}

const tableLoading = ref(false)
const resourceLoading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const latestChangedId = ref<number>()
const editingId = ref<number>()
const dialogMode = ref<DialogMode>('create')
const formRef = ref<FormInstance>()

const query = reactive({
  keyword: '',
  categoryId: undefined as number | undefined,
  isActive: undefined as number | undefined,
  orderBy: '' as WecomSortProp,
  asc: false,
  pageNum: 1,
  pageSize: 20,
})

const categoryOptions = ref<CategoryOption[]>([])
const templateOptions = ref<TemplateListOutput[]>([])
const bindingList = ref<WecomGroupBindingListOutput[]>([])

const form = reactive<BindingFormState>({
  chatId: '',
  chatName: '',
  defaultCategoryId: undefined,
  templateId: undefined,
  webhookUrl: '',
  isActive: 1,
})

const formRules: FormRules<BindingFormState> = {
  chatId: [{ required: true, message: '请输入群ChatID', trigger: 'blur' }],
  defaultCategoryId: [{ required: true, message: '请选择默认分类', trigger: 'change' }],
  webhookUrl: [
    {
      validator: (_rule, value: string, callback) => {
        if (!value) {
          callback()
          return
        }
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
}

const categoryOptionMap = computed(() => {
  const map = new Map<number, CategoryOption>()
  categoryOptions.value.forEach((item) => {
    map.set(item.value, item)
  })
  return map
})

const templateNameMap = computed(() => {
  const map = new Map<number, string>()
  templateOptions.value.forEach((item) => {
    map.set(item.id, item.name)
  })
  return map
})

const filteredCategoryOptions = computed(() => {
  if (!form.templateId) {
    return categoryOptions.value
  }
  return categoryOptions.value.filter((item) => item.templateId === form.templateId)
})

const filteredBindingList = computed(() => {
  const keyword = query.keyword.trim().toLowerCase()
  return bindingList.value.filter((item) => {
    const keywordMatched =
      !keyword ||
      item.chatId.toLowerCase().includes(keyword) ||
      (item.chatName || '').toLowerCase().includes(keyword) ||
      (item.defaultCategoryName || '').toLowerCase().includes(keyword) ||
      getTemplateNameByCategory(item.defaultCategoryId).toLowerCase().includes(keyword)
    const categoryMatched = !query.categoryId || item.defaultCategoryId === query.categoryId
    const statusMatched = query.isActive === undefined || item.isActive === query.isActive
    return keywordMatched && categoryMatched && statusMatched
  })
})

const sortedBindingList = computed(() => {
  if (!query.orderBy) {
    return filteredBindingList.value
  }
  const result = [...filteredBindingList.value]
  const direction = query.asc ? 1 : -1
  const sortProp = query.orderBy as Exclude<WecomSortProp, ''>
  result.sort((left, right) => direction * compareByProp(left, right, sortProp))
  return result
})

const total = computed(() => sortedBindingList.value.length)

const pagedBindingList = computed(() => {
  const start = (query.pageNum - 1) * query.pageSize
  return sortedBindingList.value.slice(start, start + query.pageSize)
})

const latestChangedRecord = computed(
  () =>
    sortedBindingList.value.find((item) => item.id === latestChangedId.value) ??
    sortedBindingList.value[0] ??
    undefined,
)

const latestChangeTime = computed(() =>
  formatDateTime(latestChangedRecord.value?.updateTime || latestChangedRecord.value?.createTime),
)

const latestChangeBy = computed(
  () => latestChangedRecord.value?.updateBy || latestChangedRecord.value?.createBy || '系统',
)

function compareByProp(
  left: WecomGroupBindingListOutput,
  right: WecomGroupBindingListOutput,
  prop: Exclude<WecomSortProp, ''>,
): number {
  const leftValue = getSortValue(left, prop)
  const rightValue = getSortValue(right, prop)
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

function getSortValue(item: WecomGroupBindingListOutput, prop: Exclude<WecomSortProp, ''>): unknown {
  if (prop === 'templateName') {
    return getTemplateNameByCategory(item.defaultCategoryId)
  }
  return item[prop as keyof WecomGroupBindingListOutput]
}

function getTemplateIdByCategory(categoryId?: number): number | undefined {
  if (!categoryId) {
    return undefined
  }
  return categoryOptionMap.value.get(categoryId)?.templateId
}

function getTemplateNameByCategory(categoryId?: number): string {
  const templateId = getTemplateIdByCategory(categoryId)
  if (!templateId) {
    return '-'
  }
  return templateNameMap.value.get(templateId) || `模板#${templateId}`
}

function getCategoryName(categoryId?: number): string {
  if (!categoryId) {
    return '-'
  }
  return categoryOptionMap.value.get(categoryId)?.label || '-'
}

function getStatusTagType(isActive: number): 'success' | 'info' {
  return isActive === 1 ? 'success' : 'info'
}

function resetForm(): void {
  form.chatId = ''
  form.chatName = ''
  form.defaultCategoryId = undefined
  form.templateId = undefined
  form.webhookUrl = ''
  form.isActive = 1
  editingId.value = undefined
}

function normalizePage(): void {
  const maxPage = Math.max(1, Math.ceil(total.value / query.pageSize))
  if (query.pageNum > maxPage) {
    query.pageNum = maxPage
  }
}

function resolveRowClassName(payload: { row: Record<string, unknown> }): string {
  if (Number(payload.row.id) === latestChangedId.value) {
    return 'recently-changed-row'
  }
  return ''
}

function handleSortChange(payload: { prop: string; order: SortOrder }): void {
  query.orderBy = payload.order ? (payload.prop as Exclude<WecomSortProp, ''>) : ''
  query.asc = payload.order === 'ascending'
  normalizePage()
}

function handlePaginationChange(payload: { pageNum: number; pageSize: number }): void {
  query.pageNum = payload.pageNum
  query.pageSize = payload.pageSize
  normalizePage()
}

function handleSearch(): void {
  query.pageNum = 1
  normalizePage()
}

function handleReset(): void {
  query.keyword = ''
  query.categoryId = undefined
  query.isActive = undefined
  query.orderBy = ''
  query.asc = false
  query.pageNum = 1
  query.pageSize = 20
}

function handleCategoryChange(value?: number): void {
  if (!value) {
    form.templateId = undefined
    return
  }
  form.templateId = getTemplateIdByCategory(value)
}

function handleTemplateChange(value?: number): void {
  if (!value || !form.defaultCategoryId) {
    return
  }
  const categoryTemplateId = getTemplateIdByCategory(form.defaultCategoryId)
  if (categoryTemplateId !== value) {
    form.defaultCategoryId = undefined
  }
}

function isChatIdDuplicated(chatId: string): boolean {
  const normalizedChatId = chatId.trim().toLowerCase()
  if (!normalizedChatId) {
    return false
  }
  return bindingList.value.some(
    (item) => item.chatId.trim().toLowerCase() === normalizedChatId && item.id !== editingId.value,
  )
}

function flattenCategoryOptions(nodes: CategoryTreeOutput[], parentPath = ''): CategoryOption[] {
  const result: CategoryOption[] = []
  nodes.forEach((node) => {
    const label = parentPath ? `${parentPath} / ${node.name}` : node.name
    result.push({
      label,
      value: node.id,
      templateId: node.templateId,
    })
    if (node.children?.length) {
      result.push(...flattenCategoryOptions(node.children, label))
    }
  })
  return result
}

async function loadResourceOptions(): Promise<void> {
  resourceLoading.value = true
  try {
    const [categoryTree, templateList] = await Promise.all([getCategoryTree(), getTemplateList()])
    categoryOptions.value = flattenCategoryOptions(categoryTree)
    templateOptions.value = templateList.filter((item) => item.isActive !== 0)
  } catch {
    // 请求失败时保留已有选项，避免影响已打开弹窗中的编辑操作
  } finally {
    resourceLoading.value = false
  }
}

async function loadBindingList(): Promise<void> {
  tableLoading.value = true
  try {
    bindingList.value = await getWecomGroupBindingList()
    normalizePage()
  } catch {
    // 统一错误提示由请求拦截器处理，这里保留当前列表状态
  } finally {
    tableLoading.value = false
  }
}

function openCreateDialog(): void {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: WecomGroupBindingListOutput): void {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.chatId = row.chatId
  form.chatName = row.chatName || ''
  form.defaultCategoryId = row.defaultCategoryId
  form.templateId = getTemplateIdByCategory(row.defaultCategoryId)
  form.webhookUrl = row.webhookUrl || ''
  form.isActive = row.isActive
  dialogVisible.value = true
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  if (isChatIdDuplicated(form.chatId)) {
    notifyWarning('群ChatID已存在，请使用未绑定的群ID')
    return
  }

  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      const createdId = await createWecomGroupBinding({
        chatId: form.chatId.trim(),
        chatName: form.chatName.trim() || undefined,
        defaultCategoryId: form.defaultCategoryId,
        webhookUrl: form.webhookUrl.trim() || undefined,
        isActive: form.isActive,
      })
      latestChangedId.value = createdId
      notifySuccess('企微群绑定创建成功')
    } else if (editingId.value) {
      await updateWecomGroupBinding(editingId.value, {
        chatName: form.chatName.trim() || undefined,
        defaultCategoryId: form.defaultCategoryId,
        webhookUrl: form.webhookUrl.trim() || undefined,
        isActive: form.isActive,
      })
      latestChangedId.value = editingId.value
      notifySuccess('企微群绑定更新成功')
    }

    dialogVisible.value = false
    await loadBindingList()
  } catch {
    // 失败时保留表单内容，便于用户修正后重试
  } finally {
    submitLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadResourceOptions(), loadBindingList()])
})
</script>

<template>
  <el-space direction="vertical" fill :size="12">
    <el-alert
      title="风险提示：请确保群ID、默认分类配置准确；群机器人权限变更后需及时更新绑定关系。"
      type="warning"
      :closable="false"
      show-icon
    />

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">企微群绑定管理</span>
          <el-space>
            <el-button type="primary" @click="openCreateDialog">新增绑定</el-button>
            <el-button @click="loadBindingList">刷新</el-button>
          </el-space>
        </div>
      </template>

      <el-form :inline="true" class="filter-form">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="请输入群ID、群名称、分类或模板"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryId" clearable filterable placeholder="全部分类">
            <el-option
              v-for="option in categoryOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.isActive" clearable placeholder="全部状态">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-space>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-space>
        </el-form-item>
      </el-form>

      <div class="change-metadata">
        <span>最近变更时间：{{ latestChangeTime }}</span>
        <span>变更人：{{ latestChangeBy }}</span>
      </div>

      <EmptyState v-if="!tableLoading && total === 0" description="暂无企微群绑定配置" />
      <template v-else>
        <BaseTable
          :data="pagedBindingList"
          :loading="tableLoading"
          :row-class-name="resolveRowClassName"
          @sort-change="handleSortChange"
        >
          <el-table-column label="标记" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.id === latestChangedId" type="warning">最近变更</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="chatId" label="群ID" min-width="180" sortable="custom" />
          <el-table-column prop="chatName" label="群名称" min-width="160" sortable="custom">
            <template #default="{ row }">
              {{ row.chatName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="defaultCategoryName" label="默认分类" min-width="200" sortable="custom">
            <template #default="{ row }">
              {{ row.defaultCategoryName || getCategoryName(row.defaultCategoryId) }}
            </template>
          </el-table-column>
          <el-table-column prop="templateName" label="模板" min-width="160" sortable="custom">
            <template #default="{ row }">
              {{ getTemplateNameByCategory(row.defaultCategoryId) }}
            </template>
          </el-table-column>
          <el-table-column prop="webhookUrl" label="群Webhook" min-width="220">
            <template #default="{ row }">
              <el-link v-if="row.webhookUrl" type="primary" :href="row.webhookUrl" target="_blank">
                {{ row.webhookUrl }}
              </el-link>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="isActive" label="状态" width="100" sortable="custom">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.isActive)">
                {{ row.isActive === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="180" prop="updateTime" sortable="custom">
            <template #default="{ row }">
              {{ formatDateTime(row.updateTime || row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
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
    :title="dialogMode === 'create' ? '新增企微群绑定' : '编辑企微群绑定'"
    width="680px"
  >
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
      <el-form-item label="群ChatID" prop="chatId" required>
        <el-input
          v-model="form.chatId"
          :disabled="dialogMode === 'edit'"
          maxlength="100"
          show-word-limit
          placeholder="请输入企微群ChatID"
        />
      </el-form-item>
      <el-form-item label="群名称" prop="chatName">
        <el-input v-model="form.chatName" maxlength="200" show-word-limit placeholder="请输入群名称（选填）" />
      </el-form-item>
      <el-form-item label="模板" prop="templateId">
        <el-select
          v-model="form.templateId"
          clearable
          filterable
          :loading="resourceLoading"
          placeholder="请选择模板（用于联动分类）"
          @change="handleTemplateChange"
        >
          <el-option
            v-for="template in templateOptions"
            :key="template.id"
            :label="template.name"
            :value="template.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="默认分类" prop="defaultCategoryId" required>
        <el-select
          v-model="form.defaultCategoryId"
          clearable
          filterable
          :loading="resourceLoading"
          placeholder="请选择默认分类"
          @change="handleCategoryChange"
        >
          <el-option
            v-for="option in filteredCategoryOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.templateId && filteredCategoryOptions.length === 0">
        <span class="hint-text">当前模板下暂无可选分类，请切换模板或先维护分类模板关系。</span>
      </el-form-item>
      <el-form-item label="群Webhook地址" prop="webhookUrl">
        <el-input v-model="form.webhookUrl" placeholder="请输入群机器人Webhook地址（选填）" />
      </el-form-item>
      <el-form-item label="状态" prop="isActive" required>
        <el-radio-group v-model="form.isActive">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

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

.hint-text {
  color: #e6a23c;
  font-size: 13px;
}

:deep(.recently-changed-row > td.el-table__cell) {
  background-color: #f0f9ff !important;
}
</style>
