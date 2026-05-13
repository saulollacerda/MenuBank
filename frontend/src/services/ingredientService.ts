import api from './api'
import type { IngredientRequest, IngredientResponse } from '@/types/Ingredient'

export const ingredientService = {
  async findAll(): Promise<IngredientResponse[]> {
    const { data } = await api.get<IngredientResponse[]>('/ingredients')
    return data
  },

  async findById(id: string): Promise<IngredientResponse> {
    const { data } = await api.get<IngredientResponse>(`/ingredients/${id}`)
    return data
  },

  async create(request: IngredientRequest): Promise<IngredientResponse> {
    const { data } = await api.post<IngredientResponse>('/ingredients', request)
    return data
  },

  async update(id: string, request: IngredientRequest): Promise<IngredientResponse> {
    const { data } = await api.put<IngredientResponse>(`/ingredients/${id}`, request)
    return data
  },

  async remove(id: string): Promise<void> {
    await api.delete(`/ingredients/${id}`)
  },
}

