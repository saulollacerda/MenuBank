import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { CategoryRequest, CategoryResponse } from '@/types/Category'
import { categoryService } from '@/services/categoryService'

export const useCategoryStore = defineStore('category', () => {
  const items = ref<CategoryResponse[]>([])
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
        items.value = await categoryService.findAll()
        loaded.value = true
      } catch (e: unknown) {
        loaded.value = false
        error.value = 'Erro ao carregar categorias'
        throw e
      } finally {
        loading.value = false
        fetchAllInFlight = null
      }
    })()

    return fetchAllInFlight
  }

  async function create(request: CategoryRequest) {
    loading.value = true
    error.value = null
    try {
      const created = await categoryService.create(request)
      items.value.push(created)
      loaded.value = true
      return created
    } catch (e: unknown) {
      error.value = 'Erro ao criar categoria'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function update(id: string, request: CategoryRequest) {
    loading.value = true
    error.value = null
    try {
      const updated = await categoryService.update(id, request)
      const index = items.value.findIndex((item) => item.id === id)
      if (index !== -1) items.value[index] = updated
      loaded.value = true
      return updated
    } catch (e: unknown) {
      error.value = 'Erro ao atualizar categoria'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function remove(id: string) {
    loading.value = true
    error.value = null
    try {
      await categoryService.remove(id)
      items.value = items.value.filter((item) => item.id !== id)
      loaded.value = true
    } catch (e: unknown) {
      error.value = 'Erro ao excluir categoria'
      throw e
    } finally {
      loading.value = false
    }
  }

  return { items, loading, error, fetchAll, create, update, remove }
})

