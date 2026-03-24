import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCategoryStore } from '@/stores/categoryStore'
import { categoryService } from '@/services/categoryService'

vi.mock('@/services/categoryService')

const mockedService = vi.mocked(categoryService)

describe('categoryStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchAll should populate items', async () => {
    const mockData = [{ id: '1', name: 'Bebidas' }]
    mockedService.findAll.mockResolvedValue(mockData)

    const store = useCategoryStore()
    await store.fetchAll()

    expect(store.items).toEqual(mockData)
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('fetchAll should set error on failure', async () => {
    mockedService.findAll.mockRejectedValue(new Error('Network error'))

    const store = useCategoryStore()

    await expect(store.fetchAll()).rejects.toThrow()
    expect(store.error).toBe('Erro ao carregar categorias')
    expect(store.loading).toBe(false)
  })

  it('create should add item to the list', async () => {
    const created = { id: '1', name: 'Bebidas' }
    mockedService.create.mockResolvedValue(created)

    const store = useCategoryStore()
    const result = await store.create({ name: 'Bebidas' })

    expect(result).toEqual(created)
    expect(store.items).toContainEqual(created)
  })

  it('update should replace existing item', async () => {
    const store = useCategoryStore()
    store.items = [{ id: '1', name: 'Bebidas' }]

    const updated = { id: '1', name: 'Lanches' }
    mockedService.update.mockResolvedValue(updated)

    await store.update('1', { name: 'Lanches' })

    expect(store.items[0]).toEqual(updated)
  })

  it('remove should filter out the item', async () => {
    const store = useCategoryStore()
    store.items = [
      { id: '1', name: 'Bebidas' },
      { id: '2', name: 'Lanches' },
    ]
    mockedService.remove.mockResolvedValue()

    await store.remove('1')

    expect(store.items).toHaveLength(1)
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    expect(store.items[0]!.id).toBe('2')
  })
})



