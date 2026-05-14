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
    await wrapper.get('[data-testid="ingredient-cost-per-unit-input"]').setValue('0.02')
    await wrapper.get('[data-testid="ingredient-default-quantity-input"]').setValue('20')

    await wrapper.get('form').trigger('submit')

    expect(ingredientStoreMock.create).toHaveBeenCalledWith({
      name: 'Leite Ninho',
      unit: 'g',
      costPerUnit: 0.02,
      defaultQuantity: 20,
    })
  })

  it('should accept cost per unit with four decimal places', async () => {
    const wrapper = mount(IngredientsView)

    await wrapper.get('button.btn.btn-primary').trigger('click')

    await wrapper.get('input[placeholder="Nome do ingrediente"]').setValue('Açúcar refinado')
    await wrapper.get('input[placeholder="Ex: kg, L, un"]').setValue('g')
    await wrapper.get('[data-testid="ingredient-cost-per-unit-input"]').setValue('0.0035')

    await wrapper.get('form').trigger('submit')

    expect(ingredientStoreMock.create).toHaveBeenCalledWith(
      expect.objectContaining({ costPerUnit: 0.0035 }),
    )
  })

  it('should configure cost per unit input with step of 0.0001', () => {
    const wrapper = mount(IngredientsView)
    wrapper.get('button.btn.btn-primary').trigger('click')
    return wrapper.vm.$nextTick().then(() => {
      const input = wrapper.get('[data-testid="ingredient-cost-per-unit-input"]')
      expect(input.attributes('step')).toBe('0.0001')
    })
  })
})
