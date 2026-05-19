import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'

let ingredientStoreMock: any
let ingredientCategoryStoreMock: any

vi.mock('@/stores/ingredientStore', () => ({
  useIngredientStore: () => ingredientStoreMock,
}))

vi.mock('@/stores/ingredientCategoryStore', () => ({
  useIngredientCategoryStore: () => ingredientCategoryStoreMock,
}))

import IngredientsView from '@/views/IngredientsView.vue'

describe('IngredientsView', () => {
  beforeEach(() => {
    ingredientStoreMock = {
      items: [],
      loading: false,
      error: null,
      search: '',
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      fetchAll: vi.fn(),
      fetchPage: vi.fn(),
      create: vi.fn().mockResolvedValue({}),
      update: vi.fn(),
      remove: vi.fn(),
    }
    ingredientCategoryStoreMock = {
      items: [],
      loading: false,
      error: null,
      fetchAll: vi.fn(),
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
      ingredientCategoryId: null,
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

  it('should compute costPerUnit from purchase price divided by purchase quantity when auto mode is enabled', async () => {
    const wrapper = mount(IngredientsView)

    await wrapper.get('button.btn.btn-primary').trigger('click')

    await wrapper.get('input[placeholder="Nome do ingrediente"]').setValue('Açaí GOAT')
    await wrapper.get('input[placeholder="Ex: kg, L, un"]').setValue('g')

    await wrapper.get('[data-testid="ingredient-cost-auto-checkbox"]').setValue(true)

    expect(wrapper.find('[data-testid="ingredient-cost-per-unit-input"]').exists()).toBe(false)

    await wrapper.get('[data-testid="ingredient-purchase-price-input"]').setValue('195')
    await wrapper.get('[data-testid="ingredient-purchase-quantity-input"]').setValue('9000')

    const computed = wrapper.get('[data-testid="ingredient-cost-per-unit-computed"]')
    expect(computed.text()).toMatch(/0[,.]0217/)

    await wrapper.get('form').trigger('submit')

    expect(ingredientStoreMock.create).toHaveBeenCalledWith(
      expect.objectContaining({
        name: 'Açaí GOAT',
        unit: 'g',
        costPerUnit: expect.closeTo(0.02167, 4),
      }),
    )
  })

  it('should not submit when auto mode is enabled and purchase quantity is zero', async () => {
    const wrapper = mount(IngredientsView)

    await wrapper.get('button.btn.btn-primary').trigger('click')

    await wrapper.get('input[placeholder="Nome do ingrediente"]').setValue('Teste')
    await wrapper.get('input[placeholder="Ex: kg, L, un"]').setValue('g')

    await wrapper.get('[data-testid="ingredient-cost-auto-checkbox"]').setValue(true)
    await wrapper.get('[data-testid="ingredient-purchase-price-input"]').setValue('100')
    await wrapper.get('[data-testid="ingredient-purchase-quantity-input"]').setValue('0')

    await wrapper.get('form').trigger('submit')

    expect(ingredientStoreMock.create).not.toHaveBeenCalled()
  })
})
