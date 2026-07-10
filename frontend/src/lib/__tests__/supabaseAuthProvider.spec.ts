import { describe, it, expect, vi, beforeEach } from 'vitest'

const {
  getSession,
  onAuthStateChange,
  signInWithPassword,
  signUp,
  signOut,
  resetPasswordForEmail,
  updateUser,
} = vi.hoisted(() => ({
  getSession: vi.fn(),
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
      onAuthStateChange,
      signInWithPassword,
      signUp,
      signOut,
      resetPasswordForEmail,
      updateUser,
    },
  },
}))

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
})
