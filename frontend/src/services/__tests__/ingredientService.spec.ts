import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ingredientService } from '@/services/ingredientService'
import api from '@/services/api'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('ingredientService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('findAll should GET /ingredients', async () => {
    const mockData = [
      { id: '1', name: 'Farinha', unit: 'kg', costPerUnit: 5.0, status: 'ACTIVE' },
    ]
    vi.mocked(api.get).mockResolvedValue({ data: mockData })

    const result = await ingredientService.findAll()

    expect(api.get).toHaveBeenCalledWith('/ingredients')
    expect(result).toEqual(mockData)
  })

  it('create should POST /ingredients', async () => {
    const request = { name: 'Farinha', unit: 'kg', costPerUnit: 5.0 }
    const mockData = { id: '1', ...request, status: 'ACTIVE' }
    vi.mocked(api.post).mockResolvedValue({ data: mockData })

    const result = await ingredientService.create(request)

    expect(api.post).toHaveBeenCalledWith('/ingredients', request)
    expect(result).toEqual(mockData)
  })

  it('update should PUT /ingredients/:id', async () => {
    const request = { name: 'Farinha de Trigo', unit: 'kg', costPerUnit: 6.0 }
    const mockData = { id: '1', ...request, status: 'ACTIVE' }
    vi.mocked(api.put).mockResolvedValue({ data: mockData })

    const result = await ingredientService.update('1', request)

    expect(api.put).toHaveBeenCalledWith('/ingredients/1', request)
    expect(result).toEqual(mockData)
  })

  it('remove should DELETE /ingredients/:id', async () => {
    vi.mocked(api.delete).mockResolvedValue({})

    await ingredientService.remove('1')

    expect(api.delete).toHaveBeenCalledWith('/ingredients/1')
  })
})
