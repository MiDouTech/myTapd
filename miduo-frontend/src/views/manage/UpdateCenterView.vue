<script setup lang="ts">
/* eslint-disable vue/no-v-html -- 周报 Markdown 已在 renderMarkdown 中先 escapeHtml，再生成受控 HTML 标签。 */
import { computed, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import {
  Calendar,
  CollectionTag,
  DocumentChecked,
  Files,
  Link,
  Promotion,
  Refresh,
  Tickets,
} from '@element-plus/icons-vue'

import { useUpdateCenterStore } from '@/stores/updateCenter'
import type {
  ChangelogDayOutput,
  ChangelogEntryOutput,
  ChangelogFragmentOutput,
  ChangelogReleaseOutput,
  WeeklyReportSummaryOutput,
} from '@/types/updateCenter'
import { notifyError, notifySuccess } from '@/utils/feedback'
import { formatDateTime } from '@/utils/formatter'

interface TypeMeta {
  label: string
  tagType: 'success' | 'warning' | 'primary' | 'info' | 'danger'
}

interface TypeOption {
  value: string
  label: string
  count: number
}

const TYPE_META: Record<string, TypeMeta> = {
  feat: { label: '新功能', tagType: 'success' },
  fix: { label: '修复', tagType: 'warning' },
  perf: { label: '优化', tagType: 'primary' },
  refactor: { label: '重构', tagType: 'info' },
  docs: { label: '文档', tagType: 'primary' },
  chore: { label: '杂项', tagType: 'info' },
  test: { label: '测试', tagType: 'success' },
  ci: { label: '构建', tagType: 'info' },
  build: { label: '构建', tagType: 'info' },
  release: { label: '发布', tagType: 'warning' },
  security: { label: '安全', tagType: 'danger' },
  ops: { label: '运维', tagType: 'warning' },
  style: { label: '样式', tagType: 'primary' },
  polish: { label: '润色', tagType: 'primary' },
  rule: { label: '规范', tagType: 'danger' },
  merge: { label: '合并', tagType: 'info' },
  revert: { label: '回滚', tagType: 'danger' },
}

const TYPE_ORDER = [
  'feat',
  'fix',
  'perf',
  'refactor',
  'docs',
  'chore',
  'test',
  'ci',
  'build',
  'release',
  'security',
  'ops',
  'style',
  'polish',
  'rule',
  'merge',
  'revert',
]

const updateCenterStore = useUpdateCenterStore()
const {
  currentWeek,
  releases,
  githubLogs,
  weeklyReports,
  weeklyReportDetails,
  unreadCount,
  loadingCurrent,
  loadingReleases,
  loadingMoreFragments,
  loadingReleaseVersions,
  loadingGitHubLogs,
  loadingMoreGitHubLogs,
  loadingWeeklyReports,
  loadingWeeklyReportDetails,
} = storeToRefs(updateCenterStore)

const primaryTab = ref<'update_center' | 'weekly_reports' | 'ai_news'>('update_center')
const activeTab = ref<'releases' | 'fragments' | 'github_logs'>('releases')
const selectedType = ref('all')
const refreshing = ref(false)
const selectedWeeklyReportFile = ref('')

const typeOptions = computed<TypeOption[]>(() => {
  const counter = new Map<string, number>()
  for (const entry of allChangelogEntries.value) {
    const key = normalizeType(entry.type)
    counter.set(key, (counter.get(key) || 0) + 1)
  }
  const sortedKeys = Array.from(counter.keys()).sort((a, b) => {
    const left = TYPE_ORDER.indexOf(a)
    const right = TYPE_ORDER.indexOf(b)
    if (left === -1 && right === -1) {
      return a.localeCompare(b)
    }
    if (left === -1) {
      return 1
    }
    if (right === -1) {
      return -1
    }
    return left - right
  })
  return [
    {
      value: 'all',
      label: '全部',
      count: allChangelogEntries.value.length,
    },
    ...sortedKeys.map((key) => ({
      value: key,
      label: getTypeLabel(key),
      count: counter.get(key) || 0,
    })),
  ]
})

const allChangelogEntries = computed<ChangelogEntryOutput[]>(() => {
  const entries: ChangelogEntryOutput[] = []
  for (const release of releases.value?.releases || []) {
    for (const day of release.days || []) {
      entries.push(...(day.entries || []))
    }
  }
  for (const fragment of currentWeek.value?.fragments || []) {
    entries.push(...(fragment.entries || []))
  }
  return entries
})

const filteredFragments = computed<ChangelogFragmentOutput[]>(() =>
  (currentWeek.value?.fragments || [])
    .map((fragment) => ({
      ...fragment,
      entries: filterEntries(fragment.entries),
    }))
    .filter((fragment) => fragment.entries.length > 0),
)

const totalReleaseCount = computed(() => releases.value?.totalReleases || 0)
const totalReleaseEntryCount = computed(() => releases.value?.totalEntries || 0)
const totalFragmentEntryCount = computed(() => currentWeek.value?.totalEntries || 0)
const totalGitCount = computed(
  () => githubLogs.value?.repoTotalCommitCount || githubLogs.value?.totalCount || 0,
)
const weeklyReportList = computed<WeeklyReportSummaryOutput[]>(
  () => weeklyReports.value?.reports || [],
)
const selectedWeeklyReportDetail = computed(() =>
  selectedWeeklyReportFile.value
    ? weeklyReportDetails.value[selectedWeeklyReportFile.value] || null
    : null,
)
const renderedWeeklyReport = computed(() =>
  renderMarkdown(selectedWeeklyReportDetail.value?.content || ''),
)
const weeklyReportOutline = computed(() =>
  extractMarkdownOutline(selectedWeeklyReportDetail.value?.content || ''),
)
const latestFetchedAt = computed(() => {
  const values = [
    currentWeek.value?.fetchedAt,
    releases.value?.fetchedAt,
    githubLogs.value?.fetchedAt,
    weeklyReports.value?.fetchedAt,
  ].filter(Boolean) as string[]
  const sortedValues = values.sort()
  return sortedValues.length > 0 ? sortedValues[sortedValues.length - 1] : ''
})

onMounted(async () => {
  await loadInitialData()
  updateCenterStore.markAsSeen()
})

async function loadInitialData(): Promise<void> {
  await Promise.all([
    updateCenterStore.loadCurrentWeek(),
    updateCenterStore.loadReleases({ summary: true }),
    updateCenterStore.loadGitHubLogs(),
    updateCenterStore.loadWeeklyReports(),
  ])
  await ensureReleaseDetails()
  await ensureWeeklyReportSelection()
}

async function ensureReleaseDetails(): Promise<void> {
  const releaseList = releases.value?.releases || []
  await Promise.all(
    releaseList
      .filter((release) => release.entriesOmitted)
      .slice(0, 4)
      .map((release) => updateCenterStore.loadReleaseDetail(release.version)),
  )
}

async function handleRefresh(): Promise<void> {
  refreshing.value = true
  try {
    await updateCenterStore.refreshAll()
    await ensureReleaseDetails()
    await ensureWeeklyReportSelection(true)
    notifySuccess('更新中心已刷新')
  } catch {
    notifyError('刷新更新中心失败')
  } finally {
    refreshing.value = false
  }
}

async function ensureWeeklyReportSelection(force = false): Promise<void> {
  const reports = weeklyReportList.value
  if (!reports.length) {
    selectedWeeklyReportFile.value = ''
    return
  }
  if (
    !selectedWeeklyReportFile.value ||
    !reports.some((item) => item.fileName === selectedWeeklyReportFile.value)
  ) {
    selectedWeeklyReportFile.value = reports[0]?.fileName || ''
  }
  await updateCenterStore.loadWeeklyReportDetail(selectedWeeklyReportFile.value, force)
}

async function handleLoadReleaseDetail(release: ChangelogReleaseOutput): Promise<void> {
  await updateCenterStore.loadReleaseDetail(release.version)
}

function normalizeType(type: string): string {
  return String(type || '')
    .trim()
    .toLowerCase()
}

function getTypeLabel(type: string): string {
  return TYPE_META[normalizeType(type)]?.label || type || '其他'
}

function getTypeTagType(type: string): TypeMeta['tagType'] {
  return TYPE_META[normalizeType(type)]?.tagType || 'info'
}

function filterEntries(entries: ChangelogEntryOutput[] = []): ChangelogEntryOutput[] {
  if (selectedType.value === 'all') {
    return entries
  }
  return entries.filter((entry) => normalizeType(entry.type) === selectedType.value)
}

function filteredReleaseDays(release: ChangelogReleaseOutput): ChangelogDayOutput[] {
  return (release.days || [])
    .map((day) => ({
      ...day,
      entries: filterEntries(day.entries),
    }))
    .filter((day) => day.entries.length > 0)
}

function hasVisibleReleaseEntries(release: ChangelogReleaseOutput): boolean {
  if (release.entriesOmitted) {
    return true
  }
  return filteredReleaseDays(release).length > 0
}

function formatSourceLabel(source?: string): string {
  if (source === 'local') {
    return '本地仓库'
  }
  if (source === 'github') {
    return 'GitHub'
  }
  return '暂无数据源'
}

function handleSelectWeeklyReport(fileName: string): void {
  selectedWeeklyReportFile.value = fileName
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function renderInlineMarkdown(value: string): string {
  let result = escapeHtml(value)
  result = result.replace(/`([^`]+)`/g, '<code>$1</code>')
  result = result.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  return result
}

function renderMarkdown(markdown: string): string {
  if (!markdown) {
    return ''
  }
  const lines = markdown.split(/\r?\n/)
  const html: string[] = []
  let inCodeBlock = false
  let codeLines: string[] = []
  let inTable = false

  const closeTable = () => {
    if (inTable) {
      html.push('</tbody></table>')
      inTable = false
    }
  }

  for (const rawLine of lines) {
    const line = rawLine.trimEnd()
    if (line.startsWith('```')) {
      if (inCodeBlock) {
        html.push(`<pre><code>${escapeHtml(codeLines.join('\n'))}</code></pre>`)
        codeLines = []
        inCodeBlock = false
      } else {
        closeTable()
        inCodeBlock = true
      }
      continue
    }

    if (inCodeBlock) {
      codeLines.push(rawLine)
      continue
    }

    if (!line.trim()) {
      closeTable()
      continue
    }

    if (line === '---') {
      closeTable()
      html.push('<hr />')
      continue
    }

    const heading = line.match(/^(#{1,4})\s+(.+)$/)
    if (heading) {
      closeTable()
      const level = heading[1]?.length || 1
      const headingText = heading[2] || ''
      html.push(`<h${level}>${renderInlineMarkdown(headingText)}</h${level}>`)
      continue
    }

    if (line.startsWith('>')) {
      closeTable()
      html.push(`<blockquote>${renderInlineMarkdown(line.replace(/^>\s?/, ''))}</blockquote>`)
      continue
    }

    if (line.startsWith('|') && line.endsWith('|')) {
      const cells = line
        .slice(1, -1)
        .split('|')
        .map((cell) => cell.trim())
      const isDivider = cells.every((cell) => /^:?-{3,}:?$/.test(cell))
      if (isDivider) {
        continue
      }
      if (!inTable) {
        html.push('<table><tbody>')
        inTable = true
      }
      html.push(
        `<tr>${cells.map((cell) => `<td>${renderInlineMarkdown(cell)}</td>`).join('')}</tr>`,
      )
      continue
    }

    const listItem = line.match(/^[-*]\s+(.+)$/)
    if (listItem) {
      closeTable()
      html.push(`<p class="weekly-list-item">• ${renderInlineMarkdown(listItem[1] || '')}</p>`)
      continue
    }

    closeTable()
    html.push(`<p>${renderInlineMarkdown(line)}</p>`)
  }

  closeTable()
  if (inCodeBlock) {
    html.push(`<pre><code>${escapeHtml(codeLines.join('\n'))}</code></pre>`)
  }
  return html.join('')
}

function extractMarkdownOutline(markdown: string): Array<{ level: number; title: string }> {
  if (!markdown) {
    return []
  }
  return markdown
    .split(/\r?\n/)
    .map((line) => {
      const match = line.trim().match(/^(#{1,3})\s+(.+)$/)
      if (!match) {
        return null
      }
      return {
        level: match[1]?.length || 1,
        title: match[2]?.replace(/\*\*/g, '').trim() || '',
      }
    })
    .filter((item): item is { level: number; title: string } => Boolean(item?.title))
}

watch(weeklyReportList, () => {
  void ensureWeeklyReportSelection()
})

watch(selectedWeeklyReportFile, (fileName) => {
  if (fileName) {
    void updateCenterStore.loadWeeklyReportDetail(fileName)
  }
})
</script>

<template>
  <div class="update-center-page">
    <section class="update-hero">
      <div>
        <el-tag class="hero-tag" type="primary" effect="plain">管理 / 更新中心</el-tag>
        <h1>更新中心</h1>
        <p>像公告栏一样，把仓库里的版本日志、待发布变更和 Git 提交整理到一起。</p>
      </div>
      <el-button type="primary" :icon="Refresh" :loading="refreshing" @click="handleRefresh">
        刷新
      </el-button>
    </section>

    <el-card shadow="never" class="section-card">
      <el-tabs v-model="primaryTab" class="primary-tabs">
        <el-tab-pane label="更新中心" name="update_center">
          <section class="stat-grid">
            <el-card shadow="never" class="stat-card">
              <el-icon><DocumentChecked /></el-icon>
              <div>
                <span>已发布版本</span>
                <strong>{{ totalReleaseCount }}</strong>
                <small>{{ totalReleaseEntryCount }} 条更新</small>
              </div>
            </el-card>
            <el-card shadow="never" class="stat-card">
              <el-icon><Tickets /></el-icon>
              <div>
                <span>待发布更新</span>
                <strong>{{ totalFragmentEntryCount }}</strong>
                <small>{{ currentWeek?.totalDays || 0 }} 个日期组</small>
              </div>
            </el-card>
            <el-card shadow="never" class="stat-card">
              <el-icon><Promotion /></el-icon>
              <div>
                <span>Git 提交</span>
                <strong>{{ totalGitCount }}</strong>
                <small>{{ githubLogs?.logs.length || 0 }} 条已加载</small>
              </div>
            </el-card>
            <el-card shadow="never" class="stat-card">
              <el-icon><CollectionTag /></el-icon>
              <div>
                <span>未读提醒</span>
                <strong>{{ unreadCount }}</strong>
                <small>进入页面后自动标记已读</small>
              </div>
            </el-card>
          </section>

          <el-card shadow="never" class="filter-card">
            <div class="filter-row">
              <span class="filter-title">筛选</span>
              <el-radio-group v-model="selectedType" size="small">
                <el-radio-button
                  v-for="option in typeOptions"
                  :key="option.value"
                  :label="option.value"
                >
                  {{ option.label }} {{ option.count }}
                </el-radio-button>
              </el-radio-group>
            </div>
            <div class="source-line">
              <span>待发布：{{ formatSourceLabel(currentWeek?.source) }}</span>
              <span>已发布：{{ formatSourceLabel(releases?.source) }}</span>
              <span>周报：{{ formatSourceLabel(weeklyReports?.source) }}</span>
              <span>Git：{{ formatSourceLabel(githubLogs?.source) }}</span>
              <span>最近刷新：{{ formatDateTime(latestFetchedAt) }}</span>
            </div>
          </el-card>

          <el-card shadow="never" class="content-card">
            <el-tabs v-model="activeTab" class="content-tabs">
              <el-tab-pane label="已发布" name="releases">
                <div v-loading="loadingReleases" class="release-list">
                  <el-empty
                    v-if="!releases?.dataSourceAvailable || !releases.releases.length"
                    description="暂无已发布更新，请先维护 CHANGELOG.md"
                  />
                  <el-card
                    v-for="release in releases?.releases || []"
                    v-show="hasVisibleReleaseEntries(release)"
                    :key="release.version"
                    shadow="never"
                    class="release-card"
                  >
                    <div class="release-head">
                      <div>
                        <h3>{{ release.version }}</h3>
                        <p>
                          <el-icon><Calendar /></el-icon>
                          {{ release.releaseDate || '未填写发布日期' }}
                          · {{ release.entryCount || 0 }} 条更新
                        </p>
                      </div>
                      <el-button
                        v-if="release.entriesOmitted"
                        :loading="loadingReleaseVersions[release.version]"
                        size="small"
                        @click="handleLoadReleaseDetail(release)"
                      >
                        加载详情
                      </el-button>
                    </div>

                    <div v-if="release.highlights?.length" class="highlight-list">
                      <el-tag
                        v-for="item in release.highlights"
                        :key="item"
                        type="primary"
                        effect="plain"
                      >
                        {{ item }}
                      </el-tag>
                    </div>

                    <div
                      v-for="day in filteredReleaseDays(release)"
                      :key="day.date"
                      class="day-group"
                    >
                      <h4>{{ day.date }}</h4>
                      <div
                        v-for="entry in day.entries"
                        :key="`${day.date}-${entry.description}`"
                        class="entry-row"
                      >
                        <el-tag :type="getTypeTagType(entry.type)" effect="light">
                          {{ getTypeLabel(entry.type) }}
                        </el-tag>
                        <span class="entry-module">{{ entry.module }}</span>
                        <span class="entry-desc">{{ entry.description }}</span>
                      </div>
                    </div>
                  </el-card>
                </div>
              </el-tab-pane>

              <el-tab-pane label="待发布" name="fragments">
                <div v-loading="loadingCurrent" class="fragment-list">
                  <el-empty
                    v-if="!currentWeek?.dataSourceAvailable || !filteredFragments.length"
                    description="暂无待发布更新，请在 changelogs/ 下新增更新碎片"
                  />
                  <el-card
                    v-for="fragment in filteredFragments"
                    :key="fragment.fileName"
                    shadow="never"
                    class="fragment-card"
                  >
                    <div class="fragment-head">
                      <h3>{{ fragment.date }}</h3>
                      <el-tag type="info" effect="plain">{{ fragment.fileName }}</el-tag>
                    </div>
                    <div
                      v-for="entry in fragment.entries"
                      :key="`${fragment.fileName}-${entry.description}`"
                      class="entry-row"
                    >
                      <el-tag :type="getTypeTagType(entry.type)" effect="light">
                        {{ getTypeLabel(entry.type) }}
                      </el-tag>
                      <span class="entry-module">{{ entry.module }}</span>
                      <span class="entry-desc">{{ entry.description }}</span>
                    </div>
                  </el-card>
                  <div v-if="currentWeek?.hasMore" class="load-more">
                    <el-button
                      :loading="loadingMoreFragments"
                      @click="updateCenterStore.loadMoreFragments"
                    >
                      加载更多待发布更新
                    </el-button>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="GitHub提交" name="github_logs">
                <div v-loading="loadingGitHubLogs" class="github-panel">
                  <el-empty
                    v-if="!githubLogs?.dataSourceAvailable || !githubLogs.logs.length"
                    description="暂无 Git 提交记录，请确认运行环境包含 .git 目录"
                  />
                  <el-table
                    v-else
                    :data="githubLogs.logs"
                    :border="false"
                    :stripe="true"
                    :header-cell-style="{ backgroundColor: '#f5f7fa' }"
                  >
                    <el-table-column label="提交" min-width="260" align="center">
                      <template #default="{ row }">
                        <div class="commit-cell">
                          <el-tag type="info" effect="plain">{{ row.shortSha }}</el-tag>
                          <span>{{ row.message }}</span>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column prop="authorName" label="作者" width="150" align="center" />
                    <el-table-column label="提交时间" width="190" align="center">
                      <template #default="{ row }">
                        {{ formatDateTime(row.commitTimeUtc) }}
                      </template>
                    </el-table-column>
                    <el-table-column label="链接" width="120" align="center">
                      <template #default="{ row }">
                        <el-link
                          v-if="row.htmlUrl"
                          :href="row.htmlUrl"
                          target="_blank"
                          type="primary"
                        >
                          查看
                          <el-icon><Link /></el-icon>
                        </el-link>
                        <span v-else>-</span>
                      </template>
                    </el-table-column>
                  </el-table>
                  <div v-if="githubLogs?.hasMore" class="load-more">
                    <el-button
                      :loading="loadingMoreGitHubLogs"
                      @click="updateCenterStore.loadMoreGitHubLogs"
                    >
                      加载更多提交
                    </el-button>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="周报" name="weekly_reports">
          <div v-loading="loadingWeeklyReports" class="weekly-report-panel">
            <el-empty
              v-if="!weeklyReports?.dataSourceAvailable || !weeklyReportList.length"
              description="暂无周报，请在 doc/ 下新增 report.YYYY-Wxx.md"
            />
            <template v-else>
              <aside class="weekly-report-sidebar">
                <div class="weekly-sidebar-title">
                  <strong>周报列表</strong>
                  <span>{{ weeklyReportList.length }} 篇</span>
                </div>
                <button
                  v-for="report in weeklyReportList"
                  :key="report.fileName"
                  type="button"
                  class="weekly-report-item"
                  :class="{ active: selectedWeeklyReportFile === report.fileName }"
                  @click="handleSelectWeeklyReport(report.fileName)"
                >
                  <strong>{{ report.title }}</strong>
                  <span>{{ report.period || report.reportWeek || report.fileName }}</span>
                  <small>{{ report.fileName }}</small>
                </button>
              </aside>
              <article class="weekly-report-content">
                <div class="weekly-report-head">
                  <div>
                    <h3>{{ selectedWeeklyReportDetail?.title || '周报' }}</h3>
                    <p>
                      {{
                        selectedWeeklyReportDetail?.period || selectedWeeklyReportDetail?.reportWeek
                      }}
                      · {{ selectedWeeklyReportDetail?.fileName }}
                    </p>
                  </div>
                  <el-tag type="primary" effect="plain">
                    {{
                      formatSourceLabel(selectedWeeklyReportDetail?.source || weeklyReports?.source)
                    }}
                  </el-tag>
                </div>
                <div
                  v-loading="
                    Boolean(
                      selectedWeeklyReportFile &&
                      loadingWeeklyReportDetails[selectedWeeklyReportFile],
                    )
                  "
                  class="weekly-markdown"
                  v-html="renderedWeeklyReport"
                />
              </article>
              <aside class="weekly-outline">
                <strong>目录</strong>
                <span v-if="!weeklyReportOutline.length">暂无目录</span>
                <p
                  v-for="item in weeklyReportOutline"
                  :key="`${item.level}-${item.title}`"
                  :class="`level-${item.level}`"
                >
                  {{ item.title }}
                </p>
              </aside>
            </template>
          </div>
        </el-tab-pane>

        <el-tab-pane label="AI 大事" name="ai_news">
          <div class="ai-news-placeholder">
            <el-icon><Files /></el-icon>
            <h3>AI 大事</h3>
            <p>暂未接入数据源，后续可接入 AI 新闻、内部智能体更新或行业动态。</p>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.update-center-page {
  min-height: 100%;
  padding: 20px;
  background: #f5f7fb;
}

.update-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
  color: #ffffff;
  background: linear-gradient(135deg, #1675d1 0%, #0f4f96 100%);
  border-radius: 16px;
  box-shadow: 0 12px 28px rgba(22, 117, 209, 0.22);
}

.update-hero h1 {
  margin: 10px 0 6px;
  font-size: 24px;
  font-weight: 700;
}

.update-hero p {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.82);
}

.hero-tag {
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.3);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-card .el-icon {
  width: 42px;
  height: 42px;
  color: #1675d1;
  background: #e8f3ff;
  border-radius: 12px;
}

.stat-card span,
.stat-card small {
  display: block;
  color: #6b7280;
}

.stat-card strong {
  display: block;
  margin: 4px 0;
  font-size: 24px;
  color: #1f2937;
}

.section-card,
.filter-card,
.content-card {
  margin-top: 16px;
  border: none;
  border-radius: 14px;
}

.section-card :deep(.el-card__body) {
  padding-top: 12px;
}

.primary-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.content-tabs {
  margin-top: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  overflow-x: auto;
}

.filter-title {
  flex: 0 0 auto;
  font-weight: 600;
  color: #1f2937;
}

.source-line {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
  font-size: 12px;
  color: #6b7280;
}

.release-list,
.fragment-list,
.github-panel {
  min-height: 240px;
}

.release-card,
.fragment-card {
  margin-bottom: 12px;
  border-radius: 12px;
}

.release-head,
.fragment-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.release-head h3,
.fragment-head h3 {
  margin: 0;
  color: #1f2937;
}

.release-head p {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.highlight-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0;
}

.day-group {
  margin-top: 14px;
}

.day-group h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: #374151;
}

.entry-row {
  display: grid;
  grid-template-columns: 82px 120px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-top: 1px solid #edf0f5;
}

.entry-module {
  color: #4b5563;
  font-weight: 500;
}

.entry-desc {
  color: #1f2937;
  line-height: 1.5;
}

.commit-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 14px 0 2px;
}

