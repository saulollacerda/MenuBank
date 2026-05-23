import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

let productStoreMock: any
let ingredientStoreMock: any
let categoryStoreMock: any
let anotaAIStoreMock: any

vi.mock('@/stores/productStore', () => ({
  useProductStore: () => productStoreMock,
}))

vi.mock('@/stores/ingredientStore', () => ({
  useIngredientStore: () => ingredientStoreMock,
}))

vi.mock('@/stores/categoryStore', () => ({
  useCategoryStore: () => categoryStoreMock,
}))

vi.mock('@/stores/anotaAIStore', () => ({
  useAnotaAIStore: () => anotaAIStoreMock,
}))

import ProductsView from '@/views/ProductsView.vue'

describe('ProductsView', () => {
  beforeEach(() => {
    productStoreMock = {
      items: [
        {
          id: 'p1',
          name: 'Açaí 330ml',
          price: 20,
          status: 'ACTIVE',
          categoryId: 'cat1',
          categoryName: 'Bebidas',
        },
      ],
      productIngredients: [],
      recipeItems: [],
      loading: false,
      error: null,
      search: '',
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      fetchAll: vi.fn(),
      fetchPage: vi.fn(),
      create: vi.fn().mockResolvedValue({}),
      update: vi.fn().mockResolvedValue({}),
      remove: vi.fn(),
      fetchProductIngredients: vi.fn(),
      addProductIngredient: vi.fn().mockResolvedValue({}),
      removeProductIngredient: vi.fn(),
      clearRecipe: vi.fn(),
    }

    ingredientStoreMock = {
      items: [
        {
          id: 'i1',
          name: 'Leite Ninho',
          unit: 'g',
          costPerUnit: 0.02,
          defaultQuantity: 20,
          status: 'ACTIVE',
        },
      ],
      loading: false,
      error: null,
      fetchAll: vi.fn(),
      fetchPage: vi.fn(),
    }

    categoryStoreMock = {
      items: [
        { id: 'cat1', name: 'Bebidas' },
        { id: 'cat2', name: 'Lanches' },
      ],
      loading: false,
      error: null,
      fetchAll: vi.fn(),
    }

    anotaAIStoreMock = {
      syncingOrders: false,
      syncingCatalog: false,
      lastResult: null,
      error: null,
      syncOrders: vi.fn(),
      syncCatalog: vi.fn(),
      clearResult: vi.fn(),
    }
  })

  it('should prefill recipe quantity when ingredient has default quantity', async () => {
    const wrapper = mount(ProductsView)

    await wrapper.get('button.btn.btn-primary.btn-sm').trigger('click')
    await flushPromises()

    await wrapper.get('[data-testid="recipe-ingredient-select"]').setValue('i1')

    const quantityInput = wrapper.get('[data-testid="recipe-quantity-input"]').element as HTMLInputElement
    expect(quantityInput.value).toBe('20')
  })

  it('should render category column with categoryName for each product', () => {
    const wrapper = mount(ProductsView)
    const rows = wrapper.findAll('tbody tr')
    expect(rows.length).toBeGreaterThan(0)
    expect(rows[0]!.text()).toContain('Bebidas')
  })

  it('should submit new product with the selected categoryId', async () => {
    const wrapper = mount(ProductsView)

    await wrapper.get('[data-testid="new-product-button"]').trigger('click')

    await wrapper.get('[data-testid="product-name-input"]').setValue('X-Burguer')
    await wrapper.get('[data-testid="product-price-input"]').setValue('25.9')
    await wrapper.get('[data-testid="product-category-select"]').setValue('cat2')

    await wrapper.get('[data-testid="product-form"]').trigger('submit')
    await flushPromises()

    expect(productStoreMock.create).toHaveBeenCalledWith({
      name: 'X-Burguer',
      price: 25.9,
      categoryId: 'cat2',
    })
  })

  it('should preselect the current category when editing a product', async () => {
    const wrapper = mount(ProductsView)

    await wrapper.get('[data-testid="product-p1-edit-button"]').trigger('click')

    const select = wrapper.get('[data-testid="product-category-select"]').element as HTMLSelectElement
    expect(select.value).toBe('cat1')
  })
})
