import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { DashboardResponse } from '@/types/Dashboard'
import { dashboardService } from '@/services/dashboardService'

function lastDayOfMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate()
}

function pad2(n: number): string {
  return String(n).padStart(2, '0')
}

export const useDashboardStore = defineStore('dashboard', () => {
  const data = ref<DashboardResponse | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const exporting = ref(false)

  const filterMode = ref<'month' | 'custom'>('month')

  const now = new Date()
  const selectedYear = ref<number>(now.getFullYear())
  const selectedMonthNumber = ref<number>(now.getMonth() + 1)

  const selectedMonth = computed<string>(
    () => `${selectedYear.value}-${pad2(selectedMonthNumber.value)}`,
  )

  // used only in custom mode
  const startDate = ref<string>('')
  const endDate = ref<string>('')

  const resolvedStartDate = computed<string>(() => {
    if (filterMode.value === 'month') return `${selectedMonth.value}-01`
    return startDate.value
  })

  const resolvedEndDate = computed<string>(() => {
    if (filterMode.value === 'month') {
      const last = lastDayOfMonth(selectedYear.value, selectedMonthNumber.value)
      return `${selectedMonth.value}-${pad2(last)}`
    }
    return endDate.value
  })

  const loaded = ref(false)
  const loadedKey = ref<string | null>(null)
  let fetchDashboardInFlight: Promise<void> | null = null

  function getCurrentKey(): string {
    return `${resolvedStartDate.value}|${resolvedEndDate.value}`
  }

  async function fetchDashboard(force = false) {
    const key = getCurrentKey()

    if (!force && loaded.value && loadedKey.value === key) return
    if (!force && fetchDashboardInFlight) return fetchDashboardInFlight

    loading.value = true
    error.value = null

    fetchDashboardInFlight = (async () => {
      try {
        data.value = await dashboardService.getDashboard(
          resolvedStartDate.value || undefined,
          resolvedEndDate.value || undefined,
        )
        loaded.value = true
        loadedKey.value = key
      } catch (e: unknown) {
        loaded.value = false
        loadedKey.value = null
        error.value = 'Erro ao carregar dashboard'
        throw e
      } finally {
        loading.value = false
        fetchDashboardInFlight = null
      }
    })()

    return fetchDashboardInFlight
  }

  async function exportDashboard() {
    exporting.value = true
    try {
      const blob = await dashboardService.exportDashboard(
        resolvedStartDate.value || undefined,
        resolvedEndDate.value || undefined,
      )
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      const label =
        filterMode.value === 'month'
          ? selectedMonth.value
          : `${resolvedStartDate.value}-${resolvedEndDate.value}`
      a.download = `menubank-export-${label}.xlsx`
      a.click()
      URL.revokeObjectURL(url)
    } finally {
      exporting.value = false
    }
  }

  async function exportDayClosing() {
    exporting.value = true
    try {
      const today = new Date()
      const y = today.getFullYear()
      const m = pad2(today.getMonth() + 1)
      const d = pad2(today.getDate())
      const iso = `${y}-${m}-${d}`

      const blob = await dashboardService.exportDashboard(iso, iso)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `menubank-fechamento-${iso}.xlsx`
      a.click()
      URL.revokeObjectURL(url)
    } finally {
      exporting.value = false
    }
  }

  return {
    data,
    loading,
    error,
    exporting,
    filterMode,
    selectedYear,
    selectedMonthNumber,
    selectedMonth,
    startDate,
    endDate,
    resolvedStartDate,
    resolvedEndDate,
    fetchDashboard,
    exportDashboard,
    exportDayClosing,
  }
})
