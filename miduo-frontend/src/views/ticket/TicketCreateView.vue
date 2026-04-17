<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { getCategoryTree } from '@/api/category'
import { createTicket } from '@/api/ticket'
import { getTemplateList } from '@/api/template'
import { getUserList } from '@/api/user'
import type { CategoryTreeOutput } from '@/types/category'
import type { TemplateFieldConfigItem, TemplateListOutput } from '@/types/template'
import type { TicketCreateInput } from '@/types/ticket'
import type { UserListOutput } from '@/types/user'
import { notifySuccess, notifyWarning } from '@/utils/feedback'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

/** 外部游客不允许指定处理人 */
const isGuest = computed(() => authStore.isGuest)

const loading = ref(false)
const submitLoading = ref(false)
const categoryTree = ref<CategoryTreeOutput[]>([])
const templates = ref<TemplateListOutput[]>([])
const users = ref<UserListOutput[]>([])
const templateId = ref<number>()
const dynamicFields = ref<TemplateFieldConfigItem[]>([])
const customFields = reactive<Record<string, string>>({})

const form = reactive({
  title: '',
  description: '',
  categoryId: undefined as number | undefined,
  priority: 'medium',
  expectedTime: '',
  assigneeId: undefined as number | undefined,
  source: 'web',
})

async function loadBaseData(): Promise<void> {
  loading.value = true
  try {
    if (isGuest.value) {
      const tree = await getCategoryTree()
      categoryTree.value = tree
    } else {
      const [tree, userList] = await Promise.all([getCategoryTree(), getUserList({})])
      categoryTree.value = tree
      users.value = userList
    }
  } finally {
    loading.value = false
  }
}

function parseTemplateFields(fieldsConfig?: string): TemplateFieldConfigItem[] {
  if (!fieldsConfig) {
    return []
  }
  try {
    const parsed = JSON.parse(fieldsConfig)
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed
      .filter((item) => item?.key && item?.label)
      .map((item) => ({
        key: String(item.key),
        label: String(item.label),
        type: item.type || 'input',
        placeholder: item.placeholder || `请输入${item.label}`,
        required: Boolean(item.required),
        options: Array.isArray(item.options)
          ? item.options.map((option: { label?: string; value?: string }) => ({
              label: String(option.label || option.value || ''),
              value: String(option.value || option.label || ''),
            }))
          : undefined,
      }))
  } catch {
    return []
  }
}

watch(
  () => form.categoryId,
  async (categoryId) => {
    templateId.value = undefined
    dynamicFields.value = []
    Object.keys(customFields).forEach((key) => {
      delete customFields[key]
    })
    if (!categoryId) {
      templates.value = []
      return
    }
    templates.value = await getTemplateList(categoryId)
  },
)

watch(templateId, (value) => {
  Object.keys(customFields).forEach((key) => {
    delete customFields[key]
  })
  const currentTemplate = templates.value.find((item) => item.id === value)
  dynamicFields.value = parseTemplateFields(currentTemplate?.fieldsConfig)
  dynamicFields.value.forEach((field) => {
    customFields[field.key] = ''
  })
})

