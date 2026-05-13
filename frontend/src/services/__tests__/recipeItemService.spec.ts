import { describe, it, expect, vi, beforeEach } from 'vitest'
import { recipeItemService } from '@/services/recipeItemService'
import api from '@/services/api'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('recipeItemService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('findByProductId should GET /products/:id/recipe-items', async () => {
    const mockData = [
      {
        id: 'r1',
        productId: 'p1',
        ingredientId: 'i1',
        ingredientName: 'Farinha',
        ingredientUnit: 'kg',
        quantity: 0.5,
        costPerUnit: 5.0,
        totalCost: 2.5,
      },
    ]
    vi.mocked(api.get).mockResolvedValue({ data: mockData })

    const result = await recipeItemService.findByProductId('p1')

    expect(api.get).toHaveBeenCalledWith('/products/p1/recipe-items')
    expect(result).toEqual(mockData)
  })

  it('add should POST /products/:id/recipe-items', async () => {
    const request = { ingredientId: 'i1', quantity: 0.5 }
    const mockData = {
      id: 'r1',
      productId: 'p1',
      ingredientId: 'i1',
      ingredientName: 'Farinha',
      ingredientUnit: 'kg',
      quantity: 0.5,
      costPerUnit: 5.0,
      totalCost: 2.5,
    }
    vi.mocked(api.post).mockResolvedValue({ data: mockData })

    const result = await recipeItemService.add('p1', request)

    expect(api.post).toHaveBeenCalledWith('/products/p1/recipe-items', request)
    expect(result).toEqual(mockData)
  })

  it('update should PUT /products/:productId/recipe-items/:recipeItemId', async () => {
    const request = { ingredientId: 'i1', quantity: 1.0 }
    const mockData = {
      id: 'r1',
      productId: 'p1',
      ingredientId: 'i1',
      ingredientName: 'Farinha',
      ingredientUnit: 'kg',
      quantity: 1.0,
      costPerUnit: 5.0,
      totalCost: 5.0,
    }
    vi.mocked(api.put).mockResolvedValue({ data: mockData })

    const result = await recipeItemService.update('p1', 'r1', request)

    expect(api.put).toHaveBeenCalledWith('/products/p1/recipe-items/r1', request)
    expect(result).toEqual(mockData)
  })

  it('remove should DELETE /products/:productId/recipe-items/:recipeItemId', async () => {
    vi.mocked(api.delete).mockResolvedValue({})

    await recipeItemService.remove('p1', 'r1')

    expect(api.delete).toHaveBeenCalledWith('/products/p1/recipe-items/r1')
  })
})
