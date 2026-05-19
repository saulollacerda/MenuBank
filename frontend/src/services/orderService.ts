import api from './api'
import type { OrderRequest, OrderResponse } from '@/types/Order'
import type { Page, PageParams } from '@/types/Page'
import { DEFAULT_PAGE_SIZE } from '@/types/Page'

export const orderService = {
  async findAll(params: PageParams = {}): Promise<Page<OrderResponse>> {
    const { data } = await api.get<Page<OrderResponse>>('/orders', {
      params: {
        search: params.search ?? '',
        page: params.page ?? 0,
        size: params.size ?? DEFAULT_PAGE_SIZE,
      },
    })
    return data
  },

  async findById(id: string): Promise<OrderResponse> {
    const { data } = await api.get<OrderResponse>(`/orders/${id}`)
    return data
  },

  async create(request: OrderRequest): Promise<OrderResponse> {
    const { data } = await api.post<OrderResponse>('/orders', request)
    return data
  },

  async update(id: string, request: OrderRequest): Promise<OrderResponse> {
    const { data } = await api.put<OrderResponse>(`/orders/${id}`, request)
    return data
  },

  async remove(id: string): Promise<void> {
    await api.delete(`/orders/${id}`)
  },
}
