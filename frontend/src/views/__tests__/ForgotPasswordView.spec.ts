import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, enableAutoUnmount, RouterLinkStub } from '@vue/test-utils'
import ForgotPasswordView from '@/views/ForgotPasswordView.vue'

const requestPasswordReset = vi.fn()
const authState = {
  error: null as string | null,
  loading: false,
  passwordResetEmailSent: false,
  requestPasswordReset,
}

vi.mock('@/stores/authStore', () => ({ useAuthStore: () => authState }))

enableAutoUnmount(afterEach)

const GLOBAL = { global: { stubs: { RouterLink: RouterLinkStub } } }

beforeEach(() => {
  vi.clearAllMocks()
  authState.error = null
  authState.loading = false
  authState.passwordResetEmailSent = false
})

describe('ForgotPasswordView', () => {
  it('envia o email de recuperação informado', async () => {
    requestPasswordReset.mockResolvedValue(undefined)
    const wrapper = mount(ForgotPasswordView, GLOBAL)

    await wrapper.find('input[type="email"]').setValue('a@b.com')
    await wrapper.find('form').trigger('submit')

    expect(requestPasswordReset).toHaveBeenCalledWith('a@b.com')
  })

  it('mostra a confirmação de envio quando o email foi enviado', () => {
    authState.passwordResetEmailSent = true
    const wrapper = mount(ForgotPasswordView, GLOBAL)

    expect(wrapper.text()).toContain('Enviamos um link de recuperação')
  })

  it('mostra o erro do store quando o envio falha', () => {
    authState.error = 'Erro ao enviar o email de recuperação. Tente novamente.'
    const wrapper = mount(ForgotPasswordView, GLOBAL)

    expect(wrapper.text()).toContain('Erro ao enviar o email de recuperação')
  })

  it('tem link de volta para o login', () => {
    const wrapper = mount(ForgotPasswordView, GLOBAL)

    const links = wrapper.findAllComponents(RouterLinkStub)
    expect(links.some((l) => l.props('to') === '/login')).toBe(true)
  })
})
