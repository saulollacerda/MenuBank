import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { ProductRequest, ProductResponse, RecipeItemResponse } from '@/types/Product'
import type { RecipeItemRequest } from '@/types/Product'
import { productService } from '@/services/productService'
import { recipeItemService } from '@/services/recipeItemService'

export const useProductStore = defineStore('product', () => {
  const items = ref<ProductResponse[]>([])
  const recipeItems = ref<RecipeItemResponse[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchAll() {
    loading.value = true
    error.value = null
    try {
      items.value = await productService.findAll()
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
      items.value.push(created)
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
      items.value = items.value.filter((item) => item.id !== id)
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
      // Refresh the product to get updated estimatedCost/margin
      const updated = await productService.findById(productId)
      const index = items.value.findIndex((item) => item.id === productId)
      if (index !== -1) items.value[index] = updated
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
      // Refresh the product to get updated estimatedCost/margin
      const updated = await productService.findById(productId)
      const index = items.value.findIndex((item) => item.id === productId)
      if (index !== -1) items.value[index] = updated
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
    fetchAll,
    create,
    update,
    remove,
    fetchRecipeItems,
    addRecipeItem,
    removeRecipeItem,
  }
})

