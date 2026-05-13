import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

let orderStoreMock: any
let customerStoreMock: any
let productStoreMock: any
let ingredientStoreMock: any

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

import OrdersView from '@/views/OrdersView.vue'

describe('OrdersView', () => {
  beforeEach(() => {
    orderStoreMock = {
      items: [],
      loading: false,
      error: null,
      fetchAll: vi.fn(),
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
      items: [{ id: 'i1', name: 'Granola', unit: 'g', costPerUnit: 0.05, status: 'ACTIVE' }],
      loading: false,
      error: null,
      fetchAll: vi.fn(),
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
      items: [
        {
          productId: 'p1',
          quantity: 2,
          extraIngredients: [{ ingredientId: 'i1', quantity: 1.5 }],
        },
      ],
    })
  })
})




