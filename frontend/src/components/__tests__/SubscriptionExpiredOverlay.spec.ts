import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { reactive } from 'vue'
import SubscriptionExpiredOverlay from '@/components/SubscriptionExpiredOverlay.vue'
import { billingService } from '@/services/billingService'
import type { SubscriptionResponse } from '@/types/Billing'

vi.mock('@/services/billingService', () => ({
  billingService: {
    listPlans: vi.fn(),
    getMySubscription: vi.fn(),
    createCheckout: vi.fn(),
  },
}))

const mockRoute = reactive({ path: '/dashboard', query: {} as Record<string, string> })
const mockPush = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => ({ push: mockPush, replace: vi.fn() }),
}))

const mockedBilling = vi.mocked(billingService)

const PAST = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString()
const FUTURE = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString()

function subscriptionOf(overrides: Partial<SubscriptionResponse> = {}): SubscriptionResponse {
  return {
    id: 'sub-1',
    merchantId: 'm-1',
    planId: null,
    planName: null,
    status: 'TRIAL',
    trialEndsAt: PAST,
    currentPeriodStart: null,
    currentPeriodEnd: null,
    createdAt: '2026-07-01T10:00:00',
    updatedAt: '2026-07-01T10:00:00',
    ...overrides,
  }
}

async function mountOverlay(subscription: SubscriptionResponse) {
  mockedBilling.getMySubscription.mockResolvedValue(subscription)
  const wrapper = mount(SubscriptionExpiredOverlay)
  await flushPromises()
  return wrapper
}

describe('SubscriptionExpiredOverlay', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mockRoute.path = '/dashboard'
    mockRoute.query = {}
  })

  it('bloqueia a tela quando o trial expirou', async () => {
    const wrapper = await mountOverlay(subscriptionOf())

    const overlay = wrapper.find('[data-testid="subscription-expired-overlay"]')
    expect(overlay.exists()).toBe(true)
    expect(overlay.text()).toContain('assinatura expirou')
  })

  it('não aparece quando a assinatura está vigente', async () => {
    const wrapper = await mountOverlay(subscriptionOf({ trialEndsAt: FUTURE }))

    expect(wrapper.find('[data-testid="subscription-expired-overlay"]').exists()).toBe(false)
  })

  it('não bloqueia a seção de plano e pagamento (para permitir renovar)', async () => {
    mockRoute.path = '/settings'
    mockRoute.query = { section: 'billing' }
    const wrapper = await mountOverlay(subscriptionOf())

    expect(wrapper.find('[data-testid="subscription-expired-overlay"]').exists()).toBe(false)
  })

  it('botão de renovar leva à seção de plano e pagamento', async () => {
    const wrapper = await mountOverlay(subscriptionOf())

    await wrapper.find('[data-testid="subscription-expired-renew"]').trigger('click')

    expect(mockPush).toHaveBeenCalledWith({ path: '/settings', query: { section: 'billing' } })
  })
})
