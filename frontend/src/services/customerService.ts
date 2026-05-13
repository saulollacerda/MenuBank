import api from './api'
import type { CustomerRequest, CustomerResponse } from '@/types/Customer'

export const customerService = {
  async findAll(): Promise<CustomerResponse[]> {
    const { data } = await api.get<CustomerResponse[]>('/customers')
    return data
  },

  async findById(id: string): Promise<CustomerResponse> {
    const { data } = await api.get<CustomerResponse>(`/customers/${id}`)
    return data
  },

  async create(request: CustomerRequest): Promise<CustomerResponse> {
    const { data } = await api.post<CustomerResponse>('/customers', request)
    return data
  },

  async update(id: string, request: CustomerRequest): Promise<CustomerResponse> {
    const { data } = await api.put<CustomerResponse>(`/customers/${id}`, request)
    return data
  },

  async remove(id: string): Promise<void> {
    await api.delete(`/customers/${id}`)
  },
}

