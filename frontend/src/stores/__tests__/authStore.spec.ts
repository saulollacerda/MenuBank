import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

const {
  signIn,
  signUp,
  signOut,
  init,
  onAuthChange,
  getAccessToken,
  requestPasswordReset,
  updatePassword,
} = vi.hoisted(() => ({
  signIn: vi.fn(),
  signUp: vi.fn(),
  signOut: vi.fn(),
  init: vi.fn(),
  onAuthChange: vi.fn(),
  getAccessToken: vi.fn(),
  requestPasswordReset: vi.fn(),
  updatePassword: vi.fn(),
}))

vi.mock('@/lib/authProvider', async () => {
  const actual = await vi.importActual<typeof import('@/lib/authTypes')>('@/lib/authTypes')
  return {
    authProvider: {
      signIn,
      signUp,
      signOut,
      init,
      onAuthChange,
      getAccessToken,
      requestPasswordReset,
      updatePassword,
    },
    AuthError: actual.AuthError,
  }
})

const getMe = vi.fn()
const provision = vi.fn()

vi.mock('@/services/userService', () => ({
  userService: { getMe: (...a: unknown[]) => getMe(...a) },
}))
vi.mock('@/services/authService', () => ({
  authService: { provision: (...a: unknown[]) => provision(...a) },
}))

import { useAuthStore } from '@/stores/authStore'
import { AuthError } from '@/lib/authTypes'

const merchant = { id: 'm1', merchantName: 'Loja', email: 'a@b.com' }

function sessionWith(meta: Record<string, unknown> = {}) {
  return { accessToken: 't', user: { email: 'a@b.com', user_metadata: meta } }
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
  init.mockResolvedValue(null)
})

