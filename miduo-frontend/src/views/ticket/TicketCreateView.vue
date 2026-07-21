<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Document as DocumentOutlined, Plus } from '@element-plus/icons-vue'

import { getCategoryTree } from '@/api/category'
import { createTicket, uploadTicketImage } from '@/api/ticket'
import { getTemplateList } from '@/api/template'
import { getUserList } from '@/api/user'
import EmptyState from '@/components/common/EmptyState.vue'
import type { CategoryTreeOutput } from '@/types/category'
import type { TemplateFieldConfigItem, TemplateListOutput } from '@/types/template'
import type { TicketCreateInput } from '@/types/ticket'
import type { UserListOutput } from '@/types/user'
import { formatFileSize } from '@/utils/formatter'
import { notifySuccess, notifyWarning } from '@/utils/feedback'
import { useAuthStore } from '@/stores/auth'

interface PendingAttachmentItem {
  uid: string
  file: File
  previewUrl?: string
}

const ATTACHMENT_ACCEPT =
  'image/jpeg,image/jpg,image/png,image/gif,image/webp,image/bmp,.xls,.xlsx,.txt,.csv,.pdf,.doc,.docx,.zip,.rar,.7z,.mp4,.mov,.avi,.webm,.mkv,.wmv,.m4v'
const MAX_ATTACHMENT_SIZE = 200 * 1024 * 1024

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
const pendingAttachments = ref<PendingAttachmentItem[]>([])
const attachmentUploadRef = ref()

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

function isImageFile(fileType?: string): boolean {
  if (!fileType) {
    return false
  }
  return fileType.startsWith('image/')
}

function isVideoFile(fileType?: string): boolean {
  if (!fileType) {
    return false
  }
  return fileType.startsWith('video/')
}

function createPendingAttachment(file: File): PendingAttachmentItem {
  const item: PendingAttachmentItem = {
    uid: `${file.name}-${file.size}-${file.lastModified}-${Date.now()}`,
    file,
  }
  if (isImageFile(file.type) || isVideoFile(file.type)) {
    item.previewUrl = URL.createObjectURL(file)
  }
  return item
}

function handleAttachmentSelect(uploadFile: { raw?: File }): void {
  const file = uploadFile?.raw
  if (!file) {
    return
  }
  if (file.size > MAX_ATTACHMENT_SIZE) {
    notifyWarning(`文件「${file.name}」超过 200MB 上限，请压缩后重试`)
    return
  }
  const duplicated = pendingAttachments.value.some(
    (item) => item.file.name === file.name && item.file.size === file.size,
  )
  if (duplicated) {
    notifyWarning(`文件「${file.name}」已在待上传列表中`)
    return
  }
  pendingAttachments.value.push(createPendingAttachment(file))
  if (attachmentUploadRef.value) {
    ;(attachmentUploadRef.value as { clearFiles: () => void }).clearFiles()
  }
}

function removePendingAttachment(uid: string): void {
  const target = pendingAttachments.value.find((item) => item.uid === uid)
  if (target?.previewUrl) {
    URL.revokeObjectURL(target.previewUrl)
  }
  pendingAttachments.value = pendingAttachments.value.filter((item) => item.uid !== uid)
}

const pendingAttachmentImageUrls = computed(() => {
  return pendingAttachments.value
    .filter((item) => isImageFile(item.file.type) && item.previewUrl)
    .map((item) => item.previewUrl as string)
})

function getPendingAttachmentImageIndex(previewUrl?: string): number {
  if (!previewUrl) {
    return 0
  }
  const index = pendingAttachmentImageUrls.value.indexOf(previewUrl)
  return index >= 0 ? index : 0
}

async function uploadPendingAttachments(ticketId: number): Promise<string[]> {
  const failedFiles: string[] = []
  for (const item of pendingAttachments.value) {
    try {
      await uploadTicketImage(ticketId, item.file, 'attachment')
    } catch {
      failedFiles.push(item.file.name)
    }
  }
  return failedFiles
}

