<script setup lang="ts">
import { ArrowDown, ArrowUp, Plus } from '@element-plus/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'

import { getCustomFieldList } from '@/api/customField'
import {
  createTemplate,
  deleteTemplate,
  getTemplateAdminList,
  getTemplateDetail,
  updateTemplate,
} from '@/api/template'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { CustomFieldOutput } from '@/types/customField'
import type {
  TemplateAdminListOutput,
  TemplateCustomFieldInput,
  TemplateFieldOutput,
} from '@/types/template'
import { confirmAction, notifySuccess, notifyWarning } from '@/utils/feedback'
import { formatDateTime } from '@/utils/formatter'

type EditorMode = 'create' | 'edit'

const loading = ref(false)
const editorLoading = ref(false)
const submitLoading = ref(false)
const editorVisible = ref(false)
const pickerVisible = ref(false)
const previewVisible = ref(false)
const editorMode = ref<EditorMode>('create')
const templates = ref<TemplateAdminListOutput[]>([])
const customFieldPool = ref<CustomFieldOutput[]>([])
const selectedPoolIds = ref<number[]>([])
const previewFields = ref<TemplateFieldOutput[]>([])
const previewName = ref('')

const query = reactive({
  keyword: '',
  isActive: undefined as number | undefined,
  pageNum: 1,
  pageSize: 20,
})

const form = reactive({
  id: undefined as number | undefined,
  name: '',
  description: '',
  isSystem: 0,
  isActive: 1,
  fields: [] as TemplateFieldOutput[],
})

const filteredTemplates = computed(() => {
  const keyword = query.keyword.trim().toLowerCase()
  return templates.value.filter((item) => {
    const keywordMatched =
      !keyword ||
      item.name.toLowerCase().includes(keyword) ||
      item.templateCode.toLowerCase().includes(keyword) ||
      (item.description || '').toLowerCase().includes(keyword)
    return keywordMatched && (query.isActive === undefined || item.isActive === query.isActive)
  })
})
const total = computed(() => filteredTemplates.value.length)
const pagedTemplates = computed(() => {
  const start = (query.pageNum - 1) * query.pageSize
  return filteredTemplates.value.slice(start, start + query.pageSize)
})
const customFields = computed(() => form.fields.filter((item) => item.fieldSource === 'CUSTOM'))
const availableCustomFields = computed(() => {
  const added = new Set(customFields.value.map((item) => item.customFieldId))
  return customFieldPool.value.filter((item) => item.isActive === 1 && !added.has(item.id))
})

function systemFields(): TemplateFieldOutput[] {
  return [
    {
      fieldSource: 'SYSTEM',
      fieldCode: 'title',
      fieldTitle: '标题',
      fieldType: 'INPUT',
      fieldTypeLabel: '单行文本',
      sortOrder: 1,
      isEnabled: 1,
      isRequired: 1,
      deletable: false,
      definitionActive: true,
    },
    {
      fieldSource: 'SYSTEM',
      fieldCode: 'description',
      fieldTitle: '问题描述',
      fieldType: 'TEXTAREA',
      fieldTypeLabel: '多行文本',
      sortOrder: 2,
      isEnabled: 1,
      isRequired: 1,
      deletable: false,
      definitionActive: true,
    },
  ]
}

function resetForm(): void {
  form.id = undefined
  form.name = ''
  form.description = ''
  form.isSystem = 0
  form.isActive = 1
  form.fields = systemFields()
}

async function loadTemplates(): Promise<void> {
  loading.value = true
  try {
    templates.value = await getTemplateAdminList()
  } finally {
    loading.value = false
  }
}

async function ensureCustomFields(): Promise<void> {
  if (!customFieldPool.value.length) customFieldPool.value = await getCustomFieldList()
}

async function openCreate(): Promise<void> {
  editorMode.value = 'create'
  resetForm()
  editorVisible.value = true
  await ensureCustomFields()
}

