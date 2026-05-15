import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { PaymentMethodRequest, PaymentMethodResponse } from '@/types/PaymentMethod'
import { paymentMethodService } from '@/services/paymentMethodService'

export const usePaymentMethodStore = defineStore('paymentMethod', () => {
  const items = ref<PaymentMethodResponse[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const loaded = ref(false)

  let fetchAllInFlight: Promise<void> | null = null

  async function fetchAll(force = false) {
    if (!force && loaded.value) return
    if (!force && fetchAllInFlight) return fetchAllInFlight

    loading.value = true
    error.value = null

    fetchAllInFlight = (async () => {
      try {
        items.value = await paymentMethodService.findAll()
        loaded.value = true
      } catch (e: unknown) {
        loaded.value = false
        error.value = 'Erro ao carregar formas de pagamento'
        throw e
      } finally {
        loading.value = false
        fetchAllInFlight = null
      }
    })()

    return fetchAllInFlight
  }

  async function create(request: PaymentMethodRequest) {
    loading.value = true
    error.value = null
    try {
      const created = await paymentMethodService.create(request)
      items.value.push(created)
      loaded.value = true
      return created
    } catch (e: unknown) {
      error.value = 'Erro ao criar forma de pagamento'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function update(id: string, request: PaymentMethodRequest) {
    loading.value = true
    error.value = null
    try {
      const updated = await paymentMethodService.update(id, request)
      const index = items.value.findIndex((item) => item.id === id)
      if (index !== -1) items.value[index] = updated
      loaded.value = true
      return updated
    } catch (e: unknown) {
      error.value = 'Erro ao atualizar forma de pagamento'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function remove(id: string) {
    loading.value = true
    error.value = null
    try {
      await paymentMethodService.remove(id)
      items.value = items.value.filter((item) => item.id !== id)
      loaded.value = true
    } catch (e: unknown) {
      error.value = 'Erro ao excluir forma de pagamento'
      throw e
    } finally {
      loading.value = false
    }
  }

  return { items, loading, error, fetchAll, create, update, remove }
})
