import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useIngredientStore } from '@/stores/ingredientStore'
import { ingredientService } from '@/services/ingredientService'

vi.mock('@/services/ingredientService')

const mockedService = vi.mocked(ingredientService)

describe('ingredientStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchAll should populate items', async () => {
    const mockData = [
      { id: '1', name: 'Farinha', unit: 'kg', costPerUnit: 5.0, status: 'ACTIVE' as const },
    ]
    mockedService.findAll.mockResolvedValue(mockData)

    const store = useIngredientStore()
    await store.fetchAll()

    expect(store.items).toEqual(mockData)
    expect(store.loading).toBe(false)
  })

  it('create should add item to the list', async () => {
    const created = { id: '1', name: 'Farinha', unit: 'kg', costPerUnit: 5.0, status: 'ACTIVE' as const }
    mockedService.create.mockResolvedValue(created)

    const store = useIngredientStore()
    await store.create({ name: 'Farinha', unit: 'kg', costPerUnit: 5.0 })

    expect(store.items).toContainEqual(created)
  })

  it('remove should filter out the item', async () => {
    const store = useIngredientStore()
    store.items = [
      { id: '1', name: 'Farinha', unit: 'kg', costPerUnit: 5.0, status: 'ACTIVE' },
      { id: '2', name: 'Açúcar', unit: 'kg', costPerUnit: 3.0, status: 'ACTIVE' },
    ]
    mockedService.remove.mockResolvedValue()

    await store.remove('1')

    expect(store.items).toHaveLength(1)
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    expect(store.items[0]!.id).toBe('2')
  })
})