async function openEdit(row: TemplateAdminListOutput): Promise<void> {
  editorMode.value = 'edit'
  editorVisible.value = true
  editorLoading.value = true
  try {
    const [detail] = await Promise.all([getTemplateDetail(row.id), ensureCustomFields()])
    form.id = detail.id
    form.name = detail.name
    form.description = detail.description || ''
    form.isSystem = detail.isSystem
    form.isActive = detail.isActive
    form.fields = detail.fields.map((field) => ({ ...field }))
  } finally {
    editorLoading.value = false
  }
}

async function openPreview(row?: TemplateAdminListOutput): Promise<void> {
  if (row) {
    const detail = await getTemplateDetail(row.id)
    previewName.value = detail.name
    previewFields.value = detail.fields.filter((field) => field.isEnabled === 1)
  } else {
    previewName.value = form.name || '未命名模板'
    previewFields.value = form.fields.filter((field) => field.isEnabled === 1)
  }
  previewVisible.value = true
}

async function openPicker(): Promise<void> {
  await ensureCustomFields()
  selectedPoolIds.value = []
  pickerVisible.value = true
}

function addPickedFields(): void {
  const picked = customFieldPool.value.filter((item) => selectedPoolIds.value.includes(item.id))
  let order = form.fields.length + 1
  picked.forEach((field) => {
    form.fields.push({
      fieldSource: 'CUSTOM',
      customFieldId: field.id,
      fieldCode: field.fieldCode,
      fieldTitle: field.fieldTitle,
      fieldType: field.fieldType,
      fieldTypeLabel: field.fieldTypeLabel || field.fieldType,
      sortOrder: order++,
      isEnabled: 1,
      isRequired: field.defaultRequired,
      placeholder: field.placeholder,
      defaultValue: field.defaultValue,
      options: field.options,
      deletable: true,
      definitionActive: true,
    })
  })
  pickerVisible.value = false
}

function moveField(index: number, offset: number): void {
  if (index < 2) return
  const target = index + offset
  if (target < 2 || target >= form.fields.length) return
  const [field] = form.fields.splice(index, 1)
  if (!field) return
  form.fields.splice(target, 0, field)
  form.fields.forEach((item, itemIndex) => (item.sortOrder = itemIndex + 1))
}

function removeField(index: number): void {
  if (index < 2) return
  form.fields.splice(index, 1)
  form.fields.forEach((item, itemIndex) => (item.sortOrder = itemIndex + 1))
}

function toCustomFieldInputs(): TemplateCustomFieldInput[] {
  return form.fields
    .filter((field) => field.fieldSource === 'CUSTOM' && field.customFieldId)
    .map((field, index) => ({
      customFieldId: field.customFieldId as number,
      sortOrder: index + 3,
      isEnabled: field.isEnabled,
      isRequired: field.isRequired,
      placeholderOverride: field.placeholder || undefined,
      defaultValueOverride: field.defaultValue || undefined,
    }))
}

async function handleSubmit(): Promise<void> {
  if (!form.name.trim()) {
    notifyWarning('请输入模板名称')
    return
  }
  if (form.fields.some((field) => field.fieldSource === 'CUSTOM' && !field.definitionActive)) {
    notifyWarning('模板中存在已停用的字段，请先移除')
    return
  }
  submitLoading.value = true
  try {
    if (editorMode.value === 'create') {
      await createTemplate({
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        customFields: toCustomFieldInputs(),
      })
      notifySuccess('模板创建成功')
    } else if (form.id) {
      await updateTemplate({
        id: form.id,
        name: form.isSystem ? undefined : form.name.trim(),
        description: form.description.trim(),
        customFields: toCustomFieldInputs(),
      })
      notifySuccess('模板更新成功')
    }
    editorVisible.value = false
    await loadTemplates()
  } finally {
    submitLoading.value = false
  }
}

async function toggleStatus(row: TemplateAdminListOutput): Promise<void> {
  if (row.isSystem) return
  await updateTemplate({ id: row.id, isActive: row.isActive ? 0 : 1 })
  notifySuccess(row.isActive ? '模板已停用' : '模板已启用')
  await loadTemplates()
}

