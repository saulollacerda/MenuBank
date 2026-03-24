import { describe, it, expect, vi, beforeEach } from 'vitest'
import { categoryService } from '@/services/categoryService'
import api from '@/services/api'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('categoryService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('findAll should GET /categories', async () => {
    const mockData = [{ id: '1', name: 'Bebidas' }]
    vi.mocked(api.get).mockResolvedValue({ data: mockData })

    const result = await categoryService.findAll()

    expect(api.get).toHaveBeenCalledWith('/categories')
    expect(result).toEqual(mockData)
  })

  it('findById should GET /categories/:id', async () => {
    const mockData = { id: '1', name: 'Bebidas' }
    vi.mocked(api.get).mockResolvedValue({ data: mockData })

    const result = await categoryService.findById('1')

    expect(api.get).toHaveBeenCalledWith('/categories/1')
    expect(result).toEqual(mockData)
  })

  it('create should POST /categories', async () => {
    const request = { name: 'Bebidas' }
    const mockData = { id: '1', name: 'Bebidas' }
    vi.mocked(api.post).mockResolvedValue({ data: mockData })

    const result = await categoryService.create(request)

    expect(api.post).toHaveBeenCalledWith('/categories', request)
    expect(result).toEqual(mockData)
  })

  it('update should PUT /categories/:id', async () => {
    const request = { name: 'Lanches' }
    const mockData = { id: '1', name: 'Lanches' }
    vi.mocked(api.put).mockResolvedValue({ data: mockData })

    const result = await categoryService.update('1', request)

    expect(api.put).toHaveBeenCalledWith('/categories/1', request)
    expect(result).toEqual(mockData)
  })

  it('remove should DELETE /categories/:id', async () => {
    vi.mocked(api.delete).mockResolvedValue({})

    await categoryService.remove('1')

    expect(api.delete).toHaveBeenCalledWith('/categories/1')
  })
})
