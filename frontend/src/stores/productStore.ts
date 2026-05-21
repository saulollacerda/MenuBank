import { ref } from 'vue'
import { defineStore } from 'pinia'
import type {
  ProductRequest,
  ProductResponse,
  ProductIngredientRequest,
  ProductIngredientResponse,
} from '@/types/Product'
import type { PageParams } from '@/types/Page'
import { DEFAULT_PAGE_SIZE } from '@/types/Page'
import { productService } from '@/services/productService'
import { productIngredientService } from '@/services/productIngredientService'

export const useProductStore = defineStore('product', () => {
  const items = ref<ProductResponse[]>([])
  const productIngredients = ref<ProductIngredientResponse[]>([])
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

  async function fetchAll(force = false) {
    if (!force && loaded.value) return
    loading.value = true
    error.value = null
    try {
      const result = await productService.findAll({ search: '', page: 0, size: 1000 })
      items.value = result.content
      loaded.value = true
    } catch (e: unknown) {
      error.value = 'Erro ao carregar produtos'
      throw e
    } finally {
      loading.value = false
    }
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

  async function fetchProductIngredients(productId: string) {
    loading.value = true
    error.value = null
    try {
      productIngredients.value = await productIngredientService.findByProductId(productId)
    } catch (e: unknown) {
      error.value = 'Erro ao carregar ficha técnica'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function addProductIngredient(productId: string, request: ProductIngredientRequest) {
    loading.value = true
    error.value = null
    try {
      const created = await productIngredientService.add(productId, request)
      productIngredients.value.push(created)
      return created
    } catch (e: unknown) {
      error.value = 'Erro ao adicionar ingrediente à ficha técnica'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function batchAddProductIngredients(
    productId: string,
    requests: ProductIngredientRequest[],
  ) {
    loading.value = true
    error.value = null
    try {
      const created = await productIngredientService.batchAdd(productId, requests)
      productIngredients.value.push(...created)
      return created
    } catch (e: unknown) {
      error.value = 'Erro ao salvar ingredientes em lote'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function clearRecipe(productId: string) {
    loading.value = true
    error.value = null
    try {
      const deleted = await productIngredientService.clear(productId)
      productIngredients.value = []
      return deleted
    } catch (e: unknown) {
      error.value = 'Erro ao limpar ficha técnica'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function removeProductIngredient(productId: string, productIngredientId: string) {
    loading.value = true
    error.value = null
    try {
      await productIngredientService.remove(productId, productIngredientId)
      productIngredients.value = productIngredients.value.filter((item) => item.id !== productIngredientId)
    } catch (e: unknown) {
      error.value = 'Erro ao remover ingrediente da ficha técnica'
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    items,
    productIngredients,
    // alias para minimizar churn em chamadores ainda usando o nome antigo
    recipeItems: productIngredients,
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
    fetchProductIngredients,
    addProductIngredient,
    batchAddProductIngredients,
    removeProductIngredient,
    // aliases para compat com chamadores antigos
    fetchRecipeItems: fetchProductIngredients,
    addRecipeItem: addProductIngredient,
    batchAddRecipeItems: batchAddProductIngredients,
    removeRecipeItem: removeProductIngredient,
    clearRecipe,
  }
})
