import api from './api'
import type { ProductRequest, ProductResponse } from '@/types/Product'
import type { Page, PageParams } from '@/types/Page'
import { DEFAULT_PAGE_SIZE } from '@/types/Page'

export const productService = {
  async findAll(params: PageParams = {}): Promise<Page<ProductResponse>> {
    const { data } = await api.get<Page<ProductResponse>>('/products', {
      params: {
        search: params.search ?? '',
        page: params.page ?? 0,
        size: params.size ?? DEFAULT_PAGE_SIZE,
      },
    })
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
