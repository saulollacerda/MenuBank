import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

const { signIn, signUp, signOut, init, onAuthChange, getAccessToken } = vi.hoisted(() => ({
  signIn: vi.fn(),
  signUp: vi.fn(),
  signOut: vi.fn(),
  init: vi.fn(),
  onAuthChange: vi.fn(),
  getAccessToken: vi.fn(),
}))

vi.mock('@/lib/authProvider', async () => {
  const actual = await vi.importActual<typeof import('@/lib/authTypes')>('@/lib/authTypes')
  return {
    authProvider: { signIn, signUp, signOut, init, onAuthChange, getAccessToken },
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

  it('logout encerra a sessão e limpa o merchant', async () => {
    signOut.mockResolvedValue(undefined)
    const store = useAuthStore()
    await store.logout()
    expect(signOut).toHaveBeenCalledOnce()
    expect(store.currentUser).toBeNull()
    expect(store.isAuthenticated).toBe(false)
  })
})
