import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

let orderStoreMock: any
let customerStoreMock: any
let productStoreMock: any
let ingredientStoreMock: any
let feeStoreMock: any
let notificationStoreMock: any

vi.mock('@/stores/orderStore', () => ({
  useOrderStore: () => orderStoreMock,
}))

vi.mock('@/stores/customerStore', () => ({
  useCustomerStore: () => customerStoreMock,
}))

vi.mock('@/stores/productStore', () => ({
  useProductStore: () => productStoreMock,
}))

vi.mock('@/stores/ingredientStore', () => ({
  useIngredientStore: () => ingredientStoreMock,
}))

vi.mock('@/stores/feeStore', () => ({
  useFeeStore: () => feeStoreMock,
}))

vi.mock('@/stores/notificationStore', () => ({
  useNotificationStore: () => notificationStoreMock,
}))

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({ currentUser: null }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ query: {} }),
}))

let includesByProductMock: Record<string, unknown[]>

vi.mock('@/services/includeService', () => ({
  includeService: {
    findByProductId: vi.fn(async (productId: string) => includesByProductMock[productId] ?? []),
  },
}))

import OrdersView from '@/views/OrdersView.vue'

const showToastMock = vi.fn()
vi.mock('@/composables/useToast', () => ({
  useToast: () => ({ showToast: showToastMock }),
}))

