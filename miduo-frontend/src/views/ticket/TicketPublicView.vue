<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { getPublicTicketDetail } from '@/api/ticket'
import type { TicketPublicDetailOutput } from '@/types/ticket'
import { filterHttpImageUrls, parseProblemScreenshotUrls } from '@/utils/problem-screenshot-urls'
import { formatTicketDescriptionForDisplay } from '@/utils/ticket-description-display'

const route = useRoute()
const loading = ref(false)
const error = ref('')
const detail = ref<TicketPublicDetailOutput>()

const descriptionDisplayHtml = computed(() => formatTicketDescriptionForDisplay(detail.value?.description))
const archivedBugReport = computed(() => detail.value?.archivedBugReport)

/** 与系统内工单详情同域，便于企微里一键打开处理页（需登录） */
const internalTicketHref = computed(() => {
  const id = detail.value?.id
  if (id == null) return ''
  if (typeof window === 'undefined') return `/ticket/detail/${id}`
  const origin = window.location.origin.replace(/\/$/, '')
  return `${origin}/ticket/detail/${id}`
})

/** 与后台客服区「问题截图」字段同源，仅解析该字段，不包含附件区 */
const problemScreenshotUrls = computed(() => {
  const raw = detail.value?.bugCustomerInfo?.problemScreenshot
  return filterHttpImageUrls(parseProblemScreenshotUrls(raw))
})

const lightboxUrls = ref<string[]>([])
const lightboxIndex = ref(0)
const lightboxScale = ref(1)

const lightboxVisible = computed(() => lightboxUrls.value.length > 0)
const lightboxCurrentSrc = computed(() => {
  const list = lightboxUrls.value
  const i = lightboxIndex.value
  if (list.length === 0 || i < 0 || i >= list.length) {
    return ''
  }
  return list[i] ?? ''
})

const lightboxCanPrev = computed(() => lightboxIndex.value > 0)
const lightboxCanNext = computed(() => lightboxIndex.value < lightboxUrls.value.length - 1)
const lightboxShowNav = computed(() => lightboxUrls.value.length > 1)

function openLightboxGallery(urls: string[], startIndex: number): void {
  if (urls.length === 0) {
    return
  }
  const i = Math.min(Math.max(0, startIndex), urls.length - 1)
  lightboxUrls.value = urls
  lightboxIndex.value = i
  lightboxScale.value = 1
}

function openLightboxSingle(src: string): void {
  if (src) {
    openLightboxGallery([src], 0)
  }
}

function closeLightbox(): void {
  lightboxUrls.value = []
  lightboxIndex.value = 0
  lightboxScale.value = 1
}

function lightboxPrev(): void {
  if (lightboxCanPrev.value) {
    lightboxIndex.value -= 1
    lightboxScale.value = 1
  }
}

function lightboxNext(): void {
  if (lightboxCanNext.value) {
    lightboxIndex.value += 1
    lightboxScale.value = 1
  }
}

function lightboxZoomIn(): void {
  lightboxScale.value = Math.min(3, Math.round((lightboxScale.value + 0.25) * 100) / 100)
}

function lightboxZoomOut(): void {
  lightboxScale.value = Math.max(0.5, Math.round((lightboxScale.value - 0.25) * 100) / 100)
}

function handleCommentImageClick(event: MouseEvent): void {
  const target = event.target as HTMLElement
  if (target.tagName === 'IMG') {
    const src = (target as HTMLImageElement).src
    if (src) {
      openLightboxSingle(src)
    }
  }
}

function handleLightboxKeydown(event: KeyboardEvent): void {
  if (!lightboxVisible.value) {
    return
  }
  if (event.key === 'Escape') {
    closeLightbox()
    return
  }
  if (lightboxShowNav.value) {
    if (event.key === 'ArrowLeft') {
      event.preventDefault()
      lightboxPrev()
      return
    }
    if (event.key === 'ArrowRight') {
      event.preventDefault()
      lightboxNext()
      return
    }
  }
  if (event.key === '+' || event.key === '=') {
    event.preventDefault()
    lightboxZoomIn()
    return
  }
  if (event.key === '-' || event.key === '_') {
    event.preventDefault()
    lightboxZoomOut()
  }
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleLightboxKeydown)
})

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

/** Bug 简报内「开始/临时解决/彻底解决」为日期口径，只展示到日，避免 00:00 造成误解 */
function formatDateOnly(dateStr?: string): string {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    if (Number.isNaN(d.getTime())) return '-'
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  } catch {
    return '-'
  }
}