describe('authStore', () => {
  it('login com sucesso resolve sessão e carrega o merchant', async () => {
    signIn.mockResolvedValue(sessionWith())
    getMe.mockResolvedValue(merchant)

    const store = useAuthStore()
    await store.login({ email: 'a@b.com', password: 'x' })

    expect(signIn).toHaveBeenCalledWith('a@b.com', 'x')
    expect(store.isAuthenticated).toBe(true)
    expect(store.currentUser).toEqual(merchant)
    expect(provision).not.toHaveBeenCalled()
  })

  it('login com credenciais inválidas seta erro pt-BR', async () => {
    signIn.mockRejectedValue(new AuthError('invalid_credentials'))

    const store = useAuthStore()
    await expect(store.login({ email: 'a@b.com', password: 'x' })).rejects.toBeTruthy()

    expect(store.error).toBe('Email ou senha inválidos')
    expect(store.isAuthenticated).toBe(false)
  })

  it('login com erro desconhecido (rede/rate limit) seta erro pt-BR genérico', async () => {
    signIn.mockRejectedValue(new AuthError('unknown'))

    const store = useAuthStore()
    await expect(store.login({ email: 'a@b.com', password: 'x' })).rejects.toBeTruthy()

    expect(store.error).toBe('Erro ao entrar. Tente novamente.')
    expect(store.isAuthenticated).toBe(false)
  })

  it('provision com 409 (email/CNPJ já cadastrado) seta erro pt-BR', async () => {
    signIn.mockResolvedValue(sessionWith({ merchantName: 'Loja', cnpj: '123' }))
    getMe.mockRejectedValue({ response: { status: 403 } })
    provision.mockRejectedValue({ response: { status: 409 } })

    const store = useAuthStore()
    await expect(store.login({ email: 'a@b.com', password: 'x' })).rejects.toBeTruthy()

    expect(store.error).toBe('Já existe um cadastro com este email ou CNPJ.')
  })

  it('provision com erro genérico seta erro pt-BR', async () => {
    signIn.mockResolvedValue(sessionWith({ merchantName: 'Loja', cnpj: '123' }))
    getMe.mockRejectedValue({ response: { status: 403 } })
    provision.mockRejectedValue({ response: { status: 500 } })

    const store = useAuthStore()
    await expect(store.login({ email: 'a@b.com', password: 'x' })).rejects.toBeTruthy()

    expect(store.error).toBe('Erro ao concluir seu cadastro. Tente novamente.')
  })

  it('1º acesso (getMe 403) dispara provision e recarrega', async () => {
    signIn.mockResolvedValue(sessionWith({ merchantName: 'Loja', cnpj: '123', phone: '9' }))
    getMe.mockRejectedValueOnce({ response: { status: 403 } }).mockResolvedValueOnce(merchant)
    provision.mockResolvedValue(merchant)

    const store = useAuthStore()
    await store.login({ email: 'a@b.com', password: 'x' })

    expect(provision).toHaveBeenCalledWith({
      merchantName: 'Loja',
      cnpj: '123',
      email: 'a@b.com',
      phone: '9',
    })
    expect(store.currentUser).toEqual(merchant)
  })

  it('register sem sessão (confirmação de email) seta awaitingEmailConfirmation', async () => {
    signUp.mockResolvedValue({ session: null })

    const store = useAuthStore()
    await store.register({
      merchantName: 'Loja',
      cnpj: '123',
      email: 'a@b.com',
      password: 'senha123',
      confirmPassword: 'senha123',
      phone: '9',
    })

    expect(signUp).toHaveBeenCalledOnce()
    const arg = signUp.mock.calls[0]![0] as Record<string, unknown>
    expect(arg).toMatchObject({ merchantName: 'Loja', cnpj: '123', phone: '9', email: 'a@b.com' })
    expect(store.awaitingEmailConfirmation).toBe(true)
    expect(store.isAuthenticated).toBe(false)
  })

  it('register com sessão imediata (local) autentica e carrega o merchant', async () => {
    signUp.mockResolvedValue({ session: sessionWith() })
    getMe.mockResolvedValue(merchant)

    const store = useAuthStore()
    await store.register({
      merchantName: 'Loja',
      cnpj: '123',
      email: 'a@b.com',
      password: 'senha123',
      confirmPassword: 'senha123',
      phone: '9',
    })

    expect(store.awaitingEmailConfirmation).toBe(false)
    expect(store.isAuthenticated).toBe(true)
    expect(store.currentUser).toEqual(merchant)
  })

  it('register com email já cadastrado seta erro pt-BR', async () => {
    signUp.mockRejectedValue(new AuthError('email_exists'))

    const store = useAuthStore()
    await expect(
      store.register({
        merchantName: 'Loja',
        cnpj: '123',
        email: 'a@b.com',
        password: 'senha123',
        confirmPassword: 'senha123',
        phone: '9',
      }),
    ).rejects.toBeTruthy()

    expect(store.error).toBe('Email já cadastrado')
    expect(store.isAuthenticated).toBe(false)
  })

  it('provisionamento concorrente (login + onAuthChange) dispara provision uma única vez', async () => {
    const store = useAuthStore()
    await store.init()
    const authChange = onAuthChange.mock.calls[0]![0] as (s: unknown) => void

    // Async delays so both racing calls observe the unprovisioned state (403)
    // before either provision request completes, as against a real backend.
    let provisioned = false
    getMe.mockImplementation(() =>
      provisioned
        ? Promise.resolve(merchant)
        : new Promise((_, reject) =>
            setTimeout(() => reject({ response: { status: 403 } }), 0),
          ),
    )
    provision.mockImplementation(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0))
      provisioned = true
      return merchant
    })
    // The local provider notifies listeners synchronously inside signIn, before
    // login() has assigned session.value — the historical duplicate-provision race.
    signIn.mockImplementation(async () => {
      authChange(sessionWith({ merchantName: 'Loja', cnpj: '123' }))
      return sessionWith({ merchantName: 'Loja', cnpj: '123' })
    })

    await store.login({ email: 'a@b.com', password: 'x' })
    await new Promise((resolve) => setTimeout(resolve, 10))

    expect(provision).toHaveBeenCalledTimes(1)
    expect(store.currentUser).toEqual(merchant)
  })

  it('init concorrente compartilha a mesma restauração (2ª chamada só resolve com a sessão pronta)', async () => {
    let resolveProviderInit!: (session: unknown) => void
    init.mockImplementation(() => new Promise((resolve) => (resolveProviderInit = resolve)))
    getMe.mockResolvedValue(merchant)

    const store = useAuthStore()
    const first = store.init()
    let secondSettled = false
    const second = store.init().then(() => (secondSettled = true))

    // Flush microtasks: the provider restore is still pending, so neither call may settle.
    await new Promise((resolve) => setTimeout(resolve, 0))
    expect(secondSettled).toBe(false)

    resolveProviderInit(sessionWith())
    await Promise.all([first, second])

    expect(store.isAuthenticated).toBe(true)
    expect(init).toHaveBeenCalledTimes(1)
  })

  it('register normaliza o CNPJ para somente dígitos antes de enviar', async () => {
    signUp.mockResolvedValue({ session: null })

    const store = useAuthStore()
    await store.register({
      merchantName: 'Loja',
      cnpj: '12.345.678/0001-95',
      email: 'a@b.com',
      password: 'senha123',
      confirmPassword: 'senha123',
      phone: '9',
    })

    expect(signUp.mock.calls[0]![0]).toMatchObject({ cnpj: '12345678000195' })
  })

  describe('recuperação de senha', () => {
    it('requestPasswordReset com sucesso marca passwordResetEmailSent', async () => {
      requestPasswordReset.mockResolvedValue(undefined)

      const store = useAuthStore()
      await store.requestPasswordReset('a@b.com')

      expect(requestPasswordReset).toHaveBeenCalledWith('a@b.com')
      expect(store.passwordResetEmailSent).toBe(true)
      expect(store.error).toBeNull()
    })

    it('requestPasswordReset not_supported (modo local) seta erro pt-BR', async () => {
      requestPasswordReset.mockRejectedValue(new AuthError('not_supported'))

      const store = useAuthStore()
      await expect(store.requestPasswordReset('a@b.com')).rejects.toBeTruthy()

      expect(store.error).toBe(
        'Recuperação de senha não está disponível no ambiente de desenvolvimento.',
      )
      expect(store.passwordResetEmailSent).toBe(false)
    })

    it('requestPasswordReset com falha seta erro pt-BR genérico', async () => {
      requestPasswordReset.mockRejectedValue(new AuthError('unknown'))

      const store = useAuthStore()
      await expect(store.requestPasswordReset('a@b.com')).rejects.toBeTruthy()

      expect(store.error).toBe('Erro ao enviar o email de recuperação. Tente novamente.')
    })

    it('updatePassword delega ao provider', async () => {
      updatePassword.mockResolvedValue(undefined)

      const store = useAuthStore()
      await store.updatePassword('novaSenha1')

      expect(updatePassword).toHaveBeenCalledWith('novaSenha1')
      expect(store.error).toBeNull()
    })

    it('updatePassword com falha seta erro pt-BR', async () => {
      updatePassword.mockRejectedValue(new AuthError('unknown'))

      const store = useAuthStore()
      await expect(store.updatePassword('novaSenha1')).rejects.toBeTruthy()

      expect(store.error).toBe('Erro ao redefinir a senha. Tente novamente.')
    })
  })

  it('logout encerra a sessão e limpa o merchant', async () => {
    signOut.mockResolvedValue(undefined)
    const store = useAuthStore()
    await store.logout()
    expect(signOut).toHaveBeenCalledOnce()
    expect(store.currentUser).toBeNull()
    expect(store.isAuthenticated).toBe(false)
  })
})
