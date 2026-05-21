import api from './api'
import type {
  ProductIngredientRequest,
  ProductIngredientResponse,
} from '@/types/Product'

const base = (productId: string) => `/products/${productId}/ingredients`

export const productIngredientService = {
  async findByProductId(productId: string): Promise<ProductIngredientResponse[]> {
    const { data } = await api.get<ProductIngredientResponse[]>(base(productId))
    return data
  },

  async add(
    productId: string,
    request: ProductIngredientRequest,
  ): Promise<ProductIngredientResponse> {
    const { data } = await api.post<ProductIngredientResponse>(base(productId), request)
    return data
  },

  async batchAdd(
    productId: string,
    requests: ProductIngredientRequest[],
  ): Promise<ProductIngredientResponse[]> {
    const { data } = await api.post<ProductIngredientResponse[]>(
      `${base(productId)}/batch`,
      requests,
    )
    return data
  },

  async update(
    productId: string,
    productIngredientId: string,
    request: ProductIngredientRequest,
  ): Promise<ProductIngredientResponse> {
    const { data } = await api.put<ProductIngredientResponse>(
      `${base(productId)}/${productIngredientId}`,
      request,
    )
    return data
  },

  async updateGrammageByIngredientId(
    productId: string,
    ingredientId: string,
    grammage: number,
  ): Promise<ProductIngredientResponse> {
    const { data } = await api.put<ProductIngredientResponse>(
      `${base(productId)}/${ingredientId}/grammage`,
      { grammage },
    )
    return data
  },

  async remove(productId: string, productIngredientId: string): Promise<void> {
    await api.delete(`${base(productId)}/${productIngredientId}`)
  },

  async clear(productId: string): Promise<number> {
    const { data } = await api.delete<{ deleted: number }>(base(productId))
    return data.deleted
  },
}

// Alias para backward-compat enquanto refatoramos chamadores
export const recipeItemService = productIngredientService
