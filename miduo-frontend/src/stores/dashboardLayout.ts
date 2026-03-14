import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { getDashboardLayout, resetDashboardLayout, saveDashboardLayout } from '@/api/dashboardLayout'
import type { DashboardLayoutItem, DashboardRowGroupKey } from '@/types/dashboardLayout'

const DEFAULT_LAYOUT: DashboardLayoutItem[] = [
  { rowGroupKey: 'overview', sortOrder: 0, isFixed: true },
  { rowGroupKey: 'trend_category', sortOrder: 1, isFixed: false },
  { rowGroupKey: 'efficiency_workload', sortOrder: 2, isFixed: false },
]

export const useDashboardLayoutStore = defineStore('dashboardLayout', () => {
  const layout = ref<DashboardLayoutItem[]>([...DEFAULT_LAYOUT])
  const isEditMode = ref(false)
  const editingLayout = ref<DashboardLayoutItem[]>([])
  const loading = ref(false)
  const saving = ref(false)

  let enterEditSnapshot: DashboardLayoutItem[] = []

  const sortedLayout = computed<DashboardLayoutItem[]>(() => {
    const source = isEditMode.value ? editingLayout.value : layout.value
    return [...source].sort((a, b) => a.sortOrder - b.sortOrder)
  })

  const draggableLayout = computed<DashboardLayoutItem[]>(() =>
    sortedLayout.value.filter((item) => !item.isFixed),
  )

  async function fetchLayout(): Promise<void> {
    loading.value = true
    try {
      const result = await getDashboardLayout()
      if (Array.isArray(result) && result.length > 0) {
        layout.value = result
      } else {
        layout.value = [...DEFAULT_LAYOUT]
      }
    } catch {
      layout.value = [...DEFAULT_LAYOUT]
    } finally {
      loading.value = false
    }
  }

  function cloneLayoutItems(items: DashboardLayoutItem[]): DashboardLayoutItem[] {
    return items.map((item) => ({ ...item }))
  }

  function enterEditMode(): void {
    enterEditSnapshot = cloneLayoutItems(layout.value)
    editingLayout.value = cloneLayoutItems(layout.value)
    isEditMode.value = true
  }

  function cancelEditMode(): void {
    editingLayout.value = cloneLayoutItems(enterEditSnapshot)
    isEditMode.value = false
  }

  async function saveLayout(): Promise<void> {
    saving.value = true
    try {
      const payload = editingLayout.value.map((item) => ({
        rowGroupKey: item.rowGroupKey as DashboardRowGroupKey,
        sortOrder: item.sortOrder,
      }))
      await saveDashboardLayout({ layouts: payload })
      layout.value = cloneLayoutItems(editingLayout.value)
      isEditMode.value = false
    } finally {
      saving.value = false
    }
  }

  async function resetLayout(): Promise<void> {
    saving.value = true
    try {
      await resetDashboardLayout()
      isEditMode.value = false
      await fetchLayout()
    } finally {
      saving.value = false
    }
  }

  function updateEditingOrder(newList: DashboardLayoutItem[]): void {
    const fixedItems = editingLayout.value.filter((item) => item.isFixed)
    const reorderedDraggable = newList.map((item, index) => ({
      ...item,
      sortOrder: index + 1,
    }))
    editingLayout.value = [...fixedItems, ...reorderedDraggable].sort(
      (a, b) => a.sortOrder - b.sortOrder,
    )
  }

  return {
    layout,
    isEditMode,
    editingLayout,
    loading,
    saving,
    sortedLayout,
    draggableLayout,
    fetchLayout,
    enterEditMode,
    cancelEditMode,
    saveLayout,
    resetLayout,
    updateEditingOrder,
  }
})
