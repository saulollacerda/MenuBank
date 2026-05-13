import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

let productStoreMock: any
let ingredientStoreMock: any

vi.mock('@/stores/productStore', () => ({
  useProductStore: () => productStoreMock,
}))

vi.mock('@/stores/ingredientStore', () => ({
  useIngredientStore: () => ingredientStoreMock,
}))

import ProductsView from '@/views/ProductsView.vue'

describe('ProductsView', () => {
  beforeEach(() => {
    productStoreMock = {
      items: [{ id: 'p1', name: 'Açaí 330ml', price: 20, estimatedCost: 5, margin: 15, cmv: 0.25, status: 'ACTIVE' }],
      recipeItems: [],
      loading: false,
      error: null,
      fetchAll: vi.fn(),
      create: vi.fn(),
      update: vi.fn(),
      remove: vi.fn(),
      fetchRecipeItems: vi.fn(),
      addRecipeItem: vi.fn().mockResolvedValue({}),
      removeRecipeItem: vi.fn(),
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
})
