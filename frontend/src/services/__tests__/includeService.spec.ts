import { describe, it, expect, vi, beforeEach } from 'vitest'
import { includeService } from '@/services/includeService'
import api from '@/services/api'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('includeService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('findByProductId should GET /products/:id/includes', async () => {
    const mockData = [
      {
        id: 'inc1',
        productId: 'p1',
        name: 'Copo',
        cost: 0.5,
        quantity: 1,
        totalCost: 0.5,
      },
    ]
    vi.mocked(api.get).mockResolvedValue({ data: mockData })

    const result = await includeService.findByProductId('p1')

    expect(api.get).toHaveBeenCalledWith('/products/p1/includes')
    expect(result).toEqual(mockData)
  })

  it('add should POST /products/:id/includes', async () => {
    const request = { name: 'Copo', cost: 0.5, quantity: 1 }
    const mockData = {
      id: 'inc1',
      productId: 'p1',
      name: 'Copo',
      cost: 0.5,
      quantity: 1,
      totalCost: 0.5,
    }
    vi.mocked(api.post).mockResolvedValue({ data: mockData })

    const result = await includeService.add('p1', request)

    expect(api.post).toHaveBeenCalledWith('/products/p1/includes', request)
    expect(result).toEqual(mockData)
  })

  it('update should PUT /products/:productId/includes/:includeId', async () => {
    const request = { name: 'Copo Grande', cost: 0.8, quantity: 1 }
    const mockData = {
      id: 'inc1',
      productId: 'p1',
      name: 'Copo Grande',
      cost: 0.8,
      quantity: 1,
      totalCost: 0.8,
    }
    vi.mocked(api.put).mockResolvedValue({ data: mockData })

    const result = await includeService.update('p1', 'inc1', request)

    expect(api.put).toHaveBeenCalledWith('/products/p1/includes/inc1', request)
    expect(result).toEqual(mockData)
  })

  it('remove should DELETE /products/:productId/includes/:includeId', async () => {
    vi.mocked(api.delete).mockResolvedValue({})

    await includeService.remove('p1', 'inc1')

    expect(api.delete).toHaveBeenCalledWith('/products/p1/includes/inc1')
  })

  it('addBatch should POST /products/:id/includes/batch', async () => {
    const requests = [
      { name: 'Copo', cost: 0.5, quantity: 1 },
      { name: 'Colher', cost: 0.1, quantity: 1 },
    ]
    vi.mocked(api.post).mockResolvedValue({ data: [] })

    await includeService.addBatch('p1', requests)

    expect(api.post).toHaveBeenCalledWith('/products/p1/includes/batch', requests)
  })

  it('clear should DELETE /products/:id/includes', async () => {
    vi.mocked(api.delete).mockResolvedValue({ data: { deleted: 5 } })

    const count = await includeService.clear('p1')

    expect(api.delete).toHaveBeenCalledWith('/products/p1/includes')
    expect(count).toBe(5)
  })
})