async function handleCreateTicket(): Promise<void> {
  if (!form.title || !form.categoryId || !form.priority) {
    notifyWarning('请先完善标题、分类和优先级')
    return
  }
  if (!String(form.description || '').trim()) {
    notifyWarning('请填写问题描述')
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
    const failedFiles = pendingAttachments.value.length
      ? await uploadPendingAttachments(ticketId)
      : []
    if (failedFiles.length) {
      notifyWarning(`工单已创建，但以下附件上传失败：${failedFiles.join('、')}`)
    } else {
      notifySuccess('工单创建成功')
    }
    await router.push(`/ticket/detail/${ticketId}`)
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadBaseData()
})

onBeforeUnmount(() => {
  pendingAttachments.value.forEach((item) => {
    if (item.previewUrl) {
      URL.revokeObjectURL(item.previewUrl)
    }
  })
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
      <el-form-item label="问题描述" required>
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="请输入问题描述"
        />
      </el-form-item>

      <el-form-item label="附件">
        <div class="attachment-section">
          <el-upload
            ref="attachmentUploadRef"
            class="attachment-upload-dropzone"
            drag
            multiple
            :show-file-list="false"
            :accept="ATTACHMENT_ACCEPT"
            :on-change="handleAttachmentSelect"
            :auto-upload="false"
          >
            <div class="upload-drag-content">
              <el-icon class="upload-drag-icon"><Plus /></el-icon>
              <div class="upload-drag-text">将文件拖到此处，或点击选择附件</div>
              <div class="upload-drag-subtext">
                支持图片、Excel、文本、PDF、Word、压缩包、视频等，单文件最大 200MB
              </div>
            </div>
          </el-upload>
          <EmptyState
            v-if="!pendingAttachments.length"
            description="暂无附件，可拖拽或点击上方区域添加文件"
          />
          <div v-else class="attachment-list">
            <div
              v-for="attachment in pendingAttachments"
              :key="attachment.uid"
              class="attachment-item"
            >
              <div class="attachment-preview">
                <el-image
                  v-if="isImageFile(attachment.file.type) && attachment.previewUrl"
                  :src="attachment.previewUrl"
                  :preview-src-list="pendingAttachmentImageUrls"
                  :initial-index="getPendingAttachmentImageIndex(attachment.previewUrl)"
                  fit="cover"
                  class="attachment-thumbnail"
                  preview-teleported
                />
                <video
                  v-else-if="isVideoFile(attachment.file.type) && attachment.previewUrl"
                  :src="attachment.previewUrl"
                  controls
                  class="attachment-video-thumbnail"
                  preload="metadata"
                />
                <el-icon v-else class="attachment-icon"><DocumentOutlined /></el-icon>
              </div>
              <div class="attachment-info">
                <div class="attachment-name" :title="attachment.file.name">
                  {{ attachment.file.name }}
                </div>
                <div class="attachment-meta">{{ formatFileSize(attachment.file.size) }}</div>
              </div>
              <el-button type="danger" link @click="removePendingAttachment(attachment.uid)">
                移除
              </el-button>
            </div>
          </div>
        </div>
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

.attachment-section {
  width: 100%;
}

.attachment-upload-dropzone {
  width: 100%;
  margin-bottom: 12px;

  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100%;
    padding: 24px 16px;
    border-color: #dcdfe6;
    border-radius: 8px;

    &:hover {
      border-color: #1675d1;
    }
  }
}

.upload-drag-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.upload-drag-icon {
  font-size: 24px;
  color: #1675d1;
}

.upload-drag-text {
  font-size: 14px;
  color: #303133;
}

.upload-drag-subtext {
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
}

.attachment-preview {
  flex-shrink: 0;
  width: 60px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
}

.attachment-thumbnail {
  width: 60px;
  height: 60px;
}

.attachment-video-thumbnail {
  width: 120px;
  height: 68px;
  object-fit: cover;
  border-radius: 4px;
  background-color: #000;
}

.attachment-icon {
  font-size: 28px;
  color: #909399;
}

.attachment-info {
  flex: 1;
  min-width: 0;
}

.attachment-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

@media (max-width: 768px) {
  .create-form {
    max-width: 100%;
  }
}
</style>
