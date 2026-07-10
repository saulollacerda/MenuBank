import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, enableAutoUnmount, RouterLinkStub } from '@vue/test-utils'
import EmailVerifiedView from '@/views/EmailVerifiedView.vue'

const authState = { isAuthenticated: false }

vi.mock('@/stores/authStore', () => ({ useAuthStore: () => authState }))

enableAutoUnmount(afterEach)

const GLOBAL = { global: { stubs: { RouterLink: RouterLinkStub } } }

beforeEach(() => {
  authState.isAuthenticated = false
})

describe('EmailVerifiedView', () => {
  it('autenticado (link de confirmação já conectou): CTA leva ao painel', () => {
    authState.isAuthenticated = true
    const wrapper = mount(EmailVerifiedView, GLOBAL)

    const link = wrapper.findComponent(RouterLinkStub)
    expect(link.props('to')).toBe('/dashboard')
    expect(wrapper.text()).toContain('Ir para o painel')
    expect(wrapper.text()).not.toContain('Faça login')
  })

  it('não autenticado (link aberto em outro navegador): CTA leva ao login', () => {
    const wrapper = mount(EmailVerifiedView, GLOBAL)

    const link = wrapper.findComponent(RouterLinkStub)
    expect(link.props('to')).toBe('/login')
    expect(wrapper.text()).toContain('Ir para o login')
  })
})
