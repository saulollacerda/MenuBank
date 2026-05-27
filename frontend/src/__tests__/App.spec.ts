import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia } from 'pinia'
import App from '../App.vue'

const mockStorage: Record<string, string> = {}
const localStorageMock: Storage = {
  getItem: vi.fn((key: string) => mockStorage[key] ?? null),
  setItem: vi.fn((key: string, value: string) => {
    mockStorage[key] = value
  }),
  removeItem: vi.fn((key: string) => {
    delete mockStorage[key]
  }),
  clear: vi.fn(() => {
    Object.keys(mockStorage).forEach((key) => delete mockStorage[key])
  }),
  get length() {
    return Object.keys(mockStorage).length
  },
  key: vi.fn((index: number) => Object.keys(mockStorage)[index] ?? null),
}

vi.stubGlobal('localStorage', localStorageMock)

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: { template: '<div>Dashboard</div>' } },
    { path: '/login', component: { template: '<div>Login</div>' }, meta: { public: true } },
    {
      path: '/register',
      component: { template: '<div>Register</div>' },
      meta: { public: true },
    },
    { path: '/orders', component: { template: '<div>Orders</div>' } },
    { path: '/products', component: { template: '<div>Products</div>' } },
    { path: '/categories', component: { template: '<div>Categories</div>' } },
    { path: '/ingredients', component: { template: '<div>Ingredients</div>' } },
    { path: '/customers', component: { template: '<div>Customers</div>' } },
  ],
})

describe('App', () => {
  beforeEach(() => {
    localStorageMock.clear()
    vi.clearAllMocks()
  })

  it('renders the sidebar with navigation links when authenticated', async () => {
    localStorageMock.setItem('menubank_token', 'fake-token')
    localStorageMock.setItem(
      'menubank_user',
      JSON.stringify({ userId: '1', email: 'test@test.com', restaurantName: 'Teste' }),
    )

    router.push('/')
    await router.isReady()

    const wrapper = mount(App, {
      global: {
        plugins: [createPinia(), router],
      },
    })

    expect(wrapper.text()).toContain('MenuBank')
    expect(wrapper.text()).toContain('Dashboard')
    expect(wrapper.text()).toContain('Pedidos')
    expect(wrapper.text()).toContain('Produtos')
    expect(wrapper.text()).toContain('Categorias')
    expect(wrapper.text()).toContain('Ingredientes')
    expect(wrapper.text()).toContain('Clientes')
  })

  it('has the correct layout structure when authenticated', async () => {
    localStorageMock.setItem('menubank_token', 'fake-token')
    localStorageMock.setItem(
      'menubank_user',
      JSON.stringify({ userId: '1', email: 'test@test.com', restaurantName: 'Teste' }),
    )

    router.push('/')
    await router.isReady()

    const wrapper = mount(App, {
      global: {
        plugins: [createPinia(), router],
      },
    })

    expect(wrapper.find('.app-layout').exists()).toBe(true)
    expect(wrapper.find('aside').exists()).toBe(true)
    expect(wrapper.find('.main-content').exists()).toBe(true)
  })

  it('does not render sidebar when not authenticated', async () => {
    router.push('/login')
    await router.isReady()

    const wrapper = mount(App, {
      global: {
        plugins: [createPinia(), router],
      },
    })

    expect(wrapper.find('.app-layout').exists()).toBe(false)
    expect(wrapper.find('aside').exists()).toBe(false)
  })
})
