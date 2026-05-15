import api from './api'
import type { PaymentMethodRequest, PaymentMethodResponse } from '@/types/PaymentMethod'

export const paymentMethodService = {
  async findAll(): Promise<PaymentMethodResponse[]> {
    const { data } = await api.get<PaymentMethodResponse[]>('/payment-methods')
    return data
  },

  async findById(id: string): Promise<PaymentMethodResponse> {
    const { data } = await api.get<PaymentMethodResponse>(`/payment-methods/${id}`)
    return data
  },

  async create(request: PaymentMethodRequest): Promise<PaymentMethodResponse> {
    const { data } = await api.post<PaymentMethodResponse>('/payment-methods', request)
    return data
  },

  async update(id: string, request: PaymentMethodRequest): Promise<PaymentMethodResponse> {
    const { data } = await api.put<PaymentMethodResponse>(`/payment-methods/${id}`, request)
    return data
  },

  async remove(id: string): Promise<void> {
    await api.delete(`/payment-methods/${id}`)
  },
}
