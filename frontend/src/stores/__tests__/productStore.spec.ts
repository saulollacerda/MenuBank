import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useProductStore } from '@/stores/productStore'
import { productService } from '@/services/productService'
import { includeService } from '@/services/includeService'

vi.mock('@/services/productService')
vi.mock('@/services/includeService')

const mockedProductService = vi.mocked(productService)
const mockedIncludeService = vi.mocked(includeService)

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

describe('productStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchPage should populate items and pagination state', async () => {
    const mockData = [
      {
        id: '1',
        name: 'Hambúrguer',
        price: 25.0,
        status: 'ACTIVE' as const,
        categoryId: 'cat1',
        categoryName: 'Lanches',
      },
    ]
    mockedProductService.findAll.mockResolvedValue(asPage(mockData))

    const store = useProductStore()
    await store.fetchPage({ search: 'hamb', page: 0 })

    expect(store.items).toEqual(mockData)
    expect(store.totalElements).toBe(1)
    expect(store.search).toBe('hamb')
    expect(store.loading).toBe(false)
  })

  it('fetchAll should call service with size=1000 (legacy entry point)', async () => {
    mockedProductService.findAll.mockResolvedValue(asPage([], 1000))

    const store = useProductStore()
    await store.fetchAll()

    expect(mockedProductService.findAll).toHaveBeenCalledWith({ search: '', page: 0, size: 1000 })
  })

  it('fetchAll should NOT pollute pagination state (size stays at default)', async () => {
    mockedProductService.findAll.mockResolvedValue(asPage([], 1000))

    const store = useProductStore()
    await store.fetchAll()

    expect(store.size).toBe(20)
    expect(store.page).toBe(0)
    expect(store.search).toBe('')
  })

  it('create should refetch the current page', async () => {
    const created = {
      id: '1',
      name: 'Hambúrguer',
      price: 25.0,
      status: 'ACTIVE' as const,
      categoryId: 'cat1',
      categoryName: 'Lanches',
    }
    mockedProductService.create.mockResolvedValue(created)
    mockedProductService.findAll.mockResolvedValue(asPage([created]))

    const store = useProductStore()
    await store.create({ name: 'Hambúrguer', price: 25.0, categoryId: 'cat1' })

    expect(mockedProductService.create).toHaveBeenCalled()
    expect(store.items).toContainEqual(created)
  })

  it('fetchIncludes should populate includes', async () => {
    const mockIncludes = [
      {
        id: 'inc1',
        productId: 'p1',
        name: 'Copo',
        cost: 0.5,
        quantity: 1,
        totalCost: 0.5,
        kind: 'PACKAGING' as const,
      },
    ]
    mockedIncludeService.findByProductId.mockResolvedValue(mockIncludes)

    const store = useProductStore()
    await store.fetchIncludes('p1')

    expect(store.includes).toEqual(mockIncludes)
  })

  it('addInclude should append to includes', async () => {
    const newInclude = {
      id: 'inc1',
      productId: 'p1',
      name: 'Copo',
      cost: 0.5,
      quantity: 1,
      totalCost: 0.5,
      kind: 'PACKAGING' as const,
    }
    mockedIncludeService.add.mockResolvedValue(newInclude)

    const store = useProductStore()
    store.items = [
      {
        id: 'p1',
        name: 'Hambúrguer',
        price: 25.0,
        status: 'ACTIVE',
        categoryId: 'cat1',
        categoryName: 'Lanches',
      },
    ]

    await store.addInclude('p1', { name: 'Copo', cost: 0.5, quantity: 1 })

    expect(store.includes).toContainEqual(newInclude)
  })

  it('updateInclude should replace the updated item in includes', async () => {
    const updated = {
      id: 'inc1',
      productId: 'p1',
      name: 'Copo Grande',
      cost: 0.8,
      quantity: 1,
      totalCost: 0.8,
      kind: 'PACKAGING' as const,
    }
    mockedIncludeService.update.mockResolvedValue(updated)

    const store = useProductStore()
    store.includes = [
      { id: 'inc1', productId: 'p1', name: 'Copo', cost: 0.5, quantity: 1, totalCost: 0.5, kind: 'PACKAGING' as const },
      { id: 'inc2', productId: 'p1', name: 'Colher', cost: 0.1, quantity: 1, totalCost: 0.1, kind: 'PACKAGING' as const },
    ]

    await store.updateInclude('p1', 'inc1', { name: 'Copo Grande', cost: 0.8, quantity: 1, kind: 'PACKAGING' })

    expect(store.includes[0]).toEqual(updated)
    expect(store.includes[1]?.name).toBe('Colher')
  })

  it('removeInclude should remove from includes', async () => {
    const store = useProductStore()
    store.includes = [
      { id: 'inc1', productId: 'p1', name: 'Copo', cost: 0.5, quantity: 1, totalCost: 0.5, kind: 'PACKAGING' as const },
      { id: 'inc2', productId: 'p1', name: 'Colher', cost: 0.1, quantity: 1, totalCost: 0.1, kind: 'PACKAGING' as const },
    ]
    mockedIncludeService.remove.mockResolvedValue()

    await store.removeInclude('p1', 'inc1')

    expect(store.includes).toEqual([
      { id: 'inc2', productId: 'p1', name: 'Colher', cost: 0.1, quantity: 1, totalCost: 0.1, kind: 'PACKAGING' },
    ])
  })

  it('clearRecipe should empty includes', async () => {
    const store = useProductStore()
    store.includes = [
      { id: 'inc1', productId: 'p1', name: 'Copo', cost: 0.5, quantity: 1, totalCost: 0.5, kind: 'PACKAGING' as const },
    ]
    mockedIncludeService.clear.mockResolvedValue(1)

    const deleted = await store.clearRecipe('p1')

    expect(deleted).toBe(1)
    expect(store.includes).toEqual([])
  })

  it('remove should call service and refetch the current page', async () => {
    const remaining = {
      id: '2',
      name: 'Pizza',
      price: 35.0,
      status: 'ACTIVE' as const,
      categoryId: 'cat1',
      categoryName: 'Lanches',
    }
    mockedProductService.remove.mockResolvedValue()
    mockedProductService.findAll.mockResolvedValue(asPage([remaining]))

    const store = useProductStore()
    await store.remove('1')

    expect(mockedProductService.remove).toHaveBeenCalledWith('1')
    expect(store.items).toEqual([remaining])
  })
})
