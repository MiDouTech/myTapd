<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { getPublicTicketDetail } from '@/api/ticket'
import type {
  TicketPublicActivityOutput,
  TicketPublicCommentOutput,
  TicketPublicDetailOutput,
} from '@/types/ticket'

const route = useRoute()
const loading = ref(false)
const error = ref('')
const detail = ref<TicketPublicDetailOutput>()
const problemDescExpanded = ref(false)
const conclusionExpanded = ref(false)

type PublicTimelineItem =
  | { kind: 'activity'; data: TicketPublicActivityOutput }
  | { kind: 'comment'; data: TicketPublicCommentOutput }

const priorityColorMap: Record<string, string> = {
  urgent: '#f56c6c',
  high: '#e6a23c',
  medium: '#409eff',
  low: '#67c23a',
}

const statusColorMap: Record<string, string> = {
  OPEN: '#409eff',
  PENDING_ASSIGN: '#909399',
  PENDING_DISPATCH: '#909399',
  PENDING_ACCEPT: '#1675d1',
  PENDING_TEST_ACCEPT: '#e6a23c',
  TESTING: '#e6a23c',
  INVESTIGATING: '#3b82f6',
  PENDING_DEV_ACCEPT: '#6b7280',
  DEVELOPING: '#3b82f6',
  PROCESSING: '#e6a23c',
  TEMP_RESOLVED: '#e6a23c',
  PENDING_CS_CONFIRM: '#f59e0b',
  PENDING_VERIFY: '#8b5cf6',
  RESOLVED: '#67c23a',
  CLOSED: '#909399',
  COMPLETED: '#67c23a',
  SUSPENDED: '#f56c6c',
}

function getPriorityColor(priority?: string): string {
  return priority ? (priorityColorMap[priority.toLowerCase()] ?? '#606266') : '#606266'
}

function getStatusColor(status?: string): string {
  return status ? (statusColorMap[status.toUpperCase()] ?? '#1675d1') : '#1675d1'
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  } catch {
    return dateStr
  }
}

function isOperationLog(type?: string): boolean {
  return type === 'OPERATION'
}

function parseTimeMs(t?: string): number {
  if (!t) return 0
  const ms = new Date(t).getTime()
  return Number.isNaN(ms) ? 0 : ms
}

const mergedTimeline = computed<PublicTimelineItem[]>(() => {
  const d = detail.value
  if (!d) return []
  const items: PublicTimelineItem[] = []
  for (const a of d.activities ?? []) {
    items.push({ kind: 'activity', data: a })
  }
  for (const c of d.comments ?? []) {
    items.push({ kind: 'comment', data: c })
  }
  items.sort((x, y) => {
    const tx =
      x.kind === 'activity' ? parseTimeMs(x.data.createTime) : parseTimeMs(x.data.createTime)
    const ty =
      y.kind === 'activity' ? parseTimeMs(y.data.createTime) : parseTimeMs(y.data.createTime)
    if (tx !== ty) return tx - ty
    return x.kind === 'activity' && y.kind === 'comment' ? -1 : 1
  })
  return items
})

const hasTimelineItems = computed(() => mergedTimeline.value.length > 0)

const terminalStatuses = new Set(['closed', 'completed', 'rejected'])

const showResolutionCard = computed(() => {
  const d = detail.value
  if (!d?.resolutionSummary?.trim()) return false
  const s = (d.status || '').toLowerCase()
  return terminalStatuses.has(s)
})

function isImageAttachment(fileType?: string): boolean {
  if (!fileType) return false
  return fileType.toLowerCase().startsWith('image/')
}

