import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  getUpdateCenterCurrentWeek,
  getUpdateCenterGitHubLogs,
  getUpdateCenterReleaseDetail,
  getUpdateCenterReleases,
} from '@/api/updateCenter'
import type {
  ChangelogEntryOutput,
  ChangelogFragmentOutput,
  CurrentWeekOutput,
  GitHubLogsOutput,
  ReleasesOutput,
} from '@/types/updateCenter'

const STORAGE_KEY = 'miduo:update-center:v1'
const DEFAULT_FRAGMENT_LIMIT = 4
const FRAGMENT_LOAD_MORE_LIMIT = 6
const DEFAULT_RELEASE_LIMIT = 8
const DEFAULT_GITHUB_LIMIT = 80

interface UpdateCenterCachePayload {
  currentWeek: CurrentWeekOutput | null
  releases: ReleasesOutput | null
  githubLogs: GitHubLogsOutput | null
  lastSeenAt: string | null
}

function readCache(): UpdateCenterCachePayload {
  if (typeof window === 'undefined') {
    return emptyCache()
  }
  try {
    const raw = window.sessionStorage.getItem(STORAGE_KEY)
    return raw ? { ...emptyCache(), ...JSON.parse(raw) } : emptyCache()
  } catch {
    return emptyCache()
  }
}

function emptyCache(): UpdateCenterCachePayload {
  return {
    currentWeek: null,
    releases: null,
    githubLogs: null,
    lastSeenAt: null,
  }
}

function writeCache(payload: UpdateCenterCachePayload): void {
  if (typeof window === 'undefined') {
    return
  }
  try {
    window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
  } catch {
    // 为什么忽略缓存写入失败：浏览器隐私模式或容量限制不应影响更新中心主流程。
  }
}

