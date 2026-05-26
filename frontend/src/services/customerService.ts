import api from './api'
import type { CustomerRequest, CustomerResponse } from '@/types/Customer'
import type { Page, PageParams } from '@/types/Page'
import { DEFAULT_PAGE_SIZE } from '@/types/Page'

export const customerService = {
  async findAll(params: PageParams = {}): Promise<Page<CustomerResponse>> {
    const { data } = await api.get<Page<CustomerResponse>>('/customers', {
      params: {
        search: params.search ?? '',
        page: params.page ?? 0,
        size: params.size ?? DEFAULT_PAGE_SIZE,
      },
    })
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
