import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

let orderStoreMock: any
let customerStoreMock: any
let productStoreMock: any
let ingredientStoreMock: any
let feeStoreMock: any
let anotaAIStoreMock: any
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

vi.mock('@/stores/anotaAIStore', () => ({
  useAnotaAIStore: () => anotaAIStoreMock,
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

import OrdersView from '@/views/OrdersView.vue'

describe('OrdersView', () => {
  beforeEach(() => {
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

    anotaAIStoreMock = {
      syncingOrders: false,
      syncingCatalog: false,
      lastResult: null,
      error: null,
      syncOrders: vi.fn(),
      syncCatalog: vi.fn(),
      clearResult: vi.fn(),
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
  })

  it('should submit order with extra ingredients', async () => {
    const wrapper = mount(OrdersView)

    await wrapper.get('[data-testid="new-order-button"]').trigger('click')

    await wrapper.get('[data-testid="order-customer-select"]').setValue('c1')
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
        },
      ],
    })
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
      (wrapper.get('[data-testid="order-customer-select"]').element as HTMLSelectElement).value,
    ).toBe('c1')
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




