import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ref } from 'vue'
import RegisterView from '@/views/RegisterView.vue'

const registerMock = vi.fn()
const pushMock = vi.fn()
const error = ref<string | null>(null)
const loading = ref(false)

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    register: registerMock,
    loading,
    error,
  }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock,
  }),
}))

function mountView() {
  return mount(RegisterView, {
    global: {
      stubs: {
        RouterLink: true,
      },
    },
  })
}

async function fillForm(wrapper: ReturnType<typeof mountView>, overrides?: Partial<Record<string, string>>) {
  const values = {
    restaurantName: 'Restaurante Teste',
    cnpj: '12345678000195',
    email: 'teste@email.com',
    password: 'senha123',
    confirmPassword: 'senha123',
    phone: '11999999999',
    ...overrides,
  }

  await wrapper.find('#restaurantName').setValue(values.restaurantName)
  await wrapper.find('#cnpj').setValue(values.cnpj)
  await wrapper.find('#email').setValue(values.email)
  await wrapper.find('#password').setValue(values.password)
  await wrapper.find('#confirmPassword').setValue(values.confirmPassword)
  await wrapper.find('#phone').setValue(values.phone)
}

beforeEach(() => {
  registerMock.mockReset()
  pushMock.mockReset()
  error.value = null
  loading.value = false
})

describe('RegisterView', () => {
  it('mostra erro quando CNPJ é inválido', async () => {
    const wrapper = mountView()

    await fillForm(wrapper, { cnpj: '12345678000199' })
    await wrapper.find('form').trigger('submit')

    expect(registerMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('CNPJ inválido')
  })

  it('mostra erro quando as senhas não conferem', async () => {
    const wrapper = mountView()

    await fillForm(wrapper, { confirmPassword: 'senhaDiferente' })
    await wrapper.find('form').trigger('submit')

    expect(registerMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('As senhas não conferem')
  })

  it('envia cadastro válido e redireciona', async () => {
    const wrapper = mountView()

    registerMock.mockResolvedValueOnce({
      token: 'token',
      userId: '123',
      email: 'teste@email.com',
      restaurantName: 'Restaurante Teste',
    })

    await fillForm(wrapper)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(registerMock).toHaveBeenCalledOnce()
    expect(registerMock).toHaveBeenCalledWith({
      restaurantName: 'Restaurante Teste',
      cnpj: '12345678000195',
      email: 'teste@email.com',
      password: 'senha123',
      confirmPassword: 'senha123',
      phone: '11999999999',
    })
    expect(pushMock).toHaveBeenCalledWith('/')
  })
})

