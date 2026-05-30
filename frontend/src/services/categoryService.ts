import api from './api'
import type { CategoryRequest, CategoryResponse } from '@/types/Category'
import type { Page, PageParams } from '@/types/Page'
import { DEFAULT_PAGE_SIZE } from '@/types/Page'

export const categoryService = {
  async findAll(params: PageParams = {}): Promise<Page<CategoryResponse>> {
    const { data } = await api.get<Page<CategoryResponse>>('/categories', {
      params: {
        search: params.search ?? '',
        page: params.page ?? 0,
        size: params.size ?? DEFAULT_PAGE_SIZE,
      },
    })
    return data
  },

  async findById(id: string): Promise<CategoryResponse> {
    const { data } = await api.get<CategoryResponse>(`/categories/${id}`)
    return data
  },

  async create(request: CategoryRequest): Promise<CategoryResponse> {
    const { data } = await api.post<CategoryResponse>('/categories', request)
    return data
  },

  async update(id: string, request: CategoryRequest): Promise<CategoryResponse> {
    const { data } = await api.put<CategoryResponse>(`/categories/${id}`, request)
    return data
  },

  async remove(id: string): Promise<void> {
    await api.delete(`/categories/${id}`)
  },
}