function hasText(value?: string | null): boolean {
  return value != null && value.trim().length > 0
}

const visibleArchivedBugSections = computed(() => {
  const summary = archivedBugReport.value
  if (!summary) {
    return []
  }
  return [
    {
      key: 'introducedProject',
      title: '引入项目',
      value: summary.introducedProject,
    },
    {
      key: 'impactScope',
      title: '影响范围',
      value: summary.impactScope,
    },
    {
      key: 'problemDesc',
      title: '问题描述',
      value: summary.problemDesc,
    },
    {
      key: 'logicCause',
      title: '逻辑归因',
      value: [summary.logicCauseLevel1, summary.logicCauseLevel2, summary.logicCauseDetail]
        .filter(hasText)
        .join(' / '),
    },
    {
      key: 'solution',
      title: '解决方案',
      value: summary.solution,
    },
    {
      key: 'tempSolution',
      title: '临时方案',
      value: summary.tempSolution,
    },
  ].filter((item) => hasText(item.value))
})

function isOperationLog(type?: string): boolean {
  return type === 'OPERATION'
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

onMounted(() => {
  loadDetail()
  document.addEventListener('keydown', handleLightboxKeydown)
})
</script>

<template>
  <div class="public-ticket-page">
    <header class="page-header">
      <div class="brand">
        <span class="brand-icon">🎫</span>
        <span class="brand-name">工单详情</span>
      </div>
      <a
        v-if="internalTicketHref"
        class="internal-ticket-link"
        :href="internalTicketHref"
        target="_blank"
        rel="noopener noreferrer"
      >
        去系统处理
      </a>
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
        <div v-if="archivedBugReport" class="card archived-bug-report-card">
          <div class="archived-bug-report-head">
            <div class="archived-bug-report-title-wrap">
              <div class="archived-bug-report-label">归档 Bug 简报</div>
              <h2 class="archived-bug-report-title">
                {{ archivedBugReport.reportNo || '已归档简报' }}
              </h2>
            </div>
            <span class="archived-bug-report-status">
              {{ archivedBugReport.statusLabel || archivedBugReport.status || '已归档' }}
            </span>
          </div>
          <div class="archived-bug-report-meta">
            <span v-if="archivedBugReport.defectCategory">缺陷分类：{{ archivedBugReport.defectCategory }}</span>
            <span v-if="archivedBugReport.severityLevel">严重级别：{{ archivedBugReport.severityLevel }}</span>
            <span v-if="archivedBugReport.responsibleUserNames">责任人：{{ archivedBugReport.responsibleUserNames }}</span>
            <span v-if="hasText(archivedBugReport.reporterName)">反馈人：{{ archivedBugReport.reporterName }}</span>
            <span v-if="archivedBugReport.startDate">开始时间：{{ formatDateOnly(archivedBugReport.startDate) }}</span>
            <span v-if="archivedBugReport.tempResolveDate">临时解决时间：{{ formatDateOnly(archivedBugReport.tempResolveDate) }}</span>
            <span v-if="archivedBugReport.resolveDate">彻底解决日期：{{ formatDateOnly(archivedBugReport.resolveDate) }}</span>
            <span>归档时间：{{ formatDate(archivedBugReport.reviewedAt || archivedBugReport.updateTime) }}</span>
          </div>
          <div class="archived-bug-report-body">
            <div
              v-for="section in visibleArchivedBugSections"
              :key="section.key"
              class="archived-bug-report-section"
            >
              <span class="archived-bug-report-section-title">{{ section.title }}</span>
              <p class="archived-bug-report-section-value">{{ section.value }}</p>
            </div>
          </div>
        </div>

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
            <div class="info-text-block">{{ detail.bugCustomerInfo.problemDesc }}</div>
          </div>
          <div v-if="detail.bugCustomerInfo.expectedResult" class="info-block-full">
            <span class="info-label">预期结果</span>
            <div class="info-text-block">{{ detail.bugCustomerInfo.expectedResult }}</div>
          </div>
          <div class="info-block-full customer-problem-screenshots">
            <span class="info-label">问题截图</span>
            <div v-if="problemScreenshotUrls.length > 0" class="screenshot-thumb-row">
              <button
                v-for="(url, idx) in problemScreenshotUrls"
                :key="idx"
                type="button"
                class="screenshot-thumb-btn"
                :title="'点击查看第 ' + (idx + 1) + ' 张'"
                @click="openLightboxGallery(problemScreenshotUrls, idx)"
              >
                <img :src="url" class="screenshot-thumb" alt="问题截图缩略图" />
              </button>
            </div>
            <div v-else class="info-text-block info-text-muted">—</div>
          </div>
        </div>

        <!-- 工单描述 -->
        <div v-if="descriptionDisplayHtml" class="card desc-card">
          <h2 class="card-title">工单描述</h2>
          <!-- eslint-disable-next-line vue/no-v-html -->
          <div class="desc-content" v-html="descriptionDisplayHtml" />
        </div>

        <!-- 处理记录 -->
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
              <div v-if="isOperationLog(comment.type)" class="comment-body comment-body-plain">
                {{ comment.content }}
              </div>
              <!-- eslint-disable-next-line vue/no-v-html -->
              <div v-else class="comment-body" v-html="comment.content || '-'" @click="handleCommentImageClick" />
            </div>
          </div>
        </div>
      </template>
    </main>

    <footer class="page-footer">
      <p>米多工单系统 · 此页面无需登录即可访问</p>
    </footer>
  </div>

  <!-- 图片预览灯箱（处理记录单图 / 问题截图多图） -->
  <Teleport to="body">
    <div v-if="lightboxVisible" class="lightbox-overlay" @click.self="closeLightbox">
      <button type="button" class="lightbox-close" aria-label="关闭" @click="closeLightbox">✕</button>
      <div v-if="lightboxShowNav" class="lightbox-toolbar">
        <button type="button" class="lightbox-tool-btn" :disabled="!lightboxCanPrev" @click="lightboxPrev">
          上一张
        </button>
        <span class="lightbox-counter">{{ lightboxIndex + 1 }} / {{ lightboxUrls.length }}</span>
        <button type="button" class="lightbox-tool-btn" :disabled="!lightboxCanNext" @click="lightboxNext">
          下一张
        </button>
        <span class="lightbox-toolbar-sep" />
        <button type="button" class="lightbox-tool-btn" @click="lightboxZoomOut">缩小</button>
        <button type="button" class="lightbox-tool-btn" @click="lightboxZoomIn">放大</button>
      </div>
      <div v-else class="lightbox-toolbar lightbox-toolbar-single">
        <button type="button" class="lightbox-tool-btn" @click="lightboxZoomOut">缩小</button>
        <button type="button" class="lightbox-tool-btn" @click="lightboxZoomIn">放大</button>
      </div>
      <div class="lightbox-stage">
        <button
          v-if="lightboxShowNav && lightboxCanPrev"
          type="button"
          class="lightbox-side lightbox-side-left"
          aria-label="上一张"
          @click.stop="lightboxPrev"
        >
          ‹
        </button>
        <img
          :src="lightboxCurrentSrc"
          class="lightbox-img"
          alt="图片预览"
          :style="{ transform: `scale(${lightboxScale})` }"
        />
        <button
          v-if="lightboxShowNav && lightboxCanNext"
          type="button"
          class="lightbox-side lightbox-side-right"
          aria-label="下一张"
          @click.stop="lightboxNext"
        >
          ›
        </button>
      </div>
    </div>
  </Teleport>
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.internal-ticket-link {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #1675d1;
  background: #fff;
  text-decoration: none;
  white-space: nowrap;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
}

.internal-ticket-link:active {
  opacity: 0.92;
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

.archived-bug-report-card {
  border: 2px solid #1675d1;
  box-shadow: 0 8px 20px rgba(22, 117, 209, 0.16);
  background: linear-gradient(145deg, #eef6ff 0%, #ffffff 60%);
}

.archived-bug-report-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.archived-bug-report-label {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  color: #1675d1;
  background: rgba(22, 117, 209, 0.12);
  margin-bottom: 6px;
}

.archived-bug-report-title {
  margin: 0;
  font-size: 20px;
  line-height: 1.35;
  color: #0f3f7a;
}

.archived-bug-report-status {
  align-self: flex-start;
  white-space: nowrap;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: #1675d1;
}

.archived-bug-report-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-bottom: 10px;
  color: #4e5969;
  font-size: 13px;
}

.archived-bug-report-body {
  display: grid;
  gap: 8px;
}

.archived-bug-report-section {
  background: #f7fbff;
  border: 1px solid #d5e9ff;
  border-radius: 8px;
  padding: 10px 12px;
}

.archived-bug-report-section-title {
  display: inline-block;
  color: #1675d1;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 4px;
}

.archived-bug-report-section-value {
  margin: 0;
  color: #1f2937;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
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

/* 问题截图（缩略图，与后台同一数据源 problemScreenshot） */
.customer-problem-screenshots {
  margin-top: 12px;
}

.info-text-muted {
  color: #909399;
  text-align: center;
}

.screenshot-thumb-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 6px;
}

.screenshot-thumb-btn {
  padding: 0;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #fff;
  cursor: zoom-in;
  overflow: hidden;
  flex-shrink: 0;
  transition: box-shadow 0.15s, border-color 0.15s;
}

.screenshot-thumb-btn:hover {
  border-color: #1675d1;
  box-shadow: 0 2px 8px rgba(22, 117, 209, 0.2);
}

.screenshot-thumb {
  display: block;
  width: 96px;
  height: 72px;
  object-fit: cover;
}

/* 问题描述 */
.desc-content {
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  word-break: break-word;
  overflow: hidden;
  white-space: pre-line;

  :deep(p) {
    margin: 0 0 8px;
    max-width: 100%;
  }

  :deep(p:last-child) {
    margin-bottom: 0;
  }

  :deep(img) {
    display: block !important;
    max-width: 100% !important;
    width: auto !important;
    height: auto !important;
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
  overflow: hidden;
  max-width: 100%;
  box-sizing: border-box;
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
  overflow: hidden;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;

  :deep(p) {
    margin: 0 0 8px;
  }

  :deep(p:last-child) {
    margin-bottom: 0;
  }

  :deep(p),
  :deep(div) {
    max-width: 100%;
    overflow-wrap: break-word;
  }

  :deep(img) {
    display: block !important;
    max-width: 100% !important;
    width: 100% !important;
    height: auto !important;
    border-radius: 4px;
    cursor: zoom-in;
    transition: opacity 0.15s;
    object-fit: contain;

    &:hover {
      opacity: 0.85;
    }
  }

  :deep(figure) {
    display: block !important;
    max-width: 100% !important;
    width: 100% !important;
    margin: 0 0 8px !important;
    overflow: hidden;
    box-sizing: border-box;
  }

  :deep(figure img) {
    display: block !important;
    max-width: 100% !important;
    width: 100% !important;
    height: auto !important;
    object-fit: contain;
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

  .lightbox-side-left {
    left: 4px;
  }

  .lightbox-side-right {
    right: 4px;
  }

  .card {
    border-radius: 8px;
    padding: 14px;
  }

  .ticket-title {
    font-size: 16px;
  }

  .archived-bug-report-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .archived-bug-report-title {
    font-size: 18px;
  }

  .info-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }
}

/* 图片预览灯箱 */
.lightbox-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56px 16px 24px;
  gap: 12px;
}

.lightbox-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px 12px;
  z-index: 10001;
}

.lightbox-toolbar-single {
  position: fixed;
  top: 56px;
  left: 50%;
  transform: translateX(-50%);
}

.lightbox-tool-btn {
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.35);
  color: #fff;
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.lightbox-tool-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.22);
}

