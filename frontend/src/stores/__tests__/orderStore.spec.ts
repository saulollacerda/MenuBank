import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useOrderStore } from '@/stores/orderStore'
import { orderService } from '@/services/orderService'

vi.mock('@/services/orderService')

const mockedService = vi.mocked(orderService)

describe('orderStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchAll should populate items', async () => {
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
    mockedService.findAll.mockResolvedValue(mockData)

    const store = useOrderStore()
    await store.fetchAll()

    expect(store.items).toEqual(mockData)
    expect(store.loading).toBe(false)
  })

  it('fetchAll should not call service again when data is already loaded', async () => {
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
    mockedService.findAll.mockResolvedValue(mockData)

    const store = useOrderStore()
    await store.fetchAll()
    await store.fetchAll()

    expect(mockedService.findAll).toHaveBeenCalledTimes(1)
    expect(store.items).toEqual(mockData)
  })

  it('create should add item to the list', async () => {
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

  it('remove should filter out the item', async () => {
    const store = useOrderStore()
    store.items = [
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
    mockedService.remove.mockResolvedValue()

    await store.remove('1')

    expect(store.items).toHaveLength(0)
  })
})

