import api from './api'
import type { CheckoutResponse, PlanResponse, SubscriptionResponse } from '@/types/Billing'

export const billingService = {
  async listPlans(): Promise<PlanResponse[]> {
    const { data } = await api.get<PlanResponse[]>('/plans')
    return data
  },

  async getMySubscription(): Promise<SubscriptionResponse> {
    const { data } = await api.get<SubscriptionResponse>('/subscription/me')
    return data
  },

  async createCheckout(planId: string): Promise<CheckoutResponse> {
    const { data } = await api.post<CheckoutResponse>('/subscription/checkout', { planId })
    return data
  },
}
