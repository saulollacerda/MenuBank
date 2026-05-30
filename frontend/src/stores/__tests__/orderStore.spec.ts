import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useOrderStore } from '@/stores/orderStore'
import { orderService } from '@/services/orderService'

vi.mock('@/services/orderService')

const mockedService = vi.mocked(orderService)

function asPage<T>(content: T[], size = 20) {
  return {
    content,
    totalElements: content.length,
    totalPages: content.length === 0 ? 0 : 1,
    number: 0,
    size,
    first: true,
    last: true,
    empty: content.length === 0,
  }
}

describe('orderStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchPage should populate items and pagination state', async () => {
    const mockData = [
      {
        id: '1',
        dateTime: '2026-03-24T10:00:00',
        customerId: 'c1',
        customerName: 'João',
        status: 'PENDING' as const,
        totalValue: 50.0,
        estimatedProfit: 20.0,
        items: [],
      },
    ]
    mockedService.findAll.mockResolvedValue(asPage(mockData))

    const store = useOrderStore()
    await store.fetchPage({ search: 'joão' })

    expect(store.items).toEqual(mockData)
    expect(store.search).toBe('joão')
    expect(store.loading).toBe(false)
  })

  it('create should refetch the current page', async () => {
    const created = {
      id: '1',
      dateTime: '2026-03-24T10:00:00',
      customerId: 'c1',
      customerName: 'João',
      status: 'PENDING' as const,
      totalValue: 50.0,
      estimatedProfit: 20.0,
      items: [],
    }
    mockedService.create.mockResolvedValue(created)
    mockedService.findAll.mockResolvedValue(asPage([created]))

    const store = useOrderStore()
    await store.create({
      customerId: 'c1',
      items: [
        {
          productId: 'p1',
          quantity: 2,
          extraIngredients: [{ ingredientId: 'i1', quantity: 1.5 }],
        },
      ],
    })

    expect(store.items).toContainEqual(created)
  })

  it('remove should call service and refetch the current page', async () => {
    mockedService.remove.mockResolvedValue()
    mockedService.findAll.mockResolvedValue(asPage([]))

    const store = useOrderStore()
    await store.remove('1')

    expect(mockedService.remove).toHaveBeenCalledWith('1')
    expect(store.items).toHaveLength(0)
  })
})
