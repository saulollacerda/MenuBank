import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { OrderRequest, OrderResponse } from '@/types/Order'
import { DEFAULT_PAGE_SIZE } from '@/types/Page'
import { orderService, type OrderFilterParams } from '@/services/orderService'
import { useDashboardStore } from '@/stores/dashboardStore'

/** Extrai o `detail` do ProblemDetail retornado pelo backend, quando presente. */
function extractDetail(e: unknown): string | null {
  if (typeof e === 'object' && e !== null && 'response' in e) {
    const data = (e as { response?: { data?: { detail?: unknown } } }).response?.data
    if (typeof data?.detail === 'string' && data.detail.trim()) return data.detail
  }
  return null
}

export const useOrderStore = defineStore('order', () => {
  const items = ref<OrderResponse[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const loaded = ref(false)

  const search = ref('')
  const page = ref(0)
  const size = ref(DEFAULT_PAGE_SIZE)
  const totalElements = ref(0)
  const totalPages = ref(0)
  const sort = ref<string>('dateTime,desc')

  function refreshDashboard() {
    const dashboardStore = useDashboardStore()
    void dashboardStore.fetchDashboard(true).catch(() => {
      // dashboard refresh is best-effort; errors are surfaced by the dashboard store itself
    })
  }

  async function fetchPage(params: OrderFilterParams = {}, silent = false) {
    loading.value = true
    if (!silent) error.value = null
    try {
      const effectiveSort = params.sort ?? sort.value
      const result = await orderService.findAll({
        search: params.search ?? search.value,
        page: params.page ?? page.value,
        size: params.size ?? size.value,
        sort: effectiveSort,
      })
      items.value = result.content
      search.value = params.search ?? search.value
      sort.value = effectiveSort
      page.value = result.number
      size.value = result.size
      totalElements.value = result.totalElements
      totalPages.value = result.totalPages
      loaded.value = true
      error.value = null
    } catch (e: unknown) {
      loaded.value = false
      if (!silent) error.value = 'Erro ao carregar pedidos'
      throw e
    } finally {
      loading.value = false
    }
  }

  // Legacy entry point: fills `items` without touching pagination state.
  async function fetchAll(force = false) {
    if (!force && loaded.value) return
    loading.value = true
    error.value = null
    try {
      const result = await orderService.findAll({ search: '', page: 0, size: 1000 })
      items.value = result.content
      loaded.value = true
    } catch (e: unknown) {
      error.value = 'Erro ao carregar pedidos'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function create(request: OrderRequest) {
    loading.value = true
    error.value = null
    try {
      const created = await orderService.create(request)
      await fetchPage({})
      refreshDashboard()
      return created
    } catch (e: unknown) {
      error.value = extractDetail(e) ?? 'Erro ao criar pedido'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function update(id: string, request: OrderRequest) {
    loading.value = true
    error.value = null
    try {
      const updated = await orderService.update(id, request)
      const index = items.value.findIndex((item) => item.id === id)
      if (index !== -1) items.value[index] = updated
      refreshDashboard()
      return updated
    } catch (e: unknown) {
      error.value = extractDetail(e) ?? 'Erro ao atualizar pedido'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function remove(id: string) {
    loading.value = true
    error.value = null
    try {
      await orderService.remove(id)
      await fetchPage({})
      refreshDashboard()
    } catch (e: unknown) {
      error.value = 'Erro ao excluir pedido'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function findById(id: string): Promise<OrderResponse> {
    try {
      return await orderService.findById(id)
    } catch (e: unknown) {
      error.value = 'Erro ao carregar detalhes do pedido'
      throw e
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
    sort,
    fetchPage,
    fetchAll,
    create,
    update,
    remove,
    findById,
  }
})
