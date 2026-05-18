import api from './api'
import type { AnotaAIKeyRequest, UserResponse } from '@/types/User'

export const userService = {
  async getById(id: string): Promise<UserResponse> {
    const { data } = await api.get<UserResponse>(`/users/${id}`)
    return data
  },

  async updateAnotaAIKey(request: AnotaAIKeyRequest): Promise<UserResponse> {
    const { data } = await api.put<UserResponse>('/users/me/anota-ai-key', request)
    return data
  },
}
