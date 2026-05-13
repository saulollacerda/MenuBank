import api from './api'
import type { OrderRequest, OrderResponse } from '@/types/Order'

export const orderService = {
  async findAll(): Promise<OrderResponse[]> {
    const { data } = await api.get<OrderResponse[]>('/orders')
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

