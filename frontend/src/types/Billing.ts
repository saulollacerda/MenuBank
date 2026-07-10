export type SubscriptionStatus = 'TRIAL' | 'ACTIVE' | 'PAST_DUE' | 'CANCELED'

export interface PlanResponse {
  id: string
  name: string
  minRevenue: number
  maxRevenue: number | null
  priceMonthly: number
  features: Record<string, unknown>
  active: boolean
  createdAt: string
}

export interface SubscriptionResponse {
  id: string
  merchantId: string
  planId: string | null
  planName: string | null
  status: SubscriptionStatus
  trialEndsAt: string | null
  currentPeriodStart: string | null
  currentPeriodEnd: string | null
  createdAt: string
  updatedAt: string
}

export interface CheckoutResponse {
  url: string
}
