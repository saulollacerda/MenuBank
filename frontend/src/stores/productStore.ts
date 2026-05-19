import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { ProductRequest, ProductResponse, RecipeItemResponse } from '@/types/Product'
import type { RecipeItemRequest } from '@/types/Product'
import type { PageParams } from '@/types/Page'
import { DEFAULT_PAGE_SIZE } from '@/types/Page'
import { productService } from '@/services/productService'
import { recipeItemService } from '@/services/recipeItemService'

export const useProductStore = defineStore('product', () => {
  const items = ref<ProductResponse[]>([])
  const recipeItems = ref<RecipeItemResponse[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const loaded = ref(false)

  // pagination state
  const search = ref('')
  const page = ref(0)
  const size = ref(DEFAULT_PAGE_SIZE)
  const totalElements = ref(0)
  const totalPages = ref(0)

  async function fetchPage(params: PageParams = {}) {
    loading.value = true
    error.value = null
    try {
      const result = await productService.findAll({
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
      error.value = 'Erro ao carregar produtos'
      throw e
    } finally {
      loading.value = false
    }
  }

  // legacy entry point used by forms/dropdowns that need the full list
  async function fetchAll(force = false) {
    if (!force && loaded.value) return
    await fetchPage({ search: '', page: 0, size: 1000 })
  }

  async function create(request: ProductRequest) {
    loading.value = true
    error.value = null
    try {
      const created = await productService.create(request)
      await fetchPage({})
      return created
    } catch (e: unknown) {
      error.value = 'Erro ao criar produto'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function update(id: string, request: ProductRequest) {
    loading.value = true
    error.value = null
    try {
      const updated = await productService.update(id, request)
      const index = items.value.findIndex((item) => item.id === id)
      if (index !== -1) items.value[index] = updated
      return updated
    } catch (e: unknown) {
      error.value = 'Erro ao atualizar produto'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function remove(id: string) {
    loading.value = true
    error.value = null
    try {
      await productService.remove(id)
      await fetchPage({})
    } catch (e: unknown) {
      error.value = 'Erro ao excluir produto'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchRecipeItems(productId: string) {
    loading.value = true
    error.value = null
    try {
      recipeItems.value = await recipeItemService.findByProductId(productId)
    } catch (e: unknown) {
      error.value = 'Erro ao carregar ficha técnica'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function addRecipeItem(productId: string, request: RecipeItemRequest) {
    loading.value = true
    error.value = null
    try {
      const created = await recipeItemService.add(productId, request)
      recipeItems.value.push(created)
      return created
    } catch (e: unknown) {
      error.value = 'Erro ao adicionar ingrediente à ficha técnica'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function removeRecipeItem(productId: string, recipeItemId: string) {
    loading.value = true
    error.value = null
    try {
      await recipeItemService.remove(productId, recipeItemId)
      recipeItems.value = recipeItems.value.filter((item) => item.id !== recipeItemId)
    } catch (e: unknown) {
      error.value = 'Erro ao remover ingrediente da ficha técnica'
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    items,
    recipeItems,
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
    fetchRecipeItems,
    addRecipeItem,
    removeRecipeItem,
  }
})
