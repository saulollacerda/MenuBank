import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { reactive } from 'vue'

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
    dashboardStoreMock = reactive({
      selectedMonthNumber: 7,
      selectedYear: 2026,
      data: null,
      error: null,
      loading: false,
      exporting: false,
      fetchDashboard: vi.fn(async () => {}),
      exportDashboard: vi.fn(),
      exportDayClosing: vi.fn(),
      ingredientRanking: [],
      ingredientRankingLoading: false,
      ingredientRankingError: null,
      fetchIngredientRanking: vi.fn(async () => {}),
      filterMode: 'month',
      startDate: '',
      endDate: '',
    })
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

  it('renderiza o card de margem de lucro média formatado em pt-BR', () => {
    dashboardStoreMock.data = {
      totalSales: 1000,
      orderCount: 10,
      averageTicket: 100,
      estimatedProfit: 580,
      averageMarginPct: 58,
      salesByDay: [],
      topProducts: [],
    }
    const wrapper = mount(DashboardView)
    expect(wrapper.text()).toContain('Margem de lucro média')
    expect(wrapper.text()).toContain('58,0%')
  })

  it('exibe traço quando averageMarginPct é nulo', () => {
    dashboardStoreMock.data = {
      totalSales: 0,
      orderCount: 0,
      averageTicket: 0,
      estimatedProfit: 0,
      averageMarginPct: null,
      salesByDay: [],
      topProducts: [],
    }
    const wrapper = mount(DashboardView)
    expect(wrapper.text()).toContain('Margem de lucro média')
  })

  it('renderiza o botão de fechamento com o rótulo "Fechamento do dia"', () => {
    const wrapper = mount(DashboardView)
    expect(wrapper.text()).toContain('Fechamento do dia')
  })

  it('busca o ranking de ingredientes ao montar', () => {
    mount(DashboardView)
    expect(dashboardStoreMock.fetchIngredientRanking).toHaveBeenCalled()
  })

  it('não renderiza mais o card de horário de pico', () => {
    const wrapper = mount(DashboardView)
    expect(wrapper.text()).not.toContain('Horário de pico')
  })

  it('renderiza o ranking de ingredientes com nome, gramatura e custo', () => {
    dashboardStoreMock.ingredientRanking = [
      { ingredientName: 'Carne', unit: 'kg', totalQuantity: 12.5, totalCost: 350 },
      { ingredientName: 'Farinha', unit: 'g', totalQuantity: 850, totalCost: 12.4 },
    ]
    const wrapper = mount(DashboardView)
    const text = wrapper.text()
    expect(text).toContain('Ranking de ingredientes')
    expect(text).toContain('Carne')
    expect(text).toContain('12,5 kg')
    expect(text).toContain('R$ 350,00')
    expect(text).toContain('Farinha')
    expect(text).toContain('850 g')
  })

  it('limita o ranking de ingredientes a 10 itens', () => {
    dashboardStoreMock.ingredientRanking = Array.from({ length: 15 }, (_, i) => ({
      ingredientName: `Ingrediente ${i + 1}`,
      unit: 'kg',
      totalQuantity: 15 - i,
      totalCost: 100 - i,
    }))
    const wrapper = mount(DashboardView)
    const rows = wrapper.findAll('[data-testid="ingredient-ranking-row"]')
    expect(rows).toHaveLength(10)
    expect(rows[0]!.text()).toContain('Ingrediente 1')
    expect(rows[9]!.text()).toContain('Ingrediente 10')
  })

  it('a lista do ranking de ingredientes tem rolagem própria', () => {
    dashboardStoreMock.ingredientRanking = Array.from({ length: 15 }, (_, i) => ({
      ingredientName: `Ingrediente ${i + 1}`,
      unit: 'kg',
      totalQuantity: 15 - i,
      totalCost: 100 - i,
    }))
    const wrapper = mount(DashboardView)
    const list = wrapper.find('[data-testid="ingredient-ranking-list"]')
    expect(list.exists()).toBe(true)
    expect(list.attributes('style')).toContain('overflow-y: auto')
    expect(list.attributes('style')).toContain('max-height')
  })

  it('exibe estado vazio quando não há ingredientes no período', () => {
    dashboardStoreMock.ingredientRanking = []
    dashboardStoreMock.ingredientRankingLoading = false
    const wrapper = mount(DashboardView)
    expect(wrapper.text()).toContain('Nenhum ingrediente no período.')
  })

  it('exibe estado de carregamento do ranking de ingredientes', () => {
    dashboardStoreMock.ingredientRanking = []
    dashboardStoreMock.ingredientRankingLoading = true
    const wrapper = mount(DashboardView)
    expect(wrapper.text()).toContain('Carregando…')
  })

  describe('intervalo personalizado', () => {
    async function selectRange(wrapper: ReturnType<typeof mount>) {
      await wrapper.find('[data-testid="period-picker-toggle"]').trigger('click')
      await wrapper.find('[data-testid="period-tab-custom"]').trigger('click')
      await wrapper.find('[data-testid="period-calendar-prev"]').trigger('click')
      await wrapper.find('[data-testid="period-day-2026-06-16"]').trigger('click')
      await wrapper.find('[data-testid="period-calendar-next"]').trigger('click')
      await wrapper.find('[data-testid="period-day-2026-07-07"]').trigger('click')
    }

    it('selecionar um intervalo grava filterMode, startDate e endDate no store', async () => {
      const wrapper = mount(DashboardView)
      await selectRange(wrapper)
      expect(dashboardStoreMock.filterMode).toBe('custom')
      expect(dashboardStoreMock.startDate).toBe('2026-06-16')
      expect(dashboardStoreMock.endDate).toBe('2026-07-07')
    })

    it('selecionar um intervalo dispara o refetch do dashboard e do ranking', async () => {
      const wrapper = mount(DashboardView)
      dashboardStoreMock.fetchDashboard.mockClear()
      dashboardStoreMock.fetchIngredientRanking.mockClear()
      await selectRange(wrapper)
      expect(dashboardStoreMock.fetchDashboard).toHaveBeenCalledWith(true)
      expect(dashboardStoreMock.fetchIngredientRanking).toHaveBeenCalled()
    })

    it('o subtítulo passa a exibir o intervalo selecionado', async () => {
      const wrapper = mount(DashboardView)
      await selectRange(wrapper)
      expect(wrapper.text()).toContain('16/06/2026 a 07/07/2026')
    })

    it('selecionar um mês volta o store para o modo mês e refaz a busca', async () => {
      dashboardStoreMock.filterMode = 'custom'
      dashboardStoreMock.startDate = '2026-06-16'
      dashboardStoreMock.endDate = '2026-07-07'
      const wrapper = mount(DashboardView)
      dashboardStoreMock.fetchDashboard.mockClear()
      await wrapper.find('[data-testid="period-picker-toggle"]').trigger('click')
      await wrapper.find('[data-testid="period-tab-month"]').trigger('click')
      await wrapper.find('[data-testid="period-month-4"]').trigger('click')
      expect(dashboardStoreMock.filterMode).toBe('month')
      expect(dashboardStoreMock.selectedMonthNumber).toBe(4)
      expect(dashboardStoreMock.fetchDashboard).toHaveBeenCalledWith(true)
    })
  })
})
