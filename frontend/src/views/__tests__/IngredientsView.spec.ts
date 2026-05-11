import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'

let ingredientStoreMock: any

vi.mock('@/stores/ingredientStore', () => ({
  useIngredientStore: () => ingredientStoreMock,
}))

import IngredientsView from '@/views/IngredientsView.vue'

describe('IngredientsView', () => {
  beforeEach(() => {
    ingredientStoreMock = {
      items: [],
      loading: false,
      error: null,
      fetchAll: vi.fn(),
      create: vi.fn().mockResolvedValue({}),
      update: vi.fn(),
      remove: vi.fn(),
    }
  })

  it('should submit ingredient with default quantity', async () => {
    const wrapper = mount(IngredientsView)

    await wrapper.get('button.btn.btn-primary').trigger('click')

    await wrapper.get('input[placeholder="Nome do ingrediente"]').setValue('Leite Ninho')
    await wrapper.get('input[placeholder="Ex: kg, L, un"]').setValue('g')
    await wrapper.get('input[placeholder="0,00"]').setValue('0.02')
    await wrapper.get('[data-testid="ingredient-default-quantity-input"]').setValue('20')

    await wrapper.get('form').trigger('submit')

    expect(ingredientStoreMock.create).toHaveBeenCalledWith({
      name: 'Leite Ninho',
      unit: 'g',
      costPerUnit: 0.02,
      defaultQuantity: 20,
    })
  })
})
