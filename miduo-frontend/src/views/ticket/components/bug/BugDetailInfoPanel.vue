<template>
  <div class="bug-detail-info-panel">
    <div class="panel-title">基础信息</div>
    <div class="info-list">
      <!-- 状态 -->
      <div class="info-row">
        <span class="info-label">
          <el-icon><CircleCheck /></el-icon> 状态
        </span>
        <span class="info-value">
          <BugStatusBadge v-if="detail.status" :status="detail.status" :status-label="detail.statusLabel" />
          <span v-else class="empty-value">-</span>
        </span>
      </div>

      <!-- 公司名称 -->
      <div class="info-row editable-row" v-if="canEditCustomerInfo">
        <span class="info-label">
          <el-icon><Document /></el-icon> 公司名称
        </span>
        <div class="info-value editable-value">
          <template v-if="editingField !== 'companyName'">
            <span class="value-text" @click="startEdit('companyName')">
              {{ detail.bugCustomerInfo?.companyName || '-' }}
            </span>
            <el-icon class="edit-icon" @click="startEdit('companyName')"><Edit /></el-icon>
          </template>
          <template v-else>
            <el-input v-model="editValues.companyName" size="small" @keyup.enter="saveField('companyName')" @keyup.escape="cancelEdit" />
            <el-button type="primary" link size="small" @click="saveField('companyName')">
              <el-icon><Check /></el-icon>
            </el-button>
            <el-button link size="small" @click="cancelEdit">
              <el-icon><Close /></el-icon>
            </el-button>
          </template>
        </div>
      </div>
      <div class="info-row" v-else>
        <span class="info-label"><el-icon><Document /></el-icon> 公司名称</span>
        <span class="info-value">{{ detail.bugCustomerInfo?.companyName || '-' }}</span>
      </div>

      <!-- 商户编号 -->
      <div class="info-row editable-row" v-if="canEditCustomerInfo">
        <span class="info-label"><el-icon><Key /></el-icon> 商户编号</span>
        <div class="info-value editable-value">
          <template v-if="editingField !== 'merchantNo'">
            <span class="value-text" @click="startEdit('merchantNo')">
              {{ detail.bugCustomerInfo?.merchantNo || '-' }}
            </span>
            <el-icon class="edit-icon" @click="startEdit('merchantNo')"><Edit /></el-icon>
          </template>
          <template v-else>
            <el-input v-model="editValues.merchantNo" size="small" @keyup.enter="saveField('merchantNo')" @keyup.escape="cancelEdit" />
            <el-button type="primary" link size="small" @click="saveField('merchantNo')"><el-icon><Check /></el-icon></el-button>
            <el-button link size="small" @click="cancelEdit"><el-icon><Close /></el-icon></el-button>
          </template>
        </div>
      </div>
      <div class="info-row" v-else>
        <span class="info-label"><el-icon><Key /></el-icon> 商户编号</span>
        <span class="info-value">{{ detail.bugCustomerInfo?.merchantNo || '-' }}</span>
      </div>

      <!-- 反馈人 -->
      <div class="info-row">
        <span class="info-label"><el-icon><User /></el-icon> 反馈人</span>
        <span class="info-value">{{ detail.creatorName || '-' }}</span>
      </div>

      <!-- 反馈时间 -->
      <div class="info-row">
        <span class="info-label"><el-icon><Calendar /></el-icon> 反馈时间</span>
        <span class="info-value">{{ formatDateTime(detail.createTime) || '-' }}</span>
      </div>

      <!-- 创建人（系统锁定） -->
      <div class="info-row">
        <span class="info-label"><el-icon><Lock /></el-icon> 创建人</span>
        <span class="info-value">{{ detail.creatorName || '-' }}</span>
      </div>

      <!-- 创建时间 -->
      <div class="info-row">
        <span class="info-label"><el-icon><Calendar /></el-icon> 创建时间</span>
        <span class="info-value">{{ formatDateTime(detail.createTime) || '-' }}</span>
      </div>

      <!-- 影响范围 -->
      <div class="info-row">
        <span class="info-label"><el-icon><Warning /></el-icon> 影响范围</span>
        <span class="info-value">{{ detail.bugTestInfo?.impactScope || '-' }}</span>
      </div>

      <!-- 缺陷等级 -->
      <div class="info-row">
        <span class="info-label"><el-icon><Star /></el-icon> 缺陷等级</span>
        <span class="info-value">
          <span v-if="detail.bugTestInfo?.severityLevel" class="severity-tag" :class="'severity-' + detail.bugTestInfo.severityLevel.toLowerCase()">
            {{ detail.bugTestInfo.severityLevel }}
          </span>
          <span v-else class="empty-value">-</span>
        </span>
      </div>

      <!-- 缺陷划分（来自简报） -->
      <div class="info-row">
        <span class="info-label"><el-icon><Grid /></el-icon> 缺陷划分</span>
        <span class="info-value">{{ detail.bugSummaryInfo?.defectCategoryLabel || detail.bugSummaryInfo?.defectCategory || '-' }}</span>
      </div>

      <!-- 有效报告（来自简报） -->
      <div class="info-row">
        <span class="info-label"><el-icon><CircleCheck /></el-icon> 有效报告</span>
        <span class="info-value" :class="validReportClass">
          {{ detail.bugSummaryInfo?.isValidReportLabel || '-' }}
        </span>
      </div>

      <!-- 处理人 -->
      <div class="info-row">
        <span class="info-label"><el-icon><Avatar /></el-icon> 处理人</span>
        <span class="info-value">{{ detail.assigneeName || '-' }}</span>
      </div>

      <!-- 责任人（来自简报） -->
      <div class="info-row">
        <span class="info-label"><el-icon><UserFilled /></el-icon> 责任人</span>
        <span class="info-value">{{ detail.bugSummaryInfo?.responsibleUserName || '-' }}</span>
      </div>

      <!-- 预计结束 -->
      <div class="info-row editable-row" v-if="canEditCustomerInfo">
        <span class="info-label"><el-icon><Calendar /></el-icon> 预计结束</span>
        <div class="info-value editable-value">
          <template v-if="editingField !== 'expectedTime'">
            <span class="value-text" @click="startEdit('expectedTime')">
              {{ formatDateTime(detail.expectedTime) || '-' }}
            </span>
            <el-icon class="edit-icon" @click="startEdit('expectedTime')"><Edit /></el-icon>
          </template>
          <template v-else>
            <el-date-picker
              v-model="editValues.expectedTime"
              type="datetime"
              size="small"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm:ss"
              @change="saveField('expectedTime')"
              @blur="cancelEdit"
              style="width: 160px"
            />
          </template>
        </div>
      </div>
      <div class="info-row" v-else>
        <span class="info-label"><el-icon><Calendar /></el-icon> 预计结束</span>
        <span class="info-value">{{ formatDateTime(detail.expectedTime) || '-' }}</span>
      </div>

      <!-- 解决时间 -->
      <div class="info-row">
        <span class="info-label"><el-icon><Calendar /></el-icon> 解决时间</span>
        <span class="info-value">{{ formatDateTime(detail.resolvedAt) || '-' }}</span>
      </div>

      <!-- 是否逾期 -->
      <div class="info-row">
        <span class="info-label"><el-icon><AlarmClock /></el-icon> 是否逾期</span>
        <span class="info-value">
          <el-tag v-if="detail.bugSummaryInfo?.isOverdue === true" type="danger" size="small">已逾期</el-tag>
          <el-tag v-else-if="detail.bugSummaryInfo?.isOverdue === false" type="success" size="small">未逾期</el-tag>
          <span v-else class="empty-value">-</span>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { AlarmClock, Avatar, Calendar, Check, CircleCheck, Close, Document, Edit, Grid, Key, Lock, Star, User, UserFilled, Warning } from '@element-plus/icons-vue'
