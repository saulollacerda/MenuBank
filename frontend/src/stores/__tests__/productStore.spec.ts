import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useProductStore } from '@/stores/productStore'
import { productService } from '@/services/productService'
import { recipeItemService } from '@/services/recipeItemService'

vi.mock('@/services/productService')
vi.mock('@/services/recipeItemService')

const mockedProductService = vi.mocked(productService)
const mockedRecipeItemService = vi.mocked(recipeItemService)

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

  it('fetchRecipeItems should populate recipeItems', async () => {
    const mockRecipeItems = [
      {
        id: 'r1',
        productId: 'p1',
        ingredientId: 'i1',
        ingredientName: 'Farinha',
        ingredientUnit: 'kg',
        quantity: 0.5,
        costPerUnit: 5.0,
        totalCost: 2.5,
      },
    ]
    mockedRecipeItemService.findByProductId.mockResolvedValue(mockRecipeItems)

    const store = useProductStore()
    await store.fetchRecipeItems('p1')

    expect(store.recipeItems).toEqual(mockRecipeItems)
  })

  it('addRecipeItem should add to recipeItems and refresh product', async () => {
    const newRecipeItem = {
      id: 'r1',
      productId: 'p1',
      ingredientId: 'i1',
      ingredientName: 'Farinha',
      ingredientUnit: 'kg',
      quantity: 0.5,
      costPerUnit: 5.0,
      totalCost: 2.5,
    }
    const updatedProduct = {
      id: 'p1',
      name: 'Hambúrguer',
      price: 25.0,
      estimatedCost: 2.5,
      margin: 22.5,
      status: 'ACTIVE' as const,
      cmv: 2.5,
      categoryId: 'cat1',
      categoryName: 'Lanches',
    }
    mockedRecipeItemService.add.mockResolvedValue(newRecipeItem)
    mockedProductService.findById.mockResolvedValue(updatedProduct)

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

    await store.addRecipeItem('p1', { ingredientId: 'i1', quantity: 0.5 })

    expect(store.recipeItems).toContainEqual(newRecipeItem)
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