async function loadDetail(): Promise<void> {
  const ticketNo = String(route.params.ticketNo || '')
  if (!ticketNo) {
    error.value = '工单编号不能为空'
    return
  }
  loading.value = true
  error.value = ''
  try {
    detail.value = await getPublicTicketDetail(ticketNo)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '加载失败，请稍后重试'
    error.value = msg
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <div class="public-ticket-page">
    <header class="page-header">
      <div class="brand">
        <span class="brand-icon">🎫</span>
        <span class="brand-name">工单详情</span>
      </div>
    </header>

    <main class="page-content">
      <div v-if="loading" class="center-state">
        <div class="spinner" />
        <p>加载中...</p>
      </div>

      <div v-else-if="error" class="center-state error-state">
        <div class="error-icon">⚠️</div>
        <p class="error-text">{{ error }}</p>
        <p class="error-hint">如有问题请联系相关人员</p>
      </div>

      <template v-else-if="detail">
        <!-- 工单头部信息卡片 -->
        <div class="card ticket-header-card">
          <div class="ticket-badges">
            <span
              class="badge status-badge"
              :style="{ backgroundColor: getStatusColor(detail.status), color: '#fff' }"
            >
              {{ detail.statusLabel || detail.status || '-' }}
            </span>
            <span
              class="badge priority-badge"
              :style="{ borderColor: getPriorityColor(detail.priority), color: getPriorityColor(detail.priority) }"
            >
              {{ detail.priorityLabel || detail.priority || '-' }}
            </span>
          </div>
          <h1 class="ticket-title">{{ detail.title || '（无标题）' }}</h1>
          <p class="ticket-no">{{ detail.ticketNo }}</p>
        </div>

        <!-- 处理结论 -->
        <div v-if="showResolutionCard" class="card resolution-card">
          <h2 class="card-title">处理结论</h2>
          <div
            class="resolution-text"
            :class="{ 'resolution-text-clamp': !conclusionExpanded && (detail.resolutionSummary?.length ?? 0) > 200 }"
          >
            {{ detail.resolutionSummary }}
          </div>
          <button
            v-if="(detail.resolutionSummary?.length ?? 0) > 200"
            type="button"
            class="text-link-btn"
            @click="conclusionExpanded = !conclusionExpanded"
          >
            {{ conclusionExpanded ? '收起' : '展开全文' }}
          </button>
        </div>

        <!-- 工单基本信息 -->
        <div class="card info-card">
          <h2 class="card-title">基本信息</h2>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">分类</span>
              <span class="info-value category-path">{{ detail.categoryFullPath || detail.categoryName || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">来源</span>
              <span class="info-value">{{ detail.sourceLabel || detail.source || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">优先级</span>
              <span class="info-value">
                <span
                  v-if="detail.priorityLabel"
                  class="priority-chip"
                  :style="{ color: getPriorityColor(detail.priority), borderColor: getPriorityColor(detail.priority) }"
                >{{ detail.priorityLabel }}</span>
                <span v-else>-</span>
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">创建人</span>
              <span class="info-value">{{ detail.creatorName || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">处理人</span>
              <span class="info-value">{{ detail.assigneeName || '待分配' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">期望完成</span>
              <span class="info-value">{{ formatDate(detail.expectedTime) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">创建时间</span>
              <span class="info-value">{{ formatDate(detail.createTime) }}</span>
            </div>
            <div v-if="detail.resolvedAt" class="info-item">
              <span class="info-label">完成时间</span>
              <span class="info-value">{{ formatDate(detail.resolvedAt) }}</span>
            </div>
            <div v-if="detail.closedAt" class="info-item">
              <span class="info-label">关闭时间</span>
              <span class="info-value">{{ formatDate(detail.closedAt) }}</span>
            </div>
          </div>
        </div>

        <!-- 客户信息 -->
        <div v-if="detail.bugCustomerInfo" class="card customer-card">
          <h2 class="card-title">客户信息</h2>
          <div class="info-grid">
            <div v-if="detail.bugCustomerInfo.merchantNo" class="info-item">
              <span class="info-label">商户编号</span>
              <span class="info-value">{{ detail.bugCustomerInfo.merchantNo }}</span>
            </div>
            <div v-if="detail.bugCustomerInfo.companyName" class="info-item">
              <span class="info-label">公司名称</span>
              <span class="info-value">{{ detail.bugCustomerInfo.companyName }}</span>
            </div>
            <div v-if="detail.bugCustomerInfo.merchantAccount" class="info-item">
              <span class="info-label">商户账号</span>
              <span class="info-value">{{ detail.bugCustomerInfo.merchantAccount }}</span>
            </div>
            <div v-if="detail.bugCustomerInfo.sceneCode" class="info-item">
              <span class="info-label">场景码</span>
              <span class="info-value">{{ detail.bugCustomerInfo.sceneCode }}</span>
            </div>
          </div>
          <div v-if="detail.bugCustomerInfo.problemDesc" class="info-block-full">
            <span class="info-label">问题描述</span>
            <div
              class="info-text-block"
              :class="{ 'text-clamp': !problemDescExpanded && (detail.bugCustomerInfo.problemDesc.length > 280) }"
            >
              {{ detail.bugCustomerInfo.problemDesc }}
            </div>
            <button
              v-if="detail.bugCustomerInfo.problemDesc.length > 280"
              type="button"
              class="text-link-btn"
              @click="problemDescExpanded = !problemDescExpanded"
            >
              {{ problemDescExpanded ? '收起' : '展开全文' }}
            </button>
          </div>
          <div v-if="detail.bugCustomerInfo.expectedResult" class="info-block-full">
            <span class="info-label">预期结果</span>
            <div class="info-text-block">{{ detail.bugCustomerInfo.expectedResult }}</div>
          </div>

          <!-- 排障信息 -->
          <div v-if="detail.bugCustomerInfo.troubleshooting" class="info-block-full troubleshoot-block">
            <span class="info-label">排障信息</span>
            <div class="troubleshoot-grid">
              <div v-if="detail.bugCustomerInfo.troubleshooting.requestUrl" class="troubleshoot-row">
                <span class="tk">请求URL</span>
                <span class="tv">{{ detail.bugCustomerInfo.troubleshooting.requestUrl }}</span>
              </div>
              <div v-if="detail.bugCustomerInfo.troubleshooting.httpStatus" class="troubleshoot-row">
                <span class="tk">HTTP状态</span>
                <span class="tv">{{ detail.bugCustomerInfo.troubleshooting.httpStatus }}</span>
              </div>
              <div v-if="detail.bugCustomerInfo.troubleshooting.bizErrorCode" class="troubleshoot-row">
                <span class="tk">业务错误码</span>
                <span class="tv">{{ detail.bugCustomerInfo.troubleshooting.bizErrorCode }}</span>
              </div>
              <div v-if="detail.bugCustomerInfo.troubleshooting.traceId" class="troubleshoot-row">
                <span class="tk">TraceId</span>
                <span class="tv">{{ detail.bugCustomerInfo.troubleshooting.traceId }}</span>
              </div>
              <div v-if="detail.bugCustomerInfo.troubleshooting.occurredAt" class="troubleshoot-row">
                <span class="tk">发生时间</span>
                <span class="tv">{{ formatDate(detail.bugCustomerInfo.troubleshooting.occurredAt) }}</span>
              </div>
              <div v-if="detail.bugCustomerInfo.troubleshooting.clientTypeLabel" class="troubleshoot-row">
                <span class="tk">客户端</span>
                <span class="tv">{{ detail.bugCustomerInfo.troubleshooting.clientTypeLabel }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 证据：截图 + 附件 -->
        <div
          v-if="detail.bugCustomerInfo?.problemScreenshot || (detail.publicAttachments?.length ?? 0) > 0"
          class="card evidence-card"
        >
          <h2 class="card-title">证据</h2>
          <div v-if="detail.bugCustomerInfo?.problemScreenshot" class="info-block-full">
            <span class="info-label">问题截图</span>
            <div class="screenshot-wrap">
              <img
                v-for="(url, idx) in detail.bugCustomerInfo.problemScreenshot.split(',')"
                :key="'s' + idx"
                :src="url.trim()"
                class="screenshot-img"
                alt="问题截图"
              />
            </div>
          </div>
          <div v-if="detail.publicAttachments?.length" class="info-block-full">
            <span class="info-label">附件</span>
            <ul class="attachment-list">
              <li v-for="att in detail.publicAttachments" :key="att.id" class="attachment-row">
                <template v-if="isImageAttachment(att.fileType) && att.fileUrl">
                  <a :href="att.fileUrl" target="_blank" rel="noopener noreferrer" class="attachment-thumb-link">
                    <img :src="att.fileUrl" class="attachment-thumb" alt="" />
                  </a>
                  <span class="attachment-name">{{ att.fileName || '图片' }}</span>
                </template>
                <template v-else>
                  <a
                    v-if="att.fileUrl"
                    :href="att.fileUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="attachment-link"
                  >{{ att.fileName || '下载附件' }}</a>
                  <span v-else class="attachment-name">{{ att.fileName || '附件' }}</span>
                </template>
              </li>
            </ul>
          </div>
        </div>

        <!-- 处理动态 -->
        <div class="card comments-card">
          <h2 class="card-title">处理动态</h2>
          <div v-if="!hasTimelineItems" class="empty-comments">暂无处理动态</div>
          <div v-else class="comment-list">
            <template v-for="item in mergedTimeline" :key="item.kind + '-' + item.data.id">
              <div v-if="item.kind === 'activity'" class="comment-item activity-item">
                <div class="comment-header">
                  <div class="comment-avatar activity-avatar">{{ (item.data.eventTypeLabel || '系')[0] }}</div>
                  <div class="comment-meta">
                    <span class="comment-user">{{ item.data.operatorName || '系统' }}</span>
                    <span class="activity-tag">{{ item.data.eventTypeLabel || '系统' }}</span>
                    <span class="comment-time">{{ formatDate(item.data.createTime) }}</span>
                  </div>
                </div>
                <div class="comment-body comment-body-plain">{{ item.data.summary || '-' }}</div>
              </div>
              <div v-else class="comment-item">
                <div class="comment-header">
                  <div class="comment-avatar">{{ ((item.data.userName || '?')[0] ?? '?').toUpperCase() }}</div>
                  <div class="comment-meta">
                    <span class="comment-user">{{ item.data.userName || '系统' }}</span>
                    <span class="comment-time">{{ formatDate(item.data.createTime) }}</span>
                  </div>
                </div>
                <div v-if="isOperationLog(item.data.type)" class="comment-body comment-body-plain">
                  {{ item.data.content }}
                </div>
                <!-- eslint-disable-next-line vue/no-v-html -->
                <div v-else class="comment-body" v-html="item.data.content || '-'" />
              </div>
            </template>
          </div>
        </div>

        <!-- 工单描述（补充） -->
        <div v-if="detail.description && !detail.descriptionDuplicateOfProblemDesc" class="card desc-card">
          <h2 class="card-title">工单描述（补充）</h2>
          <!-- eslint-disable-next-line vue/no-v-html -->
          <div class="desc-content" v-html="detail.description" />
        </div>
        <div v-else-if="detail.description && detail.descriptionDuplicateOfProblemDesc" class="card desc-card muted-desc-card">
          <h2 class="card-title">工单描述</h2>
          <p class="dedupe-hint">与上方「问题描述」一致，已折叠避免重复展示。</p>
        </div>
      </template>
    </main>

    <footer class="page-footer">
      <p>米多工单系统 · 此页面无需登录即可访问</p>
    </footer>
  </div>
</template>

<style scoped>
* {
  box-sizing: border-box;
}

.public-ticket-page {
  min-height: 100vh;
  background: #f4f6f9;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  color: #303133;
}

/* 顶部导航 */
.page-header {
  background: #1675d1;
  padding: 12px 16px;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(22, 117, 209, 0.3);
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-icon {
  font-size: 18px;
}

.brand-name {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

/* 主内容区 */
.page-content {
  flex: 1;
  padding: 16px;
  max-width: 720px;
  margin: 0 auto;
  width: 100%;
}

/* 卡片 */
.card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
  letter-spacing: 0.5px;
}

/* 工单头部卡片 */
.ticket-header-card {
  padding-top: 20px;
  padding-bottom: 20px;
}

.ticket-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  line-height: 20px;
}

.priority-badge {
  background: transparent;
  border: 1px solid currentColor;
}

.ticket-title {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.4;
  margin: 0 0 6px 0;
  color: #1a1a1a;
}

.ticket-no {
  font-size: 13px;
  color: #909399;
  margin: 0;
  font-weight: 500;
}

.resolution-card {
  border: 1px solid #bae6fd;
  background: linear-gradient(180deg, #f0f9ff 0%, #fff 100%);
}

.resolution-text {
  font-size: 14px;
  line-height: 1.7;
  color: #0c4a6e;
  white-space: pre-wrap;
  word-break: break-word;
}

.resolution-text-clamp {
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.text-clamp {
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.text-link-btn {
  margin-top: 8px;
  padding: 0;
  border: none;
  background: none;
  color: #1675d1;
  font-size: 13px;
  cursor: pointer;
}

.text-link-btn:hover {
  text-decoration: underline;
}

.troubleshoot-block .troubleshoot-grid {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.troubleshoot-row {
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: 8px;
  font-size: 13px;
  align-items: start;
}

.troubleshoot-row .tk {
  color: #909399;
}

.troubleshoot-row .tv {
  color: #303133;
  word-break: break-all;
}

.attachment-list {
  list-style: none;
  margin: 8px 0 0;
  padding: 0;
}

.attachment-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.attachment-thumb-link {
  display: block;
  flex-shrink: 0;
}

.attachment-thumb {
  width: 72px;
  height: 72px;
  border-radius: 6px;
  border: 1px solid #e5e6eb;
  object-fit: cover;
  display: block;
}

.attachment-name,
.attachment-link {
  font-size: 13px;
  color: #1675d1;
  word-break: break-all;
}

.activity-item .activity-avatar {
  background: #64748b;
}

.activity-tag {
  margin-left: 6px;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  background: #e0f2fe;
  color: #0369a1;
}

.muted-desc-card {
  opacity: 0.9;
}

.dedupe-hint {
  margin: 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: 12px;
  color: #909399;
}

.info-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  word-break: break-all;
}

/* 分类路径 */
.category-path {
  color: #1675d1;
}

/* 优先级标签 */
.priority-chip {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 20px;
  border: 1px solid currentColor;
  font-size: 12px;
  font-weight: 500;
}

/* 客户信息卡片全宽字段 */
.info-block-full {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-text-block {
  font-size: 14px;
  color: #303133;
  line-height: 1.7;
  white-space: pre-wrap;
  background: #f8fafc;
  border-radius: 6px;
  padding: 10px 12px;
  margin-top: 4px;
  word-break: break-word;
}

/* 问题截图 */
.screenshot-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
}

.screenshot-img {
  max-width: 100%;
  max-height: 240px;
  border-radius: 6px;
  border: 1px solid #e5e6eb;
  object-fit: contain;
}

/* 问题描述 */
.desc-content {
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  word-break: break-word;

  :deep(p) {
    margin: 0 0 8px;
  }

  :deep(p:last-child) {
    margin-bottom: 0;
  }

  :deep(img) {
    max-width: 100%;
    border-radius: 4px;
  }
}

/* 评论列表 */
.empty-comments {
  text-align: center;
  color: #c0c4cc;
  padding: 24px 0;
  font-size: 13px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.comment-item {
  border-radius: 8px;
  background: #f8fafc;
  padding: 12px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #1675d1;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.comment-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.comment-user {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.comment-time {
  font-size: 11px;
  color: #909399;
}

.comment-body {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  word-break: break-word;

  :deep(p) {
    margin: 0 0 8px;
  }

  :deep(p:last-child) {
    margin-bottom: 0;
  }

  :deep(img) {
    max-width: 100%;
    border-radius: 4px;
  }

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 8px 0;
  }

  :deep(th),
  :deep(td) {
    border: 1px solid #e5e6eb;
    padding: 6px 8px;
  }
}

.comment-body-plain {
  white-space: pre-wrap;
}

/* 加载/错误状态 */
.center-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  color: #909399;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e4e7ed;
  border-top-color: #1675d1;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-state {
  color: #f56c6c;
}

.error-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.error-text {
  font-size: 15px;
  font-weight: 500;
  margin: 0 0 6px;
}

.error-hint {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

/* 底部 */
.page-footer {
  padding: 16px;
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

/* 手机端适配 */
@media (max-width: 480px) {
  .page-content {
    padding: 12px;
  }

  .card {
    border-radius: 8px;
    padding: 14px;
  }

  .ticket-title {
    font-size: 16px;
  }

  .info-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }
}
</style>
