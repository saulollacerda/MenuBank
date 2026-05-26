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

  it('findAll should GET /orders with pagination params', async () => {
    const mockPage = {
      content: [
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
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
      first: true,
      last: true,
      empty: false,
    }
    vi.mocked(api.get).mockResolvedValue({ data: mockPage })

    const result = await orderService.findAll()

    expect(api.get).toHaveBeenCalledWith('/orders', {
      params: { search: '', page: 0, size: 20 },
    })
    expect(result).toEqual(mockPage)
  })

  it('create should POST /orders', async () => {
    const request = {
      customerId: 'c1',
      items: [
        {
          productId: 'p1',
          quantity: 2,
          extraIngredients: [{ ingredientId: 'i1', quantity: 1.5 }],
        },
      ],
    }
    const mockData = {
      id: '1',
      dateTime: '2026-03-24T10:00:00',
      customerId: 'c1',
      customerName: 'João',
      status: 'PENDING',
      totalValue: 50.0,
      estimatedProfit: 20.0,
      items: [
        {
          id: 'oi1',
          productId: 'p1',
          productName: 'Hambúrguer',
          quantity: 2,
          unitPrice: 25.0,
          extraIngredients: [
            {
              id: 'e1',
              ingredientId: 'i1',
              ingredientName: 'Bacon',
              ingredientUnit: 'g',
              quantity: 1.5,
              costPerUnit: 0.1,
              totalCost: 0.3,
            },
          ],
        },
      ],
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
