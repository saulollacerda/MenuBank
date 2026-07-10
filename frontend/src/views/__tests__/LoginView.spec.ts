import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, enableAutoUnmount, RouterLinkStub } from '@vue/test-utils'
import LoginView from '@/views/LoginView.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

const authState = {
  error: null as string | null,
  loading: false,
  login: vi.fn(),
}

vi.mock('@/stores/authStore', () => ({ useAuthStore: () => authState }))

enableAutoUnmount(afterEach)

const GLOBAL = { global: { stubs: { RouterLink: RouterLinkStub } } }

beforeEach(() => {
  vi.clearAllMocks()
  authState.error = null
  authState.loading = false
})

describe('LoginView', () => {
  it('"Esqueceu a senha?" é um link para /esqueci-senha', () => {
    const wrapper = mount(LoginView, GLOBAL)

    const links = wrapper.findAllComponents(RouterLinkStub)
    const forgot = links.find((l) => l.props('to') === '/esqueci-senha')
    expect(forgot).toBeDefined()
    expect(forgot!.text()).toContain('Esqueceu a senha?')
  })

  it('não exibe a opção sem efeito "Manter conectado"', () => {
    const wrapper = mount(LoginView, GLOBAL)

    expect(wrapper.text()).not.toContain('Manter conectado')
  })
})
