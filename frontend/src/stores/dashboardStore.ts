import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { DashboardResponse } from '@/types/Dashboard'
import { dashboardService } from '@/services/dashboardService'

export const useDashboardStore = defineStore('dashboard', () => {
  const data = ref<DashboardResponse | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const startDate = ref<string>('')
  const endDate = ref<string>('')

  const loaded = ref(false)
  const loadedKey = ref<string | null>(null)
  let fetchDashboardInFlight: Promise<void> | null = null

  function getCurrentKey(): string {
    return `${startDate.value}|${endDate.value}`
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
          startDate.value || undefined,
          endDate.value || undefined,
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

  return { data, loading, error, startDate, endDate, fetchDashboard }
})

