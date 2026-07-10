import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, enableAutoUnmount, RouterLinkStub, flushPromises } from '@vue/test-utils'
import ResetPasswordView from '@/views/ResetPasswordView.vue'

const updatePassword = vi.fn()
const authState = {
  error: null as string | null,
  loading: false,
  updatePassword,
}

vi.mock('@/stores/authStore', () => ({ useAuthStore: () => authState }))

enableAutoUnmount(afterEach)

const GLOBAL = { global: { stubs: { RouterLink: RouterLinkStub } } }

beforeEach(() => {
  vi.clearAllMocks()
  authState.error = null
  authState.loading = false
})

describe('ResetPasswordView', () => {
  it('senha com menos de 6 caracteres é bloqueada', async () => {
    const wrapper = mount(ResetPasswordView, GLOBAL)

    await wrapper.find('#newPassword').setValue('abc12')
    await wrapper.find('#confirmNewPassword').setValue('abc12')
    await wrapper.find('form').trigger('submit')

    expect(updatePassword).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('no mínimo 6 caracteres')
  })

  it('senhas divergentes são bloqueadas', async () => {
    const wrapper = mount(ResetPasswordView, GLOBAL)

    await wrapper.find('#newPassword').setValue('senha123')
    await wrapper.find('#confirmNewPassword').setValue('outraSenha')
    await wrapper.find('form').trigger('submit')

    expect(updatePassword).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('As senhas não conferem')
  })

  it('redefine a senha e mostra o estado de sucesso com CTA ao painel', async () => {
    updatePassword.mockResolvedValue(undefined)
    const wrapper = mount(ResetPasswordView, GLOBAL)

    await wrapper.find('#newPassword').setValue('novaSenha1')
    await wrapper.find('#confirmNewPassword').setValue('novaSenha1')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(updatePassword).toHaveBeenCalledWith('novaSenha1')
    expect(wrapper.text()).toContain('Senha redefinida')
    const links = wrapper.findAllComponents(RouterLinkStub)
    expect(links.some((l) => l.props('to') === '/dashboard')).toBe(true)
  })

  it('mostra o erro do store quando a redefinição falha', async () => {
    updatePassword.mockRejectedValue(new Error('boom'))
    authState.error = 'Erro ao redefinir a senha. Tente novamente.'
    const wrapper = mount(ResetPasswordView, GLOBAL)

    await wrapper.find('#newPassword').setValue('novaSenha1')
    await wrapper.find('#confirmNewPassword').setValue('novaSenha1')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Erro ao redefinir a senha')
    expect(wrapper.text()).not.toContain('Senha redefinida')
  })
})