import { computed, reactive, ref } from 'vue'

import { updateBugCustomerInfo } from '@/api/ticket'
import { useAuthStore } from '@/stores/auth'
import type { TicketDetailOutput } from '@/types/ticket'
import { notifyError, notifySuccess } from '@/utils/feedback'
import { formatDateTime } from '@/utils/formatter'

import BugStatusBadge from './BugStatusBadge.vue'

const props = defineProps<{
  detail: TicketDetailOutput
  ticketId: number
}>()

const emit = defineEmits<{
  refresh: []
}>()

const authStore = useAuthStore()

const editingField = ref<string | null>(null)
const editValues = reactive<Record<string, string>>({})

const roleCodes = computed(() =>
  (authStore.userInfo?.roleCodes ?? []).map((c: string) => String(c).toUpperCase()),
)

const isAdmin = computed(() =>
  roleCodes.value.includes('ADMIN') || roleCodes.value.includes('TICKET_ADMIN'),
)

const canEditCustomerInfo = computed(
  () => isAdmin.value || roleCodes.value.includes('CUSTOMER_SERVICE'),
)

const validReportClass = computed(() => {
  const v = props.detail.bugSummaryInfo?.isValidReport
  if (v === 'YES') return 'valid-yes'
  if (v === 'NO') return 'valid-no'
  return ''
})

