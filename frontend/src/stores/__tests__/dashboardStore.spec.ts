import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useDashboardStore } from '@/stores/dashboardStore'
import { dashboardService } from '@/services/dashboardService'

vi.mock('@/services/dashboardService')

const mockedService = vi.mocked(dashboardService)

// helper to mock URL APIs unavailable in jsdom
function mockBrowserDownloadAPIs() {
  const mockUrl = 'blob:mock-url'
  vi.stubGlobal('URL', {
    createObjectURL: vi.fn(() => mockUrl),
    revokeObjectURL: vi.fn(),
  })
  const mockAnchor = { href: '', download: '', click: vi.fn() }
  vi.spyOn(document, 'createElement').mockReturnValue(mockAnchor as unknown as HTMLElement)
  return { mockUrl, mockAnchor }
}

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
    store.filterMode = 'custom'
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
    store.filterMode = 'custom'
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
    store.filterMode = 'custom'
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

  describe('filterMode = month', () => {
    it('selectedMonth should be derived from selectedYear and selectedMonthNumber', () => {
      const store = useDashboardStore()
      store.selectedYear = 2026
      store.selectedMonthNumber = 3
      expect(store.selectedMonth).toBe('2026-03')
    })

    it('resolvedStartDate should be first day of selected month', () => {
      const store = useDashboardStore()
      store.filterMode = 'month'
      store.selectedYear = 2026
      store.selectedMonthNumber = 3
      expect(store.resolvedStartDate).toBe('2026-03-01')
    })

    it('resolvedEndDate should be last day of selected month', () => {
      const store = useDashboardStore()
      store.filterMode = 'month'
      store.selectedYear = 2026
      store.selectedMonthNumber = 2
      expect(store.resolvedEndDate).toBe('2026-02-28')
    })

    it('resolvedEndDate should handle leap year February', () => {
      const store = useDashboardStore()
      store.filterMode = 'month'
      store.selectedYear = 2024
      store.selectedMonthNumber = 2
      expect(store.resolvedEndDate).toBe('2024-02-29')
    })

    it('fetchDashboard should pass resolved month dates to service', async () => {
      const mockData = { totalSales: 0, orderCount: 0, averageTicket: 0, estimatedProfit: 0, salesByDay: [], topProducts: [] }
      mockedService.getDashboard.mockResolvedValue(mockData)

      const store = useDashboardStore()
      store.filterMode = 'month'
      store.selectedYear = 2026
      store.selectedMonthNumber = 3
      await store.fetchDashboard()

      expect(mockedService.getDashboard).toHaveBeenCalledWith('2026-03-01', '2026-03-31')
    })
  })

  describe('filterMode = custom', () => {
    it('resolvedStartDate and resolvedEndDate should use custom dates', () => {
      const store = useDashboardStore()
      store.filterMode = 'custom'
      store.startDate = '2026-03-05'
      store.endDate = '2026-03-20'
      expect(store.resolvedStartDate).toBe('2026-03-05')
      expect(store.resolvedEndDate).toBe('2026-03-20')
    })
  })

  describe('exportDashboard', () => {
    it('should call exportDashboard service with resolved dates', async () => {
      const { mockAnchor } = mockBrowserDownloadAPIs()
      const mockBlob = new Blob(['xlsx'])
      mockedService.exportDashboard.mockResolvedValue(mockBlob)

      const store = useDashboardStore()
      store.filterMode = 'month'
      store.selectedYear = 2026
      store.selectedMonthNumber = 3
      await store.exportDashboard()

      expect(mockedService.exportDashboard).toHaveBeenCalledWith('2026-03-01', '2026-03-31')
      expect(mockAnchor.click).toHaveBeenCalled()
    })

    it('should set exporting to false after completion', async () => {
      mockBrowserDownloadAPIs()
      mockedService.exportDashboard.mockResolvedValue(new Blob(['xlsx']))

      const store = useDashboardStore()
      await store.exportDashboard()

      expect(store.exporting).toBe(false)
    })
  })

  describe('exportDayClosing', () => {
    it('should call service with today as both startDate and endDate', async () => {
      mockBrowserDownloadAPIs()
      mockedService.exportDashboard.mockResolvedValue(new Blob(['xlsx']))

      const today = new Date()
      const y = today.getFullYear()
      const m = String(today.getMonth() + 1).padStart(2, '0')
      const d = String(today.getDate()).padStart(2, '0')
      const iso = `${y}-${m}-${d}`

      const store = useDashboardStore()
      // even if filter is set to a different month, day closing must use today
      store.filterMode = 'month'
      store.selectedYear = 2026
      store.selectedMonthNumber = 1
      await store.exportDayClosing()

      expect(mockedService.exportDashboard).toHaveBeenCalledWith(iso, iso)
    })

    it('should set exporting to false after completion', async () => {
      mockBrowserDownloadAPIs()
      mockedService.exportDashboard.mockResolvedValue(new Blob(['xlsx']))

      const store = useDashboardStore()
      await store.exportDayClosing()

      expect(store.exporting).toBe(false)
    })
  })
})

