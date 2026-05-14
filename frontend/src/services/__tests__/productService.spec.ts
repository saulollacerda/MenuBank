import { describe, it, expect, vi, beforeEach } from 'vitest'
import { productService } from '@/services/productService'
import api from '@/services/api'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('productService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('findAll should GET /products', async () => {
    const mockData = [
      {
        id: '1',
        name: 'Hambúrguer',
        price: 25.0,
        estimatedCost: 10.0,
        margin: 15.0,
        status: 'ACTIVE',
        cmv: 10.0,
      },
    ]
    vi.mocked(api.get).mockResolvedValue({ data: mockData })

    const result = await productService.findAll()

    expect(api.get).toHaveBeenCalledWith('/products')
    expect(result).toEqual(mockData)
  })

  it('findById should GET /products/:id', async () => {
    const mockData = {
      id: '1',
      name: 'Hambúrguer',
      price: 25.0,
      estimatedCost: 10.0,
      margin: 15.0,
      status: 'ACTIVE',
      cmv: 10.0,
    }
    vi.mocked(api.get).mockResolvedValue({ data: mockData })

    const result = await productService.findById('1')

    expect(api.get).toHaveBeenCalledWith('/products/1')
    expect(result).toEqual(mockData)
  })

  it('create should POST /products', async () => {
    const request = { name: 'Hambúrguer', price: 25.0, categoryId: 'cat1' }
    const mockData = {
      id: '1',
      ...request,
      estimatedCost: null,
      margin: null,
      status: 'ACTIVE',
      cmv: null,
      categoryName: 'Lanches',
    }
    vi.mocked(api.post).mockResolvedValue({ data: mockData })

    const result = await productService.create(request)

    expect(api.post).toHaveBeenCalledWith('/products', request)
    expect(result).toEqual(mockData)
  })

  it('update should PUT /products/:id', async () => {
    const request = { name: 'Hambúrguer Especial', price: 30.0, categoryId: 'cat1' }
    const mockData = {
      id: '1',
      ...request,
      estimatedCost: 10.0,
      margin: 20.0,
      status: 'ACTIVE',
      cmv: 10.0,
      categoryName: 'Lanches',
    }
    vi.mocked(api.put).mockResolvedValue({ data: mockData })

    const result = await productService.update('1', request)

    expect(api.put).toHaveBeenCalledWith('/products/1', request)
    expect(result).toEqual(mockData)
  })

  it('remove should DELETE /products/:id', async () => {
    vi.mocked(api.delete).mockResolvedValue({})

    await productService.remove('1')

    expect(api.delete).toHaveBeenCalledWith('/products/1')
  })
})
