import { describe, it, expect, vi, beforeEach } from 'vitest'
import { orderService } from '@/services/orderService'
import api from '@/services/api'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('orderService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('findAll should GET /orders', async () => {
    const mockData = [
      {
        id: '1',
        dateTime: '2026-03-24T10:00:00',
        customerId: 'c1',
        customerName: 'João',
        status: 'PENDING',
        totalValue: 50.0,
        estimatedProfit: 20.0,
        items: [],
      },
    ]
    vi.mocked(api.get).mockResolvedValue({ data: mockData })

    const result = await orderService.findAll()

    expect(api.get).toHaveBeenCalledWith('/orders')
    expect(result).toEqual(mockData)
  })

  it('create should POST /orders', async () => {
    const request = {
      customerId: 'c1',
      items: [{ productId: 'p1', quantity: 2 }],
    }
    const mockData = {
      id: '1',
      dateTime: '2026-03-24T10:00:00',
      customerId: 'c1',
      customerName: 'João',
      status: 'PENDING',
      totalValue: 50.0,
      estimatedProfit: 20.0,
      items: [{ id: 'oi1', productId: 'p1', productName: 'Hambúrguer', quantity: 2, unitPrice: 25.0 }],
    }
    vi.mocked(api.post).mockResolvedValue({ data: mockData })

    const result = await orderService.create(request)

    expect(api.post).toHaveBeenCalledWith('/orders', request)
    expect(result).toEqual(mockData)
  })

  it('remove should DELETE /orders/:id', async () => {
    vi.mocked(api.delete).mockResolvedValue({})

    await orderService.remove('1')

    expect(api.delete).toHaveBeenCalledWith('/orders/1')
  })
})
