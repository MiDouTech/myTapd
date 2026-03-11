<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { getPublicTicketDetail } from '@/api/ticket'
import type { TicketPublicDetailOutput } from '@/types/ticket'

const route = useRoute()
const loading = ref(false)
const error = ref('')
const detail = ref<TicketPublicDetailOutput>()

const priorityColorMap: Record<string, string> = {
  urgent: '#f56c6c',
  high: '#e6a23c',
  medium: '#409eff',
  low: '#67c23a',
}

const statusColorMap: Record<string, string> = {
  OPEN: '#409eff',
  PENDING_DISPATCH: '#909399',
  PROCESSING: '#e6a23c',
  PENDING_ACCEPT: '#1675d1',
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

        <!-- 工单基本信息 -->
        <div class="card info-card">
          <h2 class="card-title">基本信息</h2>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">分类</span>
              <span class="info-value">{{ detail.categoryFullPath || detail.categoryName || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">来源</span>
              <span class="info-value">{{ detail.sourceLabel || detail.source || '-' }}</span>
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

        <!-- 问题描述 -->
        <div v-if="detail.description" class="card desc-card">
          <h2 class="card-title">问题描述</h2>
          <div class="desc-content">{{ detail.description }}</div>
        </div>

        <!-- 评论记录 -->
        <div class="card comments-card">
          <h2 class="card-title">处理记录</h2>
          <div v-if="!detail.comments || detail.comments.length === 0" class="empty-comments">
            暂无处理记录
          </div>
          <div v-else class="comment-list">
            <div
              v-for="comment in detail.comments"
              :key="comment.id"
              class="comment-item"
            >
              <div class="comment-header">
                <div class="comment-avatar">{{ ((comment.userName || '?')[0] ?? '?').toUpperCase() }}</div>
                <div class="comment-meta">
                  <span class="comment-user">{{ comment.userName || '系统' }}</span>
                  <span class="comment-time">{{ formatDate(comment.createTime) }}</span>
                </div>
              </div>
              <div class="comment-body">{{ comment.content }}</div>
            </div>
          </div>
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

/* 问题描述 */
.desc-content {
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
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
  white-space: pre-wrap;
  word-break: break-word;
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
