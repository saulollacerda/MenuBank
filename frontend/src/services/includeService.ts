import api from './api'
import type { IncludeRequest, IncludeResponse } from '@/types/Product'

const base = (productId: string) => `/products/${productId}/includes`

export const includeService = {
  async findByProductId(productId: string): Promise<IncludeResponse[]> {
    const { data } = await api.get<IncludeResponse[]>(base(productId))
    return data
  },

  async add(productId: string, request: IncludeRequest): Promise<IncludeResponse> {
    const { data } = await api.post<IncludeResponse>(base(productId), request)
    return data
  },

  async addBatch(productId: string, requests: IncludeRequest[]): Promise<IncludeResponse[]> {
    const { data } = await api.post<IncludeResponse[]>(`${base(productId)}/batch`, requests)
    return data
  },

  async update(
    productId: string,
    includeId: string,
    request: IncludeRequest,
  ): Promise<IncludeResponse> {
    const { data } = await api.put<IncludeResponse>(`${base(productId)}/${includeId}`, request)
    return data
  },

  async remove(productId: string, includeId: string): Promise<void> {
    await api.delete(`${base(productId)}/${includeId}`)
  },

  async clear(productId: string): Promise<number> {
    const { data } = await api.delete<{ deleted: number }>(base(productId))
    return data.deleted
  },
}
