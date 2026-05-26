import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useIngredientStore } from '@/stores/ingredientStore'
import { ingredientService } from '@/services/ingredientService'

vi.mock('@/services/ingredientService')

const mockedService = vi.mocked(ingredientService)

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

describe('ingredientStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchPage should populate items and pagination state', async () => {
    const mockData = [
      {
        id: '1',
        name: 'Farinha',
        unit: 'kg',
        costPerUnit: 5.0,
        status: 'ACTIVE' as const,
      },
    ]
    mockedService.findAll.mockResolvedValue(asPage(mockData))

    const store = useIngredientStore()
    await store.fetchPage({ search: 'farinha' })

    expect(store.items).toEqual(mockData)
    expect(store.search).toBe('farinha')
    expect(store.loading).toBe(false)
  })

  it('create should refetch the current page', async () => {
    const created = {
      id: '1',
      name: 'Farinha',
      unit: 'kg',
      costPerUnit: 5.0,
      status: 'ACTIVE' as const,
    }
    mockedService.create.mockResolvedValue(created)
    mockedService.findAll.mockResolvedValue(asPage([created]))

    const store = useIngredientStore()
    await store.create({ name: 'Farinha', unit: 'kg', costPerUnit: 5.0 })

    expect(store.items).toContainEqual(created)
  })

  it('remove should call service and refetch the current page', async () => {
    const remaining = {
      id: '2',
      name: 'Açúcar',
      unit: 'kg',
      costPerUnit: 3.0,
      status: 'ACTIVE' as const,
    }
    mockedService.remove.mockResolvedValue()
    mockedService.findAll.mockResolvedValue(asPage([remaining]))

    const store = useIngredientStore()
    await store.remove('1')

    expect(mockedService.remove).toHaveBeenCalledWith('1')
    expect(store.items).toEqual([remaining])
  })
})
