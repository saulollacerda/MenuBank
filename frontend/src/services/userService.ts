import api from './api'
import type { AnotaAIKeyRequest, UserResponse } from '@/types/User'

export const userService = {
  /** Current authenticated merchant (resolved server-side from the token). */
  async getMe(): Promise<UserResponse> {
    const { data } = await api.get<UserResponse>('/merchants/me')
    return data
  },

  async updateAnotaAIKey(request: AnotaAIKeyRequest): Promise<UserResponse> {
    const { data } = await api.put<UserResponse>('/merchants/me/anota-ai-key', request)
    return data
  },
}
