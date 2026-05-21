import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useProductStore } from '@/stores/productStore'
import { productService } from '@/services/productService'
import { productIngredientService } from '@/services/productIngredientService'

vi.mock('@/services/productService')
vi.mock('@/services/productIngredientService')

const mockedProductService = vi.mocked(productService)
const mockedProductIngredientService = vi.mocked(productIngredientService)

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
        estimatedCost: 10.0,
        margin: 15.0,
        status: 'ACTIVE' as const,
        cmv: 10.0,
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

    // size in store state should remain at default (20), not 1000,
    // so a subsequent fetchPage from a list view starts paginated correctly
    expect(store.size).toBe(20)
    expect(store.page).toBe(0)
    expect(store.search).toBe('')
  })

  it('create should refetch the current page', async () => {
    const created = {
      id: '1',
      name: 'Hambúrguer',
      price: 25.0,
      estimatedCost: null,
      margin: null,
      status: 'ACTIVE' as const,
      cmv: null,
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

  it('fetchProductIngredients should populate productIngredients', async () => {
    const mockProductIngredients = [
      {
        id: 'r1',
        productId: 'p1',
        ingredientId: 'i1',
        ingredientName: 'Farinha',
        ingredientUnit: 'kg',
        grammage: 0.5,
        isOptional: false,
        costPerUnit: 5.0,
        totalCost: 2.5,
      },
    ]
    mockedProductIngredientService.findByProductId.mockResolvedValue(mockProductIngredients)

    const store = useProductStore()
    await store.fetchProductIngredients('p1')

    expect(store.productIngredients).toEqual(mockProductIngredients)
  })

  it('addProductIngredient should add to productIngredients', async () => {
    const newPI = {
      id: 'r1',
      productId: 'p1',
      ingredientId: 'i1',
      ingredientName: 'Farinha',
      ingredientUnit: 'kg',
      grammage: 0.5,
      isOptional: false,
      costPerUnit: 5.0,
      totalCost: 2.5,
    }
    mockedProductIngredientService.add.mockResolvedValue(newPI)

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

    await store.addProductIngredient('p1', { ingredientId: 'i1', grammage: 0.5, isOptional: false })

    expect(store.productIngredients).toContainEqual(newPI)
  })

  it('remove should call service and refetch the current page', async () => {
    const remaining = {
      id: '2',
      name: 'Pizza',
      price: 35.0,
      estimatedCost: null,
      margin: null,
      status: 'ACTIVE' as const,
      cmv: null,
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
