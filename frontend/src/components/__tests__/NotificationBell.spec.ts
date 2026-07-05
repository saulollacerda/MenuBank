import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import NotificationBell from '@/components/NotificationBell.vue'
import { notificationService } from '@/services/notificationService'
import type { NotificationResponse } from '@/types/Notification'

vi.mock('@/services/notificationService')

const mockedService = vi.mocked(notificationService)

function buildNotification(overrides: Partial<NotificationResponse> = {}): NotificationResponse {
  return {
    id: 'n-1',
    type: 'MISSING_INGREDIENT',
    title: 'Ingrediente não cadastrado',
    message: "O ingrediente 'Pistache' apareceu em um pedido mas não está cadastrado.",
    referenceData: 'pistache',
    referenceDisplay: 'Pistache',
    status: 'UNREAD',
    createdAt: '2026-05-22T12:00:00Z',
    resolvedAt: null,
    ...overrides,
  }
}

function asPage<T>(content: T[]) {
  return {
    content,
    totalElements: content.length,
    totalPages: content.length === 0 ? 0 : 1,
    number: 0,
    size: 20,
    first: true,
    last: true,
    empty: content.length === 0,
  }
}

async function mountAndOpen(items: NotificationResponse[]) {
  mockedService.findAll.mockResolvedValue(asPage(items))
  mockedService.unreadCount.mockResolvedValue(items.length)

  const wrapper = mount(NotificationBell)
  await wrapper.find('[data-testid="notification-bell-button"]').trigger('click')
  await flushPromises()
  return wrapper
}

describe('NotificationBell', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('MISSING_INGREDIENT não resolvida mostra ações "Cadastrar ingrediente" e "Descartar"', async () => {
    const wrapper = await mountAndOpen([buildNotification({ id: 'n-1', type: 'MISSING_INGREDIENT' })])

    expect(wrapper.find('[data-testid="notification-n-1-action-button"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="notification-n-1-dismiss-button"]').exists()).toBe(true)
  })

  it('MISSING_PRODUCT não resolvida mostra "Descartar" mas não "Cadastrar ingrediente"', async () => {
    const wrapper = await mountAndOpen([
      buildNotification({
        id: 'n-2',
        type: 'MISSING_PRODUCT',
        title: 'Produto não cadastrado',
        message: "O produto 'Produto Fantasma' apareceu em um pedido mas não está cadastrado.",
        referenceData: 'produto fantasma',
        referenceDisplay: 'Produto Fantasma',
      }),
    ])

    expect(wrapper.find('[data-testid="notification-n-2-action-button"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="notification-n-2-dismiss-button"]').exists()).toBe(true)
  })
})