.weekly-report-panel {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 220px;
  gap: 16px;
  min-height: 520px;
}

.weekly-report-sidebar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 12px;
  overflow-y: auto;
  border-right: 1px solid #edf0f5;
}

.weekly-sidebar-title,
.weekly-outline {
  color: #6b7280;
  font-size: 12px;
}

.weekly-sidebar-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2px 4px;
}

.weekly-sidebar-title strong,
.weekly-outline strong {
  color: #1f2937;
}

.weekly-report-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  padding: 12px;
  text-align: left;
  cursor: pointer;
  background: #ffffff;
  border: 1px solid #edf0f5;
  border-radius: 12px;
  transition: all 0.18s ease;
}

.weekly-report-item:hover,
.weekly-report-item.active {
  background: #eef6ff;
  border-color: #1675d1;
  box-shadow: 0 8px 20px rgba(22, 117, 209, 0.1);
}

.weekly-report-item strong {
  color: #1f2937;
}

.weekly-report-item span,
.weekly-report-item small {
  color: #6b7280;
}

.weekly-report-content {
  min-width: 0;
  padding: 4px 6px 4px 0;
}

.weekly-outline {
  position: sticky;
  top: 12px;
  align-self: start;
  padding-left: 12px;
  border-left: 1px solid #edf0f5;
}

