<template>
  <div class="bug-detail-info-panel">
    <!-- 基础信息组 -->
    <div class="panel-group">
      <div class="panel-group-title">基础信息</div>
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

        <!-- 处理人 -->
        <div class="info-row">
          <span class="info-label"><el-icon><Avatar /></el-icon> 处理人</span>
          <span class="info-value">{{ detail.assigneeName || '-' }}</span>
        </div>

        <!-- 催办次数 -->
        <div class="info-row">
          <span class="info-label"><el-icon><Bell /></el-icon> 催办次数</span>
          <span class="info-value">{{ detail.urgeCount ?? 0 }}</span>
        </div>

        <!-- 创建人 -->
        <div class="info-row">
          <span class="info-label"><el-icon><User /></el-icon> 创建人</span>
          <span class="info-value">{{ detail.creatorName || '-' }}</span>
        </div>

        <!-- 创建时间 -->
        <div class="info-row">
          <span class="info-label"><el-icon><Calendar /></el-icon> 创建时间</span>
          <span class="info-value">{{ formatDateTime(detail.createTime) || '-' }}</span>
        </div>

        <!-- 预计结束（可编辑） -->
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
        <div class="info-row" v-if="detail.resolvedAt">
          <span class="info-label"><el-icon><Calendar /></el-icon> 解决时间</span>
          <span class="info-value">{{ formatDateTime(detail.resolvedAt) || '-' }}</span>
        </div>

        <!-- 是否逾期 -->
        <div class="info-row" v-if="detail.bugSummaryInfo?.isOverdue !== null && detail.bugSummaryInfo?.isOverdue !== undefined">
          <span class="info-label"><el-icon><AlarmClock /></el-icon> 是否逾期</span>
          <span class="info-value">
            <el-tag v-if="detail.bugSummaryInfo?.isOverdue === true" type="danger" size="small">已逾期</el-tag>
            <el-tag v-else type="success" size="small">未逾期</el-tag>
          </span>
        </div>
      </div>
    </div>

    <!-- 客户信息组 -->
    <div class="panel-group">
      <div class="panel-group-title">客户信息</div>
      <div class="info-list">
        <!-- 公司名称（可编辑） -->
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

        <!-- 商户编号（可编辑） -->
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
      </div>
    </div>

    <!-- 缺陷信息组 -->
    <div class="panel-group">
      <div class="panel-group-title">缺陷信息</div>
      <div class="info-list">
        <!-- 缺陷等级 -->
        <div class="info-row">
          <span class="info-label"><el-icon><Star /></el-icon> 缺陷等级</span>
          <span class="info-value">
            <span
              v-if="detail.bugTestInfo?.severityLevel"
              class="severity-tag"
              :class="'severity-' + normalizeSeverity(detail.bugTestInfo.severityLevel).toLowerCase()"
            >
              {{ SEVERITY_LABEL_MAP[normalizeSeverity(detail.bugTestInfo.severityLevel)] || detail.bugTestInfo.severityLevel }}
            </span>
            <span v-else class="empty-value">-</span>
          </span>
        </div>

        <!-- 影响范围 -->
        <div class="info-row">
          <span class="info-label"><el-icon><Warning /></el-icon> 影响范围</span>
          <span class="info-value">
            {{ IMPACT_SCOPE_LABEL_MAP[detail.bugTestInfo?.impactScope?.toUpperCase() ?? ''] || detail.bugTestInfo?.impactScope || '-' }}
          </span>
        </div>

        <!-- 缺陷划分 -->
        <div class="info-row" v-if="detail.bugSummaryInfo">
          <span class="info-label"><el-icon><Grid /></el-icon> 缺陷划分</span>
          <span class="info-value">{{ detail.bugSummaryInfo?.defectCategoryLabel || detail.bugSummaryInfo?.defectCategory || '-' }}</span>
        </div>

        <!-- 有效报告 -->
        <div class="info-row" v-if="detail.bugSummaryInfo">
          <span class="info-label"><el-icon><CircleCheck /></el-icon> 有效报告</span>
          <span class="info-value" :class="validReportClass">
            {{ detail.bugSummaryInfo?.isValidReportLabel || '-' }}
          </span>
        </div>

        <!-- 责任人 -->
        <div class="info-row" v-if="detail.bugSummaryInfo?.responsibleUserName">
          <span class="info-label"><el-icon><UserFilled /></el-icon> 责任人</span>
          <span class="info-value">{{ detail.bugSummaryInfo?.responsibleUserName }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  AlarmClock,
  Avatar,
  Bell,
  Calendar,
  Check,
  CircleCheck,
  Close,
  Document,
  Edit,
  Grid,
  Key,
  Star,
  User,
  UserFilled,
  Warning,
} from '@element-plus/icons-vue'
import { computed, reactive, ref } from 'vue'