async function handleDelete(row: TemplateAdminListOutput): Promise<void> {
  await confirmAction(`确认删除模板“${row.name}”吗？`, '删除模板')
  await deleteTemplate(row.id)
  notifySuccess('模板已删除')
  await loadTemplates()
}

function previewComponent(field: TemplateFieldOutput): string {
  return field.fieldType
}

function handlePagination(payload: { pageNum: number; pageSize: number }): void {
  query.pageNum = payload.pageNum
  query.pageSize = payload.pageSize
}

function resetQuery(): void {
  query.keyword = ''
  query.isActive = undefined
  query.pageNum = 1
}

onMounted(loadTemplates)
</script>

<template>
  <div class="template-page">
    <el-card shadow="never">
      <div class="page-header">
        <div>
          <div class="page-title">模板设置</div>
          <div class="page-subtitle">配置工单的固定字段和可复用自定义字段。</div>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreate">添加模板</el-button>
      </div>
      <el-form :inline="true" class="query-form" @submit.prevent>
        <el-form-item label="关键字"
          ><el-input v-model="query.keyword" clearable placeholder="模板名称 / 编码 / 描述"
        /></el-form-item>
        <el-form-item label="状态"
          ><el-select v-model="query.isActive" clearable placeholder="全部状态"
            ><el-option label="启用" :value="1" /><el-option label="停用" :value="0" /></el-select
        ></el-form-item>
        <el-form-item
          ><el-button type="primary" @click="query.pageNum = 1">查询</el-button
          ><el-button @click="resetQuery">重置</el-button
          ><el-button link @click="loadTemplates">刷新</el-button></el-form-item
        >
      </el-form>
      <EmptyState v-if="!loading && total === 0" description="暂无模板" />
      <template v-else>
        <BaseTable :data="pagedTemplates" :loading="loading">
          <el-table-column label="模板名称" min-width="190"
            ><template #default="{ row }"
              ><span class="template-name">{{ row.name }}</span
              ><el-tag v-if="row.isSystem" size="small" type="warning" effect="plain"
                >系统默认</el-tag
              ></template
            ></el-table-column
          >
          <el-table-column
            prop="description"
            label="模板描述"
            min-width="250"
            show-overflow-tooltip
          />
          <el-table-column prop="fieldCount" label="字段数量" width="100" align="center" />
          <el-table-column label="状态" width="100"
            ><template #default="{ row }"
              ><el-tag :type="row.isActive ? 'success' : 'info'">{{
                row.isActive ? '启用' : '停用'
              }}</el-tag></template
            ></el-table-column
          >
          <el-table-column label="更新时间" width="180"
            ><template #default="{ row }">{{
              formatDateTime(row.updateTime || row.createTime)
            }}</template></el-table-column
          >
          <el-table-column label="操作" width="230" fixed="right"
            ><template #default="{ row }"
              ><el-button type="primary" link @click="openEdit(row)">编辑</el-button
              ><el-button type="primary" link @click="openPreview(row)">预览</el-button
              ><el-button
                v-if="!row.isSystem"
                :type="row.isActive ? 'warning' : 'success'"
                link
                @click="toggleStatus(row)"
                >{{ row.isActive ? '停用' : '启用' }}</el-button
              ><el-button v-if="!row.isSystem" type="danger" link @click="handleDelete(row)"
                >删除</el-button
              ></template
            ></el-table-column
          >
        </BaseTable>
        <BasePagination
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          @update="handlePagination"
        />
      </template>
    </el-card>
  </div>

  <el-drawer
    v-model="editorVisible"
    :title="editorMode === 'create' ? '添加模板' : '编辑模板'"
    size="900px"
    destroy-on-close
  >
    <div v-loading="editorLoading" class="editor-body">
      <el-alert
        v-if="form.isSystem"
        title="这是系统默认模板，不可删除或停用；标题和问题描述固定开启且必填。"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-form label-position="top" class="template-form">
        <div class="section-title">基本信息</div>
        <el-form-item label="模板名称" required
          ><el-input
            v-model="form.name"
            :disabled="Boolean(form.isSystem)"
            maxlength="100"
            show-word-limit
        /></el-form-item>
        <el-form-item label="模板描述"
          ><el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
        /></el-form-item>
      </el-form>
      <div class="field-section">
        <div class="field-section-header">
          <div>
            <div class="section-title">模板字段</div>
            <div class="section-tip">
              标题和问题描述为系统必填字段，自定义字段可调整顺序和模板内规则。
            </div>
          </div>
          <el-button type="primary" plain :icon="Plus" @click="openPicker">添加字段</el-button>
        </div>
        <BaseTable :data="form.fields">
          <el-table-column label="顺序" width="80"
            ><template #default="{ $index }">{{
              String($index + 1).padStart(2, '0')
            }}</template></el-table-column
          >
          <el-table-column prop="fieldTitle" label="字段名称" min-width="140"
            ><template #default="{ row }"
              >{{ row.fieldTitle }}
              <el-tag v-if="!row.definitionActive" type="danger" size="small"
                >已停用</el-tag
              ></template
            ></el-table-column
          >
          <el-table-column prop="fieldTypeLabel" label="字段类型" width="120" />
          <el-table-column label="状态" width="100"
            ><template #default="{ row }"
              ><el-switch
                v-model="row.isEnabled"
                :active-value="1"
                :inactive-value="0"
                :disabled="row.fieldSource === 'SYSTEM'" /></template
          ></el-table-column>
          <el-table-column label="是否必填" width="110"
            ><template #default="{ row }"
              ><el-switch
                v-model="row.isRequired"
                :active-value="1"
                :inactive-value="0"
                :disabled="row.fieldSource === 'SYSTEM'" /></template
          ></el-table-column>
          <el-table-column label="占位提示" min-width="180"
            ><template #default="{ row }"
              ><el-input
                v-model="row.placeholder"
                placeholder="可选"
                :disabled="row.fieldSource === 'SYSTEM'" /></template
          ></el-table-column>
          <el-table-column label="默认值" min-width="150"
            ><template #default="{ row }"
              ><el-input
                v-model="row.defaultValue"
                placeholder="可选"
                :disabled="row.fieldSource === 'SYSTEM'" /></template
          ></el-table-column>
          <el-table-column label="操作" width="160" fixed="right"
            ><template #default="{ $index, row }"
              ><template v-if="row.fieldSource === 'CUSTOM'"
                ><el-button
                  link
                  :icon="ArrowUp"
                  :disabled="$index <= 2"
                  @click="moveField($index, -1)"
                /><el-button
                  link
                  :icon="ArrowDown"
                  :disabled="$index >= form.fields.length - 1"
                  @click="moveField($index, 1)"
                /><el-button type="danger" link @click="removeField($index)"
                  >删除</el-button
                ></template
              ><span v-else class="locked-text">固定字段</span></template
            ></el-table-column
          >
        </BaseTable>
      </div>
    </div>
    <template #footer
      ><el-button @click="editorVisible = false">取消</el-button
      ><el-button @click="openPreview()">预览</el-button
      ><el-button type="primary" :loading="submitLoading" @click="handleSubmit"
        >保存</el-button
      ></template
    >
  </el-drawer>

  <el-dialog v-model="pickerVisible" title="添加自定义字段" width="720px">
    <div class="picker-tip">仅展示已启用且尚未加入当前模板的字段。</div>
    <el-checkbox-group v-model="selectedPoolIds" class="field-picker-list">
      <el-checkbox
        v-for="field in availableCustomFields"
        :key="field.id"
        :value="field.id"
        class="field-picker-item"
        ><span class="picker-name">{{ field.fieldTitle }}</span
        ><el-tag size="small" effect="plain">{{ field.fieldTypeLabel || field.fieldType }}</el-tag
        ><span class="picker-desc">{{ field.description || '暂无描述' }}</span></el-checkbox
      >
    </el-checkbox-group>
    <EmptyState v-if="availableCustomFields.length === 0" description="没有可添加的自定义字段" />
    <template #footer
      ><span class="selected-count">已选择 {{ selectedPoolIds.length }} 项</span
      ><el-button @click="pickerVisible = false">取消</el-button
      ><el-button type="primary" :disabled="!selectedPoolIds.length" @click="addPickedFields"
        >添加</el-button
      ></template
    >
  </el-dialog>

  <el-drawer v-model="previewVisible" title="模板预览" size="560px">
    <div class="preview-title">{{ previewName }}</div>
    <el-form label-position="top" class="preview-form">
      <el-form-item
        v-for="field in previewFields"
        :key="field.fieldCode"
        :label="field.fieldTitle"
        :required="Boolean(field.isRequired)"
      >
        <el-input
          v-if="previewComponent(field) === 'TEXTAREA'"
          type="textarea"
          :rows="3"
          :placeholder="field.placeholder || `请输入${field.fieldTitle}`"
          :model-value="field.defaultValue"
        />
        <el-select
          v-else-if="['SELECT', 'RADIO', 'CHECKBOX', 'CASCADER'].includes(previewComponent(field))"
          :placeholder="field.placeholder || '请选择'"
          style="width: 100%"
          ><el-option
            v-for="option in field.options || []"
            :key="option.value"
            :label="option.label"
            :value="option.value"
        /></el-select>
        <el-date-picker
          v-else-if="previewComponent(field) === 'DATE'"
          type="date"
          :placeholder="field.placeholder || '请选择日期'"
          style="width: 100%"
        />
        <el-upload
          v-else-if="previewComponent(field) === 'ATTACHMENT'"
          action="#"
          :auto-upload="false"
          ><el-button>选择附件</el-button></el-upload
        >
        <el-input
          v-else
          :placeholder="field.placeholder || `请输入${field.fieldTitle}`"
          :model-value="field.defaultValue"
        />
      </el-form-item>
    </el-form>
  </el-drawer>
