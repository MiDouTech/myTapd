<script setup lang="ts">
import { ArrowLeft, Delete, Plus, UploadFilled } from '@element-plus/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  createCustomField,
  deleteCustomField,
  getCustomFieldList,
  updateCustomField,
} from '@/api/customField'
import BasePagination from '@/components/common/BasePagination.vue'
import BaseTable from '@/components/common/BaseTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type {
  CustomFieldCreateInput,
  CustomFieldOption,
  CustomFieldOutput,
  CustomFieldType,
  CustomFieldUpdateInput,
} from '@/types/customField'
import { confirmAction, notifySuccess, notifyWarning } from '@/utils/feedback'
import { formatDateTime } from '@/utils/formatter'

type EditorMode = 'create' | 'edit'
type EditorStep = 'type' | 'config'

const fieldTypeOptions: Array<{
  label: string
  value: CustomFieldType
  description: string
}> = [
  { label: '单行文本', value: 'INPUT', description: '获取少量文本，如姓名、编号' },
  { label: '多行文本', value: 'TEXTAREA', description: '获取大量文本，如问题描述、处理方案' },
  { label: '日期', value: 'DATE', description: '选择日期，如 2026-08-16' },
  { label: '时间', value: 'TIME', description: '选择时间，如 16:00' },
  { label: '日期和时间', value: 'DATETIME', description: '选择完整日期和时间' },
  { label: '数值', value: 'NUMBER', description: '填写数值，支持整数和小数' },
  { label: '下拉列表', value: 'SELECT', description: '从多个选项中选择一个' },
  { label: '单选框', value: 'RADIO', description: '平铺展示选项并选择一个' },
  { label: '复选框', value: 'CHECKBOX', description: '从多个选项中选择多个' },
  { label: '级联', value: 'CASCADER', description: '选择多级关联选项' },
  { label: '附件', value: 'ATTACHMENT', description: '上传图片、视频或文件' },
]
const optionFieldTypes: CustomFieldType[] = ['SELECT', 'RADIO', 'CHECKBOX', 'CASCADER']
const lengthFieldTypes: CustomFieldType[] = ['INPUT', 'TEXTAREA']

const loading = ref(false)
const submitLoading = ref(false)
const switchLoadingId = ref<number>()
const drawerVisible = ref(false)
const editorMode = ref<EditorMode>('create')
const editorStep = ref<EditorStep>('type')
const fieldList = ref<CustomFieldOutput[]>([])

const query = reactive({
  keyword: '',
  fieldType: '' as CustomFieldType | '',
  isActive: undefined as number | undefined,
  pageNum: 1,
  pageSize: 20,
})

const form = reactive({
  id: undefined as number | undefined,
  fieldTitle: '',
  fieldType: 'INPUT' as CustomFieldType,
  description: '',
  placeholder: '',
  defaultRequired: 0,
  defaultValue: '',
  maxLength: 200,
  options: [] as CustomFieldOption[],
})

const isOptionField = computed(() => optionFieldTypes.includes(form.fieldType))
const isLengthField = computed(() => lengthFieldTypes.includes(form.fieldType))

const filteredFields = computed(() => {
  const keyword = query.keyword.trim().toLowerCase()
  return fieldList.value.filter((field) => {
    const keywordMatched =
      !keyword ||
      field.fieldTitle.toLowerCase().includes(keyword) ||
      field.fieldCode.toLowerCase().includes(keyword) ||
      (field.description || '').toLowerCase().includes(keyword)
    const typeMatched = !query.fieldType || field.fieldType === query.fieldType
    const statusMatched = query.isActive === undefined || field.isActive === query.isActive
    return keywordMatched && typeMatched && statusMatched
  })
})

const total = computed(() => filteredFields.value.length)
const pagedFields = computed(() => {
  const start = (query.pageNum - 1) * query.pageSize
  return filteredFields.value.slice(start, start + query.pageSize)
})

function resetForm(): void {
  form.id = undefined
  form.fieldTitle = ''
  form.fieldType = 'INPUT'
  form.description = ''
  form.placeholder = ''
  form.defaultRequired = 0
  form.defaultValue = ''
  form.maxLength = 200
  form.options = []
}

function normalizePage(): void {
  const maxPage = Math.max(1, Math.ceil(total.value / query.pageSize))
  if (query.pageNum > maxPage) query.pageNum = maxPage
}

async function loadFields(): Promise<void> {
  loading.value = true
  try {
    fieldList.value = await getCustomFieldList()
    normalizePage()
  } finally {
    loading.value = false
  }
}

