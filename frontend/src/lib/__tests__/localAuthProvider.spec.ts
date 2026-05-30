import { describe, it, expect, vi, beforeEach } from 'vitest'

const { post } = vi.hoisted(() => ({ post: vi.fn() }))

vi.mock('axios', () => ({
  default: { create: () => ({ post }) },
}))

import { localAuthProvider } from '@/lib/localAuthProvider'
import { AuthError } from '@/lib/authTypes'

// JWT with payload {"email":"dev@example.com"} (signature irrelevant for the test).
const TOKEN =
  'eyJhbGciOiJSUzI1NiJ9.eyJlbWFpbCI6ImRldkBleGFtcGxlLmNvbSJ9.sig'

// This env's global localStorage is an unusable stub; inject a clean in-memory one.
const memoryStore = new Map<string, string>()
vi.stubGlobal('localStorage', {
  getItem: (key: string) => memoryStore.get(key) ?? null,
  setItem: (key: string, value: string) => void memoryStore.set(key, value),
  removeItem: (key: string) => void memoryStore.delete(key),
  clear: () => memoryStore.clear(),
})

beforeEach(() => {
  vi.clearAllMocks()
  memoryStore.clear()
})

describe('localAuthProvider', () => {
  it('signUp posta em /auth/dev-register, persiste o token e expõe a sessão', async () => {
    post.mockResolvedValue({ data: { accessToken: TOKEN } })

    const result = await localAuthProvider.signUp({
      email: 'dev@example.com',
      password: 'senha123',
      merchantName: 'Loja',
      cnpj: '123',
      phone: '9',
    })

    expect(post).toHaveBeenCalledWith(
      '/auth/dev-register',
      expect.objectContaining({ email: 'dev@example.com', merchantName: 'Loja', cnpj: '123' }),
    )
    expect(result.session?.accessToken).toBe(TOKEN)
    expect(result.session?.user.email).toBe('dev@example.com')
    expect(await localAuthProvider.getAccessToken()).toBe(TOKEN)
  })

  it('signIn posta em /auth/dev-login e persiste o token', async () => {
    post.mockResolvedValue({ data: { accessToken: TOKEN } })

    const session = await localAuthProvider.signIn('dev@example.com', 'senha123')

    expect(post).toHaveBeenCalledWith('/auth/dev-login', {
      email: 'dev@example.com',
      password: 'senha123',
    })
    expect(session.accessToken).toBe(TOKEN)
    expect(await localAuthProvider.getAccessToken()).toBe(TOKEN)
  })

  it('signIn com falha lança AuthError invalid_credentials', async () => {
    post.mockRejectedValue({ response: { status: 401 } })

    await expect(localAuthProvider.signIn('a@b.com', 'x')).rejects.toMatchObject({
      code: 'invalid_credentials',
    })
    expect(await localAuthProvider.getAccessToken()).toBeNull()
  })

  it('signUp com 409 lança AuthError email_exists', async () => {
    post.mockRejectedValue({ response: { status: 409 } })

    await expect(
      localAuthProvider.signUp({
        email: 'a@b.com',
        password: 'x',
        merchantName: 'L',
        cnpj: '1',
      }),
    ).rejects.toBeInstanceOf(AuthError)
  })

  it('signOut limpa o token', async () => {
    post.mockResolvedValue({ data: { accessToken: TOKEN } })
    await localAuthProvider.signIn('dev@example.com', 'senha123')

    await localAuthProvider.signOut()

    expect(await localAuthProvider.getAccessToken()).toBeNull()
    expect(await localAuthProvider.init()).toBeNull()
  })
})
