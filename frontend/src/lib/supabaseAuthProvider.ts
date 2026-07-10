import type { Session } from '@supabase/supabase-js'
import { supabase } from './supabase'
import { AuthError, type AuthProvider, type AuthSession, type SignUpForm } from './authTypes'

/** Maps a Supabase session to the backend-agnostic {@link AuthSession}. */
function toSession(session: Session | null): AuthSession | null {
  if (!session) return null
  return {
    accessToken: session.access_token,
    user: {
      email: session.user.email ?? '',
      user_metadata: session.user.user_metadata ?? {},
    },
  }
}

// Cached in-memory token updated by onAuthStateChange and init().
// Avoids calling supabase.auth.getSession() on every API request, which would
// trigger concurrent calls and hit Supabase rate limits (HTTP 429).
let cachedToken: string | null = null

/** Production auth backend: delegates to Supabase. */
export const supabaseAuthProvider: AuthProvider = {
  async init() {
    const { data } = await supabase.auth.getSession()
    cachedToken = data.session?.access_token ?? null
    return toSession(data.session)
  },

  onAuthChange(callback) {
    supabase.auth.onAuthStateChange((_event, session) => {
      cachedToken = session?.access_token ?? null
      callback(toSession(session))
    })
  },

  async signIn(email, password) {
    const { data, error } = await supabase.auth.signInWithPassword({ email, password })
    if (error) {
      const msg = error.message.toLowerCase()
      const code = msg.includes('email not confirmed')
        ? 'email_not_confirmed'
        : msg.includes('invalid login credentials')
          ? 'invalid_credentials'
          : 'unknown' // network failure, rate limit, misconfiguration — not the user's fault
      throw new AuthError(code, error.message)
    }
    cachedToken = data.session?.access_token ?? null
    return toSession(data.session)!
  },

  async signUp(form: SignUpForm) {
    const { data, error } = await supabase.auth.signUp({
      email: form.email,
      password: form.password,
      options: {
        data: {
          merchantName: form.merchantName,
          cnpj: form.cnpj,
          phone: form.phone ?? null,
        },
        emailRedirectTo: `${window.location.origin}/email-verificado`,
      },
    })
    if (error) {
      const msg = error.message.toLowerCase()
      throw new AuthError(msg.includes('already') ? 'email_exists' : 'unknown', error.message)
    }
    // Email confirmation ON + email already registered: Supabase obfuscates the
    // duplicate as a 200 with an empty identities array and sends no email.
    if (data.user && data.user.identities?.length === 0) {
      throw new AuthError('email_exists')
    }
    // Email confirmation ON → session is null; confirmation OFF → session is set.
    cachedToken = data.session?.access_token ?? null
    return { session: toSession(data.session) }
  },

  async signOut() {
    cachedToken = null
    await supabase.auth.signOut()
  },

  async getAccessToken() {
    return cachedToken
  },

  async requestPasswordReset(email) {
    const { error } = await supabase.auth.resetPasswordForEmail(email, {
      redirectTo: `${window.location.origin}/redefinir-senha`,
    })
    if (error) throw new AuthError('unknown', error.message)
  },

  async updatePassword(password) {
    const { error } = await supabase.auth.updateUser({ password })
    if (error) throw new AuthError('unknown', error.message)
  },
}
