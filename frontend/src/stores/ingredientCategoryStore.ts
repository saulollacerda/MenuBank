import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { IngredientCategoryRequest, IngredientCategoryResponse } from '@/types/IngredientCategory'
import { ingredientCategoryService } from '@/services/ingredientCategoryService'

export const useIngredientCategoryStore = defineStore('ingredientCategory', () => {
  const items = ref<IngredientCategoryResponse[]>([])
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
        items.value = await ingredientCategoryService.findAll()
        loaded.value = true
      } catch (e: unknown) {
        loaded.value = false
        error.value = 'Erro ao carregar categorias de ingrediente'
        throw e
      } finally {
        loading.value = false
        fetchAllInFlight = null
      }
    })()

    return fetchAllInFlight
  }

  async function create(request: IngredientCategoryRequest) {
    loading.value = true
    error.value = null
    try {
      const created = await ingredientCategoryService.create(request)
      items.value.push(created)
      return created
    } catch (e: unknown) {
      error.value = 'Erro ao criar categoria de ingrediente'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function update(id: string, request: IngredientCategoryRequest) {
    loading.value = true
    error.value = null
    try {
      const updated = await ingredientCategoryService.update(id, request)
      const index = items.value.findIndex((item) => item.id === id)
      if (index !== -1) items.value[index] = updated
      return updated
    } catch (e: unknown) {
      error.value = 'Erro ao atualizar categoria de ingrediente'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function remove(id: string) {
    loading.value = true
    error.value = null
    try {
      await ingredientCategoryService.remove(id)
      items.value = items.value.filter((item) => item.id !== id)
    } catch (e: unknown) {
      error.value = 'Erro ao excluir categoria de ingrediente'
      throw e
    } finally {
      loading.value = false
    }
  }

  return { items, loading, error, loaded, fetchAll, create, update, remove }
})
