import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
let productStoreMock: any
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let categoryStoreMock: any
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let anotaAIStoreMock: any

vi.mock('@/stores/productStore', () => ({
  useProductStore: () => productStoreMock,
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
      includes: [],
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
      fetchIncludes: vi.fn(),
      addInclude: vi.fn().mockResolvedValue({}),
      removeInclude: vi.fn(),
      clearRecipe: vi.fn(),
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

  it('should render category column with categoryName for each product', () => {
    const wrapper = mount(ProductsView)
    expect(wrapper.text()).toContain('Açaí 330ml')
    expect(wrapper.text()).toContain('Bebidas')
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

  it('should add include with name/cost/quantity to the ficha tecnica', async () => {
    const wrapper = mount(ProductsView)

    // Abre o modal de ficha tecnica do primeiro produto
    await wrapper.get('[data-testid="product-p1-recipe-button"]').trigger('click')
    await flushPromises()

    await wrapper.get('[data-testid="recipe-name-input"]').setValue('Copo')
    await wrapper.get('[data-testid="recipe-cost-input"]').setValue('0.5')
    await wrapper.get('[data-testid="recipe-quantity-input"]').setValue('1')

    await wrapper.get('[data-testid="recipe-add-form"]').trigger('submit')
    await flushPromises()

    expect(productStoreMock.addInclude).toHaveBeenCalledWith('p1', {
      name: 'Copo',
      cost: 0.5,
      quantity: 1,
    })
  })
})
