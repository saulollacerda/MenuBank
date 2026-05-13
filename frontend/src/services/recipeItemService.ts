import api from './api'
import type { RecipeItemRequest, RecipeItemResponse } from '@/types/Product'

export const recipeItemService = {
  async findByProductId(productId: string): Promise<RecipeItemResponse[]> {
    const { data } = await api.get<RecipeItemResponse[]>(
      `/products/${productId}/recipe-items`,
    )
    return data
  },

  async add(productId: string, request: RecipeItemRequest): Promise<RecipeItemResponse> {
    const { data } = await api.post<RecipeItemResponse>(
      `/products/${productId}/recipe-items`,
      request,
    )
    return data
  },

  async update(
    productId: string,
    recipeItemId: string,
    request: RecipeItemRequest,
  ): Promise<RecipeItemResponse> {
    const { data } = await api.put<RecipeItemResponse>(
      `/products/${productId}/recipe-items/${recipeItemId}`,
      request,
    )
    return data
  },

  async remove(productId: string, recipeItemId: string): Promise<void> {
    await api.delete(`/products/${productId}/recipe-items/${recipeItemId}`)
  },
}

