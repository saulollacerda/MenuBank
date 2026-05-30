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

/** Production auth backend: delegates to Supabase. */
export const supabaseAuthProvider: AuthProvider = {
  async init() {
    const { data } = await supabase.auth.getSession()
    return toSession(data.session)
  },

  onAuthChange(callback) {
    supabase.auth.onAuthStateChange((_event, session) => callback(toSession(session)))
  },

  async signIn(email, password) {
    const { data, error } = await supabase.auth.signInWithPassword({ email, password })
    if (error) {
      const msg = error.message.toLowerCase()
      throw new AuthError(
        msg.includes('email not confirmed') ? 'email_not_confirmed' : 'invalid_credentials',
        error.message,
      )
    }
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
        emailRedirectTo: `${window.location.origin}/login`,
      },
    })
    if (error) {
      const msg = error.message.toLowerCase()
      throw new AuthError(msg.includes('already') ? 'email_exists' : 'unknown', error.message)
    }
    // Email confirmation ON → no session yet.
    return { session: toSession(data.session) }
  },

  async signOut() {
    await supabase.auth.signOut()
  },

  async getAccessToken() {
    const { data } = await supabase.auth.getSession()
    return data.session?.access_token ?? null
  },
}
