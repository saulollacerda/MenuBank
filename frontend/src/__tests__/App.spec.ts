import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia } from 'pinia'
import App from '../App.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: { template: '<div>Dashboard</div>' } },
    { path: '/orders', component: { template: '<div>Orders</div>' } },
    { path: '/products', component: { template: '<div>Products</div>' } },
    { path: '/categories', component: { template: '<div>Categories</div>' } },
    { path: '/ingredients', component: { template: '<div>Ingredients</div>' } },
    { path: '/customers', component: { template: '<div>Customers</div>' } },
  ],
})

describe('App', () => {
  it('renders the sidebar with navigation links', async () => {
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

  it('has the correct layout structure', async () => {
    router.push('/')
    await router.isReady()

    const wrapper = mount(App, {
      global: {
        plugins: [createPinia(), router],
      },
    })

    expect(wrapper.find('.app-layout').exists()).toBe(true)
    expect(wrapper.find('.sidebar').exists()).toBe(true)
    expect(wrapper.find('.main-content').exists()).toBe(true)
  })
})
