import api from './api'
import type { IngredientCategoryRequest, IngredientCategoryResponse } from '@/types/IngredientCategory'

export const ingredientCategoryService = {
  async findAll(): Promise<IngredientCategoryResponse[]> {
    const { data } = await api.get<IngredientCategoryResponse[]>('/ingredient-categories')
    return data
  },

  async findById(id: string): Promise<IngredientCategoryResponse> {
    const { data } = await api.get<IngredientCategoryResponse>(`/ingredient-categories/${id}`)
    return data
  },

  async create(request: IngredientCategoryRequest): Promise<IngredientCategoryResponse> {
    const { data } = await api.post<IngredientCategoryResponse>('/ingredient-categories', request)
    return data
  },

  async update(id: string, request: IngredientCategoryRequest): Promise<IngredientCategoryResponse> {
    const { data } = await api.put<IngredientCategoryResponse>(`/ingredient-categories/${id}`, request)
    return data
  },

  async remove(id: string): Promise<void> {
    await api.delete(`/ingredient-categories/${id}`)
  },
}
