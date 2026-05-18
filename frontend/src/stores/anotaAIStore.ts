import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { AnotaAISyncResult } from '@/types/AnotaAI'
import { anotaAIService } from '@/services/anotaAIService'
import { useOrderStore } from '@/stores/orderStore'
import { useProductStore } from '@/stores/productStore'
import { useCategoryStore } from '@/stores/categoryStore'

export const useAnotaAIStore = defineStore('anotaAI', () => {
  const syncingOrders = ref(false)
  const syncingCatalog = ref(false)
  const lastResult = ref<AnotaAISyncResult | null>(null)
  const error = ref<string | null>(null)

  async function syncOrders() {
    syncingOrders.value = true
    error.value = null
    try {
      const result = await anotaAIService.syncOrders()
      lastResult.value = result
      const orderStore = useOrderStore()
      await orderStore.fetchAll(true).catch(() => {})
      return result
    } catch (e: unknown) {
      const err = e as { response?: { data?: { detail?: string } } }
      error.value = err.response?.data?.detail ?? 'Erro ao importar pedidos do Anota.AI'
      throw e
    } finally {
      syncingOrders.value = false
    }
  }

  async function syncCatalog() {
    syncingCatalog.value = true
    error.value = null
    try {
      const result = await anotaAIService.syncCatalog()
      lastResult.value = result
      const productStore = useProductStore()
      const categoryStore = useCategoryStore()
      await Promise.all([
        productStore.fetchAll(true).catch(() => {}),
        categoryStore.fetchAll(true).catch(() => {}),
      ])
      return result
    } catch (e: unknown) {
      const err = e as { response?: { data?: { detail?: string } } }
      error.value = err.response?.data?.detail ?? 'Erro ao sincronizar o cardápio do Anota.AI'
      throw e
    } finally {
      syncingCatalog.value = false
    }
  }

  function clearResult() {
    lastResult.value = null
    error.value = null
  }

  return {
    syncingOrders,
    syncingCatalog,
    lastResult,
    error,
    syncOrders,
    syncCatalog,
    clearResult,
  }
})
