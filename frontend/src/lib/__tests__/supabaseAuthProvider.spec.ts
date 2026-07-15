import { describe, it, expect, vi, beforeEach } from 'vitest'

const {
  getSession,
  refreshSession,
  onAuthStateChange,
  signInWithPassword,
  signUp,
  signOut,
  resetPasswordForEmail,
  updateUser,
} = vi.hoisted(() => ({
  getSession: vi.fn(),
  refreshSession: vi.fn(),
  onAuthStateChange: vi.fn(),
  signInWithPassword: vi.fn(),
  signUp: vi.fn(),
  signOut: vi.fn(),
  resetPasswordForEmail: vi.fn(),
  updateUser: vi.fn(),
}))

vi.mock('@/lib/supabase', () => ({
  supabase: {
    auth: {
      getSession,
      refreshSession,
      onAuthStateChange,
      signInWithPassword,
      signUp,
      signOut,
      resetPasswordForEmail,
      updateUser,
    },
  },
}))

// jsdom's global localStorage is unreliable across suites; inject a clean in-memory one.
const memoryStore = new Map<string, string>()
vi.stubGlobal('localStorage', {
  get length() {
    return memoryStore.size
  },
  key: (index: number) => Array.from(memoryStore.keys())[index] ?? null,
  getItem: (key: string) => memoryStore.get(key) ?? null,
  setItem: (key: string, value: string) => void memoryStore.set(key, value),
  removeItem: (key: string) => void memoryStore.delete(key),
  clear: () => memoryStore.clear(),
})

/** Epoch seconds `delta` seconds from now. */
function epoch(delta: number): number {
  return Math.floor(Date.now() / 1000) + delta
}

import { supabaseAuthProvider } from '@/lib/supabaseAuthProvider'
import { AuthError } from '@/lib/authTypes'

const supabaseUser = {
  email: 'a@b.com',
  user_metadata: { merchantName: 'Loja' },
  identities: [{ id: 'i1' }],
}

const supabaseSession = { access_token: 'tok', user: supabaseUser }

const signUpForm = {
  email: 'a@b.com',
  password: 'senha123',
  merchantName: 'Loja',
  cnpj: '123',
  phone: '9',
}

beforeEach(() => {
  vi.clearAllMocks()
  memoryStore.clear()
})

