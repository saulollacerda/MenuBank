import api from './api'
import type { CategoryRequest, CategoryResponse } from '@/types/Category'

export const categoryService = {
  async findAll(): Promise<CategoryResponse[]> {
    const { data } = await api.get<CategoryResponse[]>('/categories')
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

