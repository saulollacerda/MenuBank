import api from './api'
import type { AnotaAISyncResult } from '@/types/AnotaAI'

export const anotaAIService = {
  async syncOrders(): Promise<AnotaAISyncResult> {
    const { data } = await api.post<AnotaAISyncResult>('/integrations/anotaai/orders')
    return data
  },

  async syncCatalog(options: { clearRecipes?: boolean } = {}): Promise<AnotaAISyncResult> {
    const { data } = await api.post<AnotaAISyncResult>('/integrations/anotaai/catalog', null, {
      params: options.clearRecipes ? { clearRecipes: true } : undefined,
    })
    return data
  },
}
