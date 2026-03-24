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

  async function fetchDashboard() {
    loading.value = true
    error.value = null
    try {
      data.value = await dashboardService.getDashboard(
        startDate.value || undefined,
        endDate.value || undefined,
      )
    } catch (e: unknown) {
      error.value = 'Erro ao carregar dashboard'
      throw e
    } finally {
      loading.value = false
    }
  }

  return { data, loading, error, startDate, endDate, fetchDashboard }
})

