import { ref } from 'vue'
import { defineStore } from 'pinia'
import { createStaleCache } from '@/utils/staleCache'

const TTL_MS = 5 * 60 * 1000
import type { IngredientRequest, IngredientResponse } from '@/types/Ingredient'
import type { PageParams } from '@/types/Page'
import { DEFAULT_PAGE_SIZE } from '@/types/Page'
import { ingredientService } from '@/services/ingredientService'

export const useIngredientStore = defineStore('ingredient', () => {
  const items = ref<IngredientResponse[]>([])
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
      const result = await ingredientService.findAll({
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
      error.value = 'Erro ao carregar ingredientes'
      throw e
    } finally {
      loading.value = false
    }
  }

  // Legacy entry point: fills `items` for forms/dropdowns without touching
  // pagination state (so a later list view starts with default size).
  async function fetchAll(force = false) {
    if (!force && loaded.value && !cache.isStale()) return
    loading.value = true
    error.value = null
    try {
      const result = await ingredientService.findAll({ search: '', page: 0, size: 1000 })
      items.value = result.content
      loaded.value = true
      cache.markFresh()
    } catch (e: unknown) {
      error.value = 'Erro ao carregar ingredientes'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function create(request: IngredientRequest) {
    loading.value = true
    error.value = null
    try {
      const created = await ingredientService.create(request)
      cache.invalidate()
      await fetchPage({})
      return created
    } catch (e: unknown) {
      error.value = 'Erro ao criar ingrediente'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function update(id: string, request: IngredientRequest) {
    loading.value = true
    error.value = null
    try {
      const updated = await ingredientService.update(id, request)
      const index = items.value.findIndex((item) => item.id === id)
      if (index !== -1) items.value[index] = updated
      return updated
    } catch (e: unknown) {
      error.value = 'Erro ao atualizar ingrediente'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function remove(id: string) {
    loading.value = true
    error.value = null
    try {
      await ingredientService.remove(id)
      cache.invalidate()
      await fetchPage({})
    } catch (e: unknown) {
      error.value = 'Erro ao excluir ingrediente'
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