function openCreateDrawer(): void {
  editorMode.value = 'create'
  editorStep.value = 'type'
  resetForm()
  drawerVisible.value = true
}

function openEditDrawer(row: CustomFieldOutput): void {
  editorMode.value = 'edit'
  editorStep.value = 'config'
  form.id = row.id
  form.fieldTitle = row.fieldTitle
  form.fieldType = row.fieldType
  form.description = row.description || ''
  form.placeholder = row.placeholder || ''
  form.defaultRequired = row.defaultRequired
  form.defaultValue = row.defaultValue || ''
  form.maxLength = row.maxLength || 200
  form.options = (row.options || []).map((option, index) => ({
    ...option,
    sortOrder: option.sortOrder ?? index + 1,
    enabled: option.enabled ?? 1,
  }))
  drawerVisible.value = true
}

function selectFieldType(type: CustomFieldType): void {
  form.fieldType = type
  form.options = optionFieldTypes.includes(type)
    ? [
        { label: '选项1', value: 'OPTION_1', sortOrder: 1, enabled: 1 },
        { label: '选项2', value: 'OPTION_2', sortOrder: 2, enabled: 1 },
      ]
    : []
  form.maxLength = type === 'TEXTAREA' ? 2000 : 200
  editorStep.value = 'config'
}

function addOption(): void {
  form.options.push({ label: '', value: '', sortOrder: form.options.length + 1, enabled: 1 })
}

function removeOption(index: number): void {
  form.options.splice(index, 1)
  form.options.forEach((option, optionIndex) => (option.sortOrder = optionIndex + 1))
}

function validateForm(): boolean {
  if (!form.fieldTitle.trim()) {
    notifyWarning('请输入字段标题')
    return false
  }
  if (isOptionField.value) {
    if (!form.options.length) {
      notifyWarning('请至少添加一个选项')
      return false
    }
    const values = new Set<string>()
    for (const option of form.options) {
      if (!option.label.trim() || !option.value.trim()) {
        notifyWarning('请完善选项名称和选项值')
        return false
      }
      if (!/^[A-Za-z][A-Za-z0-9_-]{0,99}$/.test(option.value)) {
        notifyWarning('选项值须以字母开头，且只能包含字母、数字、下划线和短横线')
        return false
      }
      if (values.has(option.value)) {
        notifyWarning(`选项值不能重复：${option.value}`)
        return false
      }
      values.add(option.value)
    }
  }
  return true
}

function buildBasePayload(): CustomFieldCreateInput {
  return {
    fieldTitle: form.fieldTitle.trim(),
    fieldType: form.fieldType,
    description: form.description.trim() || undefined,
    placeholder: form.placeholder.trim() || undefined,
    defaultRequired: form.defaultRequired,
    defaultValue: form.defaultValue.trim() || undefined,
    maxLength: isLengthField.value ? form.maxLength : undefined,
    options: isOptionField.value
      ? form.options.map((option, index) => ({
          label: option.label.trim(),
          value: option.value.trim(),
          sortOrder: index + 1,
          enabled: option.enabled ?? 1,
        }))
      : undefined,
  }
}

async function handleSubmit(): Promise<void> {
  if (!validateForm()) return
  submitLoading.value = true
  try {
    const payload = buildBasePayload()
    if (editorMode.value === 'create') {
      await createCustomField(payload)
      notifySuccess('自定义字段创建成功')
    } else {
      if (!form.id) return
      const updatePayload: CustomFieldUpdateInput = {
        id: form.id,
        fieldTitle: payload.fieldTitle,
        description: payload.description,
        placeholder: payload.placeholder,
        defaultRequired: payload.defaultRequired,
        defaultValue: payload.defaultValue,
        maxLength: payload.maxLength,
        options: payload.options,
      }
      await updateCustomField(updatePayload)
      notifySuccess('自定义字段更新成功')
    }
    drawerVisible.value = false
    await loadFields()
  } finally {
    submitLoading.value = false
  }
}

async function handleSwitch(
  row: CustomFieldOutput,
  value: string | number | boolean,
): Promise<void> {
  const isActive = Number(value)
  if (isActive === row.isActive) return
  switchLoadingId.value = row.id
  try {
    await updateCustomField({
      id: row.id,
      fieldTitle: row.fieldTitle,
      defaultRequired: row.defaultRequired,
      isActive,
    })
    row.isActive = isActive
    notifySuccess(isActive ? '字段已启用' : '字段已停用')
  } finally {
    switchLoadingId.value = undefined
  }
}

