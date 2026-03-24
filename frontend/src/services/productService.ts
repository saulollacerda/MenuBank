import api from './api'
import type { ProductRequest, ProductResponse } from '@/types/Product'

export const productService = {
  async findAll(): Promise<ProductResponse[]> {
    const { data } = await api.get<ProductResponse[]>('/products')
    return data
  },

  async findById(id: string): Promise<ProductResponse> {
    const { data } = await api.get<ProductResponse>(`/products/${id}`)
    return data
  },

  async create(request: ProductRequest): Promise<ProductResponse> {
    const { data } = await api.post<ProductResponse>('/products', request)
    return data
  },

  async update(id: string, request: ProductRequest): Promise<ProductResponse> {
    const { data } = await api.put<ProductResponse>(`/products/${id}`, request)
    return data
  },

  async remove(id: string): Promise<void> {
    await api.delete(`/products/${id}`)
  },
}

