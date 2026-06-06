<template>
  <div class="approval-panel">
    <!-- 无审批任务时的占位 -->
    <el-empty v-if="!loading && (!data || !data.nodes || data.nodes.length === 0)"
              description="暂无审批任务"
              :image-size="80" />

    <template v-else>
      <!-- 我的待审批操作区 -->
      <div v-if="data?.myPendingTaskId" class="approval-action-card">
        <div class="approval-action-card__header">
          <el-icon color="#e6a23c"><WarningFilled /></el-icon>
          <span>待您审批</span>
        </div>
        <div class="approval-action-card__body">
          <el-button type="success" @click="openActionDialog('approve')">
            <el-icon><Check /></el-icon>
            同意
          </el-button>
          <el-button type="danger" @click="openActionDialog('reject')">
            <el-icon><Close /></el-icon>
            驳回
          </el-button>
          <el-button @click="openActionDialog('transfer')">
            <el-icon><Right /></el-icon>
            转交
          </el-button>
        </div>
      </div>

      <!-- 审批节点列表 -->
      <div class="approval-nodes">
        <div v-for="node in data?.nodes" :key="node.nodeKey" class="approval-node">
          <div class="approval-node__header">
            <span class="approval-node__name">{{ node.nodeName }}</span>
            <el-tag size="small" type="info">{{ approveModeLabel(node.approveMode) }}</el-tag>
          </div>
          <div class="approval-node__tasks">
            <div v-for="task in node.tasks" :key="task.taskId" class="approval-task-item">
              <el-avatar :size="28" class="approval-task-item__avatar">
                {{ task.assigneeName?.charAt(0) }}
              </el-avatar>
              <div class="approval-task-item__info">
                <span class="approval-task-item__name">{{ task.assigneeName }}</span>
                <el-tag
                  :type="taskStatusType(task.taskStatus)"
                  size="small"
                  class="approval-task-item__status">
                  {{ task.taskStatusLabel }}
                </el-tag>
              </div>
              <div v-if="task.remark" class="approval-task-item__remark">
                "{{ task.remark }}"
              </div>
              <div v-if="task.operateTime" class="approval-task-item__time">
                {{ formatTime(task.operateTime) }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 审批历史时间轴 -->
      <div v-if="data?.records && data.records.length > 0" class="approval-timeline">
        <div class="approval-timeline__title">审批历史</div>
        <el-timeline>
          <el-timeline-item
            v-for="record in data.records"
            :key="record.recordId"
            :timestamp="formatTime(record.createTime)"
            placement="top"
            :type="timelineItemType(record.actionType)">
            <div class="approval-timeline-item">
              <span class="approval-timeline-item__operator">{{ record.operatorName }}</span>
              <el-tag :type="actionTagType(record.actionType)" size="small">
                {{ record.actionLabel }}
              </el-tag>
              <span v-if="record.targetAssigneeName" class="approval-timeline-item__target">
                → {{ record.targetAssigneeName }}
              </span>
              <div v-if="record.remark" class="approval-timeline-item__remark">
                {{ record.remark }}
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </template>

    <!-- 审批操作弹窗 -->
    <el-dialog
      v-model="actionDialogVisible"
      :title="actionDialogTitle"
      width="460px"
      :close-on-click-modal="false">
      <el-form :model="actionForm" label-width="80px">
        <el-form-item v-if="actionType === 'transfer'" label="转交给" required>
          <el-select
            v-model="actionForm.targetAssigneeId"
            filterable
            remote
            :remote-method="searchUsers"
            placeholder="搜索用户"
            style="width: 100%">
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="user.name"
              :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input
            v-model="actionForm.remark"
            type="textarea"
            :rows="3"
            :placeholder="actionType === 'reject' ? '请填写驳回原因' : '选填'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionDialogVisible = false">取消</el-button>
        <el-button
          :type="actionType === 'approve' ? 'success' : actionType === 'reject' ? 'danger' : 'primary'"
          :loading="submitting"
          @click="submitAction">
          确认{{ actionDialogTitle }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { WarningFilled, Check, Close, Right } from '@element-plus/icons-vue'
import { getApprovalTasks, performApproval } from '@/api/approval'
import type { ApprovalTaskOutput } from '@/types/approval'
import { APPROVAL_MODE_LABELS, APPROVAL_TASK_STATUS_TYPES } from '@/types/approval'
import request from '@/utils/request'

const props = defineProps<{
  ticketId: number
  visible?: boolean
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

const loading = ref(false)
const data = ref<ApprovalTaskOutput | null>(null)
const actionDialogVisible = ref(false)
const actionType = ref<'approve' | 'reject' | 'transfer'>('approve')
const submitting = ref(false)
const userOptions = ref<{ id: number; name: string }[]>([])
const actionForm = ref({
  remark: '',
  targetAssigneeId: undefined as number | undefined
})

const actionDialogTitle = {
  approve: '同意',
  reject: '驳回',
  transfer: '转交'
}

watch(
  () => props.ticketId,
  (id) => { if (id) fetchData(id) },
  { immediate: true }
)

async function fetchData(ticketId: number) {
  loading.value = true
  try {
    data.value = await getApprovalTasks(ticketId)
  } catch {
    // 无审批任务时后端正常返回空，此处静默处理
  } finally {
    loading.value = false
  }
}

function openActionDialog(type: 'approve' | 'reject' | 'transfer') {
  actionType.value = type
  actionForm.value = { remark: '', targetAssigneeId: undefined }
  actionDialogVisible.value = true
}

async function submitAction() {
  if (!data.value?.myPendingTaskId) return
  if (actionType.value === 'transfer' && !actionForm.value.targetAssigneeId) {
    ElMessage.warning('请选择转交目标人')
    return
  }
  submitting.value = true
  try {
    await performApproval({
      taskId: data.value.myPendingTaskId,
      actionType: actionType.value,
      remark: actionForm.value.remark,
      targetAssigneeId: actionForm.value.targetAssigneeId
    })
    ElMessage.success('操作成功')
    actionDialogVisible.value = false
    await fetchData(props.ticketId)
    emit('refresh')
  } catch (e: unknown) {
    ElMessage.error((e as Error)?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function searchUsers(query: string) {
  if (!query) return
  try {
    const res = await request.get('/api/user/list', { params: { keyword: query, pageSize: 20 } })
    userOptions.value = (res as { list?: { id: number; name: string }[] })?.list || []
  } catch {
    userOptions.value = []
  }
}

function approveModeLabel(mode: string): string {
  return APPROVAL_MODE_LABELS[mode] ?? mode
}

function taskStatusType(status: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  return (APPROVAL_TASK_STATUS_TYPES[status] ?? '') as '' | 'success' | 'warning' | 'info' | 'danger'
}

function timelineItemType(actionType: string): 'success' | 'danger' | 'warning' | 'info' | '' {
  if (actionType === 'approve') return 'success'
  if (actionType === 'reject') return 'danger'
  return 'warning'
}

function actionTagType(actionType: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  if (actionType === 'approve') return 'success'
  if (actionType === 'reject') return 'danger'
  return 'info'
}

function formatTime(timeStr: string | null): string {
  if (!timeStr) return ''
  return new Date(timeStr).toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped lang="scss">
.approval-panel {
  padding: 0 4px;
}

.approval-action-card {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 20px;

  &__header {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 500;
    margin-bottom: 10px;
    color: #ad6800;
  }

  &__body {
    display: flex;
    gap: 10px;
  }
}

.approval-nodes {
  margin-bottom: 20px;
}

.approval-node {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 12px;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 14px;
    background: #f5f7fa;
    border-bottom: 1px solid #ebeef5;
    font-size: 13px;
    font-weight: 500;
  }

  &__tasks {
    padding: 10px 14px;
  }
}

.approval-task-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;

  &__avatar {
    background: #1675d1;
    color: #fff;
    font-size: 12px;
    flex-shrink: 0;
  }

  &__info {
    display: flex;
    align-items: center;
    gap: 6px;
    flex: 1;
  }

  &__name {
    font-size: 13px;
  }

  &__remark {
    font-size: 12px;
    color: #909399;
    font-style: italic;
  }

  &__time {
    font-size: 11px;
    color: #c0c4cc;
    margin-left: auto;
  }
}

.approval-timeline {
  &__title {
    font-size: 13px;
    font-weight: 500;
    color: #606266;
    margin-bottom: 10px;
  }
}

.approval-timeline-item {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;

  &__operator {
    font-weight: 500;
    font-size: 13px;
  }

  &__target {
    font-size: 12px;
    color: #606266;
  }

  &__remark {
    width: 100%;
    font-size: 12px;
    color: #909399;
    margin-top: 2px;
  }
}
</style>