function startEdit(field: string): void {
  editingField.value = field
  if (field === 'companyName') {
    editValues.companyName = props.detail.bugCustomerInfo?.companyName ?? ''
  } else if (field === 'merchantNo') {
    editValues.merchantNo = props.detail.bugCustomerInfo?.merchantNo ?? ''
  } else if (field === 'expectedTime') {
    editValues.expectedTime = props.detail.expectedTime ?? ''
  }
}

function cancelEdit(): void {
  editingField.value = null
}

async function saveField(field: string): Promise<void> {
  if (!editingField.value) return
  try {
    if (field === 'companyName' || field === 'merchantNo') {
      const existing = props.detail.bugCustomerInfo ?? {}
      await updateBugCustomerInfo(props.ticketId, {
        ...existing,
        companyName: field === 'companyName' ? editValues.companyName : (existing.companyName ?? ''),
        merchantNo: field === 'merchantNo' ? editValues.merchantNo : (existing.merchantNo ?? ''),
      })
      notifySuccess('保存成功')
      emit('refresh')
    }
  } catch {
    notifyError('保存失败，请重试')
  } finally {
    editingField.value = null
  }
}
</script>

<style scoped>
.bug-detail-info-panel {
  padding: 0 12px;
}

.panel-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  padding: 12px 0 8px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 4px;
}

.info-list {
  display: flex;
  flex-direction: column;
}

.info-row {
  display: flex;
  align-items: center;
  padding: 7px 4px;
  border-bottom: 1px solid #f5f5f5;
  min-height: 32px;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 90px;
  flex-shrink: 0;
  font-size: 12px;
  color: #909399;
}

.info-label .el-icon {
  font-size: 12px;
}

.info-value {
  flex: 1;
  font-size: 13px;
  color: #303133;
  min-width: 0;
  word-break: break-all;
}

.empty-value {
  color: #c0c4cc;
}

.editable-row:hover .edit-icon {
  opacity: 1;
}

.editable-value {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.value-text {
  flex: 1;
  cursor: pointer;
  border-radius: 3px;
  padding: 1px 3px;
  transition: background 0.15s;
}

.value-text:hover {
  background: #f0f9ff;
}

.edit-icon {
  opacity: 0;
  cursor: pointer;
  color: #1675d1;
  font-size: 13px;
  transition: opacity 0.15s;
}

.severity-tag {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 500;
}

.severity-p0 { background: #fef0f0; color: #f56c6c; }
.severity-p1 { background: #fff0e6; color: #e6621c; }
.severity-p2 { background: #fdf6ec; color: #e6a23c; }
.severity-p3 { background: #f0f9ff; color: #1675d1; }
.severity-p4 { background: #f4f4f5; color: #909399; }

.valid-yes { color: #67c23a; font-weight: 500; }
.valid-no { color: #f56c6c; font-weight: 500; }
</style>