.weekly-outline p {
  margin: 8px 0;
  color: #4b5563;
}

.weekly-outline .level-2 {
  padding-left: 10px;
}

.weekly-outline .level-3 {
  padding-left: 20px;
  color: #6b7280;
}

.ai-news-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  color: #6b7280;
  text-align: center;
  background: #f8fafc;
  border: 1px dashed #dbe4f0;
  border-radius: 14px;
}

.ai-news-placeholder .el-icon {
  width: 54px;
  height: 54px;
  margin-bottom: 12px;
  color: #1675d1;
  background: #e8f3ff;
  border-radius: 16px;
}

.ai-news-placeholder h3 {
  margin: 0 0 8px;
  color: #1f2937;
}

.ai-news-placeholder p {
  margin: 0;
}

.weekly-report-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  margin-bottom: 14px;
  border-bottom: 1px solid #edf0f5;
}

.weekly-report-head h3 {
  margin: 0;
  color: #1f2937;
}

.weekly-report-head p {
  margin: 6px 0 0;
  color: #6b7280;
}

.weekly-markdown {
  min-height: 420px;
  overflow-x: auto;
  color: #1f2937;
  line-height: 1.75;
}

.weekly-markdown :deep(h1),
.weekly-markdown :deep(h2),
.weekly-markdown :deep(h3),
.weekly-markdown :deep(h4) {
  margin: 18px 0 10px;
  color: #111827;
}

