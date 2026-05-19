import { describe, it, expect, vi, beforeEach } from 'vitest'
import { customerService } from '@/services/customerService'
import api from '@/services/api'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('customerService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('findAll should GET /customers with pagination params', async () => {
    const mockPage = {
      content: [{ id: '1', name: 'João', phone: '11999', email: 'j@test.com' }],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
      first: true,
      last: true,
      empty: false,
    }
    vi.mocked(api.get).mockResolvedValue({ data: mockPage })

    const result = await customerService.findAll()

    expect(api.get).toHaveBeenCalledWith('/customers', {
      params: { search: '', page: 0, size: 20 },
    })
    expect(result).toEqual(mockPage)
  })

  it('findById should GET /customers/:id', async () => {
    const mockData = { id: '1', name: 'João', phone: '11999', email: 'j@test.com' }
    vi.mocked(api.get).mockResolvedValue({ data: mockData })

    const result = await customerService.findById('1')

    expect(api.get).toHaveBeenCalledWith('/customers/1')
    expect(result).toEqual(mockData)
  })

  it('create should POST /customers', async () => {
    const request = { name: 'João', phone: '11999', email: 'j@test.com' }
    const mockData = { id: '1', ...request }
    vi.mocked(api.post).mockResolvedValue({ data: mockData })

    const result = await customerService.create(request)

    expect(api.post).toHaveBeenCalledWith('/customers', request)
    expect(result).toEqual(mockData)
  })

  it('update should PUT /customers/:id', async () => {
    const request = { name: 'João Silva' }
    const mockData = { id: '1', name: 'João Silva', phone: '11999', email: 'j@test.com' }
    vi.mocked(api.put).mockResolvedValue({ data: mockData })

    const result = await customerService.update('1', request)

    expect(api.put).toHaveBeenCalledWith('/customers/1', request)
    expect(result).toEqual(mockData)
  })

  it('remove should DELETE /customers/:id', async () => {
    vi.mocked(api.delete).mockResolvedValue({})

    await customerService.remove('1')

    expect(api.delete).toHaveBeenCalledWith('/customers/1')
  })
})
