import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { CustomerRequest, CustomerResponse } from '@/types/Customer'
import type { PageParams } from '@/types/Page'
import { DEFAULT_PAGE_SIZE } from '@/types/Page'
import { customerService } from '@/services/customerService'

export const useCustomerStore = defineStore('customer', () => {
  const items = ref<CustomerResponse[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const loaded = ref(false)

  const search = ref('')
  const page = ref(0)
  const size = ref(DEFAULT_PAGE_SIZE)
  const totalElements = ref(0)
  const totalPages = ref(0)

  async function fetchPage(params: PageParams = {}) {
    loading.value = true
    error.value = null
    try {
      const result = await customerService.findAll({
        search: params.search ?? search.value,
        page: params.page ?? page.value,
        size: params.size ?? size.value,
      })
      items.value = result.content
      search.value = params.search ?? search.value
      page.value = result.number
      size.value = result.size
      totalElements.value = result.totalElements
      totalPages.value = result.totalPages
      loaded.value = true
    } catch (e: unknown) {
      loaded.value = false
      error.value = 'Erro ao carregar clientes'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchAll(force = false) {
    if (!force && loaded.value) return
    await fetchPage({ search: '', page: 0, size: 1000 })
  }

  async function create(request: CustomerRequest) {
    loading.value = true
    error.value = null
    try {
      const created = await customerService.create(request)
      await fetchPage({})
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
      await fetchPage({})
    } catch (e: unknown) {
      error.value = 'Erro ao excluir cliente'
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    items,
    loading,
    error,
    search,
    page,
    size,
    totalElements,
    totalPages,
    fetchPage,
    fetchAll,
    create,
    update,
    remove,
  }
})