.weekly-markdown :deep(p) {
  margin: 8px 0;
}

.weekly-markdown :deep(blockquote) {
  padding: 10px 14px;
  margin: 12px 0;
  color: #4b5563;
  background: #f5f9ff;
  border-left: 3px solid #1675d1;
  border-radius: 8px;
}

.weekly-markdown :deep(table) {
  width: 100%;
  margin: 12px 0;
  overflow: hidden;
  border-collapse: collapse;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

.weekly-markdown :deep(td) {
  padding: 9px 10px;
  border: 1px solid #e5e7eb;
}

.weekly-markdown :deep(tr:first-child td) {
  font-weight: 600;
  background: #f5f7fa;
}

.weekly-markdown :deep(pre) {
  padding: 12px;
  overflow-x: auto;
  color: #e5e7eb;
  background: #111827;
  border-radius: 10px;
}

.weekly-markdown :deep(code) {
  padding: 2px 5px;
  color: #0f4f96;
  background: #e8f3ff;
  border-radius: 5px;
}

.weekly-markdown :deep(pre code) {
  padding: 0;
  color: inherit;
  background: transparent;
}

@media (max-width: 1024px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .weekly-report-panel {
    grid-template-columns: 1fr;
  }

  .weekly-report-sidebar {
    padding-right: 0;
    border-right: none;
  }

  .weekly-outline {
    position: static;
    padding-left: 0;
    border-left: none;
  }
}

@media (max-width: 768px) {
  .update-center-page {
    padding: 12px;
  }

  .update-hero,
  .release-head,
  .fragment-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }

  .entry-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }
}
</style>
