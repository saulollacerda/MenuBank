import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, enableAutoUnmount, RouterLinkStub, type VueWrapper } from '@vue/test-utils'
import RegisterView from '@/views/RegisterView.vue'

const registerFn = vi.fn()
const authState = {
  error: null as string | null,
  loading: false,
  awaitingEmailConfirmation: false,
  register: registerFn,
}

vi.mock('@/stores/authStore', () => ({ useAuthStore: () => authState }))

enableAutoUnmount(afterEach)

const GLOBAL = { global: { stubs: { RouterLink: RouterLinkStub } } }

// Well-known valid test CNPJ (check digits pass).
const VALID_CNPJ = '11.222.333/0001-81'

function mountView() {
  return mount(RegisterView, GLOBAL)
}

async function fillForm(wrapper: VueWrapper, overrides: Record<string, string> = {}) {
  const values: Record<string, string> = {
    merchantName: 'Loja',
    cnpj: VALID_CNPJ,
    email: 'a@b.com',
    password: 'senha123',
    confirmPassword: 'senha123',
    phone: '11999999999',
    ...overrides,
  }
  for (const [id, value] of Object.entries(values)) {
    await wrapper.find(`#${id}`).setValue(value)
  }
}

async function acceptTerms(wrapper: VueWrapper) {
  const termsLabel = wrapper.findAll('label').find((l) => l.text().includes('Aceito os'))
  expect(termsLabel).toBeDefined()
  await termsLabel!.trigger('click')
}

beforeEach(() => {
  vi.clearAllMocks()
  authState.error = null
  authState.loading = false
  authState.awaitingEmailConfirmation = false
})

describe('RegisterView', () => {
  it('termos desmarcados por padrão: submit sem aceite é bloqueado', async () => {
    const wrapper = mountView()
    await fillForm(wrapper)

    await wrapper.find('form').trigger('submit')

    expect(registerFn).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('aceitar os termos')
  })

  it('senha com menos de 6 caracteres é bloqueada no frontend', async () => {
    const wrapper = mountView()
    await acceptTerms(wrapper)
    await fillForm(wrapper, { password: 'abc12', confirmPassword: 'abc12' })

    await wrapper.find('form').trigger('submit')

    expect(registerFn).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('no mínimo 6 caracteres')
  })

  it('senhas divergentes são bloqueadas no frontend', async () => {
    const wrapper = mountView()
    await acceptTerms(wrapper)
    await fillForm(wrapper, { confirmPassword: 'outraSenha' })

    await wrapper.find('form').trigger('submit')

    expect(registerFn).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('As senhas não conferem')
  })

  it('CNPJ inválido é bloqueado no frontend', async () => {
    const wrapper = mountView()
    await acceptTerms(wrapper)
    await fillForm(wrapper, { cnpj: '11.111.111/1111-11' })

    await wrapper.find('form').trigger('submit')

    expect(registerFn).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('CNPJ inválido')
  })

  it('formulário válido com termos aceitos chama register com os dados preenchidos', async () => {
    registerFn.mockResolvedValue(undefined)
    const wrapper = mountView()
    await acceptTerms(wrapper)
    await fillForm(wrapper)

    await wrapper.find('form').trigger('submit')

    expect(registerFn).toHaveBeenCalledOnce()
    expect(registerFn).toHaveBeenCalledWith({
      merchantName: 'Loja',
      cnpj: VALID_CNPJ,
      email: 'a@b.com',
      password: 'senha123',
      confirmPassword: 'senha123',
      phone: '11999999999',
    })
  })

  it('mostra o aviso de confirmação quando o cadastro aguarda o email', () => {
    authState.awaitingEmailConfirmation = true
    const wrapper = mountView()

    expect(wrapper.text()).toContain('email de confirmação')
  })

  it('inputs obrigatórios têm required e a senha tem minlength', () => {
    const wrapper = mountView()

    for (const id of ['merchantName', 'cnpj', 'email', 'password', 'confirmPassword']) {
      expect(wrapper.find(`#${id}`).attributes('required'), `#${id} required`).toBeDefined()
    }
    expect(wrapper.find('#password').attributes('minlength')).toBe('6')
  })
})