async function handleDelete(row: CustomFieldOutput): Promise<void> {
  await confirmAction(`确认删除自定义字段“${row.fieldTitle}”吗？删除后不可恢复。`, '删除字段')
  await deleteCustomField(row.id)
  notifySuccess('自定义字段已删除')
  await loadFields()
}

function handleReset(): void {
  query.keyword = ''
  query.fieldType = ''
  query.isActive = undefined
  query.pageNum = 1
}

function handlePaginationChange(payload: { pageNum: number; pageSize: number }): void {
  query.pageNum = payload.pageNum
  query.pageSize = payload.pageSize
  normalizePage()
}

function tagType(type: CustomFieldType): 'primary' | 'success' | 'warning' | 'info' {
  if (type === 'SELECT' || type === 'RADIO' || type === 'CHECKBOX') return 'success'
  if (type === 'ATTACHMENT') return 'warning'
  if (type === 'DATE' || type === 'TIME' || type === 'DATETIME') return 'primary'
  return 'info'
}

onMounted(loadFields)
</script>

<template>
  <div class="custom-field-page">
    <el-card shadow="never" class="page-card">
      <div class="page-header">
        <div>
          <div class="page-title">自定义字段</div>
          <div class="page-subtitle">集中维护可复用的工单字段，后续可添加到不同工单模板中。</div>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreateDrawer">添加字段</el-button>
      </div>

      <el-form :inline="true" class="query-form" @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="字段标题 / 编码 / 描述"
            @keyup.enter="query.pageNum = 1"
          />
        </el-form-item>
        <el-form-item label="字段类型">
          <el-select v-model="query.fieldType" clearable placeholder="全部类型">
            <el-option
              v-for="item in fieldTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
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
          <el-button type="primary" @click="query.pageNum = 1">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button link @click="loadFields">刷新</el-button>
        </el-form-item>
      </el-form>

      <EmptyState v-if="!loading && total === 0" description="暂无自定义字段，点击右上角添加字段" />
      <template v-else>
        <BaseTable :data="pagedFields" :loading="loading">
          <el-table-column prop="fieldTitle" label="字段标题" min-width="150" />
          <el-table-column prop="fieldCode" label="字段编码" min-width="190">
            <template #default="{ row }"
              ><code class="field-code">{{ row.fieldCode }}</code></template
            >
          </el-table-column>
          <el-table-column label="字段类型" width="130">
            <template #default="{ row }">
              <el-tag :type="tagType(row.fieldType)" effect="plain">{{
                row.fieldTypeLabel || row.fieldType
              }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="默认必填" width="100" align="center">
            <template #default="{ row }">{{ row.defaultRequired ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column prop="referenceCount" label="模板引用" width="100" align="center" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-switch
                :model-value="row.isActive"
                :active-value="1"
                :inactive-value="0"
                :loading="switchLoadingId === row.id"
                @change="handleSwitch(row, $event)"
              />
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="180">
            <template #default="{ row }">{{
              formatDateTime(row.updateTime || row.createTime)
            }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right" align="center">
            <template #default="{ row }">
              <el-button type="primary" link @click="openEditDrawer(row)">编辑</el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
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
  </div>

  <el-drawer
    v-model="drawerVisible"
    :title="
      editorMode === 'edit' ? '编辑自定义字段' : editorStep === 'type' ? '添加字段' : '配置字段'
    "
    :size="editorStep === 'type' ? '920px' : '680px'"
    destroy-on-close
    class="custom-field-drawer"
  >
    <div v-if="editorStep === 'type'" class="type-picker">
      <div class="type-picker-intro">
        <div class="type-picker-title">选择字段类型</div>
        <div class="type-picker-desc">通过模板示例预览填写效果，选择后再配置字段内容。</div>
      </div>
      <div class="type-table-header">
        <span>类型名称</span>
        <span>类型模板示例</span>
        <span>适用场景</span>
        <span>操作</span>
      </div>
      <div class="type-list">
        <div v-for="item in fieldTypeOptions" :key="item.value" class="type-row">
          <div class="type-name">{{ item.label }}</div>
          <div class="type-preview">
            <el-input v-if="item.value === 'INPUT'" disabled placeholder="请输入" />
            <el-input
              v-else-if="item.value === 'TEXTAREA'"
              disabled
              type="textarea"
              :rows="2"
              placeholder="请输入"
            />
            <el-date-picker
              v-else-if="item.value === 'DATE'"
              disabled
              type="date"
              placeholder="请选择日期"
            />
            <el-time-picker v-else-if="item.value === 'TIME'" disabled placeholder="请选择时间" />
            <el-date-picker
              v-else-if="item.value === 'DATETIME'"
              disabled
              type="datetime"
              placeholder="请选择日期和时间"
            />
            <el-input-number
              v-else-if="item.value === 'NUMBER'"
              disabled
              :controls="false"
              placeholder="请输入数值"
            />
            <el-select v-else-if="item.value === 'SELECT'" disabled placeholder="请选择">
              <el-option label="选项1" value="1" />
            </el-select>
            <el-radio-group v-else-if="item.value === 'RADIO'" disabled>
              <el-radio value="1">选项1</el-radio><el-radio value="2">选项2</el-radio>
            </el-radio-group>
            <el-checkbox-group v-else-if="item.value === 'CHECKBOX'" disabled>
              <el-checkbox value="1">选项1</el-checkbox><el-checkbox value="2">选项2</el-checkbox>
            </el-checkbox-group>
            <el-cascader v-else-if="item.value === 'CASCADER'" disabled placeholder="请选择" />
            <div v-else class="upload-preview">
              <el-icon><UploadFilled /></el-icon><span>点击或拖拽上传文件</span>
            </div>
          </div>
          <div class="type-description">{{ item.description }}</div>
          <div><el-button type="primary" @click="selectFieldType(item.value)">添加</el-button></div>
        </div>
      </div>
    </div>

    <div v-else class="field-editor">
      <button
        v-if="editorMode === 'create'"
        type="button"
        class="back-button"
        @click="editorStep = 'type'"
      >
        <el-icon><ArrowLeft /></el-icon><span>返回选择类型</span>
      </button>
      <div class="selected-type-card">
        <div>
          <span class="selected-type-label">当前字段类型</span>
          <strong>{{
            fieldTypeOptions.find((item) => item.value === form.fieldType)?.label
          }}</strong>
        </div>
        <span>字段编码将在保存时自动生成，无需手动填写</span>
      </div>
      <el-form label-position="top" class="field-form">
        <div class="form-section-title">基本信息</div>
        <el-form-item label="字段标题" required>
          <el-input
            v-model="form.fieldTitle"
            maxlength="100"
            show-word-limit
            placeholder="例如：问题还原"
          />
        </el-form-item>
        <el-form-item label="字段描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="说明字段用途，帮助使用者准确填写"
          />
        </el-form-item>
        <el-form-item label="占位提示">
          <el-input
            v-model="form.placeholder"
            maxlength="200"
            placeholder="用户填写字段时展示的提示"
          />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="默认必填">
            <el-switch v-model="form.defaultRequired" :active-value="1" :inactive-value="0" />
            <span class="switch-text">加入模板时默认设为必填</span>
          </el-form-item>
          <el-form-item v-if="isLengthField" label="最大长度">
            <el-input-number
              v-model="form.maxLength"
              :min="1"
              :max="10000"
              controls-position="right"
            />
          </el-form-item>
        </div>
        <el-form-item v-if="!isOptionField && form.fieldType !== 'ATTACHMENT'" label="默认值">
          <el-input v-model="form.defaultValue" placeholder="可选" />
        </el-form-item>

        <template v-if="isOptionField">
          <div class="option-header">
            <div>
              <div class="form-section-title">选项配置</div>
              <div class="field-tip">选项值用于流程条件和数据统计，建议使用稳定的英文编码。</div>
            </div>
            <el-button type="primary" link :icon="Plus" @click="addOption">添加选项</el-button>
          </div>
          <div class="option-column-labels">
            <span></span><span>选项名称</span><span>选项值</span><span></span>
          </div>
          <div class="option-list">
            <div v-for="(option, index) in form.options" :key="index" class="option-row">
              <span class="option-index">{{ index + 1 }}</span>
              <el-input v-model="option.label" placeholder="例如：通过" />
              <el-input v-model="option.value" placeholder="例如：APPROVED" />
              <el-button type="danger" link :icon="Delete" @click="removeOption(index)" />
            </div>
          </div>
        </template>
      </el-form>
    </div>
    <template #footer>
      <template v-if="editorStep === 'config'">
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </template>
  </el-drawer>