</template>

<style scoped lang="scss">
.template-page {
  width: 100%;
}
.page-header,
.field-section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.page-title {
  color: #1d2129;
  font-size: 18px;
  font-weight: 600;
}
.page-subtitle,
.section-tip,
.picker-tip {
  margin-top: 5px;
  color: #86909c;
  font-size: 13px;
}
.query-form {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
  padding: 14px 16px 0;
  margin-bottom: 16px;
  background: #f7f8fa;
  border-radius: 8px;
}
.query-form :deep(.el-input),
.query-form :deep(.el-select) {
  width: 230px;
}
.template-name {
  margin-right: 8px;
  font-weight: 500;
}
.editor-body {
  min-height: 520px;
}
.template-form {
  padding: 20px;
  margin-top: 16px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
}
.section-title {
  color: #1d2129;
  font-size: 16px;
  font-weight: 600;
}
.template-form .section-title {
  margin-bottom: 16px;
}
.field-section {
  margin-top: 22px;
}
.locked-text {
  color: #86909c;
  font-size: 13px;
}
.field-picker-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 440px;
  margin-top: 16px;
  overflow: auto;
}
.field-picker-item {
  width: 100%;
  height: auto;
  padding: 14px;
  margin-right: 0;
  border: 1px solid #e5e6eb;
  border-radius: 7px;
}
.field-picker-item:hover {
  border-color: #409eff;
  background: #f5f9ff;
}
.picker-name {
  display: inline-block;
  min-width: 130px;
  color: #1d2129;
  font-weight: 500;
}
.picker-desc {
  margin-left: 12px;
  color: #86909c;
}
.selected-count {
  margin-right: auto;
  color: #86909c;
}
.preview-title {
  padding-bottom: 16px;
  margin-bottom: 20px;
  color: #1d2129;
  border-bottom: 1px solid #e5e6eb;
  font-size: 18px;
  font-weight: 600;
}
.preview-form {
  padding: 0 6px;
}
@media (max-width: 768px) {
  .page-header,
  .field-section-header {
    flex-direction: column;
  }
  .query-form :deep(.el-input),
  .query-form :deep(.el-select) {
    width: 100%;
  }
}
</style>
