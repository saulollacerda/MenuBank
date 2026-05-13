import { describe, it, expect, vi, beforeEach } from 'vitest'
import { dashboardService } from '@/services/dashboardService'
import api from '@/services/api'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('dashboardService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getDashboard should GET /dashboard without params when no dates provided', async () => {
    const mockData = {
      totalSales: 1000.0,
      orderCount: 10,
      averageTicket: 100.0,
      estimatedProfit: 400.0,
      salesByDay: [],
      topProducts: [],
    }
    vi.mocked(api.get).mockResolvedValue({ data: mockData })

    const result = await dashboardService.getDashboard()

    expect(api.get).toHaveBeenCalledWith('/dashboard', { params: {} })
    expect(result).toEqual(mockData)
  })

  it('getDashboard should GET /dashboard with date params', async () => {
    const mockData = {
      totalSales: 500.0,
      orderCount: 5,
      averageTicket: 100.0,
      estimatedProfit: 200.0,
      salesByDay: [{ date: '2026-03-24', total: 500.0 }],
      topProducts: [{ productName: 'Hambúrguer', quantitySold: 10 }],
    }
    vi.mocked(api.get).mockResolvedValue({ data: mockData })

    const result = await dashboardService.getDashboard('2026-03-01', '2026-03-24')

    expect(api.get).toHaveBeenCalledWith('/dashboard', {
      params: { startDate: '2026-03-01', endDate: '2026-03-24' },
    })
    expect(result).toEqual(mockData)
  })
})