</template>

<style scoped lang="scss">
.custom-field-page,
.page-card {
  width: 100%;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-title {
  color: #1d2129;
  font-size: 18px;
  font-weight: 600;
  line-height: 26px;
}

.page-subtitle {
  margin-top: 4px;
  color: #86909c;
  font-size: 13px;
}

.query-form {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
  padding: 14px 16px 0;
  margin-bottom: 16px;
  background: var(--md-bg-panel, #f7f8fa);
  border-radius: 8px;

  :deep(.el-input),
  :deep(.el-select) {
    width: 210px;
  }
}

.field-code {
  padding: 3px 7px;
  color: #165dff;
  background: #eef4ff;
  border-radius: 4px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.field-form {
  padding: 20px 24px 32px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
}

.type-picker-intro {
  padding: 0 4px 18px;
}

.type-picker-title {
  color: #1d2129;
  font-size: 18px;
  font-weight: 600;
}

.type-picker-desc {
  margin-top: 6px;
  color: #86909c;
  font-size: 13px;
}

.type-table-header,
.type-row {
  display: grid;
  grid-template-columns: 120px minmax(260px, 1.4fr) minmax(210px, 1fr) 76px;
  align-items: center;
  gap: 20px;
}

.type-table-header {
  min-height: 48px;
  padding: 0 20px;
  color: #4e5969;
  background: #f2f3f5;
  border-radius: 8px 8px 0 0;
  font-size: 13px;
  font-weight: 600;
}

.type-list {
  border: 1px solid #e5e6eb;
  border-top: 0;
  border-radius: 0 0 8px 8px;
  overflow: hidden;
}

.type-row {
  min-height: 92px;
  padding: 14px 20px;
  background: #fff;
  border-bottom: 1px solid #f0f1f2;
  transition: background 0.2s ease;

  &:last-child {
    border-bottom: 0;
  }

  &:hover {
    background: #f7faff;
  }
}

.type-name {
  color: #1d2129;
  font-weight: 600;
}

.type-preview {
  min-width: 0;

  :deep(.el-input),
  :deep(.el-select),
  :deep(.el-date-editor),
  :deep(.el-input-number),
  :deep(.el-cascader) {
    width: 100%;
  }
}

.type-description {
  color: #4e5969;
  font-size: 13px;
  line-height: 20px;
}

.upload-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 58px;
  color: #86909c;
  background: #fafbfc;
  border: 1px dashed #c9cdd4;
  border-radius: 6px;

  .el-icon {
    color: #1677ff;
    font-size: 22px;
  }
}

.field-editor {
  max-width: 620px;
  margin: 0 auto;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 0;
  margin-bottom: 16px;
  color: #4e5969;
  background: transparent;
  border: 0;
  cursor: pointer;

  &:hover {
    color: #1677ff;
  }
}

.selected-type-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  margin-bottom: 16px;
  color: #86909c;
  background: #f2f7ff;
  border: 1px solid #d6e8ff;
  border-radius: 8px;
  font-size: 12px;

  strong {
    margin-left: 10px;
    color: #1677ff;
    font-size: 14px;
  }
}

.selected-type-label {
  color: #4e5969;
}

.form-section-title {
  margin-bottom: 14px;
  color: #1d2129;
  font-size: 15px;
  font-weight: 600;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.field-tip {
  margin-top: 5px;
  color: #86909c;
  font-size: 12px;
  line-height: 18px;
}

.switch-text {
  margin-left: 10px;
  color: #4e5969;
  font-size: 13px;
}

.option-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
}

.option-header .form-section-title {
  margin-bottom: 0;
}

.option-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 16px;
}

.option-column-labels {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) minmax(0, 1fr) 32px;
  gap: 10px;
  margin-top: 16px;
  color: #86909c;
  font-size: 12px;
}

.option-row {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) minmax(0, 1fr) 32px;
  align-items: center;
  gap: 10px;
}

.option-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: #4e5969;
  background: #f2f3f5;
  border-radius: 50%;
  font-size: 12px;
}

@media (max-width: 768px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .type-table-header {
    display: none;
  }

  .type-list {
    border-top: 1px solid #e5e6eb;
    border-radius: 8px;
  }

  .type-row {
    grid-template-columns: 1fr auto;
    gap: 10px;
  }

  .type-preview,
  .type-description {
    grid-column: 1 / -1;
  }

  .selected-type-card {
    align-items: flex-start;
    flex-direction: column;
  }

  .option-row {
    grid-template-columns: 24px 1fr 32px;
  }

  .option-row :deep(.el-input):nth-of-type(2) {
    grid-column: 2;
  }
}
</style>
