import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useProductStore } from '@/stores/productStore'
import { productService } from '@/services/productService'
import { recipeItemService } from '@/services/recipeItemService'

vi.mock('@/services/productService')
vi.mock('@/services/recipeItemService')

const mockedProductService = vi.mocked(productService)
const mockedRecipeItemService = vi.mocked(recipeItemService)

describe('productStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchAll should populate items', async () => {
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
    mockedProductService.findAll.mockResolvedValue(mockData)

    const store = useProductStore()
    await store.fetchAll()

    expect(store.items).toEqual(mockData)
    expect(store.loading).toBe(false)
  })

  it('fetchAll should not call service again when data is already loaded', async () => {
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
    mockedProductService.findAll.mockResolvedValue(mockData)

    const store = useProductStore()
    await store.fetchAll()
    await store.fetchAll()

    expect(mockedProductService.findAll).toHaveBeenCalledTimes(1)
    expect(store.items).toEqual(mockData)
  })

  it('create should add item to the list', async () => {
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

    const store = useProductStore()
    await store.create({ name: 'Hambúrguer', price: 25.0, categoryId: 'cat1' })

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
        estimatedCost: null,
        margin: null,
        status: 'ACTIVE',
        cmv: null,
        categoryId: 'cat1',
        categoryName: 'Lanches',
      },
    ]

    await store.addRecipeItem('p1', { ingredientId: 'i1', quantity: 0.5 })

    expect(store.recipeItems).toContainEqual(newRecipeItem)
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    expect(store.items[0]!.estimatedCost).toBe(2.5)
  })

  it('remove should filter out the item', async () => {
    const store = useProductStore()
    store.items = [
      {
        id: '1',
        name: 'Hambúrguer',
        price: 25.0,
        estimatedCost: null,
        margin: null,
        status: 'ACTIVE',
        cmv: null,
        categoryId: 'cat1',
        categoryName: 'Lanches',
      },
      {
        id: '2',
        name: 'Pizza',
        price: 35.0,
        estimatedCost: null,
        margin: null,
        status: 'ACTIVE',
        cmv: null,
        categoryId: 'cat1',
        categoryName: 'Lanches',
      },
    ]
    mockedProductService.remove.mockResolvedValue()

    await store.remove('1')

    expect(store.items).toHaveLength(1)
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    expect(store.items[0]!.id).toBe('2')
  })
})