describe('supabaseAuthProvider', () => {
  describe('signUp', () => {
    it('retorna sessão nula quando a confirmação de email está pendente', async () => {
      signUp.mockResolvedValue({
        data: { user: supabaseUser, session: null },
        error: null,
      })

      const result = await supabaseAuthProvider.signUp(signUpForm)

      expect(result.session).toBeNull()
    })

    it('lança email_exists quando o Supabase retorna sucesso ofuscado (identities vazio)', async () => {
      // Email confirmation ON + email already registered: Supabase returns 200 with an
      // obfuscated user (empty identities) and no error — no email is actually sent.
      signUp.mockResolvedValue({
        data: { user: { ...supabaseUser, identities: [] }, session: null },
        error: null,
      })

      await expect(supabaseAuthProvider.signUp(signUpForm)).rejects.toMatchObject({
        code: 'email_exists',
      })
    })

    it('lança email_exists quando o Supabase retorna erro de email já registrado', async () => {
      signUp.mockResolvedValue({
        data: { user: null, session: null },
        error: { message: 'User already registered' },
      })

      await expect(supabaseAuthProvider.signUp(signUpForm)).rejects.toMatchObject({
        code: 'email_exists',
      })
    })
  })

  describe('signIn', () => {
    it('retorna a sessão em login com sucesso', async () => {
      signInWithPassword.mockResolvedValue({
        data: { session: supabaseSession, user: supabaseUser },
        error: null,
      })

      const session = await supabaseAuthProvider.signIn('a@b.com', 'senha123')

      expect(session.accessToken).toBe('tok')
      expect(await supabaseAuthProvider.getAccessToken()).toBe('tok')
    })

    it('lança invalid_credentials para credenciais inválidas', async () => {
      signInWithPassword.mockResolvedValue({
        data: { session: null, user: null },
        error: { message: 'Invalid login credentials' },
      })

      await expect(supabaseAuthProvider.signIn('a@b.com', 'x')).rejects.toMatchObject({
        code: 'invalid_credentials',
      })
    })

    it('lança email_not_confirmed para email não confirmado', async () => {
      signInWithPassword.mockResolvedValue({
        data: { session: null, user: null },
        error: { message: 'Email not confirmed' },
      })

      await expect(supabaseAuthProvider.signIn('a@b.com', 'x')).rejects.toMatchObject({
        code: 'email_not_confirmed',
      })
    })

    it('lança unknown (não invalid_credentials) para erro de rede/rate limit', async () => {
      signInWithPassword.mockResolvedValue({
        data: { session: null, user: null },
        error: { message: 'Request rate limit reached' },
      })

      const failure = supabaseAuthProvider.signIn('a@b.com', 'x')
      await expect(failure).rejects.toBeInstanceOf(AuthError)
      await expect(supabaseAuthProvider.signIn('a@b.com', 'x')).rejects.toMatchObject({
        code: 'unknown',
      })
    })
  })

  describe('requestPasswordReset', () => {
    it('chama resetPasswordForEmail com redirect para /redefinir-senha', async () => {
      resetPasswordForEmail.mockResolvedValue({ data: {}, error: null })

      await supabaseAuthProvider.requestPasswordReset('a@b.com')

      expect(resetPasswordForEmail).toHaveBeenCalledWith('a@b.com', {
        redirectTo: expect.stringContaining('/redefinir-senha'),
      })
    })

    it('lança AuthError unknown quando o Supabase falha', async () => {
      resetPasswordForEmail.mockResolvedValue({
        data: null,
        error: { message: 'Request rate limit reached' },
      })

      await expect(supabaseAuthProvider.requestPasswordReset('a@b.com')).rejects.toMatchObject({
        code: 'unknown',
      })
    })
  })

  describe('updatePassword', () => {
    it('chama updateUser com a nova senha', async () => {
      updateUser.mockResolvedValue({ data: { user: supabaseUser }, error: null })

      await supabaseAuthProvider.updatePassword('novaSenha1')

      expect(updateUser).toHaveBeenCalledWith({ password: 'novaSenha1' })
    })

    it('lança AuthError unknown quando falha', async () => {
      updateUser.mockResolvedValue({
        data: { user: null },
        error: { message: 'New password should be different from the old password.' },
      })

      await expect(supabaseAuthProvider.updatePassword('novaSenha1')).rejects.toMatchObject({
        code: 'unknown',
      })
    })
  })

  describe('getAccessToken', () => {
    it('retorna o token em cache sem chamar getSession quando ainda está válido', async () => {
      getSession.mockResolvedValue({
        data: { session: { access_token: 'fresh', expires_at: epoch(3600), user: supabaseUser } },
      })
      await supabaseAuthProvider.init()
      getSession.mockClear()

      const token = await supabaseAuthProvider.getAccessToken()

      expect(token).toBe('fresh')
      expect(getSession).not.toHaveBeenCalled()
    })

    it('chama getSession uma vez perto da expiração e atualiza o cache', async () => {
      getSession.mockResolvedValue({
        data: { session: { access_token: 'stale', expires_at: epoch(10), user: supabaseUser } },
      })
      await supabaseAuthProvider.init()
      getSession.mockClear()
      getSession.mockResolvedValue({
        data: { session: { access_token: 'renewed', expires_at: epoch(3600), user: supabaseUser } },
      })

      const token = await supabaseAuthProvider.getAccessToken()

      expect(token).toBe('renewed')
      expect(getSession).toHaveBeenCalledTimes(1)
    })

    it('chamadas concorrentes compartilham uma única chamada a getSession', async () => {
      getSession.mockResolvedValue({
        data: { session: { access_token: 'stale', expires_at: epoch(10), user: supabaseUser } },
      })
      await supabaseAuthProvider.init()
      getSession.mockClear()

      let resolveGetSession!: (value: unknown) => void
      getSession.mockReturnValue(
        new Promise((resolve) => {
          resolveGetSession = resolve
        }),
      )

      const p1 = supabaseAuthProvider.getAccessToken()
      const p2 = supabaseAuthProvider.getAccessToken()
      resolveGetSession({
        data: { session: { access_token: 'renewed', expires_at: epoch(3600), user: supabaseUser } },
      })
      const [t1, t2] = await Promise.all([p1, p2])

      expect(t1).toBe('renewed')
      expect(t2).toBe('renewed')
      expect(getSession).toHaveBeenCalledTimes(1)
    })
  })

  describe('refreshSession', () => {
    it('atualiza o cache e retorna o novo token em caso de sucesso', async () => {
      refreshSession.mockResolvedValue({
        data: { session: { access_token: 'refreshed', expires_at: epoch(3600), user: supabaseUser } },
        error: null,
      })

      const token = await supabaseAuthProvider.refreshSession()

      expect(token).toBe('refreshed')
      // Cache is fresh now: getAccessToken must not fall back to getSession.
      expect(await supabaseAuthProvider.getAccessToken()).toBe('refreshed')
      expect(getSession).not.toHaveBeenCalled()
    })

    it('retorna null quando o refresh falha, sem lançar', async () => {
      refreshSession.mockResolvedValue({ data: { session: null }, error: { message: 'invalid' } })

      await expect(supabaseAuthProvider.refreshSession()).resolves.toBeNull()
    })

    it('retorna null quando o refresh rejeita, sem lançar', async () => {
      refreshSession.mockRejectedValue(new Error('network'))

      await expect(supabaseAuthProvider.refreshSession()).resolves.toBeNull()
    })
  })

  describe('signOut', () => {
    it('usa scope local e limpa cache e chaves sb-*-auth-token mesmo com erro retornado', async () => {
      signInWithPassword.mockResolvedValue({
        data: { session: { access_token: 'tok', expires_at: epoch(3600), user: supabaseUser } },
        error: null,
      })
      await supabaseAuthProvider.signIn('a@b.com', 'senha123')
      getSession.mockResolvedValue({ data: { session: null } })
      localStorage.setItem('sb-abc-auth-token', 'x')
      localStorage.setItem('sb-abc-auth-token-user', 'y')
      localStorage.setItem('outra-chave', 'z')
      signOut.mockResolvedValue({ error: { message: 'network down' } })

      await supabaseAuthProvider.signOut()

      expect(signOut).toHaveBeenCalledWith({ scope: 'local' })
      expect(localStorage.getItem('sb-abc-auth-token')).toBeNull()
      expect(localStorage.getItem('sb-abc-auth-token-user')).toBeNull()
      expect(localStorage.getItem('outra-chave')).toBe('z')
      expect(await supabaseAuthProvider.getAccessToken()).toBeNull()
    })

    it('limpa cache e chaves mesmo quando o signOut do Supabase rejeita', async () => {
      signInWithPassword.mockResolvedValue({
        data: { session: { access_token: 'tok', expires_at: epoch(3600), user: supabaseUser } },
        error: null,
      })
      await supabaseAuthProvider.signIn('a@b.com', 'senha123')
      getSession.mockResolvedValue({ data: { session: null } })
      localStorage.setItem('sb-abc-auth-token', 'x')
      signOut.mockRejectedValue(new Error('network'))

      await expect(supabaseAuthProvider.signOut()).resolves.toBeUndefined()

      expect(localStorage.getItem('sb-abc-auth-token')).toBeNull()
      expect(await supabaseAuthProvider.getAccessToken()).toBeNull()
    })
  })
})