.lightbox-tool-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.lightbox-counter {
  color: #e5e7eb;
  font-size: 13px;
  min-width: 4.5em;
  text-align: center;
}

.lightbox-toolbar-sep {
  width: 1px;
  height: 20px;
  background: rgba(255, 255, 255, 0.25);
}

.lightbox-stage {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  width: 100%;
  max-height: calc(100vh - 120px);
  min-height: 0;
}

.lightbox-img {
  max-width: min(100%, 1200px);
  max-height: calc(100vh - 140px);
  width: auto;
  height: auto;
  border-radius: 6px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.5);
  object-fit: contain;
  user-select: none;
  transform-origin: center center;
  transition: transform 0.12s ease-out;
}

.lightbox-side {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 72px;
  border: none;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 28px;
  line-height: 1;
  cursor: pointer;
  z-index: 10001;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}

.lightbox-side:hover {
  background: rgba(255, 255, 255, 0.22);
}

.lightbox-side-left {
  left: 8px;
}

.lightbox-side-right {
  right: 8px;
}

.lightbox-close {
  position: fixed;
  top: 16px;
  right: 20px;
  z-index: 10002;
  background: rgba(255, 255, 255, 0.15);
  border: none;
  color: #fff;
  font-size: 20px;
  line-height: 1;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}

.lightbox-close:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>

<!-- 无 scoped：覆盖富文本里 img 的 width 等内联属性，避免评论图只占一窄条 -->
<style>
.public-ticket-page .comment-body img {
  width: 100% !important;
  max-width: 100% !important;
  height: auto !important;
  box-sizing: border-box;
}
</style>
