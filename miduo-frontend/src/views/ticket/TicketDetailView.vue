<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'

import { getUserList } from '@/api/user'
import {
  assignTicket,
  closeTicket,
  followTicket,
  getTicketDetail,
  processTicket,
  unfollowTicket,
} from '@/api/ticket'
import EmptyState from '@/components/common/EmptyState.vue'
import type { TicketDetailOutput } from '@/types/ticket'
import type { UserListOutput } from '@/types/user'
import { notifySuccess } from '@/utils/feedback'
import { formatDateTime, formatFileSize } from '@/utils/formatter'

const route = useRoute()

const loading = ref(false)
const detail = ref<TicketDetailOutput>()
const users = ref<UserListOutput[]>([])

const assignDialogVisible = ref(false)
const processDialogVisible = ref(false)
const closeDialogVisible = ref(false)
const submitLoading = ref(false)

const assignForm = reactive({
  assigneeId: undefined as number | undefined,
  remark: '',
})

const processForm = reactive({
  targetStatus: 'processing',
  targetUserId: undefined as number | undefined,
  remark: '',
})

const closeForm = reactive({
  remark: '',
})

const ticketId = computed(() => Number(route.params.id))

const customFieldEntries = computed(() => {
  if (!detail.value?.customFields) {
    return []
  }
  return Object.entries(detail.value.customFields).map(([key, value]) => ({ key, value }))
})

async function loadDetail(): Promise<void> {
  if (!ticketId.value) {
    return
  }
  loading.value = true
  try {
    const [ticketDetail, userList] = await Promise.all([
      getTicketDetail(ticketId.value),
      getUserList({}),
    ])
    detail.value = ticketDetail
    users.value = userList
  } finally {
    loading.value = false
  }
}

async function handleAssign(): Promise<void> {
  if (!assignForm.assigneeId) {
    return
  }
  submitLoading.value = true
  try {
    await assignTicket(ticketId.value, {
      assigneeId: assignForm.assigneeId!,
      remark: assignForm.remark,
    })
    notifySuccess('工单分派成功')
    assignDialogVisible.value = false
    await loadDetail()
  } finally {
    submitLoading.value = false
  }
}

async function handleProcess(): Promise<void> {
  submitLoading.value = true
  try {
    await processTicket(ticketId.value, processForm)
    notifySuccess('工单处理成功')
    processDialogVisible.value = false
    await loadDetail()
  } finally {
    submitLoading.value = false
  }
}

async function handleCloseTicket(): Promise<void> {
  submitLoading.value = true
  try {
    await closeTicket(ticketId.value, closeForm)
    notifySuccess('工单关闭成功')
    closeDialogVisible.value = false
    await loadDetail()
  } finally {
    submitLoading.value = false
  }
}

async function toggleFollow(): Promise<void> {
  if (!detail.value) {
    return
  }
  if (detail.value.isFollowed) {
    await unfollowTicket(ticketId.value)
    notifySuccess('已取消关注')
  } else {
    await followTicket(ticketId.value)
    notifySuccess('关注成功')
  }
  await loadDetail()
}

onMounted(() => {
  loadDetail()
})
</script>

<template>
  <el-space direction="vertical" fill :size="16">
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="detail-header">
          <div>
            <div class="ticket-title">{{ detail?.title || '工单详情' }}</div>
            <div class="ticket-no">工单编号：{{ detail?.ticketNo || '-' }}</div>
          </div>
          <el-space>
            <el-button type="primary" @click="processDialogVisible = true">处理</el-button>
            <el-button type="warning" @click="assignDialogVisible = true">转派</el-button>
            <el-button type="danger" @click="closeDialogVisible = true">关闭</el-button>
            <el-button @click="toggleFollow">{{
              detail?.isFollowed ? '取消关注' : '关注工单'
            }}</el-button>
          </el-space>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="分类">{{ detail?.categoryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{
          detail?.statusLabel || detail?.status || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="优先级">
          {{ detail?.priorityLabel || detail?.priority || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="处理人">{{
          detail?.assigneeName || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ detail?.creatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{
          detail?.sourceLabel || detail?.source || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{
          formatDateTime(detail?.createTime)
        }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{
          formatDateTime(detail?.updateTime)
        }}</el-descriptions-item>
        <el-descriptions-item label="期望完成时间">
          {{ formatDateTime(detail?.expectedTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="模板">{{ detail?.templateName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{
          detail?.description || '-'
        }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="section-title">自定义字段</div>
      </template>
      <EmptyState v-if="customFieldEntries.length === 0" description="暂无自定义字段" />
      <el-table v-else :data="customFieldEntries" border>
        <el-table-column prop="key" label="字段名" />
        <el-table-column prop="value" label="字段值" />
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="section-title">附件</div>
      </template>
      <EmptyState v-if="!detail?.attachments?.length" description="暂无附件" />
      <el-table v-else :data="detail.attachments" border>
        <el-table-column prop="fileName" label="文件名" min-width="220" />
        <el-table-column prop="fileType" label="类型" width="120" />
        <el-table-column label="大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="uploadedByName" label="上传人" width="120" />
        <el-table-column label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="section-title">评论与处理记录</div>
      </template>
      <EmptyState v-if="!detail?.comments?.length" description="暂无评论记录" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="comment in detail.comments"
          :key="comment.id"
          :timestamp="formatDateTime(comment.createTime)"
        >
          <div class="comment-item">
            <div class="comment-title">{{ comment.userName || '-' }}</div>
            <div class="comment-content">{{ comment.content || '-' }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </el-space>

  <el-dialog v-model="assignDialogVisible" title="分派工单" width="480px">
    <el-form label-width="90px">
      <el-form-item label="处理人" required>
        <el-select v-model="assignForm.assigneeId" placeholder="请选择处理人">
          <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="assignForm.remark" type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="assignDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleAssign">确认分派</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="processDialogVisible" title="处理工单" width="520px">
    <el-form label-width="90px">
      <el-form-item label="目标状态" required>
        <el-select v-model="processForm.targetStatus">
          <el-option label="处理中" value="processing" />
          <el-option label="待验收" value="pending_accept" />
          <el-option label="已完成" value="resolved" />
          <el-option label="挂起" value="suspended" />
        </el-select>
      </el-form-item>
      <el-form-item label="目标处理人">
        <el-select v-model="processForm.targetUserId" clearable placeholder="可选">
          <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="processForm.remark" type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="processDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleProcess">确认处理</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="closeDialogVisible" title="关闭工单" width="480px">
    <el-form label-width="90px">
      <el-form-item label="关闭原因">
        <el-input v-model="closeForm.remark" type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleCloseTicket"
        >确认关闭</el-button
      >
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.ticket-title {
  font-size: 18px;
  font-weight: 600;
}

.ticket-no {
  color: #909399;
  margin-top: 6px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

.comment-item {
  background: #f8fafc;
  border-radius: 6px;
  padding: 8px 12px;
}

.comment-title {
  font-weight: 500;
}

.comment-content {
  margin-top: 4px;
  color: #606266;
}
</style>
