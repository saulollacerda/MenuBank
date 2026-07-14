import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
let ingredientStoreMock: any
const routeMock = { query: {} as Record<string, string | string[]> }
const routerMock = { replace: vi.fn(), push: vi.fn() }

vi.mock('@/stores/ingredientStore', () => ({
  useIngredientStore: () => ingredientStoreMock,
}))

vi.mock('vue-router', () => ({
  useRoute: () => routeMock,
  useRouter: () => routerMock,
}))

vi.mock('@/services/productService', () => ({
  productService: {
    findAll: vi.fn().mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 500,
      first: true,
      last: true,
      empty: true,
    }),
  },
}))

vi.mock('@/services/includeService', () => ({
  includeService: {
    add: vi.fn().mockResolvedValue({}),
    update: vi.fn().mockResolvedValue({}),
    remove: vi.fn().mockResolvedValue(undefined),
  },
}))

vi.mock('@/services/ingredientService', () => ({
  ingredientService: {
    fetchUsages: vi.fn().mockResolvedValue([]),
  },
}))

const showToastMock = vi.fn()
vi.mock('@/composables/useToast', () => ({
  useToast: () => ({ showToast: showToastMock }),
}))

import IngredientsView from '@/views/IngredientsView.vue'
import { ingredientService } from '@/services/ingredientService'
import { includeService } from '@/services/includeService'

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
    routeMock.query = {}
    routerMock.replace.mockClear()
    routerMock.push.mockClear()
    showToastMock.mockClear()
  })

  it('should submit ingredient with default quantity', async () => {
    const wrapper = mount(IngredientsView)

    await wrapper.get('[data-testid="new-ingredient-button"]').trigger('click')

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

    await wrapper.get('[data-testid="new-ingredient-button"]').trigger('click')

    await wrapper.get('input[placeholder="Nome do ingrediente"]').setValue('Açúcar refinado')
    await wrapper.get('input[placeholder="Ex: kg, L, un"]').setValue('g')
    await wrapper.get('[data-testid="ingredient-cost-per-unit-input"]').setValue('0.0035')

    await wrapper.get('form').trigger('submit')

    expect(ingredientStoreMock.create).toHaveBeenCalledWith(
      expect.objectContaining({ costPerUnit: 0.0035 }),
    )
  })

  it('should configure cost per unit input with step of 0.0001', async () => {
    const wrapper = mount(IngredientsView)
    await wrapper.get('[data-testid="new-ingredient-button"]').trigger('click')
    const input = wrapper.get('[data-testid="ingredient-cost-per-unit-input"]')
    expect(input.attributes('step')).toBe('0.0001')
  })

  it('should compute costPerUnit from purchase price divided by purchase quantity when auto mode is enabled', async () => {
    const wrapper = mount(IngredientsView)

    await wrapper.get('[data-testid="new-ingredient-button"]').trigger('click')

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

    await wrapper.get('[data-testid="new-ingredient-button"]').trigger('click')

    await wrapper.get('input[placeholder="Nome do ingrediente"]').setValue('Teste')
    await wrapper.get('input[placeholder="Ex: kg, L, un"]').setValue('g')

    await wrapper.get('[data-testid="ingredient-cost-auto-checkbox"]').setValue(true)
    await wrapper.get('[data-testid="ingredient-purchase-price-input"]').setValue('100')
    await wrapper.get('[data-testid="ingredient-purchase-quantity-input"]').setValue('0')

    await wrapper.get('form').trigger('submit')

    expect(ingredientStoreMock.create).not.toHaveBeenCalled()
  })

  it('should pre-fill name and open modal when route has ?createName= query param', async () => {
    routeMock.query = { createName: 'Pistache' }

    const wrapper = mount(IngredientsView)

    // Wait for onMounted + async openCreateModal (loadProducts) to settle
    await flushPromises()

    const nameInput = wrapper.get('[data-testid="ingredient-name-input"]')
    expect((nameInput.element as HTMLInputElement).value).toBe('Pistache')
    // Query is cleared after open so reload doesn't re-trigger
    expect(routerMock.replace).toHaveBeenCalledWith({ query: {} })
  })

  it('should not open modal when route has no createName query', async () => {
    routeMock.query = {}

    const wrapper = mount(IngredientsView)
    await wrapper.vm.$nextTick()

    // The form input is inside the modal — should not exist when modal is closed
    expect(wrapper.find('[data-testid="ingredient-name-input"]').exists()).toBe(false)
  })

  it('should open create modal pre-filled with copied fields when duplicating', async () => {
    ingredientStoreMock.items = [
      {
        id: 'ing-1',
        name: 'Queijo Mussarela',
        unit: 'kg',
        costPerUnit: 32.5,
        defaultQuantity: 5,
        status: 'ACTIVE',
      },
    ]

    const wrapper = mount(IngredientsView)
    await flushPromises()

    await wrapper.get('[data-testid="duplicate-ingredient-button"]').trigger('click')
    await flushPromises()

    // Name is suffixed with "(cópia)" to avoid unique-name conflict
    const nameInput = wrapper.get('[data-testid="ingredient-name-input"]')
    expect((nameInput.element as HTMLInputElement).value).toBe('Queijo Mussarela (cópia)')

    // Unit and cost are copied from the source ingredient
    expect((wrapper.get('input[placeholder="Ex: kg, L, un"]').element as HTMLInputElement).value).toBe('kg')
    expect(
      (wrapper.get('[data-testid="ingredient-cost-per-unit-input"]').element as HTMLInputElement).value,
    ).toBe('32.5')

    // Submitting creates a new ingredient (not an update)
    await wrapper.get('form').trigger('submit')

    expect(ingredientStoreMock.create).toHaveBeenCalledWith({
      name: 'Queijo Mussarela (cópia)',
      unit: 'kg',
      costPerUnit: 32.5,
      defaultQuantity: 5,
    })
    expect(ingredientStoreMock.update).not.toHaveBeenCalled()
  })

  it('should copy product-specific quantities from the source ingredient when duplicating', async () => {
    ingredientStoreMock.items = [
      {
        id: 'ing-1',
        name: 'Queijo Mussarela',
        unit: 'kg',
        costPerUnit: 32.5,
        defaultQuantity: 5,
        status: 'ACTIVE',
      },
    ]
    vi.mocked(ingredientService.fetchUsages).mockResolvedValue([
      {
        includeId: 'inc-1',
        productId: 'p-1',
        productName: 'Pizza Calabresa',
        quantity: 120,
        cost: 32.5,
        totalCost: 3900,
      },
      {
        includeId: 'inc-2',
        productId: 'p-2',
        productName: 'Pizza Portuguesa',
        quantity: 90,
        cost: 32.5,
        totalCost: 2925,
      },
    ])

    const wrapper = mount(IngredientsView)
    await flushPromises()

    await wrapper.get('[data-testid="duplicate-ingredient-button"]').trigger('click')
    await flushPromises()

    // Usages of the source ingredient are fetched and shown in the modal
    expect(ingredientService.fetchUsages).toHaveBeenCalledWith('ing-1')
    expect(wrapper.text()).toContain('Pizza Calabresa')
    expect(wrapper.text()).toContain('Pizza Portuguesa')

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    // The copy adds NEW includes on each product; it must never update the
    // source ingredient's existing includes
    expect(includeService.add).toHaveBeenCalledWith('p-1', {
      name: 'Queijo Mussarela (cópia)',
      cost: 32.5,
      quantity: 120,
    })
    expect(includeService.add).toHaveBeenCalledWith('p-2', {
      name: 'Queijo Mussarela (cópia)',
      cost: 32.5,
      quantity: 90,
    })
    expect(includeService.update).not.toHaveBeenCalled()
  })

  it('should show a success toast after creating an ingredient', async () => {
    const wrapper = mount(IngredientsView)

    await wrapper.get('[data-testid="new-ingredient-button"]').trigger('click')
    await wrapper.get('input[placeholder="Nome do ingrediente"]').setValue('Morango')
    await wrapper.get('input[placeholder="Ex: kg, L, un"]').setValue('g')
    await wrapper.get('[data-testid="ingredient-cost-per-unit-input"]').setValue('0.05')

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(showToastMock).toHaveBeenCalledWith('Ingrediente criado com sucesso!')
  })

  it('should not show a toast when ingredient creation fails', async () => {
    ingredientStoreMock.create = vi.fn().mockRejectedValue(new Error('boom'))
    const wrapper = mount(IngredientsView)

    await wrapper.get('[data-testid="new-ingredient-button"]').trigger('click')
    await wrapper.get('input[placeholder="Nome do ingrediente"]').setValue('Morango')
    await wrapper.get('input[placeholder="Ex: kg, L, un"]').setValue('g')
    await wrapper.get('[data-testid="ingredient-cost-per-unit-input"]').setValue('0.05')

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(showToastMock).not.toHaveBeenCalled()
  })
})
