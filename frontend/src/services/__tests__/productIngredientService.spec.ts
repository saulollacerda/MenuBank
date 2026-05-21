import { describe, it, expect, vi, beforeEach } from 'vitest'
import { productIngredientService } from '@/services/productIngredientService'
import api from '@/services/api'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('productIngredientService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('findByProductId should GET /products/:id/ingredients', async () => {
    const mockData = [
      {
        id: 'r1',
        productId: 'p1',
        ingredientId: 'i1',
        ingredientName: 'Farinha',
        ingredientUnit: 'kg',
        grammage: 0.5,
        isOptional: false,
        costPerUnit: 5.0,
        totalCost: 2.5,
      },
    ]
    vi.mocked(api.get).mockResolvedValue({ data: mockData })

    const result = await productIngredientService.findByProductId('p1')

    expect(api.get).toHaveBeenCalledWith('/products/p1/ingredients')
    expect(result).toEqual(mockData)
  })

  it('add should POST /products/:id/ingredients', async () => {
    const request = { ingredientId: 'i1', grammage: 0.5, isOptional: false }
    const mockData = {
      id: 'r1',
      productId: 'p1',
      ingredientId: 'i1',
      ingredientName: 'Farinha',
      ingredientUnit: 'kg',
      grammage: 0.5,
      isOptional: false,
      costPerUnit: 5.0,
      totalCost: 2.5,
    }
    vi.mocked(api.post).mockResolvedValue({ data: mockData })

    const result = await productIngredientService.add('p1', request)

    expect(api.post).toHaveBeenCalledWith('/products/p1/ingredients', request)
    expect(result).toEqual(mockData)
  })

  it('update should PUT /products/:productId/ingredients/:productIngredientId', async () => {
    const request = { ingredientId: 'i1', grammage: 1.0, isOptional: false }
    const mockData = {
      id: 'r1',
      productId: 'p1',
      ingredientId: 'i1',
      ingredientName: 'Farinha',
      ingredientUnit: 'kg',
      grammage: 1.0,
      isOptional: false,
      costPerUnit: 5.0,
      totalCost: 5.0,
    }
    vi.mocked(api.put).mockResolvedValue({ data: mockData })

    const result = await productIngredientService.update('p1', 'r1', request)

    expect(api.put).toHaveBeenCalledWith('/products/p1/ingredients/r1', request)
    expect(result).toEqual(mockData)
  })

  it('remove should DELETE /products/:productId/ingredients/:productIngredientId', async () => {
    vi.mocked(api.delete).mockResolvedValue({})

    await productIngredientService.remove('p1', 'r1')

    expect(api.delete).toHaveBeenCalledWith('/products/p1/ingredients/r1')
  })

  it('batchAdd should POST /products/:id/ingredients/batch', async () => {
    const requests = [
      { ingredientId: 'i1', grammage: 100, isOptional: false },
      { ingredientId: 'i2', grammage: 20, isOptional: true },
    ]
    vi.mocked(api.post).mockResolvedValue({ data: [] })

    await productIngredientService.batchAdd('p1', requests)

    expect(api.post).toHaveBeenCalledWith('/products/p1/ingredients/batch', requests)
  })

  it('updateGrammageByIngredientId should PUT /products/:id/ingredients/:ingredientId/grammage', async () => {
    vi.mocked(api.put).mockResolvedValue({ data: {} })

    await productIngredientService.updateGrammageByIngredientId('p1', 'i1', 250)

    expect(api.put).toHaveBeenCalledWith('/products/p1/ingredients/i1/grammage', { grammage: 250 })
  })

  it('clear should DELETE /products/:id/ingredients', async () => {
    vi.mocked(api.delete).mockResolvedValue({ data: { deleted: 5 } })

    const count = await productIngredientService.clear('p1')

    expect(api.delete).toHaveBeenCalledWith('/products/p1/ingredients')
    expect(count).toBe(5)
  })
})
