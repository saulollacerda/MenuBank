import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, type VueWrapper } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import type { NotificationResponse } from '@/types/Notification'
import { NOTIFICATION_TYPE_LABELS } from '@/types/Notification'
import { UIIcon } from '@/design'

let notificationStoreMock: any

vi.mock('@/stores/notificationStore', () => ({
  useNotificationStore: () => notificationStoreMock,
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

import NotificationBell from '@/components/NotificationBell.vue'

function buildNotification(overrides: Partial<NotificationResponse> = {}): NotificationResponse {
  return {
    id: 'n-1',
    type: 'MISSING_INGREDIENT',
    title: 'Ingrediente não cadastrado',
    message: "O ingrediente 'Pistache' apareceu em um pedido mas não está cadastrado.",
    referenceData: 'pistache',
    referenceDisplay: 'Pistache',
    status: 'UNREAD',
    createdAt: '2026-07-01T12:00:00Z',
    resolvedAt: null,
    ...overrides,
  }
}

async function mountOpen(): Promise<VueWrapper> {
  const wrapper = mount(NotificationBell)
  await wrapper.find('[data-testid="notification-bell-button"]').trigger('click')
  await flushPromises()
  return wrapper
}

function typeIconOf(wrapper: VueWrapper, id: string) {
  return wrapper.find(`[data-testid="notification-${id}-type-icon"]`)
}

function iconNameInside(wrapper: VueWrapper, container: Element): string | undefined {
  const icon = wrapper
    .findAllComponents(UIIcon)
    .find((c) => container.contains(c.element))
  return icon?.props('name')
}

describe('NotificationBell', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    notificationStoreMock = {
      items: [] as NotificationResponse[],
      unreadCount: 0,
      loading: false,
      fetchAll: vi.fn().mockResolvedValue(undefined),
      refreshCount: vi.fn(),
      markRead: vi.fn().mockResolvedValue(undefined),
      dismiss: vi.fn().mockResolvedValue(undefined),
    }
  })

  it('exporta label pt-BR para o tipo ORDER_CANCELLED', () => {
    expect(NOTIFICATION_TYPE_LABELS.ORDER_CANCELLED).toBe('Pedido cancelado')
    expect(NOTIFICATION_TYPE_LABELS.MISSING_INGREDIENT).toBe('Ingrediente não cadastrado')
  })

  it('exibe ícone "x" com label pt-BR para notificação ORDER_CANCELLED', async () => {
    notificationStoreMock.items = [
      buildNotification({
        id: 'n-2',
        type: 'ORDER_CANCELLED',
        title: 'Pedido cancelado',
        message: 'O pedido ord-1 foi cancelado no iFood e removido dos ganhos.',
        referenceData: 'ord-1',
        referenceDisplay: 'ord-1',
      }),
    ]

    const wrapper = await mountOpen()

    const iconBox = typeIconOf(wrapper, 'n-2')
    expect(iconBox.exists()).toBe(true)
    expect(iconBox.attributes('aria-label')).toBe('Pedido cancelado')
    expect(iconNameInside(wrapper, iconBox.element)).toBe('x')
  })

  it('mantém ícone "alert" com label pt-BR para MISSING_INGREDIENT', async () => {
    notificationStoreMock.items = [buildNotification()]

    const wrapper = await mountOpen()

    const iconBox = typeIconOf(wrapper, 'n-1')
    expect(iconBox.exists()).toBe(true)
    expect(iconBox.attributes('aria-label')).toBe('Ingrediente não cadastrado')
    expect(iconNameInside(wrapper, iconBox.element)).toBe('alert')
  })

  it('não exibe ação "Cadastrar ingrediente" para ORDER_CANCELLED, mas permite descartar', async () => {
    notificationStoreMock.items = [
      buildNotification({ id: 'n-2', type: 'ORDER_CANCELLED', title: 'Pedido cancelado' }),
    ]

    const wrapper = await mountOpen()

    expect(wrapper.find('[data-testid="notification-n-2-action-button"]').exists()).toBe(false)
    const dismiss = wrapper.find('[data-testid="notification-n-2-dismiss-button"]')
    expect(dismiss.exists()).toBe(true)

    await dismiss.trigger('click')
    expect(notificationStoreMock.dismiss).toHaveBeenCalledWith('n-2')
  })

  it('segue exibindo a ação de cadastro para MISSING_INGREDIENT', async () => {
    notificationStoreMock.items = [buildNotification()]

    const wrapper = await mountOpen()

    expect(wrapper.find('[data-testid="notification-n-1-action-button"]').exists()).toBe(true)
  })
})
