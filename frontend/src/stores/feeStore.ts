import { ref } from 'vue'
import { defineStore } from 'pinia'
import { createStaleCache } from '@/utils/staleCache'

const TTL_MS = 5 * 60 * 1000
import type { FeeRequest, FeeResponse } from '@/types/Fee'
import type { PageParams } from '@/types/Page'
import { DEFAULT_PAGE_SIZE } from '@/types/Page'
import { feeService } from '@/services/feeService'

export const useFeeStore = defineStore('fee', () => {
  const items = ref<FeeResponse[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const loaded = ref(false)
  const cache = createStaleCache(TTL_MS)

  const search = ref('')
  const page = ref(0)
  const size = ref(DEFAULT_PAGE_SIZE)
  const totalElements = ref(0)
  const totalPages = ref(0)

  async function fetchPage(params: PageParams = {}) {
    loading.value = true
    error.value = null
    try {
      const result = await feeService.findAll({
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
      error.value = 'Erro ao carregar taxas'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchAll(force = false) {
    if (!force && loaded.value && !cache.isStale()) return
    loading.value = true
    error.value = null
    try {
      const result = await feeService.findAll({ search: '', page: 0, size: 1000 })
      items.value = result.content
      loaded.value = true
      cache.markFresh()
    } catch (e: unknown) {
      error.value = 'Erro ao carregar taxas'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function create(request: FeeRequest) {
    loading.value = true
    error.value = null
    try {
      const created = await feeService.create(request)
      cache.invalidate()
      await fetchPage({})
      return created
    } catch (e: unknown) {
      error.value = 'Erro ao criar taxa'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function update(id: string, request: FeeRequest) {
    loading.value = true
    error.value = null
    try {
      const updated = await feeService.update(id, request)
      const index = items.value.findIndex((item) => item.id === id)
      if (index !== -1) items.value[index] = updated
      return updated
    } catch (e: unknown) {
      error.value = 'Erro ao atualizar taxa'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function remove(id: string) {
    loading.value = true
    error.value = null
    try {
      await feeService.remove(id)
      cache.invalidate()
      await fetchPage({})
    } catch (e: unknown) {
      error.value = 'Erro ao excluir taxa'
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
