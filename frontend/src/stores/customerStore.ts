import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { CustomerRequest, CustomerResponse } from '@/types/Customer'
import { customerService } from '@/services/customerService'

export const useCustomerStore = defineStore('customer', () => {
  const items = ref<CustomerResponse[]>([])
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
        items.value = await customerService.findAll()
        loaded.value = true
      } catch (e: unknown) {
        loaded.value = false
        error.value = 'Erro ao carregar clientes'
        throw e
      } finally {
        loading.value = false
        fetchAllInFlight = null
      }
    })()

    return fetchAllInFlight
  }

  async function create(request: CustomerRequest) {
    loading.value = true
    error.value = null
    try {
      const created = await customerService.create(request)
      items.value.push(created)
      loaded.value = true
      return created
    } catch (e: unknown) {
      error.value = 'Erro ao criar cliente'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function update(id: string, request: CustomerRequest) {
    loading.value = true
    error.value = null
    try {
      const updated = await customerService.update(id, request)
      const index = items.value.findIndex((item) => item.id === id)
      if (index !== -1) items.value[index] = updated
      loaded.value = true
      return updated
    } catch (e: unknown) {
      error.value = 'Erro ao atualizar cliente'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function remove(id: string) {
    loading.value = true
    error.value = null
    try {
      await customerService.remove(id)
      items.value = items.value.filter((item) => item.id !== id)
      loaded.value = true
    } catch (e: unknown) {
      error.value = 'Erro ao excluir cliente'
      throw e
    } finally {
      loading.value = false
    }
  }

  return { items, loading, error, fetchAll, create, update, remove }
})