import { updateBugCustomerInfo } from '@/api/ticket'
import type { TicketDetailOutput } from '@/types/ticket'
import { notifyError, notifySuccess } from '@/utils/feedback'
import { formatDateTime } from '@/utils/formatter'

import BugStatusBadge from './BugStatusBadge.vue'

const SEVERITY_LABEL_MAP: Record<string, string> = {
  P0: '致命',
  P1: '严重',
  P2: '一般',
  P3: '轻微',
  P4: '建议',
}

const IMPACT_SCOPE_LABEL_MAP: Record<string, string> = {
  SINGLE: '单一商户',
  PARTIAL: '部分商户',
  ALL: '全部商户',
}

const props = defineProps<{
  detail: TicketDetailOutput
  ticketId: number
}>()

const emit = defineEmits<{
  refresh: []
}>()

const editingField = ref<string | null>(null)
const editValues = reactive<Record<string, string>>({})

const canEditCustomerInfo = computed(() => true)

const validReportClass = computed(() => {
  const v = props.detail.bugSummaryInfo?.isValidReport
  if (v === 'YES') return 'valid-yes'
  if (v === 'NO') return 'valid-no'
  return ''
})

function normalizeSeverity(source?: string): string {
  if (!source) {
    return ''
  }
  const value = source.trim().toUpperCase()
  if (value === 'FATAL') return 'P0'
  if (value === 'CRITICAL') return 'P1'
  if (value === 'NORMAL') return 'P2'
  if (value === 'MINOR') return 'P3'
  return value
}

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
  padding: 2px 0 8px;
}

.panel-group {
  margin-bottom: 4px;
}

.panel-group + .panel-group {
  border-top: 1px solid #ebedf0;
  padding-top: 8px;
  margin-top: 4px;
}

.panel-group-title {
  font-size: 11px;
  font-weight: 600;
  color: #909399;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 6px 4px 8px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  padding: 7px 8px;
  border-radius: 6px;
  min-height: 32px;
  transition: background 0.15s;
}

.info-row:hover {
  background: #f0f7ff;
}

.info-label {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 88px;
  flex-shrink: 0;
  font-size: 13px;
  color: #909399;
  line-height: 22px;
  padding-top: 1px;
}

.info-label .el-icon {
  font-size: 14px;
  flex-shrink: 0;
  color: #c0c4cc;
}

.info-value {
  flex: 1;
  font-size: 13px;
  color: #303133;
  min-width: 0;
  word-break: break-all;
  line-height: 22px;
}

.empty-value {
  color: #dcdfe6;
}

.editable-row:hover .edit-icon {
  opacity: 1;
}

.editable-value {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}

.value-text {
  flex: 1;
  cursor: pointer;
  border-radius: 4px;
  padding: 2px 4px;
  transition: background 0.15s;
  font-size: 13px;
}

.value-text:hover {
  background: #e8f4ff;
}

.edit-icon {
  opacity: 0;
  cursor: pointer;
  color: #1675d1;
  font-size: 13px;
  transition: opacity 0.15s;
}

/* severity tags */
.severity-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
}

.severity-p0 {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
}

.severity-p1 {
  background: #fff0e6;
  color: #e6621c;
  border: 1px solid #f9c3a0;
}

.severity-p2 {
  background: #fdf6ec;
  color: #e6a23c;
  border: 1px solid #f5dab1;
}

.severity-p3 {
  background: #f0f9ff;
  color: #1675d1;
  border: 1px solid #b3d4f5;
}

.severity-p4 {
  background: #f4f4f5;
  color: #909399;
  border: 1px solid #dcdfe6;
}

.valid-yes {
  color: #67c23a;
  font-weight: 600;
}

.valid-no {
  color: #f56c6c;
  font-weight: 600;
}
</style>
