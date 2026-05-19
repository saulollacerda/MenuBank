import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCategoryStore } from '@/stores/categoryStore'
import { categoryService } from '@/services/categoryService'

vi.mock('@/services/categoryService')

const mockedService = vi.mocked(categoryService)

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

describe('categoryStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchPage should populate items and pagination state', async () => {
    const mockData = [{ id: '1', name: 'Bebidas' }]
    mockedService.findAll.mockResolvedValue(asPage(mockData))

    const store = useCategoryStore()
    await store.fetchPage({ search: 'beb' })

    expect(store.items).toEqual(mockData)
    expect(store.totalElements).toBe(1)
    expect(store.search).toBe('beb')
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('fetchPage should set error on failure', async () => {
    mockedService.findAll.mockRejectedValue(new Error('Network error'))

    const store = useCategoryStore()

    await expect(store.fetchPage()).rejects.toThrow()
    expect(store.error).toBe('Erro ao carregar categorias')
    expect(store.loading).toBe(false)
  })

  it('create should refetch the current page', async () => {
    const created = { id: '1', name: 'Bebidas' }
    mockedService.create.mockResolvedValue(created)
    mockedService.findAll.mockResolvedValue(asPage([created]))

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

  it('remove should call service and refetch the current page', async () => {
    const remaining = { id: '2', name: 'Lanches' }
    mockedService.remove.mockResolvedValue()
    mockedService.findAll.mockResolvedValue(asPage([remaining]))

    const store = useCategoryStore()
    await store.remove('1')

    expect(mockedService.remove).toHaveBeenCalledWith('1')
    expect(store.items).toEqual([remaining])
  })
})
