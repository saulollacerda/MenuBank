import api from './api'
import type { IngredientRequest, IngredientResponse } from '@/types/Ingredient'
import type { Page, PageParams } from '@/types/Page'
import { DEFAULT_PAGE_SIZE } from '@/types/Page'

export const ingredientService = {
  async findAll(params: PageParams = {}): Promise<Page<IngredientResponse>> {
    const { data } = await api.get<Page<IngredientResponse>>('/ingredients', {
      params: {
        search: params.search ?? '',
        page: params.page ?? 0,
        size: params.size ?? DEFAULT_PAGE_SIZE,
      },
    })
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