describe('OrdersView', () => {
  beforeEach(() => {
    showToastMock.mockClear()
    orderStoreMock = {
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
      findById: vi.fn(async (id: string) =>
        orderStoreMock.items.find((o: { id: string }) => o.id === id),
      ),
      create: vi.fn().mockResolvedValue({}),
      update: vi.fn(),
      remove: vi.fn(),
    }

    customerStoreMock = {
      items: [{ id: 'c1', name: 'João' }],
      loading: false,
      error: null,
      fetchAll: vi.fn(),
    }

    productStoreMock = {
      items: [{ id: 'p1', name: 'Açaí 330ml', price: 20 }],
      loading: false,
      error: null,
      fetchAll: vi.fn(),
    }

    ingredientStoreMock = {
      items: [
        { id: 'i1', name: 'Granola', unit: 'g', costPerUnit: 0.05, status: 'ACTIVE' },
        {
          id: 'i2',
          name: 'Leite Ninho',
          unit: 'g',
          costPerUnit: 0.0035,
          defaultQuantity: 20,
          status: 'ACTIVE',
        },
      ],
      loading: false,
      error: null,
      fetchAll: vi.fn(),
    }

    feeStoreMock = {
      items: [],
      loading: false,
      error: null,
      fetchAll: vi.fn(),
    }

    notificationStoreMock = {
      items: [],
      unreadCount: 0,
      loading: false,
      error: null,
      fetchAll: vi.fn(),
      refreshCount: vi.fn(),
      markRead: vi.fn(),
      dismiss: vi.fn(),
    }

    includesByProductMock = {
      p1: [
        { id: 'inc1', productId: 'p1', name: 'Copo', cost: 0.1, quantity: 1, totalCost: 0.1, kind: 'PACKAGING' },
        { id: 'inc2', productId: 'p1', name: 'Açaí base', cost: 0.02, quantity: 150, totalCost: 3, kind: null },
        { id: 'inc3', productId: 'p1', name: 'Granola', cost: 0.05, quantity: 40, totalCost: 2, kind: 'INGREDIENT' },
      ],
    }
  })

  it('should not render the Anota.AI import button (moved to Settings > Integrações)', () => {
    const wrapper = mount(OrdersView)
    expect(wrapper.text()).not.toContain('Importar do Anota.AI')
  })

  it('should submit order with extra ingredients', async () => {
    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="new-order-button"]').trigger('click')

    await wrapper.get('[data-testid="order-customer-input"]').setValue('jo')
    await wrapper.get('[data-testid="combo-option-c1"]').trigger('click')
    await wrapper.get('[data-testid="order-item-0-product-select"]').setValue('p1')
    await wrapper.get('[data-testid="order-item-0-quantity-input"]').setValue('2')

    await wrapper.get('[data-testid="order-item-0-add-extra-button"]').trigger('click')
    await wrapper
      .get('[data-testid="order-item-0-extra-0-ingredient-select"]')
      .setValue('i1')
    await wrapper.get('[data-testid="order-item-0-extra-0-quantity-input"]').setValue('1.5')

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(orderStoreMock.create).toHaveBeenCalledWith({
      customerId: 'c1',
      origin: 'MENUBANK',
      items: [
        {
          productId: 'p1',
          quantity: 2,
          extraIngredients: [{ ingredientId: 'i1', quantity: 1.5 }],
          excludedIncludeIds: [],
        },
      ],
    })
    const payload = orderStoreMock.create.mock.calls[0][0]
    expect(payload.customerName).toBeUndefined()
  })

  it('should show a success toast after creating an order', async () => {
    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="new-order-button"]').trigger('click')
    await wrapper.get('[data-testid="order-customer-input"]').setValue('jo')
    await wrapper.get('[data-testid="combo-option-c1"]').trigger('click')
    await wrapper.get('[data-testid="order-item-0-product-select"]').setValue('p1')
    await wrapper.get('[data-testid="order-item-0-quantity-input"]').setValue('1')

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(orderStoreMock.create).toHaveBeenCalledTimes(1)
    expect(showToastMock).toHaveBeenCalledWith('Pedido criado com sucesso!')
  })

  it('should not show a toast when order creation fails', async () => {
    orderStoreMock.create = vi.fn().mockRejectedValue(new Error('boom'))
    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="new-order-button"]').trigger('click')
    await wrapper.get('[data-testid="order-customer-input"]').setValue('jo')
    await wrapper.get('[data-testid="combo-option-c1"]').trigger('click')
    await wrapper.get('[data-testid="order-item-0-product-select"]').setValue('p1')

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(showToastMock).not.toHaveBeenCalled()
  })

  it('should list the product ficha técnica as checked insumos when a product is selected', async () => {
    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="new-order-button"]').trigger('click')
    await wrapper.get('[data-testid="order-item-0-product-select"]').setValue('p1')
    await flushPromises()

    const copo = wrapper.get('[data-testid="order-item-0-insumo-inc1-checkbox"]')
    const acai = wrapper.get('[data-testid="order-item-0-insumo-inc2-checkbox"]')
    expect((copo.element as HTMLInputElement).checked).toBe(true)
    expect((acai.element as HTMLInputElement).checked).toBe(true)
    expect(wrapper.html()).toContain('Copo')
    expect(wrapper.html()).toContain('Açaí base')
  })

  it('should not pull INGREDIENT-kind includes as insumos of the order item', async () => {
    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="new-order-button"]').trigger('click')
    await wrapper.get('[data-testid="order-item-0-product-select"]').setValue('p1')
    await flushPromises()

    expect(wrapper.find('[data-testid="order-item-0-insumo-inc3-checkbox"]').exists()).toBe(false)
  })

  it('should send excludedIncludeIds when an insumo is unchecked', async () => {
    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="new-order-button"]').trigger('click')
    await wrapper.get('[data-testid="order-customer-input"]').setValue('jo')
    await wrapper.get('[data-testid="combo-option-c1"]').trigger('click')
    await wrapper.get('[data-testid="order-item-0-product-select"]').setValue('p1')
    await flushPromises()

    await wrapper.get('[data-testid="order-item-0-insumo-inc1-checkbox"]').setValue(false)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(orderStoreMock.create).toHaveBeenCalledTimes(1)
    const payload = orderStoreMock.create.mock.calls[0][0]
    expect(payload.items[0].excludedIncludeIds).toEqual(['inc1'])
  })

  it('should re-include the insumo cost when the checkbox is checked back', async () => {
    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="new-order-button"]').trigger('click')
    await wrapper.get('[data-testid="order-customer-input"]').setValue('jo')
    await wrapper.get('[data-testid="combo-option-c1"]').trigger('click')
    await wrapper.get('[data-testid="order-item-0-product-select"]').setValue('p1')
    await flushPromises()

    await wrapper.get('[data-testid="order-item-0-insumo-inc1-checkbox"]').setValue(false)
    await wrapper.get('[data-testid="order-item-0-insumo-inc1-checkbox"]').setValue(true)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    const payload = orderStoreMock.create.mock.calls[0][0]
    expect(payload.items[0].excludedIncludeIds).toEqual([])
  })

  it('should show a validation error and keep the modal open when no customer is given', async () => {
    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="new-order-button"]').trigger('click')
    await wrapper.get('[data-testid="order-item-0-product-select"]').setValue('p1')

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[data-testid="order-customer-error"]').text()).toBe(
      'Cliente é obrigatório',
    )
    expect(orderStoreMock.create).not.toHaveBeenCalled()
    expect(wrapper.find('[data-testid="order-form-title"]').exists()).toBe(true)
  })

  it('should treat a whitespace-only customer name as missing', async () => {
    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="new-order-button"]').trigger('click')
    await wrapper.get('[data-testid="order-customer-input"]').setValue('   ')
    await wrapper.get('[data-testid="order-item-0-product-select"]').setValue('p1')

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[data-testid="order-customer-error"]').text()).toBe(
      'Cliente é obrigatório',
    )
    expect(orderStoreMock.create).not.toHaveBeenCalled()
  })

  it('should clear the validation error when the user types a customer name', async () => {
    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="new-order-button"]').trigger('click')
    await wrapper.get('form').trigger('submit')
    expect(wrapper.find('[data-testid="order-customer-error"]').exists()).toBe(true)

    await wrapper.get('[data-testid="order-customer-input"]').setValue('Ma')

    expect(wrapper.find('[data-testid="order-customer-error"]').exists()).toBe(false)
  })

  it('should quick-create: submit customerName without customerId when typing a new name', async () => {
    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="new-order-button"]').trigger('click')
    await wrapper.get('[data-testid="order-customer-input"]').setValue('  Maria ')
    await wrapper.get('[data-testid="order-item-0-product-select"]').setValue('p1')

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(orderStoreMock.create).toHaveBeenCalledTimes(1)
    const payload = orderStoreMock.create.mock.calls[0][0]
    expect(payload.customerName).toBe('Maria')
    expect(payload.customerId).toBeUndefined()
  })

  it('should show the backend error inside the modal and keep it open on failure', async () => {
    orderStoreMock.create = vi.fn().mockImplementation(() => {
      orderStoreMock.error = 'Cliente é obrigatório'
      return Promise.reject(new Error('bad request'))
    })

    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="new-order-button"]').trigger('click')
    await wrapper.get('[data-testid="order-customer-input"]').setValue('Maria')
    await wrapper.get('[data-testid="order-item-0-product-select"]').setValue('p1')

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[data-testid="order-modal-error"]').text()).toContain(
      'Cliente é obrigatório',
    )
    expect(wrapper.find('[data-testid="order-form-title"]').exists()).toBe(true)
  })

  it('should auto-fill extra ingredient quantity with ingredient defaultQuantity', async () => {
    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="new-order-button"]').trigger('click')
    await wrapper.get('[data-testid="order-item-0-product-select"]').setValue('p1')
    await wrapper.get('[data-testid="order-item-0-add-extra-button"]').trigger('click')

    const quantityInput = wrapper.get(
      '[data-testid="order-item-0-extra-0-quantity-input"]',
    )
    expect((quantityInput.element as HTMLInputElement).value).toBe('1')

    await wrapper
      .get('[data-testid="order-item-0-extra-0-ingredient-select"]')
      .setValue('i2')

    expect((quantityInput.element as HTMLInputElement).value).toBe('20')
  })

  it('should render order details with extra ingredients, costs and profit breakdown', async () => {
    orderStoreMock.items = [
      {
        id: 'o1',
        dateTime: '2026-05-14T10:00:00',
        customerId: 'c1',
        customerName: 'João',
        status: 'PAID',
        totalValue: 60,
        estimatedProfit: 26,
        items: [
          {
            id: 'oi1',
            productId: 'p1',
            productName: 'Hambúrguer',
            quantity: 2,
            unitPrice: 30,
            unitCost: 17,
            totalCost: 34,
            extraIngredients: [
              {
                id: 'oei1',
                ingredientId: 'i1',
                ingredientName: 'Bacon',
                ingredientUnit: 'g',
                quantity: 50,
                costPerUnit: 0.1,
                totalCost: 10,
              },
            ],
          },
        ],
      },
    ]

    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="order-o1-detail-button"]').trigger('click')

    const detail = wrapper.get('[data-testid="order-detail-modal"]')
    const html = detail.html()
    expect(html).toContain('Bacon')
    expect(html).toContain('50')
    expect(html).toMatch(/Custo/i)
    expect(detail.get('[data-testid="order-detail-total-cost"]').text()).toMatch(/34/)
    expect(detail.get('[data-testid="order-detail-estimated-profit"]').text()).toMatch(/26/)
    expect(detail.get('[data-testid="order-detail-margin"]').text()).toMatch(/43[,.]/)
  })

  it('should render TEST order status with pt-BR label "Teste"', () => {
    orderStoreMock.items = [
      {
        id: 'o1',
        dateTime: '2026-07-01T10:00:00',
        customerId: 'c1',
        customerName: 'João',
        status: 'TEST',
        totalValue: 30,
        estimatedProfit: 10,
        items: [],
      },
    ]

    const wrapper = mount(OrdersView)

    expect(wrapper.html()).toContain('Teste')
    expect(wrapper.html()).not.toContain('>TEST<')
  })

  it('should populate form with existing order and call update on submit when editing', async () => {
    orderStoreMock.update = vi.fn().mockResolvedValue({})
    orderStoreMock.items = [
      {
        id: 'o1',
        dateTime: '2026-05-14T10:00:00',
        customerId: 'c1',
        customerName: 'João',
        status: 'PAID',
        totalValue: 60,
        estimatedProfit: 26,
        items: [
          {
            id: 'oi1',
            productId: 'p1',
            productName: 'Açaí 330ml',
            quantity: 2,
            unitPrice: 20,
            unitCost: 10,
            totalCost: 20,
            extraIngredients: [
              {
                id: 'oei1',
                ingredientId: 'i1',
                ingredientName: 'Granola',
                ingredientUnit: 'g',
                quantity: 30,
                costPerUnit: 0.05,
                totalCost: 3,
              },
            ],
          },
        ],
      },
    ]

    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="order-o1-edit-button"]').trigger('click')

    expect(wrapper.get('[data-testid="order-form-title"]').text()).toBe('Editar Pedido')
    expect(
      (wrapper.get('[data-testid="order-customer-input"]').element as HTMLInputElement).value,
    ).toBe('João')
    expect(
      (wrapper.get('[data-testid="order-item-0-product-select"]').element as HTMLSelectElement).value,
    ).toBe('p1')
    expect(
      (wrapper.get('[data-testid="order-item-0-quantity-input"]').element as HTMLInputElement).value,
    ).toBe('2')
    expect(
      (wrapper.get('[data-testid="order-item-0-extra-0-ingredient-select"]').element as HTMLSelectElement)
        .value,
    ).toBe('i1')
    expect(
      (wrapper.get('[data-testid="order-item-0-extra-0-quantity-input"]').element as HTMLInputElement)
        .value,
    ).toBe('30')

    await wrapper.get('[data-testid="order-item-0-quantity-input"]').setValue('3')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(orderStoreMock.update).toHaveBeenCalledWith(
      'o1',
      expect.objectContaining({
        customerId: 'c1',
        status: 'PAID',
        items: [
          expect.objectContaining({
            productId: 'p1',
            quantity: 3,
            extraIngredients: [{ ingredientId: 'i1', quantity: 30 }],
          }),
        ],
      }),
    )
    expect(orderStoreMock.create).not.toHaveBeenCalled()
  })

  it('should restore unchecked insumos when editing an order with exclusions', async () => {
    orderStoreMock.update = vi.fn().mockResolvedValue({})
    orderStoreMock.items = [
      {
        id: 'o1',
        dateTime: '2026-05-14T10:00:00',
        customerId: 'c1',
        customerName: 'João',
        status: 'PAID',
        totalValue: 40,
        estimatedProfit: 20,
        items: [
          {
            id: 'oi1',
            productId: 'p1',
            productName: 'Açaí 330ml',
            quantity: 1,
            unitPrice: 20,
            unitCost: 0.1,
            totalCost: 0.1,
            extraIngredients: [],
            excludedIncludeIds: ['inc2'],
          },
        ],
      },
    ]

    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="order-o1-edit-button"]').trigger('click')
    await flushPromises()

    const copo = wrapper.get('[data-testid="order-item-0-insumo-inc1-checkbox"]')
    const acai = wrapper.get('[data-testid="order-item-0-insumo-inc2-checkbox"]')
    expect((copo.element as HTMLInputElement).checked).toBe(true)
    expect((acai.element as HTMLInputElement).checked).toBe(false)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    const payload = orderStoreMock.update.mock.calls[0][1]
    expect(payload.items[0].excludedIncludeIds).toEqual(['inc2'])
  })

  it('should fall back to quantity 1 when selected ingredient has no defaultQuantity', async () => {
    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="new-order-button"]').trigger('click')
    await wrapper.get('[data-testid="order-item-0-product-select"]').setValue('p1')
    await wrapper.get('[data-testid="order-item-0-add-extra-button"]').trigger('click')

    await wrapper
      .get('[data-testid="order-item-0-extra-0-ingredient-select"]')
      .setValue('i1')

    const quantityInput = wrapper.get(
      '[data-testid="order-item-0-extra-0-quantity-input"]',
    )
    expect((quantityInput.element as HTMLInputElement).value).toBe('1')
  })
})