export const useUpdateCenterStore = defineStore('updateCenter', () => {
  const cached = readCache()

  const currentWeek = ref<CurrentWeekOutput | null>(cached.currentWeek)
  const releases = ref<ReleasesOutput | null>(cached.releases)
  const githubLogs = ref<GitHubLogsOutput | null>(cached.githubLogs)
  const lastSeenAt = ref<string | null>(cached.lastSeenAt)
  const loadingCurrent = ref(false)
  const loadingReleases = ref(false)
  const loadingMoreFragments = ref(false)
  const loadingReleaseVersions = ref<Record<string, boolean>>({})
  const loadingGitHubLogs = ref(false)
  const loadingMoreGitHubLogs = ref(false)
  const error = ref<string | null>(null)

  let currentWeekSeq = 0
  let releasesSeq = 0
  let githubSeq = 0

  const unreadCount = computed(() => {
    if (!currentWeek.value?.dataSourceAvailable) {
      return 0
    }
    const fragments = currentWeek.value.fragments || []
    const totalEntries = fragments.reduce((sum, fragment) => sum + fragment.entries.length, 0)
    if (!lastSeenAt.value) {
      return totalEntries
    }
    const seen = new Date(lastSeenAt.value)
    return fragments.reduce((sum, fragment) => {
      const fragmentDay = new Date(`${fragment.date}T23:59:59`)
      return fragmentDay > seen ? sum + fragment.entries.length : sum
    }, 0)
  })

  const recentEntries = computed<Array<ChangelogEntryOutput & { date: string; fileName: string }>>(
    () => {
      const result: Array<ChangelogEntryOutput & { date: string; fileName: string }> = []
      for (const fragment of currentWeek.value?.fragments || []) {
        for (const entry of fragment.entries) {
          result.push({ ...entry, date: fragment.date, fileName: fragment.fileName })
          if (result.length >= 5) {
            return result
          }
        }
      }
      return result
    },
  )

  function persist(): void {
    writeCache({
      currentWeek: currentWeek.value,
      releases: releases.value,
      githubLogs: githubLogs.value,
      lastSeenAt: lastSeenAt.value,
    })
  }

  async function loadCurrentWeek(options?: { daysLimit?: number; force?: boolean }): Promise<void> {
    const hasCache = currentWeek.value !== null
    if (!hasCache) {
      loadingCurrent.value = true
    }
    error.value = null
    const seq = ++currentWeekSeq
    try {
      const data = await getUpdateCenterCurrentWeek({
        daysLimit: options?.daysLimit ?? DEFAULT_FRAGMENT_LIMIT,
        force: options?.force === true,
      })
      if (seq !== currentWeekSeq) {
        return
      }
      currentWeek.value = data
      persist()
    } catch {
      if (!hasCache) {
        error.value = '加载待发布更新失败'
      }
    } finally {
      if (seq === currentWeekSeq) {
        loadingCurrent.value = false
      }
    }
  }

  async function loadMoreFragments(): Promise<void> {
    if (!currentWeek.value?.hasMore || loadingMoreFragments.value) {
      return
    }
    loadingMoreFragments.value = true
    try {
      const data = await getUpdateCenterCurrentWeek({
        daysLimit: FRAGMENT_LOAD_MORE_LIMIT,
        daysOffset: currentWeek.value.fragments.length,
      })
      const mergedFragments: ChangelogFragmentOutput[] = [
        ...currentWeek.value.fragments,
        ...(data.fragments || []),
      ]
      currentWeek.value = {
        ...data,
        fragments: mergedFragments,
      }
      persist()
    } finally {
      loadingMoreFragments.value = false
    }
  }

  async function loadReleases(options?: {
    limit?: number
    summary?: boolean
    force?: boolean
  }): Promise<void> {
    const hasCache = releases.value !== null
    if (!hasCache) {
      loadingReleases.value = true
    }
    error.value = null
    const seq = ++releasesSeq
    try {
      const data = await getUpdateCenterReleases({
        limit: options?.limit ?? DEFAULT_RELEASE_LIMIT,
        summary: options?.summary ?? true,
        force: options?.force === true,
      })
      if (seq !== releasesSeq) {
        return
      }
      releases.value = data
      persist()
    } catch {
      if (!hasCache) {
        error.value = '加载已发布更新失败'
      }
    } finally {
      if (seq === releasesSeq) {
        loadingReleases.value = false
      }
    }
  }

  async function loadReleaseDetail(version: string): Promise<void> {
    if (!releases.value || loadingReleaseVersions.value[version]) {
      return
    }
    const target = releases.value.releases.find((release) => release.version === version)
    if (!target || !target.entriesOmitted) {
      return
    }
    loadingReleaseVersions.value = {
      ...loadingReleaseVersions.value,
      [version]: true,
    }
    try {
      const detail = await getUpdateCenterReleaseDetail(version)
      releases.value = {
        ...releases.value,
        releases: releases.value.releases.map((release) =>
          release.version === version ? { ...release, ...detail, entriesOmitted: false } : release,
        ),
      }
      persist()
    } finally {
      const next = { ...loadingReleaseVersions.value }
      delete next[version]
      loadingReleaseVersions.value = next
    }
  }

  async function loadGitHubLogs(options?: { force?: boolean }): Promise<void> {
    const hasCache = githubLogs.value !== null
    if (!hasCache) {
      loadingGitHubLogs.value = true
    }
    error.value = null
    const seq = ++githubSeq
    try {
      const data = await getUpdateCenterGitHubLogs({
        limit: DEFAULT_GITHUB_LIMIT,
        force: options?.force === true,
      })
      if (seq !== githubSeq) {
        return
      }
      githubLogs.value = data
      persist()
    } catch {
      if (!hasCache) {
        error.value = '加载Git提交记录失败'
      }
    } finally {
      if (seq === githubSeq) {
        loadingGitHubLogs.value = false
      }
    }
  }

  async function loadMoreGitHubLogs(): Promise<void> {
    if (!githubLogs.value?.hasMore || !githubLogs.value.nextCursor || loadingMoreGitHubLogs.value) {
      return
    }
    loadingMoreGitHubLogs.value = true
    try {
      const data = await getUpdateCenterGitHubLogs({
        limit: DEFAULT_GITHUB_LIMIT,
        before: githubLogs.value.nextCursor,
      })
      githubLogs.value = {
        ...data,
        logs: [...githubLogs.value.logs, ...(data.logs || [])],
      }
      persist()
    } finally {
      loadingMoreGitHubLogs.value = false
    }
  }

  async function refreshAll(): Promise<void> {
    await Promise.all([
      loadCurrentWeek({ force: true }),
      loadReleases({ summary: true, force: true }),
      loadGitHubLogs({ force: true }),
    ])
  }

  function markAsSeen(): void {
    lastSeenAt.value = new Date().toISOString()
    persist()
  }

  return {
    currentWeek,
    releases,
    githubLogs,
    lastSeenAt,
    unreadCount,
    recentEntries,
    loadingCurrent,
    loadingReleases,
    loadingMoreFragments,
    loadingReleaseVersions,
    loadingGitHubLogs,
    loadingMoreGitHubLogs,
    error,
    loadCurrentWeek,
    loadMoreFragments,
    loadReleases,
    loadReleaseDetail,
    loadGitHubLogs,
    loadMoreGitHubLogs,
    refreshAll,
    markAsSeen,
  }
})
