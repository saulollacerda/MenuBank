import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'

let dashboardStoreMock: any
let orderStoreMock: any
let notificationStoreMock: any

vi.mock('@/stores/dashboardStore', () => ({
  useDashboardStore: () => dashboardStoreMock,
}))

vi.mock('@/stores/orderStore', () => ({
  useOrderStore: () => orderStoreMock,
}))

vi.mock('@/stores/notificationStore', () => ({
  useNotificationStore: () => notificationStoreMock,
}))

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({ restaurantName: 'Cantina' }),
}))

vi.mock('@/composables/usePolling', () => ({
  usePolling: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

vi.mock('@/components/NotificationBell.vue', () => ({
  default: { template: '<div data-testid="bell-stub" />' },
}))

import DashboardView from '@/views/DashboardView.vue'

describe('DashboardView — seletor de período', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-10T12:00:00'))
    dashboardStoreMock = {
      selectedMonthNumber: 7,
      selectedYear: 2026,
      data: null,
      error: null,
      loading: false,
      exporting: false,
      fetchDashboard: vi.fn(async () => {}),
      exportDashboard: vi.fn(),
      exportDayClosing: vi.fn(),
    }
    orderStoreMock = { items: [], fetchPage: vi.fn(async () => {}) }
    notificationStoreMock = { items: [], fetchAll: vi.fn(async () => {}) }
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renderiza o seletor de período com o rótulo do mês selecionado', () => {
    const wrapper = mount(DashboardView)
    const toggle = wrapper.find('[data-testid="period-picker-toggle"]')
    expect(toggle.exists()).toBe(true)
    expect(toggle.text()).toContain('Julho 2026')
  })

  it('não renderiza mais os selects de mês e ano', () => {
    const wrapper = mount(DashboardView)
    expect(wrapper.findAll('select')).toHaveLength(0)
  })

  it('selecionar um mês no seletor atualiza o store', async () => {
    const wrapper = mount(DashboardView)
    await wrapper.find('[data-testid="period-picker-toggle"]').trigger('click')
    await wrapper.find('[data-testid="period-month-2"]').trigger('click')
    expect(dashboardStoreMock.selectedMonthNumber).toBe(2)
  })
})
