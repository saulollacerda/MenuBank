import api from './api'
import type { DashboardResponse, IngredientRanking } from '@/types/Dashboard'

export const dashboardService = {
  async getDashboard(startDate?: string, endDate?: string): Promise<DashboardResponse> {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate

    const { data } = await api.get<DashboardResponse>('/dashboard', { params })
    return data
  },

  async getIngredientRanking(
    startDate?: string,
    endDate?: string,
  ): Promise<IngredientRanking[]> {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate

    const { data } = await api.get<IngredientRanking[]>('/dashboard/ingredient-ranking', {
      params,
    })
    return data
  },

  async exportDashboard(startDate?: string, endDate?: string): Promise<Blob> {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate

    const { data } = await api.get<Blob>('/export/dashboard', { params, responseType: 'blob' })
    return data
  },
}

