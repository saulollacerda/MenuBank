import api from './api'
import type { LoginRequest, LoginResponse } from '@/types/Auth'
import type { UserRequest } from '@/types/User'

export const authService = {
  async login(request: LoginRequest): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>('/auth/login', request)
    return data
  },

  async register(request: UserRequest): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>('/auth/register', request)
    return data
  },
}

