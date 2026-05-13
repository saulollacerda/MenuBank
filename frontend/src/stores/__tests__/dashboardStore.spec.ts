import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useDashboardStore } from '@/stores/dashboardStore'
import { dashboardService } from '@/services/dashboardService'

vi.mock('@/services/dashboardService')

const mockedService = vi.mocked(dashboardService)

describe('dashboardStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchDashboard should populate data', async () => {
    const mockData = {
      totalSales: 1000.0,
      orderCount: 10,
      averageTicket: 100.0,
      estimatedProfit: 400.0,
      salesByDay: [{ date: '2026-03-24', total: 1000.0 }],
      topProducts: [{ productName: 'Hambúrguer', quantitySold: 20 }],
    }
    mockedService.getDashboard.mockResolvedValue(mockData)

    const store = useDashboardStore()
    await store.fetchDashboard()

    expect(store.data).toEqual(mockData)
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('fetchDashboard should not call service again when filters did not change', async () => {
    const mockData = {
      totalSales: 1000.0,
      orderCount: 10,
      averageTicket: 100.0,
      estimatedProfit: 400.0,
      salesByDay: [{ date: '2026-03-24', total: 1000.0 }],
      topProducts: [{ productName: 'Hambúrguer', quantitySold: 20 }],
    }
    mockedService.getDashboard.mockResolvedValue(mockData)

    const store = useDashboardStore()
    store.startDate = '2026-03-24'
    store.endDate = '2026-03-24'

    await store.fetchDashboard()
    await store.fetchDashboard()

    expect(mockedService.getDashboard).toHaveBeenCalledTimes(1)
    expect(store.data).toEqual(mockData)
  })

  it('fetchDashboard should refetch when date filters change', async () => {
    const mockData1 = {
      totalSales: 1000.0,
      orderCount: 10,
      averageTicket: 100.0,
      estimatedProfit: 400.0,
      salesByDay: [],
      topProducts: [],
    }
    const mockData2 = {
      totalSales: 500.0,
      orderCount: 5,
      averageTicket: 100.0,
      estimatedProfit: 200.0,
      salesByDay: [],
      topProducts: [],
    }
    mockedService.getDashboard.mockResolvedValueOnce(mockData1).mockResolvedValueOnce(mockData2)

    const store = useDashboardStore()
    store.startDate = '2026-03-01'
    store.endDate = '2026-03-24'
    await store.fetchDashboard()

    store.startDate = '2026-02-01'
    store.endDate = '2026-02-28'
    await store.fetchDashboard()

    expect(mockedService.getDashboard).toHaveBeenCalledTimes(2)
    expect(store.data).toEqual(mockData2)
  })

  it('fetchDashboard should pass date filters', async () => {
    const mockData = {
      totalSales: 500.0,
      orderCount: 5,
      averageTicket: 100.0,
      estimatedProfit: 200.0,
      salesByDay: [],
      topProducts: [],
    }
    mockedService.getDashboard.mockResolvedValue(mockData)

    const store = useDashboardStore()
    store.startDate = '2026-03-01'
    store.endDate = '2026-03-24'
    await store.fetchDashboard()

    expect(mockedService.getDashboard).toHaveBeenCalledWith('2026-03-01', '2026-03-24')
  })

  it('fetchDashboard should set error on failure', async () => {
    mockedService.getDashboard.mockRejectedValue(new Error('Server error'))

    const store = useDashboardStore()

    await expect(store.fetchDashboard()).rejects.toThrow()
    expect(store.error).toBe('Erro ao carregar dashboard')
    expect(store.loading).toBe(false)
  })
})

