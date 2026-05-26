import api from './api'
import type { FeeRequest, FeeResponse } from '@/types/Fee'
import type { Page, PageParams } from '@/types/Page'
import { DEFAULT_PAGE_SIZE } from '@/types/Page'

export const feeService = {
  async findAll(params: PageParams = {}): Promise<Page<FeeResponse>> {
    const { data } = await api.get<Page<FeeResponse>>('/fees', {
      params: {
        search: params.search ?? '',
        page: params.page ?? 0,
        size: params.size ?? DEFAULT_PAGE_SIZE,
      },
    })
    return data
  },

  async findById(id: string): Promise<FeeResponse> {
    const { data } = await api.get<FeeResponse>(`/fees/${id}`)
    return data
  },

  async create(request: FeeRequest): Promise<FeeResponse> {
    const { data } = await api.post<FeeResponse>('/fees', request)
    return data
  },

  async update(id: string, request: FeeRequest): Promise<FeeResponse> {
    const { data } = await api.put<FeeResponse>(`/fees/${id}`, request)
    return data
  },

  async remove(id: string): Promise<void> {
    await api.delete(`/fees/${id}`)
  },
}