async function handleCreateTicket(): Promise<void> {
  if (!form.title || !form.categoryId || !form.priority) {
    notifyWarning('请先完善标题、分类和优先级')
    return
  }
  const missingRequiredField = dynamicFields.value.find(
    (field) => field.required && !String(customFields[field.key] || '').trim(),
  )
  if (missingRequiredField) {
    notifyWarning(`请填写必填字段：${missingRequiredField.label}`)
    return
  }

  const payload: TicketCreateInput = {
    title: form.title,
    description: form.description,
    categoryId: form.categoryId,
    priority: form.priority,
    expectedTime: form.expectedTime || undefined,
    assigneeId: form.assigneeId,
    source: form.source,
    customFields: { ...customFields },
  }

  submitLoading.value = true
  try {
    const ticketId = await createTicket(payload)
    notifySuccess('工单创建成功')
    await router.push(`/ticket/detail/${ticketId}`)
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadBaseData()
})
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <el-form label-width="120px" class="create-form">
      <div class="form-section-title">基本信息</div>
      <el-form-item label="工单标题" required>
        <el-input
          v-model="form.title"
          placeholder="请输入工单标题"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="工单分类" required>
        <el-tree-select
          v-model="form.categoryId"
          :data="categoryTree"
          :props="{ value: 'id', label: 'name', children: 'children' }"
          node-key="id"
          placeholder="请选择分类"
          check-strictly
          clearable
          filterable
          class="w-420"
        />
      </el-form-item>
      <el-form-item label="工单模板">
        <el-select v-model="templateId" placeholder="请选择模板（可选）" clearable class="w-420">
          <el-option
            v-for="template in templates"
            :key="template.id"
            :label="template.name"
            :value="template.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="优先级" required>
        <el-radio-group v-model="form.priority">
          <el-radio value="urgent">紧急</el-radio>
          <el-radio value="high">高</el-radio>
          <el-radio value="medium">中</el-radio>
          <el-radio value="low">低</el-radio>
        </el-radio-group>
      </el-form-item>

      <div class="form-section-title">分派与时间</div>
      <el-form-item v-if="!isGuest" label="处理人">
        <el-select
          v-model="form.assigneeId"
          placeholder="请选择处理人"
          clearable
          filterable
          class="w-420"
        >
          <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="期望完成时间">
        <el-date-picker
          v-model="form.expectedTime"
          type="datetime"
          placeholder="请选择期望完成时间"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item label="工单来源">
        <el-select v-model="form.source" class="w-420" placeholder="请选择">
          <el-option label="Web" value="web" />
          <el-option label="企业微信" value="wecom" />
          <el-option label="邮件" value="email" />
          <el-option label="电话" value="phone" />
        </el-select>
      </el-form-item>

      <div class="form-section-title">问题详情</div>
      <el-form-item label="问题描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="请输入问题描述"
        />
      </el-form-item>

      <template v-if="dynamicFields.length">
        <el-divider content-position="left">模板字段</el-divider>
        <el-form-item
          v-for="field in dynamicFields"
          :key="field.key"
          :label="field.label"
          :required="field.required"
        >
          <el-select
            v-if="field.type === 'select'"
            v-model="customFields[field.key]"
            :placeholder="field.placeholder"
            clearable
            class="w-420"
          >
            <el-option
              v-for="option in field.options || []"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-input
            v-else-if="field.type === 'textarea'"
            v-model="customFields[field.key]"
            type="textarea"
            :rows="3"
            :placeholder="field.placeholder"
          />
          <el-date-picker
            v-else-if="field.type === 'date'"
            v-model="customFields[field.key]"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="field.placeholder"
          />
          <el-input
            v-else
            v-model="customFields[field.key]"
            :placeholder="field.placeholder"
            class="w-420"
          />
        </el-form-item>
      </template>

      <el-form-item>
        <el-space>
          <el-button type="primary" :loading="submitLoading" @click="handleCreateTicket"
            >提交工单</el-button
          >
          <el-button @click="router.push('/ticket/mine')">取消</el-button>
        </el-space>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style scoped lang="scss">
.create-form {
  max-width: 860px;
  padding-top: 8px;

  :deep(.el-form-item) {
    margin-bottom: 20px;
  }

  :deep(.el-form-item__label) {
    font-size: 14px;
    font-weight: 500;
    color: #4b5563;
  }

  :deep(.el-divider__text) {
    font-size: 14px;
    font-weight: 600;
    color: #1675d1;
  }
}

.form-section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--md-text-primary, #1d2129);
  padding: 4px 0 12px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--md-border-light, #eef2f7);

  &:not(:first-child) {
    margin-top: 8px;
  }
}

.w-420 {
  max-width: 420px;
  width: 100%;
}

@media (max-width: 768px) {
  .create-form {
    max-width: 100%;
  }
}
</style>
