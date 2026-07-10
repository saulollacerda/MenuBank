import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useSubscriptionStore } from '@/stores/subscriptionStore'
import { billingService } from '@/services/billingService'
import type { SubscriptionResponse } from '@/types/Billing'

vi.mock('@/services/billingService', () => ({
  billingService: {
    listPlans: vi.fn(),
    getMySubscription: vi.fn(),
    createCheckout: vi.fn(),
  },
}))

const mockedBilling = vi.mocked(billingService)

function subscriptionOf(overrides: Partial<SubscriptionResponse> = {}): SubscriptionResponse {
  return {
    id: 'sub-1',
    merchantId: 'm-1',
    planId: null,
    planName: null,
    status: 'TRIAL',
    trialEndsAt: null,
    currentPeriodStart: null,
    currentPeriodEnd: null,
    createdAt: '2026-07-01T10:00:00',
    updatedAt: '2026-07-01T10:00:00',
    ...overrides,
  }
}

const FUTURE = new Date(Date.now() + 5 * 24 * 60 * 60 * 1000).toISOString()
const PAST = new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString()

describe('subscriptionStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetch carrega a assinatura do merchant', async () => {
    mockedBilling.getMySubscription.mockResolvedValue(subscriptionOf({ trialEndsAt: FUTURE }))
    const store = useSubscriptionStore()

    await store.fetch()

    expect(mockedBilling.getMySubscription).toHaveBeenCalled()
    expect(store.subscription?.id).toBe('sub-1')
  })

  it('isExpired é false para trial ainda vigente', async () => {
    mockedBilling.getMySubscription.mockResolvedValue(subscriptionOf({ trialEndsAt: FUTURE }))
    const store = useSubscriptionStore()
    await store.fetch()

    expect(store.isExpired).toBe(false)
  })

  it('isExpired é true para trial vencido', async () => {
    mockedBilling.getMySubscription.mockResolvedValue(subscriptionOf({ trialEndsAt: PAST }))
    const store = useSubscriptionStore()
    await store.fetch()

    expect(store.isExpired).toBe(true)
  })

  it('isExpired é false para assinatura ativa com período vigente', async () => {
    mockedBilling.getMySubscription.mockResolvedValue(
      subscriptionOf({ status: 'ACTIVE', currentPeriodEnd: FUTURE }),
    )
    const store = useSubscriptionStore()
    await store.fetch()

    expect(store.isExpired).toBe(false)
  })

  it('isExpired é true para assinatura ativa com período encerrado', async () => {
    mockedBilling.getMySubscription.mockResolvedValue(
      subscriptionOf({ status: 'ACTIVE', currentPeriodEnd: PAST }),
    )
    const store = useSubscriptionStore()
    await store.fetch()

    expect(store.isExpired).toBe(true)
  })

  it.each(['PAST_DUE', 'CANCELED'] as const)('isExpired é true para status %s', async (status) => {
    mockedBilling.getMySubscription.mockResolvedValue(subscriptionOf({ status }))
    const store = useSubscriptionStore()
    await store.fetch()

    expect(store.isExpired).toBe(true)
  })

  it('isExpired é false antes de carregar', () => {
    const store = useSubscriptionStore()

    expect(store.isExpired).toBe(false)
  })

  it('falha ao carregar não bloqueia o uso (fail-open)', async () => {
    mockedBilling.getMySubscription.mockRejectedValue(new Error('network'))
    const store = useSubscriptionStore()

    await store.fetch()

    expect(store.subscription).toBeNull()
    expect(store.isExpired).toBe(false)
  })
})
