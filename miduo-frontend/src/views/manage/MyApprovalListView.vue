<template>
  <div class="my-approval-list">
    <div class="page-header">
      <h2>待我审批</h2>
      <el-tag v-if="total > 0" type="warning" effect="dark" round>{{ total }}</el-tag>
    </div>

    <!-- 工单列表 -->
    <el-table
      :data="items"
      v-loading="loading"
      :border="false"
      :stripe="true"
      :header-cell-style="{ backgroundColor: '#f5f7fa' }"
      empty-text="暂无待审批任务"
    >
      <el-table-column label="工单编号" width="130" align="center">
        <template #default="{ row }">
          <el-link
            v-if="row.ticketNo"
            type="primary"
            :href="`/ticket/${row.ticketId}`"
            target="_blank"
          >
            {{ row.ticketNo }}
          </el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>

      <el-table-column label="工单标题" min-width="200" :show-overflow-tooltip="true">
        <template #default="{ row }">
          <el-link type="primary" :href="`/ticket/${row.ticketId}`" target="_blank">
            {{ row.ticketTitle || '(无标题)' }}
          </el-link>
        </template>
      </el-table-column>

      <el-table-column label="审批节点" width="140" align="center">
        <template #default="{ row }">
          {{ row.nodeName }}
        </template>
      </el-table-column>

      <el-table-column label="审批模式" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ approveModeLabel(row.approveMode) }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="等待时长" width="100" align="center">
        <template #default="{ row }">
          <span :class="{ 'text-warning': row.isOverdue }">
            {{ formatWaitTime(row.waitMinutes) }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="提交时间" width="160" align="center">
        <template #default="{ row }">
          {{ formatTime(row.createTime) }}
        </template>
      </el-table-column>

      <el-table-column label="截止时间" width="160" align="center">
        <template #default="{ row }">
          <span v-if="row.dueTime" :class="{ 'text-danger': row.isOverdue }">
            {{ formatTime(row.dueTime) }}
            <el-icon v-if="row.isOverdue" color="#f56c6c"><WarningFilled /></el-icon>
          </span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="success" size="small" link @click="handleAction(row, 'approve')">
            同意
          </el-button>
          <el-button type="danger" size="small" link @click="handleAction(row, 'reject')">
            驳回
          </el-button>
          <el-button size="small" link @click="handleAction(row, 'transfer')"> 转交 </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-if="total > 0"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </div>

    <!-- 审批操作弹窗 -->
    <el-dialog
      v-model="actionDialogVisible"
      :title="actionDialogTitle[currentActionType]"
      width="460px"
      :close-on-click-modal="false"
    >
      <el-form :model="actionForm" label-width="80px">
        <el-form-item v-if="currentActionType === 'transfer'" label="转交给" required>
          <el-select
            v-model="actionForm.targetAssigneeId"
            filterable
            remote
            :remote-method="searchUsers"
            placeholder="搜索用户姓名"
            style="width: 100%"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="user.name"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input
            v-model="actionForm.remark"
            type="textarea"
            :rows="3"
            :placeholder="currentActionType === 'reject' ? '请填写驳回原因（必填）' : '选填'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionDialogVisible = false">取消</el-button>
        <el-button
          :type="
            currentActionType === 'approve'
              ? 'success'
              : currentActionType === 'reject'
                ? 'danger'
                : 'primary'
          "
          :loading="submitting"
          @click="submitAction"
        >
          确认{{ actionDialogTitle[currentActionType] }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import { getMyApprovalPendingList, performApproval } from '@/api/approval'
import type { ApprovalPendingItem } from '@/types/approval'
import { APPROVAL_MODE_LABELS } from '@/types/approval'
import request from '@/utils/request'

const loading = ref(false)
const items = ref<ApprovalPendingItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

const actionDialogVisible = ref(false)
const currentActionType = ref<'approve' | 'reject' | 'transfer'>('approve')
const currentTaskId = ref<number | null>(null)
const submitting = ref(false)
const userOptions = ref<{ id: number; name: string }[]>([])
const actionForm = ref({
  remark: '',
  targetAssigneeId: undefined as number | undefined,
})

const actionDialogTitle: Record<string, string> = {
  approve: '同意',
  reject: '驳回',
  transfer: '转交',
}

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const result = await getMyApprovalPendingList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })
    items.value = result.items
    total.value = Number(result.totalCount)
  } catch {
    ElMessage.error('加载审批任务失败')
  } finally {
    loading.value = false
  }
}

function handleAction(row: ApprovalPendingItem, type: 'approve' | 'reject' | 'transfer') {
  currentTaskId.value = row.taskId
  currentActionType.value = type
  actionForm.value = { remark: '', targetAssigneeId: undefined }
  actionDialogVisible.value = true
}

async function submitAction() {
  if (!currentTaskId.value) return
  if (currentActionType.value === 'reject' && !actionForm.value.remark?.trim()) {
    ElMessage.warning('驳回原因不能为空')
    return
  }
  if (currentActionType.value === 'transfer' && !actionForm.value.targetAssigneeId) {
    ElMessage.warning('请选择转交目标人')
    return
  }
  submitting.value = true
  try {
    await performApproval({
      taskId: currentTaskId.value,
      actionType: currentActionType.value,
      remark: actionForm.value.remark,
      targetAssigneeId: actionForm.value.targetAssigneeId,
    })
    ElMessage.success('操作成功')
    actionDialogVisible.value = false
    fetchData()
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

function formatWaitTime(minutes: number | null): string {
  if (minutes === null || minutes === undefined) return '-'
  if (minutes < 60) return `${minutes}分钟`
  if (minutes < 1440) return `${Math.floor(minutes / 60)}小时`
  return `${Math.floor(minutes / 1440)}天`
}

function formatTime(timeStr: string | null): string {
  if (!timeStr) return '-'
  return new Date(timeStr).toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped lang="scss">
.my-approval-list {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;

  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.text-warning {
  color: #e6a23c;
}

.text-danger {
  color: #f56c6c;
}

.text-muted {
  color: #c0c4cc;
}
</style>
